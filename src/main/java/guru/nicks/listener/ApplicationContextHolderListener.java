package guru.nicks.listener;

import guru.nicks.ApplicationContextHolder;
import guru.nicks.log.domain.LogContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class ApplicationContextHolderListener {

    private final List<Consumer<ApplicationContextEvent>> steps = List.of(
            this::storeApplicationNameInLogContext,
            this::storeApplicationContextInGlobalHolder);

    /**
     * See class comment for details.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {
        steps.forEach(step ->
                step.accept(event));
    }

    private void storeApplicationNameInLogContext(ApplicationContextEvent event) {
        Optional.of(event)
                .map(ApplicationContextEvent::getApplicationContext)
                .map(ApplicationContext::getEnvironment)
                .map(env -> env.getProperty(ApplicationContextHolder.SPRING_APPLICATION_NAME_PROPERTY))
                .filter(StringUtils::isNotBlank)
                .ifPresent(LogContext.APP_NAME::put);
    }

    private void storeApplicationContextInGlobalHolder(ApplicationContextEvent event) {
        ApplicationContextHolder.setApplicationContext(event.getApplicationContext());
    }

}
