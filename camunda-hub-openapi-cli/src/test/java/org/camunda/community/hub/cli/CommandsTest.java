package org.camunda.community.hub.cli;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;

@WireMockTest
@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_BASEURL")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_BASEURL")
class CommandsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private String url;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) {
    url = wireMock.getHttpBaseUrl();
  }

  @Test
  void infoPrintsInfoAsJson() throws Exception {
    stubFor(
        get(urlEqualTo("/api/v2/info"))
            .willReturn(
                aResponse().withBody("{\"version\":\"1.0\",\"authorizedOrganization\":\"org\"}")));

    CliTestSupport.Result result = CliTestSupport.runAgainst(url, "info");

    assertEquals(0, result.exitCode(), result.err());
    JsonNode json = MAPPER.readTree(result.out());
    assertEquals("1.0", json.get("version").asText());
    assertEquals("org", json.get("authorizedOrganization").asText());
    verify(
        getRequestedFor(urlEqualTo("/api/v2/info"))
            .withHeader("Authorization", equalTo("Bearer cli-token")));
  }

  @Test
  void listProjectsPrintsItemsAndUsesFirstPageByDefault() throws Exception {
    stubFor(
        post(urlEqualTo("/api/v2/projects/search"))
            .willReturn(
                aResponse().withBody("{\"items\":[{\"projectKey\":\"p1\",\"name\":\"One\"}]}")));

    CliTestSupport.Result result = CliTestSupport.runAgainst(url, "listProjects");

    assertEquals(0, result.exitCode(), result.err());
    JsonNode json = MAPPER.readTree(result.out());
    assertEquals(1, json.size());
    assertEquals("p1", json.get(0).get("projectKey").asText());
    verify(
        postRequestedFor(urlEqualTo("/api/v2/projects/search"))
            .withRequestBody(equalToJson("{\"page\":{\"limit\":50,\"from\":0}}", true, true)));
  }

  @Test
  void listProjectsMapsPageToOffset() {
    stubFor(
        post(urlEqualTo("/api/v2/projects/search"))
            .willReturn(aResponse().withBody("{\"items\":[]}")));

    CliTestSupport.Result result = CliTestSupport.runAgainst(url, "listProjects", "--page", "3");

    assertEquals(0, result.exitCode(), result.err());
    verify(
        postRequestedFor(urlEqualTo("/api/v2/projects/search"))
            .withRequestBody(equalToJson("{\"page\":{\"limit\":50,\"from\":150}}", true, true)));
  }

  @Test
  void listFilesFiltersByProjectAndMapsPage() throws Exception {
    stubFor(
        post(urlEqualTo("/api/v2/files/search"))
            .willReturn(aResponse().withBody("{\"items\":[{\"fileKey\":\"f1\",\"name\":\"a\"}]}")));

    CliTestSupport.Result result =
        CliTestSupport.runAgainst(url, "listFiles", "-p", "proj-1", "-pg", "2");

    assertEquals(0, result.exitCode(), result.err());
    assertEquals("f1", MAPPER.readTree(result.out()).get(0).get("fileKey").asText());
    verify(
        postRequestedFor(urlEqualTo("/api/v2/files/search"))
            .withRequestBody(
                equalToJson(
                    "{\"page\":{\"limit\":50,\"from\":100},\"filter\":{\"projectKey\":{\"$eq\":\"proj-1\"}}}",
                    true,
                    true)));
  }

  @Test
  void downloadFilePrintsFile() throws Exception {
    stubFor(
        get(urlEqualTo("/api/v2/files/f1"))
            .willReturn(
                aResponse()
                    .withBody("{\"fileKey\":\"f1\",\"type\":\"bpmn\",\"content\":\"<bpmn/>\"}")));

    CliTestSupport.Result result = CliTestSupport.runAgainst(url, "downloadFile", "--file", "f1");

    assertEquals(0, result.exitCode(), result.err());
    assertEquals("<bpmn/>", MAPPER.readTree(result.out()).get("content").asText());
  }

  @Test
  void downloadProjectWritesFilesToOutputFolder(@TempDir Path temp) throws Exception {
    stubFor(
        post(urlEqualTo("/api/v2/files/search"))
            .willReturn(aResponse().withBody("{\"items\":[{\"fileKey\":\"f1\"}]}")));
    stubFor(
        post(urlEqualTo("/api/v2/files/search"))
            .withRequestBody(equalToJson("{\"page\":{\"from\":50}}", true, true))
            .willReturn(aResponse().withBody("{\"items\":[]}")));
    stubFor(
        get(urlEqualTo("/api/v2/files/f1"))
            .willReturn(
                aResponse()
                    .withBody(
                        "{\"fileKey\":\"f1\",\"name\":\"a\",\"type\":\"bpmn\","
                            + "\"simplePath\":\"dir/a.bpmn\",\"canonicalPath\":\"w/p\","
                            + "\"content\":\"<bpmn/>\"}")));
    Path output = temp.resolve("out");

    CliTestSupport.Result simple =
        CliTestSupport.runAgainst(url, "downloadProject", "-p", "p1", "-o", output.toString());
    CliTestSupport.Result canonical =
        CliTestSupport.runAgainst(
            url, "downloadProject", "-p", "p1", "-o", output.toString(), "-m", "CANONICAL_PATH");

    assertEquals(0, simple.exitCode(), simple.err());
    assertEquals(0, canonical.exitCode(), canonical.err());
    assertEquals("<bpmn/>", Files.readString(output.resolve("dir/a.bpmn")));
    assertEquals("<bpmn/>", Files.readString(output.resolve("w/p/a[f1].bpmn")));
  }

  @Test
  void downloadProjectReturnsErrorWhenServerPathEscapesOutputFolder(@TempDir Path temp)
      throws Exception {
    stubFor(
        post(urlEqualTo("/api/v2/files/search"))
            .willReturn(aResponse().withBody("{\"items\":[{\"fileKey\":\"f1\"}]}")));
    stubFor(
        post(urlEqualTo("/api/v2/files/search"))
            .withRequestBody(equalToJson("{\"page\":{\"from\":50}}", true, true))
            .willReturn(aResponse().withBody("{\"items\":[]}")));
    stubFor(
        get(urlEqualTo("/api/v2/files/f1"))
            .willReturn(
                aResponse()
                    .withBody(
                        "{\"fileKey\":\"f1\",\"type\":\"bpmn\",\"simplePath\":\"../evil.bpmn\","
                            + "\"content\":\"x\"}")));
    Path output = Files.createDirectories(temp.resolve("sandbox/out"));

    CliTestSupport.Result result =
        CliTestSupport.runAgainst(url, "downloadProject", "-p", "p1", "-o", output.toString());

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("outside of the output folder"), result.err());
    assertTrue(Files.notExists(temp.resolve("sandbox/evil.bpmn")));
  }

  @Test
  void apiErrorYieldsExitCodeOneAndStackTraceOnStderr() {
    stubFor(get(urlEqualTo("/api/v2/info")).willReturn(aResponse().withStatus(500)));

    CliTestSupport.Result result = CliTestSupport.runAgainst(url, "info");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("ApiException"), result.err());
    assertEquals("", result.out());
  }

  @Test
  void authenticationFailureYieldsExitCodeOne() {
    stubFor(post(urlEqualTo("/oauth/token")).willReturn(aResponse().withStatus(401)));

    CliTestSupport.Result result =
        CliTestSupport.run(
            "info",
            "--baseUrl",
            url + "/api/v2",
            "--authUrl",
            url + "/oauth/token",
            "--clientId",
            "i",
            "--clientSecret",
            "bad");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("Unable to authenticate"), result.err());
  }
}
