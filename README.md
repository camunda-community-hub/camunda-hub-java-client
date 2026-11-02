# camunda-hub-java-client
Java Client for the Camunda Hub (based on its OpenAPI spec, see the [SaaS](https://docs.camunda.io/docs/apis-tools/hub-api-saas/overview/) and [Self-Managed](https://docs.camunda.io/docs/apis-tools/hub-api-sm/overview/) API docs)

## How to use

In Spring Boot

Add dependency to Maven `pom.xml`:

```xml
  <dependency>
    <groupId>org.camunda.community.hub</groupId>
    <artifactId>camunda-hub-openapi-springboot</artifactId>
    <version>1.0.0</version>
  </dependency>
```

Add `application.yaml`:

```yaml
camunda:
  hub:
    client:
      base-url: https://hub.camunda.io/api/v2
      auth-url: https://login.cloud.camunda.io/oauth/token
      audience: api.cloud.camunda.io
      client-id: <YOUR_CLIENDID>
      client-secret: <YOUR_CLIENTSECRET>
```

Now you can just inject the API:

```java
    @Autowired
    private CamundaHubApi camundaHubApi;

    public void printProjects() {
        System.out.println(
                camundaHubApi.projectsApi().listProjects());
    }
```

### Migrating from `camunda.modeler.client.*`

The former `camunda.modeler.client.*` properties (and `CAMUNDA_MODELER_CLIENT_*` environment variables) are still read and mapped to `camunda.hub.client.*`, but are deprecated. A value set under `camunda.hub.client.*` takes precedence.
