package guru.nicks.commons.cucumber.json;

import guru.nicks.commons.auth.domain.JwkInfo;
import guru.nicks.commons.utils.crypto.PemUtils;
import guru.nicks.commons.utils.json.JwkUtils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwkUtilsSteps {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private JWKSet jwkSet;
    @Mock
    private RSAPublicKey rsaPublicKey;
    private AutoCloseable closeableMocks;
    private MockedStatic<JWKSet> mockedJwkSetStatic;

    private List<JwkInfo> jwkInfos;
    private Optional<Instant> smallestExpirationDate;
    private JwkInfo jwkInfo;
    private List<RSAPublicKey> publicKeys;

    private JwtDecoder jwtDecoder;
    private String jwkSetUrl;
    private String authProviderId;
    private String pemKeyPair;

    private JWK jwk;
    private HttpHeaders responseHeaders;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        mockedJwkSetStatic = Mockito.mockStatic(JWKSet.class);

        jwkInfos = new ArrayList<>();
        responseHeaders = new HttpHeaders();
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();

        if (mockedJwkSetStatic != null) {
            mockedJwkSetStatic.close();
        }
    }

    @DataTableType
    public JwkInfoData createJwkInfoData(Map<String, String> entry) {
        String expirationDateStr = entry.get("expirationDate");
        Instant expirationDate = ((expirationDateStr != null) && !"null".equals(expirationDateStr))
                ? Instant.parse(expirationDateStr)
                : null;

        return JwkInfoData.builder()
                .authProviderId(entry.get("authProviderId"))
                .expirationDate(expirationDate)
                .build();
    }

    @Given("the following JWK infos with expiration dates:")
    public void theFollowingJwkInfosWithExpirationDates(List<JwkInfoData> jwkInfoDataList) {
        jwkInfos = jwkInfoDataList.stream()
                .map(this::convertToJwkInfo)
                .toList();
    }

    private JwkInfo convertToJwkInfo(JwkInfoData data) {
        return JwkInfo.builder()
                .authProviderId(data.getAuthProviderId())
                .expirationDate(data.getExpirationDate())
                .keys(mock(JWKSet.class))
                .build();
    }

    @When("the smallest expiration date is found")
    public void theSmallestExpirationDateIsFound() {
        smallestExpirationDate = JwkUtils.findSmallestExpirationDate(jwkInfos);
    }

    @Then("the expiration date should be {string}")
    public void theExpirationDateShouldBe(String expectedDateStr) {
        Instant expectedDate = Instant.parse(expectedDateStr);

        assertThat(smallestExpirationDate)
                .as("expiration date value")
                .contains(expectedDate);
    }

    @Then("no expiration date should be found")
    public void noExpirationDateShouldBeFound() {
        assertThat(smallestExpirationDate)
                .as("smallest expiration date")
                .isEmpty();
    }

    @Given("a JWK info with RSA keys")
    public void aJwkInfoWithRsaKeys() {
        // mock RSA key methods to facilitate stream operations performed in JwkUtils.createPublicKey
        var rsaKey1 = mock(RSAKey.class);
        when(rsaKey1.toRSAKey())
                .thenReturn(mock(RSAKey.class));

        var rsaKey2 = mock(RSAKey.class);
        when(rsaKey2.toRSAKey())
                .thenReturn(mock(RSAKey.class));

        // Setup JWKSet to return the RSA keys. Need explicit List<JWK> (not List<RSAKey>) for return by getKeys()
        // below - because its signature is List<JWK> and not List<? extends JWK>.
        List<JWK> jwkList = List.of(rsaKey1, rsaKey2);

        var publicJwkSet = mock(JWKSet.class);
        when(publicJwkSet.getKeys())
                .thenReturn(jwkList);
        when(jwkSet.toPublicJWKSet())
                .thenReturn(publicJwkSet);

        jwkInfo = JwkInfo.builder()
                .authProviderId("test-provider")
                .keys(jwkSet)
                .build();
    }

    @When("public keys are created from the JWK info")
    public void publicKeysAreCreatedFromTheJwkInfo() {
        publicKeys = JwkUtils.extractPublicKeys(jwkInfo);
    }

    @Then("the public keys should be created successfully")
    public void thePublicKeysShouldBeCreatedSuccessfully() {
        assertThat(publicKeys)
                .as("public keys")
                .isNotNull();
    }

    @Then("the number of public keys should be {int}")
    public void theNumberOfPublicKeysShouldBe(int expectedCount) {
        assertThat(publicKeys)
                .as("public keys")
                .hasSize(expectedCount);
    }

    @Given("an RSA public key with algorithm {string}")
    public void anRsaPublicKeyWithAlgorithm(String algorithm) {
        rsaPublicKey = mock(RSAPublicKey.class);

        when(rsaPublicKey.getAlgorithm())
                .thenReturn(algorithm);
    }

    @When("a JWT decoder is created with the public key")
    public void aJwtDecoderIsCreatedWithThePublicKey() {
        jwtDecoder = JwkUtils.createJwtDecoder(rsaPublicKey);
    }

    @Then("the JWT decoder should be created successfully")
    public void theJwtDecoderShouldBeCreatedSuccessfully() {
        assertThat(jwtDecoder)
                .as("JWT decoder")
                .isNotNull();
    }

    @Given("a JWK set URL {string}")
    public void aJwkSetUrl(String url) {
        jwkSetUrl = url;
    }

    @Given("an auth provider ID {string}")
    public void anAuthProviderId(String providerId) {
        authProviderId = providerId;
    }

    @Given("the JWK set response has a cache control header with max age {int}")
    public void theJwkSetResponseHasACacheControlHeaderWithMaxAge(int maxAge) {
        responseHeaders.setCacheControl("max-age=" + maxAge);
        setupMockJwkSetResponse();
    }

    @Given("the JWK set response has no cache control header")
    public void theJwkSetResponseHasNoCacheControlHeader() {
        setupMockJwkSetResponse();
    }

    @When("the JWK set is fetched")
    public void theJwkSetIsFetched() {
        jwkInfo = JwkUtils.fetchJwkSet(authProviderId, jwkSetUrl, restTemplate);
    }

    @Then("the JWK info should have an expiration date")
    public void theJwkInfoShouldHaveAnExpirationDate() {
        assertThat(jwkInfo)
                .as("JWK info")
                .isNotNull();

        assertThat(jwkInfo.expirationDate())
                .as("expiration date")
                .isNotNull();
    }

    @Then("the JWK info should not have an expiration date")
    public void theJwkInfoShouldNotHaveAnExpirationDate() {
        assertThat(jwkInfo)
                .as("JWK info")
                .isNotNull();

        assertThat(jwkInfo.expirationDate())
                .as("expiration date")
                .isNull();
    }

    @Then("the JWK info should have auth provider ID {string}")
    public void theJwkInfoShouldHaveAuthProviderId(String expectedProviderId) {
        assertThat(jwkInfo)
                .as("JWK info")
                .isNotNull();

        assertThat(jwkInfo.authProviderId())
                .as("auth provider ID")
                .isEqualTo(expectedProviderId);
    }

    @Given("a PEM encoded random key pair")
    public void aPemEncodedRandomKeyPair() throws Exception {
        // generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKeyPem = PemUtils.encodeToPem(keyPair.getPrivate());
        String publicKeyPem = PemUtils.encodeToPem(keyPair.getPublic());
        // combine private and public key PEMs
        pemKeyPair = privateKeyPem + "\n" + publicKeyPem;
    }

    @When("the PEM is parsed to JWK")
    public void thePemIsParsedToJwk() {
        jwk = JwkUtils.parsePemToJwk(pemKeyPair);
    }

    @Then("the JWK should be created successfully")
    public void theJwkShouldBeCreatedSuccessfully() {
        assertThat(jwk)
                .as("JWK")
                .isNotNull();
    }

    /**
     * Helper method to mock the {@link RestTemplate} response returning a {@link JWKSet}.
     */
    private void setupMockJwkSetResponse() {
        // mock JWKSet
        jwkSet = mock(JWKSet.class);
        when(jwkSet.size())
                .thenReturn(2);

        // create mock response entity with empty JSON for testing
        var responseEntity = mock(ResponseEntity.class);
        when(responseEntity.getBody())
                .thenReturn("{}");
        when(responseEntity.getHeaders())
                .thenReturn(responseHeaders);

        when(restTemplate.exchange(jwkSetUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class))
                .thenReturn(responseEntity);

        // mock the static JWKSet.parse method
        mockedJwkSetStatic.when(() -> JWKSet.parse(any(String.class)))
                .thenReturn(jwkSet);
    }

    @Value
    @Builder
    public static class JwkInfoData {

        String authProviderId;
        Instant expirationDate;

    }

}
