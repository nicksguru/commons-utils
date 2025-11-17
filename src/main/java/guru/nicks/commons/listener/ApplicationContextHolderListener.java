package guru.nicks.commons.listener;

import guru.nicks.commons.ApplicationContextHolder;
import guru.nicks.commons.log.domain.LogContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reacts to {@link ContextRefreshedEvent}:
 * <ul>
 *  <li>stores {@code spring.application.name} in {@link LogContext#APP_NAME} to enrich log messages</li>
 *  <li>stores {@link ApplicationContext} in {@link ApplicationContextHolder} for such use cases when it's impossible to
 *      autowire it</li>
 * </ul>
 */
@Component
public class ApplicationContextHolderListener {

    private final List<Consumer<ApplicationContextEvent>> steps = List.of(
            this::storeApplicationNameInLogContext,
            this::storeApplicationContextInGlobalHolder);

    /**
     * See class comment for details.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {
        // this should never be null, but just in case
        if (event == null) {
            return;
        }

        steps.forEach(step ->
                step.accept(event));
    }

    private void storeApplicationNameInLogContext(ApplicationContextEvent event) {
        Optional.ofNullable(event.getApplicationContext())
                .map(ApplicationContext::getEnvironment)
                .map(env -> env.getProperty(ApplicationContextHolder.SPRING_APPLICATION_NAME_PROPERTY))
                .filter(StringUtils::isNotBlank)
                .ifPresent(LogContext.APP_NAME::put);
    }

    private void storeApplicationContextInGlobalHolder(ApplicationContextEvent event) {
        Optional.ofNullable(event.getApplicationContext())
                .ifPresent(ApplicationContextHolder::setApplicationContext);
    }

}
