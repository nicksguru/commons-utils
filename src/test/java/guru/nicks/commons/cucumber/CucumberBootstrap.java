package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.CsvUtilsWorld;
import guru.nicks.commons.cucumber.world.NumberWorld;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.test.TestCustomEpochConfig;
import guru.nicks.commons.validation.AnnotationValidator;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Called by Cucumber for each scenario. Therefore, initializes beans shared by all scenarios. Mocking should be done
 * inside step definition classes  to let them program a different behavior. However, purely default mocks can be
 * declared here.
 */
@CucumberContextConfiguration
@ContextConfiguration(classes = {
        // scenario-scoped states
        TextWorld.class, NumberWorld.class, CsvUtilsWorld.class,
        // beans
        AnnotationValidator.class, LocalValidatorFactoryBean.class, TestCustomEpochConfig.class
})
public class CucumberBootstrap {
}
