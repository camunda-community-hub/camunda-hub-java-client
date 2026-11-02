package org.camunda.community.hub;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

public class HubAuthInterceptor implements HttpRequestInterceptor {
  private final ClientCredentialsTokenProvider tokenProvider;

  public HubAuthInterceptor(ClientCredentialsTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public void process(
      HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
    httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getToken());
  }
}
