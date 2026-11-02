package org.camunda.community.hub.client.springboot;

import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
import org.camunda.community.hub.ClientCredentialsTokenProvider;
import org.camunda.community.hub.HubAuthInterceptor;
import org.camunda.community.hub.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CamundaHubProperties.class)
public class CamundaHubClientStarter {
  private final CamundaHubProperties properties;

  @Autowired
  public CamundaHubClientStarter(CamundaHubProperties properties) {
    this.properties = properties;
  }

  @Bean
  public CamundaHubApi createCamundaHubApi(ApiClient client) {
    return new CamundaHubApi(client);
  }

  @Bean
  public ClientCredentialsTokenProvider clientCredentialsTokenProvider() {
    return new ClientCredentialsTokenProvider(
        properties.authUrl().toString(),
        properties.clientId(),
        properties.clientSecret(),
        properties.audience());
  }

  @Bean
  public HubAuthInterceptor hubAuthInterceptor(ClientCredentialsTokenProvider tokenProvider) {
    return new HubAuthInterceptor(tokenProvider);
  }

  @Bean
  public ApiClient apiClient(HubAuthInterceptor authInterceptor) {
    return ApiClientFactory.getInstance()
        .get(new ApiClientConfiguration(properties.baseUrl().toString(), authInterceptor));
  }
}
