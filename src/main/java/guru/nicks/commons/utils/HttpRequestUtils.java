package guru.nicks.commons.utils;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.net.InetAddresses;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;
import static java.util.function.Predicate.not;

@UtilityClass
@Slf4j
public class HttpRequestUtils {

    /**
     * WARNING: header order matters in this list!
     */
    public static final List<String> POSSIBLE_PROXIED_CLIENT_IP_HEADERS = List.of(
            // CloudFlare
            "CF-Connecting-IP",
            // k8s, Nginx
            "X-Real-IP"
            // commented out because can be forged by hackers in the absence of legitimate headers listed above
            // "X-Forwarded-For"
    );

    /**
     * Last access (i.e. write or read) extends the cache TTL, so frequently needed status codes never expire.
     *
     * @see #resolveHttpStatus(int)
     */
    private static final Cache<Integer, Optional<HttpStatus>> HTTP_STATUS_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(14, TimeUnit.DAYS)
            .build();

    /**
     * Sets response header if its value composed of (comma-separated) parts is not blank.
     *
     * @param response     HTTP response
     * @param headerName   header name
     * @param headerValues header values (null/blank items are skipped)
     * @return {@code true} if header was set
     */
    public static boolean setNonBlankHeader(HttpServletResponse response,
            String headerName, @Nullable Collection<String> headerValues) {
        String headerValue = Optional.ofNullable(headerValues)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));

        if (StringUtils.isNotBlank(headerValue)) {
            response.setHeader(headerName, headerValue);
            return true;
        }

        return false;
    }

    /**
     * Tries each of {@link #POSSIBLE_PROXIED_CLIENT_IP_HEADERS} request headers until a non-empty IP-looking one is
     * found. If none is found, falls back to {@link HttpServletRequest#getRemoteAddr()} which may point to a proxy or
     * NAT server. Also, the Undertow implementation of the above method returns the hostname if the IP address is
     * unknown.
     *
     * @param request HTTP request
     * @return client IP address (or hostname in such cases when IP failed to parse)
     */
    public static String getRemoteIpBehindProxy(HttpServletRequest request) {
        Optional<String> proxiedIp = POSSIBLE_PROXIED_CLIENT_IP_HEADERS.stream()
                .map(request::getHeader)
                .filter(StringUtils::isNotBlank)
                .filter(not("unknown"::equalsIgnoreCase))
                .filter(InetAddresses::isInetAddress)
                .findFirst();

        // room for breakpoint
        return proxiedIp.orElseGet(request::getRemoteAddr);
    }

    /**
     * Maps {@code Accept-Language} header value (its format is something like {@code ja,en;q=0.4}) to the members of
     * this enum. Locales not existing in the enum are skipped.
     *
     * @param request        HTTP request
     * @param allowedLocales allowed locales
     * @return allowed located contained in the request header
     */
    public static List<Locale> parseAcceptLanguageHttpHeader(HttpServletRequest request,
            Collection<Locale> allowedLocales) {
        String headerValue = null;

        try {
            // don't call request.getLocales() - it returns server locale if there's no explicitly set header
            headerValue = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        } catch (RuntimeException e) {
            // do nothing - this is not an HTTP request (e.g. a Kafka message), InvocationTargetException is thrown
            // ('request' is actually a proxy, never null) with the following message:
            //
            // 'No thread-bound request found: Are you referring to request attributes outside an actual web request,
            // or processing a request outside the originally receiving thread? If you are actually operating within
            // a web request and still receive this message, your code is probably running outside DispatcherServlet:
            // In this case, use RequestContextListener or RequestContextFilter to expose the current request.'
        }

        if (StringUtils.isBlank(headerValue)) {
            return Collections.emptyList();
        }

        List<Locale.LanguageRange> languagesByPriority = Locale.LanguageRange.parse(headerValue);
        @SuppressWarnings("java:S1488") // redundant local variable, for debugging
        List<Locale> localesByPriority = Locale.filter(languagesByPriority, allowedLocales);
        return localesByPriority;
    }

    /**
     * Resolves HTTP status code to {@link HttpStatus} using cached lookup for better performance than
     * {@link HttpStatus#resolve(int)} (which performs linear lookup on each call).
     *
     * @param statusCode HTTP status code
     * @return HTTP status if resolved
     */
    public static Optional<HttpStatus> resolveHttpStatus(int statusCode) {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        return HTTP_STATUS_CACHE.get(statusCode, key ->
                Optional.ofNullable(HttpStatus.resolve(key)));
    }

    /**
     * Creates a content disposition header value with the given type and filename.
     *
     * @param type             content disposition type: 'inline' or 'attachment' ({@code null} means 'inline')
     * @param filenameSupplier creates the filename (it will be URL-encoded by this method, and newlines/slashes/control
     *                         characters will be removed)
     * @throws ValidationException invalid content disposition type (the cause if a {@link BindException} with error
     *                             code 'Enumeration' and field name 'contentDisposition')
     */
    @ConstraintArguments
    public static String createContentDispositionHeaderValue(@Nullable String type, Supplier<String> filenameSupplier) {
        if (type == null) {
            type = "inline";
        }

        // generate proper binding error for verbose error rendering (like for a DTO)
        if (!"inline".equals(type) && !"attachment".equals(type)) {
            var fieldError = new FieldError("request", "contentDisposition", type, true,
                    new String[]{"Enumeration"}, null, "Invalid content disposition");

            var result = new BeanPropertyBindingResult(type, "contentDisposition");
            result.addError(fieldError);
            throw new ValidationException(new BindException(result));
        }

        checkNotNull(filenameSupplier,
                _HttpRequestUtilsCreateContentDispositionHeaderValueArgumentsMeta.FILENAMESUPPLIER.name());
        String filename = filenameSupplier.get();
        checkNotBlank(filename, "filename");

        // sanitize CRLF to prevent header injection
        filename = filename.replaceAll("[\\r\\n]", "");
        // remove path separators to prevent path traversal
        filename = filename.replaceAll("[/\\\\]", "");
        // remove control characters
        filename = filename.replaceAll("\\p{Cntrl}", "");

        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                // URL encoding uses + for spaces, but headers need %20
                .replace("+", "%20");

        return type + "; filename=\"" + filename + "\"";
    }

}
