package guru.nicks.commons.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * This configuration is to be used in all {@link Mapper @Mapper}-annotated classes as {@link Mapper#config()}. It:
 * <ul>
 *     <li>declares mappers as Spring beans</li>
 *     <li>sets up constructor-based injection for injecting other mappers</li>
 *     <li>forbids narrowing type conversion, such as {@code long} to {@code int}</li>
 *     <li>forbids assigning nulls from source objects (think of a JSON DTO with some fields omitted) - this approach
 *         retains constructor-assigned default values during mapping involving object creation (however, it's not
 *         applicable to object updates: methods using {@link MappingTarget @MappingTarget} should leverage
 *         {@link BeanMapping#nullValuePropertyMappingStrategy()} for the same effect)
 *     </li>
 *     <li>disables warnings on unmapped target fields - because most entities have such internal fields as date of
 *         creation which can't be passed from a DTO </li>
 * </ul>
 */
@MapperConfig(
        // mappers are Spring beans
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,

        // never assign nulls from source objects
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,

        // should not convert long to int
        typeConversionPolicy = ReportingPolicy.ERROR,

        // applicable during object creation only: objects being created most often have internal audit-related fields
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public class DefaultMapStructConfig {
}
