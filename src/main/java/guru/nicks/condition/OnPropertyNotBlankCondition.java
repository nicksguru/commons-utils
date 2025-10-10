package guru.nicks.condition;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;

import static guru.nicks.validation.dsl.ValiDsl.checkNotBlank;

/**
 * Processes {@link ConditionalOnPropertyNotBlank @ConditionalOnPropertyNotBlank}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40) // borrowed from OnPropertyCondition
public class OnPropertyNotBlankCondition extends SpringBootCondition {

    private static final Class<? extends Annotation> ANNOTATION_CLASS = ConditionalOnPropertyNotBlank.class;

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var annotation = metadata.getAnnotations().get(ANNOTATION_CLASS);

        String propertyName = annotation.getString("value");
        checkNotBlank(propertyName, "property name");

        // missing property yields null value
        String propertyValue = context.getEnvironment().getProperty(propertyName);

        var conditionMessage = ConditionMessage
                .forCondition(ANNOTATION_CLASS, "(" + propertyName + ")")
                .found("value")
                .items(propertyValue);

        return StringUtils.isNotBlank(propertyValue)
                ? ConditionOutcome.match(conditionMessage)
                : ConditionOutcome.noMatch(conditionMessage);
    }

}
