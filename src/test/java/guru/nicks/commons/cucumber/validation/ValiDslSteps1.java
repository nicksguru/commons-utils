package guru.nicks.commons.cucumber.validation;

import guru.nicks.commons.cucumber.world.NumberWorld;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.validation.dsl.ValiDsl;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Step definitions for testing {@link ValiDsl} functionality.
 */
@RequiredArgsConstructor
@Slf4j
public class ValiDslSteps1 {

    // DI
    private final TextWorld textWorld;
    private final NumberWorld numberWorld;

    @When("validate: notNull")
    public void validateNotNull() {
        try {
            // treat blank string as null
            if (StringUtils.isBlank(textWorld.getInput())) {
                check((String) null, "null").notNull();
            }

            // parse int
            try {
                Integer.parseInt(textWorld.getInput());
            }
            // parse double
            catch (NumberFormatException e) {
                try {
                    Double.parseDouble(textWorld.getInput());
                }
                // treat as string
                catch (NumberFormatException e1) {
                    checkNotNull(textWorld.getInput(), "string");
                }
            }

            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: notBlank")
    public void validateNotBlank() {
        try {
            checkNotBlank(textWorld.getInput(), "input");
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: positive")
    public void validatePositive() {
        try {
            // treat blank string as null
            if (StringUtils.isBlank(textWorld.getInput())) {
                check((Integer) null, "null").positive();
            }

            // parse int
            try {
                int value = Integer.parseInt(textWorld.getInput());
                check(value, "integer").positive();
            }
            // parse double
            catch (NumberFormatException e) {
                double value = Double.parseDouble(textWorld.getInput());
                check(value, "double").positive();
            }

            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: positiveOrZero")
    public void validatePositiveOrZero() {
        try {
            // treat blank string as null
            if (StringUtils.isBlank(textWorld.getInput())) {
                check((Integer) null, "null").positiveOrZero();
            }

            // parse int
            try {
                int value = Integer.parseInt(textWorld.getInput());
                check(value, "integer").positiveOrZero();
            }
            // parse double
            catch (NumberFormatException e) {
                double value = Double.parseDouble(textWorld.getInput());
                check(value, "double").positiveOrZero();
            }

            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: lessThan")
    public void validateLessThan() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .lessThan(numberWorld.getIntValue2());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: lessThanOrEqual")
    public void validateLessThanOrEqual() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .lessThanOrEqual(numberWorld.getIntValue2());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: greaterThan")
    public void validateGreaterThan() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .greaterThan(numberWorld.getIntValue2());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: greaterThanOrEqual")
    public void validateGreaterThanOrEqual() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .greaterThanOrEqual(numberWorld.getIntValue2());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: betweenInclusive")
    public void validateBetweenInclusive() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .betweenInclusive(numberWorld.getIntValue2(), numberWorld.getIntValue3());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }

    @When("validate: eq")
    public void validateEq() {
        try {
            check(numberWorld
                    .getIntValue1(), "integer(" + numberWorld.getIntValue1() + ")")
                    .eq(numberWorld.getIntValue2());
            textWorld.setSuccess(true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            textWorld.setSuccess(false);
        }
    }
}
