package org.camunda.community.hub.download;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
import org.camunda.community.hub.client.api.FileApi;
import org.camunda.community.hub.download.Downloader.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@WireMockTest
class DownloaderTest {

  @TempDir Path temp;

  private Path output;
  private Downloader downloader;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) throws IOException {
    output = Files.createDirectories(temp.resolve("sandbox/output"));
    FileApi fileApi =
        new FileApi(
            ApiClientFactory.getInstance()
                .get(
                    new ApiClientConfiguration(
                        wireMock.getHttpBaseUrl() + "/api/v2",
                        new org.camunda.community.hub.HubAuthInterceptor(() -> "t"))));
    downloader = new Downloader(fileApi, output.toFile());
  }

  private static String file(
      String key,
      String name,
      String type,
      String simplePath,
      String canonicalPath,
      String content) {
    return """
        {"fileKey":"%s","name":"%s","type":"%s","simplePath":%s,"canonicalPath":%s,"content":"%s"}
        """
        .formatted(
            key,
            name,
            type,
            simplePath == null ? "null" : "\"" + simplePath + "\"",
            canonicalPath == null ? "null" : "\"" + canonicalPath + "\"",
            content);
  }

  /** Serves pages of search results keyed by the {@code from} offset, then an empty page. */
  private static void stubSearch(Map<Integer, List<String>> pages) {
    pages.forEach(
        (from, files) ->
            stubFor(
                post(urlEqualTo("/api/v2/files/search"))
                    .withRequestBody(equalToJson("{\"page\":{\"from\":" + from + "}}", true, true))
                    .willReturn(
                        aResponse()
                            .withBody(
                                "{\"items\":["
                                    + files.stream()
                                        .map(f -> "{\"fileKey\":\"" + f + "\"}")
                                        .collect(Collectors.joining(","))
                                    + "]}"))));
  }

  private static void stubFile(String key, String body) {
    stubFor(get(urlEqualTo("/api/v2/files/" + key)).willReturn(aResponse().withBody(body)));
  }

  private String read(String relativePath) throws IOException {
    return Files.readString(output.resolve(relativePath), StandardCharsets.UTF_8);
  }

  @Test
  void shouldWriteFilesBySimplePath() throws Exception {
    stubSearch(Map.of(0, List.of("k1", "k2"), 50, List.of()));
    stubFile("k1", file("k1", "a", "bpmn", "folder/a.bpmn", "x/y", "<bpmn/>"));
    stubFile("k2", file("k2", "b", "dmn", "b.dmn", "x/y", "<dmn/>"));

    downloader.downloadProject("p1", Mode.SIMPLE_PATH);

    assertEquals("<bpmn/>", read("folder/a.bpmn"));
    assertEquals("<dmn/>", read("b.dmn"));
  }

  @Test
  void shouldStopPagingOnEmptyPage() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of("k2"), 100, List.of()));
    stubFile("k1", file("k1", "a", "bpmn", "a.bpmn", null, "1"));
    stubFile("k2", file("k2", "b", "bpmn", "b.bpmn", null, "2"));

    downloader.downloadProject("p1", Mode.SIMPLE_PATH);

    assertEquals("1", read("a.bpmn"));
    assertEquals("2", read("b.bpmn"));
    verify(3, postRequestedFor(urlEqualTo("/api/v2/files/search")));
  }

  @Test
  void shouldWriteNothingForEmptyProject() throws Exception {
    stubSearch(Map.of(0, List.of()));

    downloader.downloadProject("p1", Mode.SIMPLE_PATH);

    try (Stream<Path> files = Files.list(output)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void shouldWriteFilesByCanonicalPathWithKeyInName() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "Order", "bpmn", "ignored.bpmn", "ws/proj/folder", "<bpmn/>"));

    downloader.downloadProject("p1", Mode.CANONICAL_PATH);

    assertEquals("<bpmn/>", read("ws/proj/folder/Order[k1].bpmn"));
  }

  @Test
  void shouldPlaceCanonicalFileInRootWhenCanonicalPathIsNullOrEmpty() throws Exception {
    stubSearch(Map.of(0, List.of("k1", "k2"), 50, List.of()));
    stubFile("k1", file("k1", "A", "form", null, null, "1"));
    stubFile("k2", file("k2", "B", "form", null, "", "2"));

    downloader.downloadProject("p1", Mode.CANONICAL_PATH);

    assertEquals("1", read("A[k1].form"));
    assertEquals("2", read("B[k2].form"));
  }

  @Test
  void shouldMapFileTypesToExtensions() throws Exception {
    stubSearch(Map.of(0, List.of("t", "m", "d", "r"), 50, List.of()));
    stubFile("t", file("t", "T", "element-template", null, null, "{}"));
    stubFile("m", file("m", "M", "markdown", null, null, "# hi"));
    stubFile("d", file("d", "D", "dmn", null, null, "<dmn/>"));
    stubFile("r", file("r", "R", "rpa", null, null, "{}"));

    downloader.downloadProject("p1", Mode.CANONICAL_PATH);

    assertTrue(Files.exists(output.resolve("T[t].json")));
    assertTrue(Files.exists(output.resolve("M[m].md")));
    assertTrue(Files.exists(output.resolve("D[d].dmn")));
    assertTrue(Files.exists(output.resolve("R[r].rpa")));
  }

  @Test
  void shouldRejectNullMode() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "A", "bpmn", "a.bpmn", null, "1"));

    assertThrows(IllegalArgumentException.class, () -> downloader.downloadProject("p1", null));
  }

  @Test
  void shouldNotWriteOutsideOutputFolderForRelativeSimplePath() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "evil", "bpmn", "../escaped.bpmn", null, "pwned"));

    assertThrows(
        IllegalArgumentException.class, () -> downloader.downloadProject("p1", Mode.SIMPLE_PATH));

    assertFalse(Files.exists(output.resolveSibling("escaped.bpmn")));
  }

  @Test
  void shouldNotWriteOutsideOutputFolderForDeepTraversal() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "evil", "bpmn", "a/../../escaped.bpmn", null, "pwned"));

    assertThrows(
        IllegalArgumentException.class, () -> downloader.downloadProject("p1", Mode.SIMPLE_PATH));

    assertFalse(Files.exists(output.resolveSibling("escaped.bpmn")));
  }

  @Test
  void shouldNotWriteOutsideOutputFolderForCanonicalPath() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "evil", "bpmn", null, "../../outside", "pwned"));

    assertThrows(
        IllegalArgumentException.class,
        () -> downloader.downloadProject("p1", Mode.CANONICAL_PATH));

    assertFalse(Files.exists(output.getParent().resolveSibling("outside")));
  }

  @Test
  void shouldNotWriteToAbsoluteSimplePath() throws Exception {
    Path target = temp.resolve("absolute.bpmn");
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "evil", "bpmn", target.toString(), null, "pwned"));

    assertThrows(
        IllegalArgumentException.class, () -> downloader.downloadProject("p1", Mode.SIMPLE_PATH));

    assertFalse(Files.exists(target));
  }

  @Test
  void shouldAllowDotSegmentsThatStayInsideOutputFolder() throws Exception {
    stubSearch(Map.of(0, List.of("k1"), 50, List.of()));
    stubFile("k1", file("k1", "ok", "bpmn", "a/../b/./c.bpmn", null, "fine"));

    downloader.downloadProject("p1", Mode.SIMPLE_PATH);

    assertEquals("fine", read("b/c.bpmn"));
  }
}
