package org.camunda.community.hub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class ClientCredentialsTokenProviderTest {

  private static final String TOKEN_PATH = "/oauth/token";

  private String baseUrl;
  private final MutableClock clock = new MutableClock();

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) {
    baseUrl = wireMock.getHttpBaseUrl();
  }

  private ClientCredentialsTokenProvider provider(String authUrl) {
    return new ClientCredentialsTokenProvider(
        authUrl,
        "my-id",
        "my-secret",
        "my-audience",
        clock,
        Duration.ofSeconds(5),
        Duration.ofSeconds(5));
  }

  private static void stubToken(String token, Long expiresIn) {
    String body =
        expiresIn == null
            ? "{\"access_token\":\"%s\"}".formatted(token)
            : "{\"access_token\":\"%s\",\"expires_in\":%d}".formatted(token, expiresIn);
    stubFor(
        post(urlEqualTo(TOKEN_PATH))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  @Test
  void shouldPostClientCredentialsFormToTokenEndpoint() {
    stubToken("t1", 3600L);

    String token = provider(baseUrl + TOKEN_PATH).getToken();

    assertEquals("t1", token);
    verify(
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(containing("grant_type=client_credentials"))
            .withRequestBody(containing("client_id=my-id"))
            .withRequestBody(containing("client_secret=my-secret"))
            .withRequestBody(containing("audience=my-audience")));
  }

  @Test
  void shouldOmitAudienceWhenNull() {
    stubToken("t1", 3600L);

    new ClientCredentialsTokenProvider(
            baseUrl + TOKEN_PATH,
            "id",
            "secret",
            null,
            clock,
            Duration.ofSeconds(5),
            Duration.ofSeconds(5))
        .getToken();

    verify(
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withRequestBody(
                com.github.tomakehurst.wiremock.client.WireMock.notMatching(".*audience.*")));
  }

  @Test
  void shouldUrlEncodeFormValues() {
    stubToken("t1", 3600L);

    new ClientCredentialsTokenProvider(
            baseUrl + TOKEN_PATH,
            "id",
            "s&c=r et",
            null,
            clock,
            Duration.ofSeconds(5),
            Duration.ofSeconds(5))
        .getToken();

    verify(
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withRequestBody(containing("client_secret=s%26c%3Dr+et")));
  }

  @Test
  void shouldDiscoverTokenEndpointFromIssuer() {
    stubDiscovery("/issuer");
    stubToken("t1", 3600L);

    assertEquals("t1", provider(baseUrl + "/issuer").getToken());
  }

  @Test
  void shouldDiscoverTokenEndpointFromIssuerWithTrailingSlash() {
    stubDiscovery("/issuer");
    stubToken("t1", 3600L);

    assertEquals("t1", provider(baseUrl + "/issuer///").getToken());
  }

  @Test
  void shouldCacheDiscoveredTokenEndpointAcrossRefreshes() {
    stubDiscovery("/issuer");
    stubToken("t1", 60L);
    ClientCredentialsTokenProvider provider = provider(baseUrl + "/issuer");

    provider.getToken();
    clock.advance(Duration.ofSeconds(120));
    provider.getToken();

    verify(1, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    verify(2, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  @Test
  void shouldFailWhenDiscoveryHasNoTokenEndpoint() {
    stubFor(
        get(urlEqualTo("/issuer/.well-known/openid-configuration"))
            .willReturn(aResponse().withBody("{\"issuer\":\"x\"}")));

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> provider(baseUrl + "/issuer").getToken());

    assertTrue(e.getMessage().contains("No token_endpoint"), e.getMessage());
  }

  @Test
  void shouldFailWhenDiscoveryReturnsError() {
    stubFor(
        get(urlEqualTo("/issuer/.well-known/openid-configuration"))
            .willReturn(aResponse().withStatus(404)));

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> provider(baseUrl + "/issuer").getToken());

    assertTrue(e.getMessage().contains("Unable to discover token endpoint"), e.getMessage());
    assertTrue(e.getMessage().contains("404"), e.getMessage());
  }

  @Test
  void shouldReuseTokenUntilExpiryMargin() {
    stubToken("t1", 100L);
    ClientCredentialsTokenProvider provider = provider(baseUrl + TOKEN_PATH);

    provider.getToken();
    clock.advance(Duration.ofSeconds(69));
    provider.getToken();

    verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  @Test
  void shouldRefreshOnceExpiryMarginHasPassed() {
    stubToken("t1", 100L);
    ClientCredentialsTokenProvider provider = provider(baseUrl + TOKEN_PATH);

    provider.getToken();
    clock.advance(Duration.ofSeconds(71));
    stubToken("t2", 100L);

    assertEquals("t2", provider.getToken());
    verify(2, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  /**
   * A response without {@code expires_in} gives no information on how long the token is valid, so
   * it is never cached and every call requests a fresh token.
   */
  @Test
  void shouldNotCacheTokenWithoutExpiresIn() {
    stubToken("t1", null);
    ClientCredentialsTokenProvider provider = provider(baseUrl + TOKEN_PATH);

    provider.getToken();
    provider.getToken();

    verify(2, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  @Test
  void shouldIncludeStatusAndUrlWhenTokenRequestFails() {
    stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withStatus(401)));

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> provider(baseUrl + TOKEN_PATH).getToken());

    assertTrue(e.getMessage().contains("401"), e.getMessage());
    assertTrue(e.getMessage().contains(baseUrl + TOKEN_PATH), e.getMessage());
  }

  @Test
  void shouldFailWhenResponseHasNoAccessToken() {
    stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody("{\"expires_in\":5}")));

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> provider(baseUrl + TOKEN_PATH).getToken());

    assertTrue(e.getMessage().contains("no access_token"), e.getMessage());
  }

  @Test
  void shouldFailOnInvalidJson() {
    stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody("<html>")));

    assertThrows(IllegalStateException.class, () -> provider(baseUrl + TOKEN_PATH).getToken());
  }

  @Test
  void shouldIssueSingleRequestForConcurrentCallers() throws Exception {
    stubToken("t1", 3600L);
    ClientCredentialsTokenProvider provider = provider(baseUrl + TOKEN_PATH);
    int threads = 8;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<String>> results = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return provider.getToken();
                }));
      }
      start.countDown();
      for (Future<String> result : results) {
        assertEquals("t1", result.get());
      }
    } finally {
      executor.shutdownNow();
    }

    verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  @Test
  void shouldRestoreInterruptFlagWhenInterrupted() {
    stubToken("t1", 3600L);
    ClientCredentialsTokenProvider provider = provider(baseUrl + TOKEN_PATH);

    Thread.currentThread().interrupt();
    try {
      assertThrows(IllegalStateException.class, provider::getToken);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  private void stubDiscovery(String issuerPath) {
    stubFor(
        get(urlEqualTo(issuerPath + "/.well-known/openid-configuration"))
            .willReturn(
                aResponse().withBody("{\"token_endpoint\":\"" + baseUrl + TOKEN_PATH + "\"}")));
  }
}
