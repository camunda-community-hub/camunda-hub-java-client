package org.camunda.community.hub.client.springboot;

import static org.camunda.community.hub.ApiClientConfiguration.*;

import io.camunda.common.auth.Authentication;
import io.camunda.common.auth.JwtConfig;
import io.camunda.common.auth.JwtCredential;
import io.camunda.common.auth.Product;
import io.camunda.common.auth.SaaSAuthentication;
import io.camunda.common.auth.SelfManagedAuthentication;
import io.camunda.common.auth.identity.IdentityConfig;
import io.camunda.common.auth.identity.IdentityContainer;
import io.camunda.common.json.JsonMapper;
import io.camunda.common.json.SdkObjectMapper;
import io.camunda.identity.sdk.Identity;
import io.camunda.identity.sdk.IdentityConfiguration;
import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
import org.camunda.community.hub.HubAuthInterceptor;
import org.camunda.community.hub.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
  @ConditionalOnMissingBean
  public JsonMapper jsonMapper() {
    return new SdkObjectMapper();
  }

  @Bean
  public JwtCredential hubCredential() {
    return new JwtCredential(
        properties.clientId(),
        properties.clientSecret(),
        properties.audience(),
        properties.authUrl().toString());
  }

  @Bean
  public JwtConfig jwtConfig(JwtCredential hubCredential) {
    JwtConfig jwtConfig = new JwtConfig();
    jwtConfig.addProduct(Product.WEB_MODELER, hubCredential);
    return jwtConfig;
  }

  @Bean
  public IdentityConfiguration identityConfiguration() {
    return new IdentityConfiguration(
        properties.identityUrl().toString(),
        properties.authUrl().toString(),
        properties.authUrl().toString(),
        properties.clientId(),
        properties.clientSecret(),
        properties.audience(),
        properties.authType().name());
  }

  @Bean
  public Identity identity(IdentityConfiguration identityConfiguration) {
    return new Identity(identityConfiguration);
  }

  @Bean
  public IdentityContainer identityContainer(
      Identity identity, IdentityConfiguration identityConfiguration) {
    return new IdentityContainer(identity, identityConfiguration);
  }

  @Bean
  public IdentityConfig identityConfig(IdentityContainer identityContainer) {
    IdentityConfig identityConfig = new IdentityConfig();
    identityConfig.addProduct(Product.WEB_MODELER, identityContainer);
    return identityConfig;
  }

  @Bean
  public Authentication authentication(
      JsonMapper jsonMapper, JwtConfig jwtConfig, IdentityConfig identityConfig) {
    if (isSaas()) {
      return SaaSAuthentication.builder()
          .withJsonMapper(jsonMapper)
          .withJwtConfig(jwtConfig)
          .build();
    } else {
      return SelfManagedAuthentication.builder()
          .withJwtConfig(jwtConfig)
          .withIdentityConfig(identityConfig)
          .build();
    }
  }

  private boolean isSaas() {
    return properties.authUrl().toString().equals(SAAS_AUTH_URL);
  }

  @Bean
  public HubAuthInterceptor hubAuthInterceptor(Authentication authentication) {
    return new HubAuthInterceptor(authentication);
  }

  @Bean
  public ApiClient apiClient(HubAuthInterceptor authInterceptor) {
    return ApiClientFactory.getInstance()
        .get(new ApiClientConfiguration(properties.baseUrl().toString(), authInterceptor));
  }
}
