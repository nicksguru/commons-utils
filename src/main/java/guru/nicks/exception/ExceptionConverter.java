package guru.nicks.exception;

import guru.nicks.utils.ReflectionUtils;

import org.springframework.core.convert.converter.Converter;

import java.lang.invoke.MethodHandles;

/**
 * Each implementing class should be a Spring bean. All such beans are picked by the converter registry. The goal is to
 * map 3rd party exceptions to {@link BusinessException} and then to custom DTO.
 * <p>
 * {@link Exception#getMessage()} is never revealed to caller, for security reasons. Instead, each error code is
 * supposed to have a translation (the translation dictionary can be downloaded by client apps) and an English fallback
 * message.
 *
 * @param <S> source exception type (root for its subclasses)
 * @param <T> target exception type
 */
public interface ExceptionConverter<S extends Throwable, T extends BusinessException> extends Converter<S, T> {

    /**
     * @see #getTargetClass()
     */
    Class<?> STATIC_THIS = MethodHandles.lookup().lookupClass();

    /**
     * Creates an exception object of class {@link #getTargetClass()} passing the cause to
     * {@link Exception#Exception(Throwable)}. This default logic is sufficient for most cases.
     *
     * @param cause original exception, becomes {@link Exception#getCause()}
     * @return converted exception
     */
    @Override
    default T convert(S cause) {
        return ReflectionUtils.instantiateWithConstructor(getTargetClass(), cause);
    }

    /**
     * Extracts {@code S} class out of generic class parameter.
     * <p>
     * Both {@code S} and {@code T} are {@link Throwable}, so this method depends on their order: {@code S} goes first
     * and therefore is found first. This order is not supposed to ever change because {@link Converter} is part of
     * Spring.
     *
     * @return exception class to be mapped
     * @throws IllegalStateException if {@code S} is not found
     */
    @SuppressWarnings("unchecked")
    default Class<S> getSourceClass() {
        return (Class<S>) ReflectionUtils
                .findMaterializedGenericType(getClass(), STATIC_THIS, Throwable.class)
                .filter(sourceClass -> !sourceClass.isInstance(BusinessException.class))
                .orElseThrow(() -> new IllegalStateException("Missing generic source class parameter in "
                        + getClass().getName()));
    }

    /**
     * Extracts {@code T} class out of generic parameter.
     *
     * @return exception class {@code S} is mapped to
     * @throws IllegalStateException if {@code T} is not found
     */
    @SuppressWarnings("unchecked")
    default Class<T> getTargetClass() {
        return (Class<T>) ReflectionUtils
                .findMaterializedGenericType(getClass(), STATIC_THIS, BusinessException.class)
                .orElseThrow(() -> new IllegalStateException("Missing generic target class parameter in "
                        + getClass().getName()));
    }

}
