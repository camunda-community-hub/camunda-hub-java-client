package org.camunda.community.hub.client.springboot;

import org.camunda.community.hub.client.api.CollaboratorsApi;
import org.camunda.community.hub.client.api.FilesApi;
import org.camunda.community.hub.client.api.FoldersApi;
import org.camunda.community.hub.client.api.InfoApi;
import org.camunda.community.hub.client.api.MilestonesApi;
import org.camunda.community.hub.client.api.ProjectsApi;
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
  public CollaboratorsApi collaboratorsApi() {
    return new CollaboratorsApi(apiClient);
  }

  @Bean
  public FilesApi filesApi() {
    return new FilesApi(apiClient);
  }

  @Bean
  public FoldersApi foldersApi() {
    return new FoldersApi(apiClient);
  }

  @Bean
  public InfoApi conditionApi() {
    return new InfoApi();
  }

  @Bean
  public MilestonesApi milestonesApi() {
    return new MilestonesApi(apiClient);
  }

  @Bean
  public ProjectsApi projectsApi() {
    return new ProjectsApi(apiClient);
  }
}
