package org.camunda.community.hub.cli;

import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
import org.camunda.community.hub.ClientCredentialsTokenProvider;
import org.camunda.community.hub.HubAuthInterceptor;
import org.camunda.community.hub.client.api.*;
import org.camunda.community.hub.client.invoker.ApiClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    description = "Camunda Hub CLI",
    mixinStandardHelpOptions = true,
    subcommands = {
      InfoCommand.class,
      ListProjectsCommand.class,
      ListFilesCommand.class,
      DownloadFileCommand.class,
      DownloadProjectCommand.class
    })
public class CommandLineApp {

  public static final int MAX_PAGE_SIZE = 50;

  @Option(
      names = {"--basePath", "-b", "--baseUrl"},
      description = "base url of API (default to Camunda Hub SaaS url)",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue = "${CAMUNDA_HUB_CLIENT_BASEURL:-${CAMUNDA_MODELER_CLIENT_BASEURL:-https://hub.camunda.io/api/v2}}",
      required = true)
  String baseUrl;

  @Option(
      names = {"--authUrl"},
      description = "token endpoint or OIDC issuer URL (default to Camunda SaaS token url)",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue =
          "${CAMUNDA_HUB_CLIENT_AUTHURL:-${CAMUNDA_MODELER_CLIENT_AUTHURL:-https://login.cloud.camunda.io/oauth/token}}",
      required = true)
  String authUrl;

  @Option(
      names = {"--audience"},
      description = "audience of API (default to Camunda Saas audience)",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue = "${CAMUNDA_HUB_CLIENT_AUDIENCE:-${CAMUNDA_MODELER_CLIENT_AUDIENCE:-api.cloud.camunda.io}}",
      required = true)
  String audience;

  @Option(
      names = {"--clientId"},
      description = "client id",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue = "${CAMUNDA_HUB_CLIENT_CLIENTID:-${CAMUNDA_MODELER_CLIENT_CLIENTID}}",
      required = true)
  String clientId;

  @Option(
      names = {"--clientSecret"},
      description = "client secret",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue = "${CAMUNDA_HUB_CLIENT_CLIENTSECRET:-${CAMUNDA_MODELER_CLIENT_CLIENTSECRET}}",
      required = true)
  String clientSecret;

  public static void main(String[] args) {
    new CommandLine(new CommandLineApp()).execute(args);
  }

  private ApiClient buildApiClient() {
    ClientCredentialsTokenProvider tokenProvider =
        new ClientCredentialsTokenProvider(authUrl, clientId, clientSecret, audience);
    return ApiClientFactory.getInstance()
        .get(new ApiClientConfiguration(baseUrl, new HubAuthInterceptor(tokenProvider)));
  }

  protected FileApi buildFileClient() {
    return new FileApi(buildApiClient());
  }

  protected FolderApi buildFolderClient() {
    return new FolderApi(buildApiClient());
  }

  protected InfoApi buildInfoClient() {
    return new InfoApi(buildApiClient());
  }

  protected ProjectApi buildProjectClient() {
    return new ProjectApi(buildApiClient());
  }
}
