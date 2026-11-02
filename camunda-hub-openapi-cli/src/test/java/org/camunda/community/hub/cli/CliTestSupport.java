package org.camunda.community.hub.cli;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

/** Runs the CLI in-process and captures what it prints. */
final class CliTestSupport {

  record Result(int exitCode, String out, String err) {}

  private CliTestSupport() {}

  static Result run(String... args) {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    try {
      int exitCode = new CommandLine(new CommandLineApp()).execute(args);
      return new Result(
          exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  /** Stubs the token endpoint and returns the global options pointing at the fake Hub. */
  static List<String> connectionArgs(String wireMockUrl, String... command) {
    stubFor(
        post(urlEqualTo("/oauth/token"))
            .willReturn(
                aResponse().withBody("{\"access_token\":\"cli-token\",\"expires_in\":3600}")));
    List<String> args = new ArrayList<>(List.of(command));
    args.addAll(
        List.of(
            "--baseUrl",
            wireMockUrl + "/api/v2",
            "--authUrl",
            wireMockUrl + "/oauth/token",
            "--clientId",
            "cli-id",
            "--clientSecret",
            "cli-secret"));
    return args;
  }

  static Result runAgainst(String wireMockUrl, String... command) {
    return run(connectionArgs(wireMockUrl, command).toArray(String[]::new));
  }
}
