package org.camunda.community.hub.cli;

import picocli.CommandLine;

@CommandLine.Command(
    name = "downloadFile",
    description = "Prints out file content.",
    mixinStandardHelpOptions = true)
public class DownloadFileCommand extends AbstractCommand {

  @CommandLine.Option(
      names = {"--file", "-f"},
      description = "file key",
      scope = CommandLine.ScopeType.LOCAL,
      required = true)
  String fileKey;

  @Override
  protected Object createResponse() throws Exception {
    return parent.buildFileClient().getFile(fileKey);
  }
}
