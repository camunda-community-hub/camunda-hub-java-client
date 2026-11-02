package org.camunda.community.hub.client.springboot;

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
import org.springframework.context.annotation.Bean;

public class CamundaHubApi {

  private final ApiClient apiClient;

  @Autowired
  public CamundaHubApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Bean
  public CatalogApi catalogApi() {
    return new CatalogApi(apiClient);
  }

  @Bean
  public ClusterApi clusterApi() {
    return new ClusterApi(apiClient);
  }

  @Bean
  public EnvironmentApi environmentApi() {
    return new EnvironmentApi(apiClient);
  }

  @Bean
  public FileApi fileApi() {
    return new FileApi(apiClient);
  }

  @Bean
  public FolderApi folderApi() {
    return new FolderApi(apiClient);
  }

  @Bean
  public InfoApi infoApi() {
    return new InfoApi(apiClient);
  }

  @Bean
  public MemberApi memberApi() {
    return new MemberApi(apiClient);
  }

  @Bean
  public ProjectApi projectApi() {
    return new ProjectApi(apiClient);
  }

  @Bean
  public ProjectSnapshotApi projectSnapshotApi() {
    return new ProjectSnapshotApi(apiClient);
  }

  @Bean
  public VersionApi versionApi() {
    return new VersionApi(apiClient);
  }

  @Bean
  public WorkspaceApi workspaceApi() {
    return new WorkspaceApi(apiClient);
  }
}
