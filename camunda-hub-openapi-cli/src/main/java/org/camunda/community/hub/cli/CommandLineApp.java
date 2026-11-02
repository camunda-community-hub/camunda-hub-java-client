package org.camunda.community.hub.cli;

import static org.camunda.community.hub.ApiClientConfiguration.*;

import io.camunda.common.auth.Authentication;
import io.camunda.common.auth.JwtConfig;
import io.camunda.common.auth.JwtCredential;
import io.camunda.common.auth.Product;
import io.camunda.common.auth.SaaSAuthentication;
import io.camunda.common.auth.SelfManagedAuthentication;
import io.camunda.common.auth.identity.IdentityConfig;
import io.camunda.common.auth.identity.IdentityContainer;
import io.camunda.common.json.JsonMapper;
import io.camunda.common.json.SdkObjectMapper;
import io.camunda.identity.sdk.Identity;
import io.camunda.identity.sdk.IdentityConfiguration;
import io.camunda.identity.sdk.IdentityConfiguration.Type;
import org.camunda.community.hub.ApiClientConfiguration;
import org.camunda.community.hub.ApiClientFactory;
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
      description = "auth url of API (default to Camunda Saas auth url)",
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

  @Option(
      names = {"--authType"},
      description = "auth type, only required for self-managed (default to KEYCLOAK)",
      scope = CommandLine.ScopeType.INHERIT,
      defaultValue = "${CAMUNDA_HUB_CLIENT_AUTHTYPE:-${CAMUNDA_MODELER_CLIENT_AUTHTYPE:-KEYCLOAK}}",
      required = true)
  Type authType;

  public static void main(String[] args) {
    new CommandLine(new CommandLineApp()).execute(args);
  }

  private JsonMapper jsonMapper() {
    return new SdkObjectMapper();
  }

  private JwtCredential hubCredential() {
    return new JwtCredential(clientId, clientSecret, audience, authUrl);
  }

  private JwtConfig jwtConfig(JwtCredential hubCredential) {
    JwtConfig jwtConfig = new JwtConfig();
    jwtConfig.addProduct(Product.WEB_MODELER, hubCredential);
    return jwtConfig;
  }

  private IdentityConfiguration identityConfiguration() {
    return new IdentityConfiguration(
        baseUrl, authUrl, authUrl, clientId, clientSecret, audience, authType.name());
  }

  private Identity identity(IdentityConfiguration identityConfiguration) {
    return new Identity(identityConfiguration);
  }

  private IdentityContainer identityContainer(
      Identity identity, IdentityConfiguration identityConfiguration) {
    return new IdentityContainer(identity, identityConfiguration);
  }

  private IdentityConfig identityConfig(IdentityContainer identityContainer) {
    IdentityConfig identityConfig = new IdentityConfig();
    identityConfig.addProduct(Product.WEB_MODELER, identityContainer);
    return identityConfig;
  }

  private Authentication authentication(
      JsonMapper jsonMapper, JwtConfig jwtConfig, IdentityConfig identityConfig) {
    if (isSaas()) {
      return SaaSAuthentication.builder()
          .withJsonMapper(jsonMapper)
          .withJwtConfig(jwtConfig)
          .build();
    } else {
      return SelfManagedAuthentication.builder()
          .withJwtConfig(jwtConfig)
          .withIdentityConfig(identityConfig)
          .build();
    }
  }

  private boolean isSaas() {
    return authUrl.equals(SAAS_AUTH_URL);
  }

  private ApiClient buildApiClient() {
    JsonMapper jsonMapper = jsonMapper();
    JwtCredential hubCredential = hubCredential();
    JwtConfig jwtConfig = jwtConfig(hubCredential);
    IdentityConfiguration identityConfiguration = identityConfiguration();
    Identity identity = identity(identityConfiguration);
    IdentityContainer identityContainer = identityContainer(identity, identityConfiguration);
    IdentityConfig identityConfig = identityConfig(identityContainer);
    return ApiClientFactory.getInstance()
        .get(
            new ApiClientConfiguration(
                baseUrl,
                new HubAuthInterceptor(
                    authentication(jsonMapper, jwtConfig, identityConfig))));
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
