package org.camunda.community.hub.client.springboot;

import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection settings of the Camunda Hub client. The defaults target Camunda SaaS. They are
 * declared here rather than in a bundled {@code application.yaml}, because an application's own
 * {@code application.yaml} shadows the one of a library on the classpath.
 *
 * @param baseUrl base URL of the Hub API
 * @param authUrl token endpoint or OIDC issuer URL
 * @param audience audience of the access token
 * @param clientId client id, required
 * @param clientSecret client secret, required
 */
@ConfigurationProperties("camunda.hub.client")
public record CamundaHubProperties(
    @DefaultValue("https://hub.camunda.io/api/v2") URL baseUrl,
    @DefaultValue("https://login.cloud.camunda.io/oauth/token") URL authUrl,
    @DefaultValue("api.cloud.camunda.io") String audience,
    String clientId,
    String clientSecret) {

  public CamundaHubProperties {
    requireText(clientId, "client-id");
    requireText(clientSecret, "client-secret");
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "Property 'camunda.hub.client.%s' must be set to use the Camunda Hub client"
              .formatted(name));
    }
  }
}
