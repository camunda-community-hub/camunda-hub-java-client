package org.camunda.community.hub.client.springboot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

@WireMockTest
class CamundaHubClientStarterTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(CamundaHubClientStarter.class)
          .withPropertyValues(
              "camunda.hub.client.client-id=id",
              "camunda.hub.client.client-secret=secret",
              "camunda.hub.client.base-url=https://hub.example/api/v2",
              "camunda.hub.client.auth-url=https://login.example/oauth/token");

  @Test
  void shouldCreateClientBeans() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(ClientCredentialsTokenProvider.class);
          assertThat(context).hasSingleBean(HubAuthInterceptor.class);
          assertThat(context).hasSingleBean(CamundaHubApi.class);
          assertThat(context.getBean(ApiClient.class).getBasePath())
              .isEqualTo("https://hub.example/api/v2");
        });
  }

  @Test
  void shouldExposeEveryApiAsBean() {
    runner.run(
        context -> {
          for (Class<?> api :
              new Class<?>[] {
                CatalogApi.class,
                ClusterApi.class,
                EnvironmentApi.class,
                FileApi.class,
                FolderApi.class,
                InfoApi.class,
                MemberApi.class,
                ProjectApi.class,
                ProjectSnapshotApi.class,
                VersionApi.class,
                WorkspaceApi.class
              }) {
            assertThat(context).hasSingleBean(api);
          }
        });
  }

  @Test
  void shouldShareApiClientBetweenApis() {
    runner.run(
        context ->
            assertThat(context.getBean(ProjectApi.class).getApiClient())
                .isSameAs(context.getBean(ApiClient.class))
                .isSameAs(context.getBean(InfoApi.class).getApiClient()));
  }

  @Test
  void shouldKeepCamundaHubApiAccessors() {
    runner.run(
        context -> {
          CamundaHubApi api = context.getBean(CamundaHubApi.class);
          assertThat(api.projectApi().getApiClient()).isSameAs(context.getBean(ApiClient.class));
        });
  }

  @Test
  void shouldFailWithClearMessageWithoutClientId() {
    new ApplicationContextRunner()
        .withUserConfiguration(CamundaHubClientStarter.class)
        .withPropertyValues("camunda.hub.client.client-secret=secret")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("camunda.hub.client.client-id"));
  }

  @Test
  void shouldFailWithClearMessageWithoutClientSecret() {
    new ApplicationContextRunner()
        .withUserConfiguration(CamundaHubClientStarter.class)
        .withPropertyValues("camunda.hub.client.client-id=id")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("camunda.hub.client.client-secret"));
  }

  @Test
  void shouldApplyDefaultsForSaasWithoutAnyYamlOnClasspathOfStarter() {
    new ApplicationContextRunner()
        .withUserConfiguration(CamundaHubClientStarter.class)
        .withPropertyValues(
            "camunda.hub.client.client-id=id", "camunda.hub.client.client-secret=secret")
        .run(
            context -> {
              CamundaHubProperties properties = context.getBean(CamundaHubProperties.class);
              assertThat(properties.baseUrl().toString())
                  .isEqualTo("https://hub.camunda.io/api/v2");
              assertThat(properties.authUrl().toString())
                  .isEqualTo("https://login.cloud.camunda.io/oauth/token");
              assertThat(properties.audience()).isEqualTo("api.cloud.camunda.io");
            });
  }

  /**
   * The test classpath contains an {@code application.yaml} (like any consuming application) that
   * shadows one shipped by the starter, so defaults must not depend on a starter-provided file.
   */
  @Test
  void shouldApplyDefaultsWhenApplicationShipsOwnApplicationYaml() {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(CamundaHubClientStarter.class)
            .web(WebApplicationType.NONE)
            .run()) {
      CamundaHubProperties properties = context.getBean(CamundaHubProperties.class);
      assertThat(properties.clientId()).isEqualTo("yaml-id");
      assertThat(properties.baseUrl().toString()).isEqualTo("https://hub.camunda.io/api/v2");
    }
  }

  @Test
  void shouldMapLegacyModelerProperties() {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(CamundaHubClientStarter.class)
            .web(WebApplicationType.NONE)
            .properties("camunda.modeler.client.base-url=https://legacy.example/api/v2")
            .run()) {
      assertThat(context.getBean(ApiClient.class).getBasePath())
          .isEqualTo("https://legacy.example/api/v2");
    }
  }

  @Test
  void shouldCallHubWithTokenFromConfiguredProvider(WireMockRuntimeInfo wireMock) throws Exception {
    stubFor(
        post(urlEqualTo("/oauth/token"))
            .willReturn(
                aResponse().withBody("{\"access_token\":\"spring-token\",\"expires_in\":3600}")));
    stubFor(
        get(urlEqualTo("/api/v2/info"))
            .willReturn(
                aResponse().withBody("{\"version\":\"9.9\",\"authorizedOrganization\":\"o\"}")));

    runner
        .withPropertyValues(
            "camunda.hub.client.base-url=" + wireMock.getHttpBaseUrl() + "/api/v2",
            "camunda.hub.client.auth-url=" + wireMock.getHttpBaseUrl() + "/oauth/token")
        .run(
            context -> {
              assertThat(context.getBean(InfoApi.class).getInfo().getVersion()).isEqualTo("9.9");
              verify(
                  getRequestedFor(urlEqualTo("/api/v2/info"))
                      .withHeader("Authorization", equalTo("Bearer spring-token")));
            });
  }
}
