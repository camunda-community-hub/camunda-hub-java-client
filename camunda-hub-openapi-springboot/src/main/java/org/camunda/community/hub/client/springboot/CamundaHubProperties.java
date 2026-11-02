package org.camunda.community.hub.client.springboot;

import io.camunda.identity.sdk.IdentityConfiguration.Type;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("camunda.hub.client")
public record CamundaHubProperties(
    URL baseUrl,
    URL authUrl,
    String audience,
    String clientId,
    String clientSecret,
    Type authType,
    URL identityUrl) {}
