package org.camunda.community.hub;

import java.util.Objects;
import java.util.function.Supplier;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/** Adds an {@code Authorization: Bearer <token>} header to every outgoing request. */
public class HubAuthInterceptor implements HttpRequestInterceptor {
  private final Supplier<String> tokenSupplier;

  /**
   * @param tokenSupplier supplies the access token, called once per request
   */
  public HubAuthInterceptor(Supplier<String> tokenSupplier) {
    this.tokenSupplier = Objects.requireNonNull(tokenSupplier);
  }

  public HubAuthInterceptor(ClientCredentialsTokenProvider tokenProvider) {
    this(tokenProvider::getToken);
  }

  @Override
  public void process(
      HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
    httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSupplier.get());
  }
}
