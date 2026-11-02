package org.camunda.community.hub.client.springboot;

import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
import org.camunda.community.hub.ClientCredentialsTokenProvider;
import org.camunda.community.hub.HubAuthInterceptor;
import org.camunda.community.hub.client.api.CatalogApi;
import org.camunda.community.hub.client.api.ClusterApi;
import org.camunda.community.hub.client.api.EnvironmentApi;
import org.camunda.community.hub.client.api.FileApi;
import org.camunda.community.hub.client.api.FolderApi;
import org.camunda.community.hub.client.api.InfoApi;
import org.camunda.community.hub.client.api.MemberApi;
import org.camunda.community.hub.client.api.ProjectApi;
import org.camunda.community.hub.client.api.ProjectSnapshotApi;
import org.camunda.community.hub.client.api.VersionApi;
import org.camunda.community.hub.client.api.WorkspaceApi;
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

  @Bean
  public CatalogApi catalogApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.catalogApi();
  }

  @Bean
  public ClusterApi clusterApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.clusterApi();
  }

  @Bean
  public EnvironmentApi environmentApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.environmentApi();
  }

  @Bean
  public FileApi fileApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.fileApi();
  }

  @Bean
  public FolderApi folderApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.folderApi();
  }

  @Bean
  public InfoApi infoApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.infoApi();
  }

  @Bean
  public MemberApi memberApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.memberApi();
  }

  @Bean
  public ProjectApi projectApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.projectApi();
  }

  @Bean
  public ProjectSnapshotApi projectSnapshotApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.projectSnapshotApi();
  }

  @Bean
  public VersionApi versionApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.versionApi();
  }

  @Bean
  public WorkspaceApi workspaceApi(CamundaHubApi camundaHubApi) {
    return camundaHubApi.workspaceApi();
  }
}
