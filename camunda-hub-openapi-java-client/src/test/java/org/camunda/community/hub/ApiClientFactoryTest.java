package org.camunda.community.hub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.camunda.community.hub.client.api.InfoApi;
import org.camunda.community.hub.client.api.ProjectApi;
import org.camunda.community.hub.client.dto.InfoResult;
import org.camunda.community.hub.client.dto.ProjectSearchQuery;
import org.camunda.community.hub.client.dto.ProjectSearchQueryResult;
import org.camunda.community.hub.client.dto.SearchQueryPage;
import org.camunda.community.hub.client.invoker.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the factory together with the generated client against a fake Hub, so path, auth header
 * and JSON (de)serialization are checked against the shape of the vendored spec.
 */
@WireMockTest
class ApiClientFactoryTest {

  private static final String API_PATH = "/api/v2";

  private ApiClient client;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) {
    client =
        ApiClientFactory.getInstance()
            .get(
                new ApiClientConfiguration(
                    wireMock.getHttpBaseUrl() + API_PATH,
                    new HubAuthInterceptor(() -> "test-token")));
  }

  @Test
  void shouldUseConfiguredBasePath(WireMockRuntimeInfo wireMock) {
    assertEquals(wireMock.getHttpBaseUrl() + API_PATH, client.getBasePath());
  }

  @Test
  void shouldCreateIndependentClientsPerConfiguration(WireMockRuntimeInfo wireMock) {
    ApiClientConfiguration configuration =
        new ApiClientConfiguration(wireMock.getHttpBaseUrl(), new HubAuthInterceptor(() -> "t"));

    ApiClientFactory factory = ApiClientFactory.getInstance();

    assertSame(factory, ApiClientFactory.getInstance());
    assertNotSame(factory.get(configuration), factory.get(configuration));
  }

  @Test
  void shouldGetInfoWithBearerToken() throws Exception {
    stubFor(
        get(urlEqualTo(API_PATH + "/info"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"version\":\"1.2.3\",\"authorizedOrganization\":\"org-1\"}")));

    InfoResult info = new InfoApi(client).getInfo();

    assertEquals("1.2.3", info.getVersion());
    assertEquals("org-1", info.getAuthorizedOrganization());
    verify(
        getRequestedFor(urlEqualTo(API_PATH + "/info"))
            .withHeader("Authorization", equalTo("Bearer test-token")));
  }

  @Test
  void shouldSearchProjects() throws Exception {
    stubFor(
        post(urlEqualTo(API_PATH + "/projects/search"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"items":[{"projectKey":"p1","name":"First","workspaceKey":"w1",
                                   "created":"2025-01-02T03:04:05Z"}],
                         "page":{"totalItems":1,"hasMoreTotalItems":false}}
                        """)));

    ProjectSearchQueryResult result =
        new ProjectApi(client)
            .searchProjects(new ProjectSearchQuery().page(new SearchQueryPage().limit(50).from(0)));

    assertEquals(1, result.getItems().size());
    assertEquals("p1", result.getItems().get(0).getProjectKey());
    assertEquals("First", result.getItems().get(0).getName());
    assertEquals(2025, result.getItems().get(0).getCreated().getYear());
    assertEquals(1L, result.getPage().getTotalItems());
    verify(
        postRequestedFor(urlEqualTo(API_PATH + "/projects/search"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
            .withRequestBody(equalToJson("{\"page\":{\"limit\":50,\"from\":0}}", true, true)));
  }
}
