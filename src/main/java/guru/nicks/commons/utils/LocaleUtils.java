package guru.nicks.commons.utils;

import guru.nicks.commons.auth.domain.OpenIdConnectData;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Locale-related utility methods.
 */
@UtilityClass
public class LocaleUtils {

    /**
     * Resolves locale priority based on authentication and HTTP request data. Takes into account, in this order:
     * <ol>
     *   <li>{@link OpenIdConnectData#getLanguageCode()} from {@code authentication}</li>
     *   <li>{@code Accept-Language} header from {@code httpServletRequest}</li>
     * </ol>
     * <p>
     * Only locales present in {@code supportedLocales} are returned. The returned list maintains the priority order
     * and contains no duplicates.
     *
     * @param authentication     current authentication, may be {@code null}
     * @param httpServletRequest current HTTP request, may be {@code null}
     * @param supportedLocales   supported locales, must not be {@code null}
     * @return locales contained in {@code supportedLocales}, never {@code null} (but possibly empty)
     * @throws IllegalArgumentException if {@code supportedLocales} is {@code null}
     */
    public static List<Locale> resolveLocalePriority(
            @Nullable Authentication authentication,
            @Nullable HttpServletRequest httpServletRequest,
            Collection<Locale> supportedLocales) {
        // speedup
        if (CollectionUtils.isEmpty(supportedLocales)) {
            return Collections.emptyList();
        }

        // current user locale, if present
        Optional<Locale> userLocale = Optional.ofNullable(authentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                //
                // for Open ID profile, get the preferred language code
                .filter(OpenIdConnectData.class::isInstance)
                .map(OpenIdConnectData.class::cast)
                .map(OpenIdConnectData::getLanguageCode)
                .filter(StringUtils::isNotBlank)
                //
                // is the language tag is unsupported, this method creates a Locale with an empty language name
                // (this is a documented behavior), which is skipped here
                .map(Locale::forLanguageTag)
                .filter(locale -> StringUtils.isNotBlank(locale.getLanguage()));

        // HTTP header locales, if present
        List<Locale> httpHeaderLocales = Optional
                .ofNullable(httpServletRequest)
                .map(request -> HttpRequestUtils.parseAcceptLanguageHttpHeader(request, supportedLocales))
                .orElseGet(Collections::emptyList);

        return Stream.concat(userLocale.stream(), httpHeaderLocales.stream())
                .filter(supportedLocales::contains)
                .distinct()
                .toList();
    }

}
