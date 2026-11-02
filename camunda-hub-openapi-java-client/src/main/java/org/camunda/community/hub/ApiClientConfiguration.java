package org.camunda.community.hub;

public record ApiClientConfiguration(String baseUrl, HubAuthInterceptor authInterceptor) {}
