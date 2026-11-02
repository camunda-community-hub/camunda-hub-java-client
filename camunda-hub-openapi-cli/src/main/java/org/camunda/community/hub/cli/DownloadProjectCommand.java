package org.camunda.community.hub.cli;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.camunda.community.hub.download.Downloader;
import org.camunda.community.hub.download.Downloader.Mode;
import picocli.CommandLine;

@CommandLine.Command(
    name = "downloadProject",
    description = "Downloads all files of the project.",
    mixinStandardHelpOptions = true)
public class DownloadProjectCommand extends AbstractCommand {

  @CommandLine.Option(
      names = {"--project", "-p"},
      description = "project key",
      scope = CommandLine.ScopeType.LOCAL,
      required = true)
  String projectKey;

  @CommandLine.Option(
      names = {"--output", "-o"},
      description = "output folder",
      scope = CommandLine.ScopeType.LOCAL)
  String output;

  @CommandLine.Option(
      names = {"--mode", "-m"},
      description = "Overwrite mode for download. Valid values are: ${COMPLETION-CANDIDATES}",
      defaultValue = "SIMPLE_PATH",
      scope = CommandLine.ScopeType.LOCAL)
  Mode mode;

  @Override
  protected Object createResponse() throws Exception {
    final File folder;
    if (output == null) {
      folder = new File(".");
    } else {
      folder = new File(output);
      FileUtils.forceMkdir(folder);
    }
    Downloader downloader = new Downloader(parent.buildFileClient(), folder);
    downloader.downloadProject(projectKey, mode);
    return null;
  }
}
