package guru.nicks;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Optional;

/**
 * Helper class which stores {@link ApplicationContext} in a static manner, for access from non-wired classes. It's
 * advisable to use beans instead whenever possible.
 *
 * @see guru.nicks.listener.ApplicationContextHolderListener
 */
@UtilityClass
@Slf4j
public class ApplicationContextHolder {

    /**
     * Property name for the application name in the {@link Environment Spring environment}.
     */
    public static final String SPRING_APPLICATION_NAME_PROPERTY = "spring.application.name";

    private static ApplicationContext applicationContext;

    /**
     * Finds application context.
     *
     * @return optional application context
     */
    public static Optional<ApplicationContext> findApplicationContext() {
        return Optional.ofNullable(applicationContext);
    }

    /**
     * Does the same as {@link #findApplicationContext()}, but throws an exception if the application context is
     * missing.
     *
     * @return application context
     * @throws IllegalStateException missing application context
     */
    public static ApplicationContext getApplicationContext() {
        return findApplicationContext()
                .orElseThrow(() -> new IllegalStateException("Missing application context in static holder"));
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextHolder.applicationContext = applicationContext;
        log.info("Stored ApplicationContext in global holder: {}", applicationContext.getDisplayName());
    }

    /**
     * Retrieves the application name from the {@link Environment Spring environment} using
     * {@link #findApplicationContext()}.
     *
     * @return optional non-blank application name from the {@value #SPRING_APPLICATION_NAME_PROPERTY} property
     */
    public static Optional<String> findApplicationName() {
        return findApplicationContext()
                .map(ApplicationContext::getEnvironment)
                .map(env -> env.getProperty(SPRING_APPLICATION_NAME_PROPERTY))
                .filter(StringUtils::isNotBlank);
    }

}
