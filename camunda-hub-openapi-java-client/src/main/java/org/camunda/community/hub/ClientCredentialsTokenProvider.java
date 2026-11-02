package org.camunda.community.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches and caches access tokens using the OAuth 2.0 client credentials flow.
 *
 * <p>The {@code authUrl} is either the token endpoint itself (path ending in {@code /token}, as for
 * Camunda SaaS) or an OIDC issuer URL, in which case the token endpoint is looked up via {@code
 * /.well-known/openid-configuration}.
 */
public class ClientCredentialsTokenProvider {

  private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  private final String authUrl;
  private final String clientId;
  private final String clientSecret;
  private final String audience;

  private String tokenEndpoint;
  private String token;
  private Instant expiresAt = Instant.MIN;

  public ClientCredentialsTokenProvider(
      String authUrl, String clientId, String clientSecret, String audience) {
    this.authUrl = authUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.audience = audience;
  }

  /** Returns a valid access token, requesting a new one if the cached token is about to expire. */
  public synchronized String getToken() {
    if (token == null || Instant.now().isAfter(expiresAt.minus(EXPIRY_MARGIN))) {
      requestToken();
    }
    return token;
  }

  private void requestToken() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("grant_type", "client_credentials");
    form.put("client_id", clientId);
    form.put("client_secret", clientSecret);
    if (audience != null) {
      form.put("audience", audience);
    }
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(resolveTokenEndpoint()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
            .build();
    JsonNode response = send(request, "Unable to authenticate");
    JsonNode accessToken = response.get("access_token");
    if (accessToken == null) {
      throw new IllegalStateException("Unable to authenticate: no access_token in response");
    }
    token = accessToken.asText();
    expiresAt = Instant.now().plusSeconds(response.path("expires_in").asLong(0));
  }

  private String resolveTokenEndpoint() {
    if (tokenEndpoint == null) {
      if (URI.create(authUrl).getPath().endsWith("/token")) {
        tokenEndpoint = authUrl;
      } else {
        HttpRequest request =
            HttpRequest.newBuilder(
                    URI.create(authUrl.replaceAll("/+$", "") + "/.well-known/openid-configuration"))
                .GET()
                .build();
        JsonNode configuration = send(request, "Unable to discover token endpoint");
        JsonNode endpoint = configuration.get("token_endpoint");
        if (endpoint == null) {
          throw new IllegalStateException("No token_endpoint found for issuer " + authUrl);
        }
        tokenEndpoint = endpoint.asText();
      }
    }
    return tokenEndpoint;
  }

  private JsonNode send(HttpRequest request, String errorMessage) {
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        throw new IllegalStateException(
            errorMessage + ": HTTP " + response.statusCode() + " from " + request.uri());
      }
      return OBJECT_MAPPER.readTree(response.body());
    } catch (IOException e) {
      throw new IllegalStateException(errorMessage, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(errorMessage, e);
    }
  }

  private static String encode(Map<String, String> form) {
    return form.entrySet().stream()
        .map(
            e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }
}
