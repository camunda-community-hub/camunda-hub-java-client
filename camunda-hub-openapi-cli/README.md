[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Camunda Hub CLI

Command line interface for Camunda Hub API ([SaaS](https://docs.camunda.io/docs/apis-tools/hub-api-saas/overview/), [Self-Managed](https://docs.camunda.io/docs/apis-tools/hub-api-sm/overview/)).

All base parameters can be populated from the command line as well:

* `CAMUNDA_HUB_CLIENT_BASEURL` -> `--baseUrl`
* `CAMUNDA_HUB_CLIENT_AUTHURL` -> `--authUrl`
* `CAMUNDA_HUB_CLIENT_AUDIENCE` -> `--audience`
* `CAMUNDA_HUB_CLIENT_CLIENTID` -> `--clientId`
* `CAMUNDA_HUB_CLIENT_CLIENTSECRET` -> `--clientSecret`

## Usage

```
Usage: <main class> [-hV] [--audience=<audience>]
                    [--authUrl=<authUrl>] [-b=<baseUrl>]
                    [--clientId=<clientId>] [--clientSecret=<clientSecret>]
                    [-t=<token>] [COMMAND]
Camunda Hub CLI
      --audience=<audience> audience of API (default to Camunda Saas audience)
      --authUrl=<authUrl>   token endpoint or OIDC issuer URL (default to Camunda
                              SaaS token url)
  -b, --baseUrl, --basePath=<baseUrl>
                            base url of API (default to Camunda Hub SaaS
                              url)
      --clientId=<clientId> client id
      --clientSecret=<clientSecret>
                            client secret
  -h, --help                Show this help message and exit.
  -t, --token=<token>       JWT token
  -V, --version             Print version information and exit.
Commands:
  info             Prints out info (permissions, authorized organization).
  listProjects     Prints out projects in the organization.
  listFiles        Prints out files in project (only metadata, no file
                     contents).
  downloadFile     Prints out file content.
  downloadProject  Downloads all files of the project.

```

Program will print out usage help for each subcommand.

### Download Project

```
Usage: <main class> downloadProject [-hV] [--audience=<audience>]
                                   
                                    [--authUrl=<authUrl>] [-b=<baseUrl>]
                                    [--clientId=<clientId>]
                                    [--clientSecret=<clientSecret>] [-m=<mode>]
                                    [-o=<output>] -p=<projectKey> [-t=<token>]
Downloads all files of the project.
      --audience=<audience> audience of API (default to Camunda Saas audience)
      --authUrl=<authUrl>   token endpoint or OIDC issuer URL (default to Camunda
                              SaaS token url)
  -b, --baseUrl, --basePath=<baseUrl>
                            base url of API (default to Camunda Hub SaaS
                              url)
      --clientId=<clientId> client id
      --clientSecret=<clientSecret>
                            client secret
  -h, --help                Show this help message and exit.
  -m, --mode=<mode>         Overwrite mode for download. Valid values are:
                              SIMPLE_PATH, CANONICAL_PATH
  -o, --output=<output>     output folder
  -p, --project=<projectKey>
                            project key
  -t, --token=<token>       JWT token
  -V, --version             Print version information and exit.

```

**Mode**

* `SIMPLE_PATH` - Recommended if you have unique folder and file names in your project. It uses the simple path to
  create files. If a folder name is ambiguous, all folders with the same name get merged. If files are ambiguous, they
  overwrite each other and the last file wins
* `CANONICAL_PATH` - Recommended if your folder and file names are not unique. It uses the canonical path. Folders are
  addressed by their unique IDs and file names are suffixed with the file key (`<name>[<key>]`). This makes names unique, but less pleasant to
  look at.

## Building from source

```sh
mvn package
cd target/
java -jar camunda-hub-cli-*-jar-with-dependencies.jar --help
```

## Contributing

* [Contributing Guide](./CONTRIBUTING.md)
* [Code of Conduct](./CODE_OF_CONDUCT.md)
