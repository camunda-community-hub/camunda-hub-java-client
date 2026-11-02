package org.camunda.community.hub;

public record ApiClientConfiguration(String baseUrl, HubAuthInterceptor authInterceptor) {
  public static final String SAAS_AUTH_URL = "https://login.cloud.camunda.io/oauth/token";
}
