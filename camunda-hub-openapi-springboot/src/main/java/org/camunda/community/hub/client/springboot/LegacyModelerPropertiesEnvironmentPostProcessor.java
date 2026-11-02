package org.camunda.community.hub.client.springboot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps the legacy {@code camunda.modeler.client.*} properties (including their environment variable
 * forms such as {@code CAMUNDA_MODELER_CLIENT_BASE_URL}) to {@code camunda.hub.client.*}.
 *
 * <p>A value that is explicitly set under {@code camunda.hub.client.*} always wins over the legacy
 * one. The mapped values are registered with the lowest precedence, so they never override any
 * other configured value.
 */
public class LegacyModelerPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String LEGACY_PREFIX = "camunda.modeler.client.";
  static final String PREFIX = "camunda.hub.client.";
  static final String PROPERTY_SOURCE_NAME = "camundaHubLegacyModelerProperties";

  private static final List<String> PROPERTY_NAMES =
      List.of("base-url", "auth-url", "audience", "client-id", "client-secret");

  private final Log log;

  public LegacyModelerPropertiesEnvironmentPostProcessor(DeferredLogFactory logFactory) {
    this.log = logFactory.getLog(LegacyModelerPropertiesEnvironmentPostProcessor.class);
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> mapped = new LinkedHashMap<>();
    for (String name : PROPERTY_NAMES) {
      String legacyValue = environment.getProperty(LEGACY_PREFIX + name);
      if (legacyValue != null && environment.getProperty(PREFIX + name) == null) {
        mapped.put(PREFIX + name, legacyValue);
        log.warn(
            "Property '%s' is deprecated, use '%s' instead"
                .formatted(LEGACY_PREFIX + name, PREFIX + name));
      }
    }
    if (!mapped.isEmpty()) {
      environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, mapped));
    }
  }
}
