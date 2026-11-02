package org.camunda.community.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.junit.jupiter.api.Test;

class HubAuthInterceptorTest {

  @Test
  void shouldSetBearerHeader() {
    HttpRequest request = new BasicHttpRequest("GET", "/x");

    new HubAuthInterceptor(() -> "abc").process(request, null, null);

    assertEquals("Bearer abc", request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
  }

  @Test
  void shouldReplaceExistingAuthorizationHeader() {
    HttpRequest request = new BasicHttpRequest("GET", "/x");
    request.setHeader(HttpHeaders.AUTHORIZATION, "Basic zzz");

    new HubAuthInterceptor(() -> "abc").process(request, null, null);

    assertEquals(1, request.getHeaders(HttpHeaders.AUTHORIZATION).length);
    assertEquals("Bearer abc", request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
  }

  @Test
  void shouldAskSupplierForEveryRequest() {
    int[] calls = {0};
    HubAuthInterceptor interceptor = new HubAuthInterceptor(() -> "t" + ++calls[0]);
    HttpRequest first = new BasicHttpRequest("GET", "/x");
    HttpRequest second = new BasicHttpRequest("GET", "/x");

    interceptor.process(first, null, null);
    interceptor.process(second, null, null);

    assertEquals("Bearer t1", first.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    assertEquals("Bearer t2", second.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
  }

  @Test
  void shouldRejectNullSupplier() {
    assertThrows(
        NullPointerException.class,
        () -> new HubAuthInterceptor((java.util.function.Supplier<String>) null));
  }
}
