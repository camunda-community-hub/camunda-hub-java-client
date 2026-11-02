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

/**
 * Entry point to all Camunda Hub APIs sharing one authenticated {@link ApiClient}. The same API
 * objects are also registered as individual beans by {@link CamundaHubClientStarter}.
 */
public class CamundaHubApi {

  private final ApiClient apiClient;

  @Autowired
  public CamundaHubApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public CatalogApi catalogApi() {
    return new CatalogApi(apiClient);
  }

  public ClusterApi clusterApi() {
    return new ClusterApi(apiClient);
  }

  public EnvironmentApi environmentApi() {
    return new EnvironmentApi(apiClient);
  }

  public FileApi fileApi() {
    return new FileApi(apiClient);
  }

  public FolderApi folderApi() {
    return new FolderApi(apiClient);
  }

  public InfoApi infoApi() {
    return new InfoApi(apiClient);
  }

  public MemberApi memberApi() {
    return new MemberApi(apiClient);
  }

  public ProjectApi projectApi() {
    return new ProjectApi(apiClient);
  }

  public ProjectSnapshotApi projectSnapshotApi() {
    return new ProjectSnapshotApi(apiClient);
  }

  public VersionApi versionApi() {
    return new VersionApi(apiClient);
  }

  public WorkspaceApi workspaceApi() {
    return new WorkspaceApi(apiClient);
  }
}
