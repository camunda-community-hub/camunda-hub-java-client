# Contributing

## Requirements

- JDK 21
- Maven 3.9+
- [pre-commit](https://pre-commit.com) (`pip install pre-commit` or `brew install pre-commit`)
- Docker (only for the Dockerfile linter hook)

## Git hooks

Install the hooks once after cloning:

```shell
pre-commit install --install-hooks
```

This installs the `pre-commit`, `commit-msg` and `pre-push` hooks:

- `pre-commit`: whitespace/EOF/YAML/XML/JSON hygiene, spotless on the staged `.java`, `.xml` and `.md` files (auto-fixes), actionlint, hadolint, gitleaks
- `commit-msg`: commit messages must follow [Conventional Commits](https://www.conventionalcommits.org) (`feat:`, `fix(deps):`, `feat!:`, ...)
- `pre-push`: `mvn -q -B verify -Pstrict -DskipTests`

Run all hooks against the whole repository with:

```shell
pre-commit run --all-files
```

`camunda-hub-openapi-java-client/openapi.json` is a vendored spec and is never modified by a hook.

CI runs the same hooks (`pre-commit run --all-files --show-diff-on-failure`).

## Strict build

```shell
mvn verify -Pstrict
```

The `strict` profile checks the Java/Maven version, duplicate dependency versions, banned dependencies (`io.camunda.spring:java-common`, `io.camunda:identity-sdk`, `junit:junit`), dependency convergence, formatting (`spotless:check`) and unused/undeclared dependencies (`dependency:analyze-only`). CI runs this profile.

Without the profile, `mvn verify` formats the sources with spotless.
