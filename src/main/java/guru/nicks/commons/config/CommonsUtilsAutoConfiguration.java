package guru.nicks.commons.config;

import guru.nicks.commons.cache.ChecksumCacheKeyGenerator;
import guru.nicks.commons.cache.ToStringJoiningCacheKeyGenerator;
import guru.nicks.commons.listener.ApplicationContextHolderListener;
import guru.nicks.commons.validation.AnnotationValidator;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Miscellaneous configuration:
 * <ul>
 *  <li>creates {@link MethodValidationPostProcessor} bean</li>
 *  <li>creates {@link AnnotationValidator} bean</li>
 * </ul>
 */
@AutoConfiguration
@Slf4j
public class CommonsUtilsAutoConfiguration {

    /**
     * Creates bean which allows to validate any bean's methods with {@link Validated} on the class level and things
     * like {@link NotBlank} on method arguments.
     *
     * @return bean
     */
    @ConditionalOnMissingBean
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        log.debug("Building {} bean", MethodValidationPostProcessor.class.getSimpleName());
        return new MethodValidationPostProcessor();
    }

    @Bean
    public AnnotationValidator annotationValidator(LocalValidatorFactoryBean localValidatorFactoryBean) {
        log.debug("Building {} bean", AnnotationValidator.class.getSimpleName());
        return new AnnotationValidator(localValidatorFactoryBean);
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationContextHolderListener applicationContextHolderListener() {
        log.debug("Building {} bean", ApplicationContextHolderListener.class.getSimpleName());
        return new ApplicationContextHolderListener();
    }

    @Bean
    public ToStringJoiningCacheKeyGenerator toStringJoiningCacheKeyGenerator() {
        log.debug("Building {} bean", ToStringJoiningCacheKeyGenerator.class.getSimpleName());
        return new ToStringJoiningCacheKeyGenerator();
    }

    @Bean
    public ChecksumCacheKeyGenerator checksumCacheKeyGenerator() {
        log.debug("Building {} bean", ChecksumCacheKeyGenerator.class.getSimpleName());
        return new ChecksumCacheKeyGenerator();
    }

}
