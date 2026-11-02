package org.camunda.community.hub.client.springboot;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.camunda.community.hub.client.dto.ProjectSearchQuery;
import org.camunda.community.hub.client.invoker.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrintModels {

  @Autowired private CamundaHubApi camundaHubApi;

  @PostConstruct
  public void print() throws ApiException {
    print(
        "Projects", camundaHubApi.projectApi().searchProjects(new ProjectSearchQuery()).getItems());
  }

  private void print(String name, List elements) {
    System.out.println(name + ":");

    for (Object e : elements) {
      System.out.println(e);
    }
  }
}
