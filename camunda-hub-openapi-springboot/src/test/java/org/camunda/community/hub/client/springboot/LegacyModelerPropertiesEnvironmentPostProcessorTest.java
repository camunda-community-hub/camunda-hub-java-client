package org.camunda.community.hub.client.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class LegacyModelerPropertiesEnvironmentPostProcessorTest {

  private final Log log = mock(Log.class);
  private final DeferredLogFactory logFactory = supplier -> log;
  private final LegacyModelerPropertiesEnvironmentPostProcessor processor =
      new LegacyModelerPropertiesEnvironmentPostProcessor(logFactory);

  private void process(MockEnvironment environment) {
    processor.postProcessEnvironment(environment, new SpringApplication());
  }

  @ParameterizedTest
  @CsvSource({
    "base-url,https://legacy.example/api",
    "auth-url,https://legacy.example/token",
    "audience,legacy-audience",
    "client-id,legacy-id",
    "client-secret,legacy-secret"
  })
  void shouldMapLegacyProperty(String name, String value) {
    MockEnvironment environment =
        new MockEnvironment().withProperty("camunda.modeler.client." + name, value);

    process(environment);

    assertEquals(value, environment.getProperty("camunda.hub.client." + name));
    verify(log).warn(contains("'camunda.modeler.client." + name + "' is deprecated"));
  }

  @Test
  void shouldMapLegacyEnvironmentVariables() {
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .replace(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of(
                    "CAMUNDA_MODELER_CLIENT_BASE_URL", "https://env.example/api",
                    "CAMUNDA_MODELER_CLIENT_CLIENT_ID", "env-id")));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertEquals("https://env.example/api", environment.getProperty("camunda.hub.client.base-url"));
    assertEquals("env-id", environment.getProperty("camunda.hub.client.client-id"));
  }

  /**
   * The CLI reads {@code CAMUNDA_MODELER_CLIENT_BASEURL}, but that is not a relaxed-binding form of
   * {@code base-url}, so the Spring Boot starter does not pick it up.
   */
  @Test
  void shouldNotMapCliStyleEnvironmentVariableWithoutUnderscore() {
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .replace(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("CAMUNDA_MODELER_CLIENT_BASEURL", "https://env.example/api")));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertNull(environment.getProperty("camunda.hub.client.base-url"));
  }

  @Test
  void shouldKeepExplicitHubValueOverLegacyValue() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("camunda.modeler.client.client-id", "legacy")
            .withProperty("camunda.hub.client.client-id", "hub")
            .withProperty("camunda.modeler.client.client-secret", "legacy-secret");

    process(environment);

    assertEquals("hub", environment.getProperty("camunda.hub.client.client-id"));
    assertEquals("legacy-secret", environment.getProperty("camunda.hub.client.client-secret"));
    verify(log, never()).warn(contains("client-id"));
  }

  @Test
  void shouldRegisterMappedValuesWithLowestPrecedence() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("camunda.modeler.client.client-id", "legacy");

    process(environment);

    PropertySource<?> last = null;
    for (PropertySource<?> source : environment.getPropertySources()) {
      last = source;
    }
    assertEquals(
        LegacyModelerPropertiesEnvironmentPostProcessor.PROPERTY_SOURCE_NAME, last.getName());
  }

  @Test
  void shouldDoNothingWithoutLegacyProperties() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("camunda.hub.client.client-id", "hub");

    process(environment);

    assertFalse(
        environment
            .getPropertySources()
            .contains(LegacyModelerPropertiesEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    verify(log, never()).warn(contains("deprecated"));
  }

  @Test
  void shouldBeRegisteredViaSpringFactories() {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(EmptyConfiguration.class)
            .web(WebApplicationType.NONE)
            .properties("camunda.modeler.client.audience=legacy-audience")
            .run()) {
      assertEquals(
          "legacy-audience", context.getEnvironment().getProperty("camunda.hub.client.audience"));
      assertTrue(
          context
              .getEnvironment()
              .getPropertySources()
              .contains(LegacyModelerPropertiesEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    }
  }

  @org.springframework.context.annotation.Configuration
  static class EmptyConfiguration {}
}
