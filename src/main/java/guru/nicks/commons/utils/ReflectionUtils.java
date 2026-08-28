package guru.nicks.commons.utils;

import guru.nicks.commons.cache.domain.CacheConstants;

import am.ik.yavi.meta.ConstraintArguments;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;
import static java.util.function.Predicate.not;

/**
 * Reflection-related utility methods.
 */
@UtilityClass
@Slf4j
public class ReflectionUtils {

    /**
     * Needed mostly during application startup, therefore expires after 24 hours.
     *
     * @see #getClassHierarchy(Class)
     */
    private static final Cache<Class<?>, Set<Class<?>>> CLASS_HIERARCHY_CACHE = Caffeine.newBuilder()
            .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    /**
     * Needed mostly during application startup, therefore expires after 24 hours.
     *
     * @see #getClassHierarchyMethods(Class)
     */
    private static final Cache<Class<?>, Set<Method>> CLASS_HIERARCHY_METHODS_CACHE = Caffeine.newBuilder()
            .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    /**
     * Needed mostly during application startup, therefore expires after 24 hours. Values are {@link Optional} so that
     * misses (the immutable, shareable {@link Optional#empty()} sentinel) are cached too.
     *
     * @see #findMaterializedGenericType(Class, Class, Class)
     */
    private static final Cache<MaterializedGenericTypeKey, Optional<Class<?>>> MATERIALIZED_GENERIC_TYPE_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
                    .expireAfterAccess(Duration.ofHours(24))
                    .build();

    /**
     * Needed mostly during application startup, therefore expires after 24 hours.
     *
     * @see #findAnnotatedMethods(Class, Class)
     */
    private static final Cache<AnnotatedMethodsKey, List<Method>> ANNOTATED_METHODS_CACHE = Caffeine.newBuilder()
            .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    /**
     * {@link ObjenesisStd} is thread-safe and caches instantiators internally, so a single shared instance suffices.
     */
    private static final Objenesis OBJENESIS = new ObjenesisStd(true);

    /**
     * Traverses class hierarchy graph (once; leverages caching) - collects all superclasses and superinterfaces.
     *
     * @param clazz class to start with
     * @return set, always starts with {@code clazz}, the order is breadth-first with respect to each node's ancestors
     */
    public static Set<Class<?>> getClassHierarchy(Class<?> clazz) {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        return CLASS_HIERARCHY_CACHE.get(clazz, WithoutCache::getClassHierarchyWithoutCache);
    }

    /**
     * Finds all explicitly declared methods (once; leverages caching) for the given class and its
     * superclass/superinterface graph, as per {@link #getClassHierarchy(Class)}. Synthetic and bridge methods are
     * skipped because they are confusing: for example, for bridge methods, same-named methods always exist in
     * subclasses with narrower value types.
     * <p>
     * NOTE: because of type erasure, all generic argument and return value types are seen as {@link Object} in method
     * signatures.
     *
     * @param clazz class to start discovery at
     * @return methods found
     */
    public static Set<Method> getClassHierarchyMethods(Class<?> clazz) {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        return CLASS_HIERARCHY_METHODS_CACHE.get(clazz, WithoutCache::getClassHierarchyMethodsWithoutCache);
    }

    /**
     * Calls {@link #getClassHierarchyMethods(Class)}, filtering the results using the given predicate.
     *
     * @param clazz           class to start discovery at
     * @param searchCondition predicate
     * @return methods found
     */
    @ConstraintArguments
    public static List<Method> findHierarchyMethods(Class<?> clazz, Predicate<? super Method> searchCondition) {
        checkNotNull(searchCondition, _ReflectionUtilsFindHierarchyMethodsArgumentsMeta.SEARCHCONDITION.name());

        return getClassHierarchyMethods(clazz)
                .stream()
                .filter(searchCondition)
                .toList();
    }

