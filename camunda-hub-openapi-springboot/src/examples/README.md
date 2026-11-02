# Manual smoke programs

`SmokeTestHubClient` and `PrintModels` call a real Camunda Hub and need credentials (see `resources/application.yaml`).
They are not compiled or run by Maven. To try them, copy them into a scratch module (or temporarily into `src/test`) and start `SmokeTestHubClient`.
