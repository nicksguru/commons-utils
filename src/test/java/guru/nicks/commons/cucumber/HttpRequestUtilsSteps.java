package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.HttpRequestUtils;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link HttpRequestUtils} functionality.
 */
@RequiredArgsConstructor
public class HttpRequestUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    private AutoCloseable closeableMocks;

    private List<HeaderValueRow> headerValueRows;
    private Collection<Locale> allowedLocales;
    private List<Locale> parsedLocales;

    private boolean headerSetResult;
    private String resolvedIp;
    private Optional<HttpStatus> resolvedStatus;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    /**
     * Creates a header value row from a DataTable row map.
     */
    @DataTableType
    public HeaderValueRow createHeaderValueRow(Map<String, String> entry) {
        String rawValue = entry.getOrDefault("value", null);
        String value = StringUtils.isNotBlank(rawValue)
                ? rawValue.strip()
                : null;

        return HeaderValueRow.builder()
                .value(value)
                .build();
    }

    /**
     * Creates a header pair (name/value) from a DataTable row map.
     */
    @DataTableType
    public HeaderPairRow createHeaderPairRow(Map<String, String> entry) {
        String name = StringUtils.isNotBlank(entry.get("name"))
                ? entry.get("name").strip()
                : null;

        String rawValue = entry.get("value");
        String value = StringUtils.isNotBlank(rawValue)
                ? rawValue.strip()
                : null;

        return HeaderPairRow.builder()
                .name(name)
                .value(value)
                .build();
    }

    /**
     * Creates a locale row from a DataTable row map.
     */
    @DataTableType
    public LocaleRow createLocaleRow(Map<String, String> entry) {
        String tag = StringUtils.isNotBlank(entry.get("tag"))
                ? entry.get("tag").strip()
                : null;

        return LocaleRow.builder()
                .tag(tag)
                .build();
    }

    /**
     * Collects header values using a DataTable.
     */
    @Given("the following header values are provided:")
    public void theFollowingHeaderValuesAreProvided(DataTable dataTable) {
        headerValueRows = dataTable.asList(HeaderValueRow.class);
    }

    /**
     * Sets up request headers from a table of name/value pairs.
     */
    @Given("request headers are configured:")
    public void requestHeadersAreConfigured(DataTable dataTable) {
        List<HeaderPairRow> headerPairs = dataTable.asList(HeaderPairRow.class);

        headerPairs.forEach(pair -> {
            if (StringUtils.isNotBlank(pair.getName())) {
                when(request.getHeader(pair.getName()))
                        .thenReturn(pair.getValue());
            }
        });
    }

    /**
     * Sets the request remote address.
     */
    @Given("request remote address is {string}")
    public void requestRemoteAddressIs(String remoteAddr) {
        String value = StringUtils.isNotBlank(remoteAddr)
                ? remoteAddr.strip()
                : null;

        when(request.getRemoteAddr())
                .thenReturn(value);
    }

    /**
     * Sets Accept-Language header on the request.
     */
    @Given("Accept-Language header is {string}")
    public void acceptLanguageHeaderIs(String headerValue) {
        String value = StringUtils.isNotBlank(headerValue)
                ? headerValue.strip()
                : null;
        when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
                .thenReturn(value);
    }

    /**
     * Sets up Accept-Language header to throw exception.
     */
    @Given("Accept-Language header access throws exception")
    public void acceptLanguageHeaderAccessThrowsException() {
        when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
                .thenThrow(new RuntimeException("No thread-bound request found"));
    }

    /**
     * Collects allowed locales from a table.
     */
    @Given("allowed locales are:")
    public void allowedLocalesAre(DataTable dataTable) {
        List<LocaleRow> allowedLocaleRows = dataTable.asList(LocaleRow.class);

        allowedLocales = allowedLocaleRows
                .stream()
                .map(LocaleRow::getTag)
                .filter(StringUtils::isNotBlank)
                .map(Locale::forLanguageTag)
                .toList();
    }

    /**
     * Calls setNonBlankHeader with provided header name and current headerValueRows.
     */
    @When("header {string} is set from provided values")
    public void headerIsSetFromProvidedValues(String headerName) {
        List<String> values = headerValueRows == null
                ? null
                : headerValueRows.stream()
                        .map(HeaderValueRow::getValue)
                        .toList();

        Throwable throwable = catchThrowable(() ->
                headerSetResult = HttpRequestUtils.setNonBlankHeader(
                        response,
                        headerName.strip(),
                        values));

        textWorld.setLastException(throwable);
    }

    /**
     * Resolves the proxied remote IP.
     */
    @When("proxied remote IP is resolved")
    public void proxiedRemoteIpIsResolved() {
        Throwable throwable = catchThrowable(() ->
                resolvedIp = HttpRequestUtils.getRemoteIpBehindProxy(request));

        textWorld.setLastException(throwable);
    }

    /**
     * Parses the Accept-Language header using provided allowed locales.
     */
    @When("Accept-Language header is parsed")
    public void acceptLanguageHeaderIsParsed() {
        Throwable throwable = catchThrowable(() ->
                parsedLocales = HttpRequestUtils.parseAcceptLanguageHttpHeader(
                        request,
                        allowedLocales));

        textWorld.setLastException(throwable);
    }

    /**
     * Resolves an HTTP status code via the cached resolver.
     */
    @When("HTTP status code {int} is resolved")
    public void httpStatusCodeIsResolved(int statusCode) {
        Throwable throwable = catchThrowable(() ->
                resolvedStatus = HttpRequestUtils.resolveHttpStatus(statusCode));

        textWorld.setLastException(throwable);
    }

    /**
     * Asserts whether header was set and verifies interaction with the response.
     */
    @Then("header should be set is {booleanValue}")
    public void headerShouldBeSetIs(boolean shouldBeSet) {
        assertThat(headerSetResult)
                .as("headerSetResult")
                .isEqualTo(shouldBeSet);

        if (shouldBeSet) {
            verify(response).setHeader(anyString(), anyString());
        } else {
            verify(response, never()).setHeader(anyString(), anyString());
        }
    }

    /**
     * Verifies the header value that was set on the response.
     */
    @Then("response header {string} should be set to {string}")
    public void responseHeaderShouldBeSetTo(String headerName, String expectedValue) {
        verify(response).setHeader(headerName, expectedValue);
    }

    /**
     * Asserts resolved IP equality.
     */
    @Then("resolved IP should equal {string}")
    public void resolvedIpShouldEqual(String expected) {
        assertThat(resolvedIp)
                .as("resolvedIp")
                .isEqualTo(expected.strip());
    }

    /**
     * Asserts parsed locales equal to expected language tags sequence.
     */
    @Then("parsed locales should equal {string}")
    public void parsedLocalesShouldEqual(String expectedTagsCsv) {
        List<String> expectedTags = StringUtils.isNotBlank(expectedTagsCsv)
                ? Stream.of(expectedTagsCsv.split(","))
                .map(String::strip)
                .filter(StringUtils::isNotBlank)
                .toList()
                : List.of();

        List<String> actualTags = parsedLocales == null
                ? List.<String>of()
                : parsedLocales.stream()
                        .map(Locale::toLanguageTag)
                        .toList();

        assertThat(actualTags)
                .as("parsedLocaleTags")
                .containsExactlyElementsOf(expectedTags);
    }

    /**
     * Asserts presence of resolved status.
     */
    @Then("resolution present is {booleanValue}")
    public void resolutionPresentIs(boolean expectedPresent) {
        assertThat(resolvedStatus != null && resolvedStatus.isPresent())
                .as("resolutionPresent")
                .isEqualTo(expectedPresent);
    }

    /**
     * Asserts resolved status name equals expected name.
     */
    @Then("resolved status name should equal {string}")
    public void resolvedStatusNameShouldEqual(String expectedName) {
        if (StringUtils.isNotBlank(expectedName)) {
            assertThat(resolvedStatus)
                    .as("resolvedStatus")
                    .isPresent();

            HttpStatus status = resolvedStatus.orElseThrow();
            assertThat(status.name())
                    .as("resolvedStatusName")
                    .isEqualTo(expectedName.strip());
        } else {
            assertThat(resolvedStatus)
                    .as("resolvedStatus")
                    .isEmpty();
        }
    }

    /**
     * Verifies that specific request headers were accessed.
     */
    @Then("request header {string} should be accessed")
    public void requestHeaderShouldBeAccessed(String headerName) {
        verify(request).getHeader(headerName);
    }

    /**
     * Verifies that remote address was accessed.
     */
    @Then("request remote address should be accessed")
    public void requestRemoteAddressShouldBeAccessed() {
        verify(request).getRemoteAddr();
    }

    @Value
    @Builder
    public static class HeaderValueRow {

        String value;

    }

    @Value
    @Builder
    public static class HeaderPairRow {

        String name;
        String value;

    }

    @Value
    @Builder
    public static class LocaleRow {

        String tag;

    }

}