    /**
     * Among {@link #getClassHierarchyMethods(Class)}, finds those annotated (directly or via subclasses / merged
     * annotations) with the given annotation (once; leverages caching).
     *
     * @param clazz           class to start discovery at
     * @param annotationClass annotation class
     * @return annotated methods found in the class hierarchy
     */
    @ConstraintArguments
    public static List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        checkNotNull(annotationClass, _ReflectionUtilsFindAnnotatedMethodsArgumentsMeta.ANNOTATIONCLASS.name());

        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        return ANNOTATED_METHODS_CACHE.get(
                new AnnotatedMethodsKey(clazz, annotationClass),
                WithoutCache::findAnnotatedMethodsWithoutCache);
    }

    /**
     * Given:
     * <ul>
     *  <li>{@code genericParent = interface/class Generic<T, U>}</li>
     *  <li>{@code where = class Concrete implements/extends Generic<ConcreteT, ConcreteU>}
     *      (directly or somewhere in the hierarchy)</li>
     *  <li>{@code genericType = T} (or any of its parent types)</li>
     * </ul> - returns {@code ConcreteT}. Searches in {@code where}'s full hierarchy graph.
     * <p>
     * If {@code where} has multiple generic types inherited from {@code genericType}, the first one is found. This
     * logic is part of the method contract and must never change.
     *
     * @param where         Class implementing/extending generic interface/subclass (in the above example, it's
     *                      {@code Concrete}).
     * @param genericParent Parent interface/subclass (in the above example, it's {@code Generic}). Its subclasses will
     *                      match too.
     * @param genericType   Generic type's class (in the above example, it's {@code T}). Its subclasses will match too,
     *                      for example passing {@link Object} simply finds the first generic type - because any class
     *                      inherits from it
     * @return optional {@code what} or its subclass
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<Class<? extends T>> findMaterializedGenericType(Class<?> where,
            Class<?> genericParent, Class<T> genericType) {
        checkNotNull(where, "where");
        checkNotNull(genericParent, "genericParent");
        checkNotNull(genericType, "genericType");

        // Optional.empty() is an immutable sentinel, so misses are cached too and the expensive hierarchy rescan
        // runs only once per key
        Optional<Class<?>> resolvedClass = MATERIALIZED_GENERIC_TYPE_CACHE.get(
                new MaterializedGenericTypeKey(where, genericParent, genericType),
                WithoutCache::getMaterializedGenericTypeWithoutCache);

        // cache erases the exact value type, but it's T for sure - see cache population logic
        return resolvedClass.map(cls -> (Class<? extends T>) cls);
    }

    /**
     * Convenient shortcut - calls {@link #findMaterializedGenericType(Class, Class, Class)} passing {@link Object}
     * class in the third argument, thus finding simply the first - no matter what it is - materialized generic type.
     */
    public static Optional<Class<?>> findFirstMaterializedGenericType(Class<?> where, Class<?> genericParent) {
        return findMaterializedGenericType(where, genericParent, Object.class);
    }

    /**
     * Creates an object of the given class even if it has no default constructor. First, the default constructor is
     * tried. If it fails, {@link ObjenesisStd} is used (WARNING: it <b>bypasses field initializers</b>!).
     * <p>
     * Normally, this method should not be used because there's a risk of creating objects with uninitialized fields
     * (see warning above) - {@link BeanUtils#instantiateClass(Class)} suffices in most cases.
     *
     * @param clazz object class
     * @param <T>   object class type
     * @return object
     * @throws BeanInstantiationException object creation failed
     * @see #instantiateWithConstructor(Class, Object)
     */
    public static <T> T instantiateEvenWithoutDefaultConstructor(Class<T> clazz) {
        checkNotNull(clazz, "clazz");

        // try default constructor first
        try {
            return BeanUtils.instantiateClass(clazz);
        }
        // Instantiate class having no default constructor. Supposing it's a rare case.
        // WARNING: this approach bypasses field initializers!
        catch (BeanInstantiationException e1) {
            try {
                return OBJENESIS.newInstance(clazz);
            } catch (Exception e2) {
                throw new BeanInstantiationException(clazz, "Failed to instantiate [" + clazz.getName() + "]: "
                        + e2.getMessage(), e2);
            }
        }
    }

    /**
     * Creates an object by calling the matching single-argument constructor.
     *
     * @param clazz          object class
     * @param constructorArg constructor argument
     * @param <T>            object class type
     * @return object
     * @throws BeanInstantiationException object creation failed: no constructor found / error in constructor / etc.
     * @see #instantiateEvenWithoutDefaultConstructor(Class)
     */
    public static <T> T instantiateWithConstructor(Class<T> clazz, @Nullable Object constructorArg) {
        checkNotNull(clazz, "clazz");

        try {
            return ConstructorUtils.invokeConstructor(clazz, constructorArg);
        } catch (NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | InstantiationException e) {
            throw new BeanInstantiationException(clazz,
                    "Failed to instantiate [" + clazz.getName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if argument is scalar i.e. one of:
     * <ul>
     *  <li>{@link String}</li>
     *  <li>{@link Class#isPrimitive() primitive}</li>
     *  <li>{@link Number}</li>
     *  <li>{@link Boolean}</li>
     *  <li>{@link Character}</li>
     *  <li>{@code null}</li>
     * </ul>
     * As to {@link Enum} members, their serialization is governed by the optional {@link JsonValue @JsonValue}
     * annotation, therefore they're not treated as scalars.
     *
     * @param obj object to check (not an object class, but the object itself)
     * @return check result
     */
    public static boolean isScalar(@Nullable Object obj) {
        return (obj == null)
                || obj instanceof String
                || obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character
                || obj.getClass().isPrimitive();
    }

    /**
     * Cache key for {@link #ANNOTATED_METHODS_CACHE}. Strong keys are intentional: Caffeine weak keys switch to
     * identity comparison, which would break record value equality.
     */
    private record AnnotatedMethodsKey(

            Class<?> clazz,
            Class<? extends Annotation> annotation) {
    }

    /**
     * Cache key for {@link #MATERIALIZED_GENERIC_TYPE_CACHE}. Strong keys are intentional: Caffeine weak keys switch to
     * identity comparison, which would break record value equality.
     */
    private record MaterializedGenericTypeKey(

            Class<?> where,
            Class<?> genericParent,
            Class<?> genericType) {
    }

    private static class WithoutCache {

        /**
         * Called on cache miss from {@link #CLASS_HIERARCHY_CACHE}.
         */
        private static Set<Class<?>> getClassHierarchyWithoutCache(Class<?> clazz) {
            checkNotNull(clazz, "clazz");

            var ancestors = new LinkedHashSet<Class<?>>();
            ancestors.add(clazz);

            // Add all direct ancestors (depth 1), then first ancestor's direct ones (depth 2), then depth 3.
            // Since no indexed access is needed, LinkedList is not used (it's slower).
            Queue<Class<?>> queue = new ArrayDeque<>();
            queue.add(clazz);

            while (!queue.isEmpty()) {
                Class<?> currentNode = queue.poll();

                Optional.ofNullable(currentNode.getSuperclass())
                        // prevent cycles (false means 'already present')
                        .filter(ancestors::add)
                        .ifPresent(queue::add);

                // sort interfaces by name to make sure their order is consistent and verifiable - otherwise the order
                // will be the same as in the 'implements' clause which may change
                Arrays.stream(currentNode.getInterfaces())
                        .sorted(Comparator.comparing(Class::getName))
                        // prevent cycles (false means 'already present')
                        .filter(ancestors::add)
                        .forEach(queue::add);
            }

            // make sure Object class goes last
            if (ancestors.remove(Object.class)) {
                ancestors.add(Object.class);
            }

            log.debug("Discovered class hierarchy for [{}]: {}", clazz.getName(), ancestors);
            return ancestors;
        }

        /**
         * Called on cache miss from {@link #CLASS_HIERARCHY_METHODS_CACHE}.
         */
        private static Set<Method> getClassHierarchyMethodsWithoutCache(Class<?> clazz) {
            var methods = getClassHierarchy(clazz)
                    .stream()
                    .map(Class::getDeclaredMethods)
                    .flatMap(Arrays::stream)
                    .filter(not(Method::isBridge).and(not(Method::isSynthetic)))
                    // store methods in the same order as the class hierarchy
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (log.isTraceEnabled()) {
                log.trace("Discovered declared methods in class hierarchy for [{}]: {}", clazz, methods);
            }

            return methods;
        }

        /**
         * Called on cache miss from {@link #ANNOTATED_METHODS_CACHE}.
         */
        private static List<Method> findAnnotatedMethodsWithoutCache(AnnotatedMethodsKey key) {
            return findHierarchyMethods(key.clazz(), method ->
                    AnnotatedElementUtils.hasAnnotation(method, key.annotation()));
        }

        /**
         * Called on cache miss from {@link #findMaterializedGenericType(Class, Class, Class)}. The exact
         * {@code ? extends T} value type is restored by the caller because the cache stores the raw type only.
         */
        private static Optional<Class<?>> getMaterializedGenericTypeWithoutCache(MaterializedGenericTypeKey key) {
            Set<Class<?>> allClasses = getClassHierarchy(key.where());

            // for 'where' and each parent class/interface, collect their generic parents: 'A implements B<C>' -> B
            Set<ParameterizedType> allGenericParents = allClasses.stream()
                    .map(cls -> ArrayUtils.addAll(cls.getGenericInterfaces(), cls.getGenericSuperclass()))
                    .flatMap(Arrays::stream)
                    .filter(ParameterizedType.class::isInstance)
                    .map(ParameterizedType.class::cast)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Optional<ParameterizedType> match = allGenericParents.stream()
                    //.peek(type -> log.debug("Checking generic interface/superclass of [{}]: {}", key.where(), type))
                    // in SomeInterface<X, Y>, getRawType() returns SomeInterface
                    .filter(type -> type.getRawType() instanceof Class)
                    .filter(type -> key.genericParent().isAssignableFrom((Class<?>) type.getRawType()))
                    //.peek(type -> log.debug("{} is a subclass/subinterface of {}", key.where(), key.genericParent()))
                    .findFirst();

            // grab [X, Y] from SomeInterface<X, Y> and convert this array to a stream of (X, Y)
            Class<?> clazz = match.map(ParameterizedType::getActualTypeArguments)
                    .stream()
                    .flatMap(Arrays::stream)
                    //.peek(type -> log.debug("Checking generic type argument [{}]", type))
                    // if generic parameter B has generic inside, extract it: 'A implements B<Map<C, D>>' -> Map,
                    // otherwise use parameter B's type as is: 'A implements B<C>' -> C
                    .map(type -> type instanceof ParameterizedType parameterizedType
                            ? parameterizedType.getRawType()
                            : type)
                    // convert Type to its subclass (Class)
                    .filter(Class.class::isInstance)
                    .map(Class.class::cast)
                    .filter(key.genericType()::isAssignableFrom)
                    // as per the method contract, do NOT sort classes, so passing genericParent=Object always returns
                    // the FIRST generic parameter
                    .findFirst()
                    .orElse(null);

            return Optional.ofNullable(clazz);
        }

    }

}
