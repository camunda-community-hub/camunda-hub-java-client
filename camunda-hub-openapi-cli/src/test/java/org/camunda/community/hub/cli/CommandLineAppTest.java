package org.camunda.community.hub.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_BASEURL")
@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_AUTHURL")
@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_AUDIENCE")
@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTID")
@ClearEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTSECRET")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_BASEURL")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_AUTHURL")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_AUDIENCE")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTID")
@ClearEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTSECRET")
class CommandLineAppTest {

  private static CommandLineApp parse(String... args) {
    CommandLineApp app = new CommandLineApp();
    new CommandLine(app).parseArgs(args);
    return app;
  }

  @Test
  void shouldApplyDefaultsForSaas() {
    CommandLineApp app = parse("info", "--clientId", "id", "--clientSecret", "secret");

    assertEquals("https://hub.camunda.io/api/v2", app.baseUrl);
    assertEquals("https://login.cloud.camunda.io/oauth/token", app.authUrl);
    assertEquals("api.cloud.camunda.io", app.audience);
  }

  @Test
  void shouldParseAllGlobalOptions() {
    CommandLineApp app =
        parse(
            "info",
            "--baseUrl",
            "https://b",
            "--authUrl",
            "https://a",
            "--audience",
            "aud",
            "--clientId",
            "id",
            "--clientSecret",
            "secret");

    assertEquals("https://b", app.baseUrl);
    assertEquals("https://a", app.authUrl);
    assertEquals("aud", app.audience);
    assertEquals("id", app.clientId);
    assertEquals("secret", app.clientSecret);
  }

  @Test
  void shouldAcceptBaseUrlAliases() {
    assertEquals(
        "https://x",
        parse("info", "-b", "https://x", "--clientId", "i", "--clientSecret", "s").baseUrl);
    assertEquals(
        "https://y",
        parse("info", "--basePath", "https://y", "--clientId", "i", "--clientSecret", "s").baseUrl);
  }

  @Test
  void shouldRequireClientIdAndSecret() {
    CliTestSupport.Result missingId = CliTestSupport.run("info", "--clientSecret", "s");
    CliTestSupport.Result missingSecret = CliTestSupport.run("info", "--clientId", "i");

    assertEquals(2, missingId.exitCode());
    assertTrue(missingId.err().contains("--clientId"), missingId.err());
    assertEquals(2, missingSecret.exitCode());
    assertTrue(missingSecret.err().contains("--clientSecret"), missingSecret.err());
  }

  @Test
  void shouldRequireProjectOptionForListFiles() {
    CliTestSupport.Result result =
        CliTestSupport.run("listFiles", "--clientId", "i", "--clientSecret", "s");

    assertEquals(2, result.exitCode());
    assertTrue(result.err().contains("--project"), result.err());
  }

  @Test
  void shouldRequireFileOptionForDownloadFile() {
    CliTestSupport.Result result =
        CliTestSupport.run("downloadFile", "--clientId", "i", "--clientSecret", "s");

    assertEquals(2, result.exitCode());
    assertTrue(result.err().contains("--file"), result.err());
  }

  @Test
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_BASEURL", value = "https://hub-env")
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_AUTHURL", value = "https://hub-auth")
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_AUDIENCE", value = "hub-aud")
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTID", value = "hub-id")
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTSECRET", value = "hub-secret")
  void shouldReadHubEnvironmentVariables() {
    CommandLineApp app = parse("info");

    assertEquals("https://hub-env", app.baseUrl);
    assertEquals("https://hub-auth", app.authUrl);
    assertEquals("hub-aud", app.audience);
    assertEquals("hub-id", app.clientId);
    assertEquals("hub-secret", app.clientSecret);
  }

  @Test
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_BASEURL", value = "https://modeler-env")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_AUTHURL", value = "https://modeler-auth")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_AUDIENCE", value = "modeler-aud")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTID", value = "modeler-id")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTSECRET", value = "modeler-secret")
  void shouldFallBackToLegacyModelerEnvironmentVariables() {
    CommandLineApp app = parse("info");

    assertEquals("https://modeler-env", app.baseUrl);
    assertEquals("https://modeler-auth", app.authUrl);
    assertEquals("modeler-aud", app.audience);
    assertEquals("modeler-id", app.clientId);
    assertEquals("modeler-secret", app.clientSecret);
  }

  @Test
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTID", value = "hub-id")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTID", value = "modeler-id")
  @SetEnvironmentVariable(key = "CAMUNDA_MODELER_CLIENT_CLIENTSECRET", value = "modeler-secret")
  void shouldPreferHubOverLegacyEnvironmentVariable() {
    CommandLineApp app = parse("info");

    assertEquals("hub-id", app.clientId);
    assertEquals("modeler-secret", app.clientSecret);
  }

  @Test
  @SetEnvironmentVariable(key = "CAMUNDA_HUB_CLIENT_CLIENTID", value = "hub-id")
  void shouldPreferCommandLineOverEnvironmentVariable() {
    CommandLineApp app = parse("info", "--clientId", "cli-id", "--clientSecret", "s");

    assertEquals("cli-id", app.clientId);
  }

  @Test
  void shouldRegisterAllSubcommands() {
    ParseResult result =
        new CommandLine(new CommandLineApp())
            .parseArgs("listProjects", "--clientId", "i", "--clientSecret", "s");

    assertEquals("listProjects", result.subcommand().commandSpec().name());
    assertNull(result.subcommand().matchedOption("--page"));
    assertEquals(
        java.util.List.of("info", "listProjects", "listFiles", "downloadFile", "downloadProject"),
        new CommandLine(new CommandLineApp()).getSubcommands().keySet().stream().toList());
  }
}
