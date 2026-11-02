package org.camunda.community.hub.cli;

import org.camunda.community.hub.client.dto.BasicStringFilterProperty;
import org.camunda.community.hub.client.dto.FileFilter;
import org.camunda.community.hub.client.dto.FileSearchQuery;
import org.camunda.community.hub.client.dto.SearchQueryPage;
import picocli.CommandLine;

@CommandLine.Command(
    name = "listFiles",
    description = "Prints out files in project (only metadata, no file contents).",
    mixinStandardHelpOptions = true)
public class ListFilesCommand extends AbstractCommand {

  @CommandLine.Option(
      names = {"--project", "-p"},
      description = "project key",
      scope = CommandLine.ScopeType.LOCAL,
      required = true)
  String projectKey;

  @CommandLine.Option(
      names = {"--page", "-pg"},
      description = "page number of the results to fetch, defaults to 0",
      scope = CommandLine.ScopeType.LOCAL)
  int page;

  @Override
  protected Object createResponse() throws Exception {
    FileSearchQuery query =
        new FileSearchQuery()
            .page(
                new SearchQueryPage()
                    .limit(CommandLineApp.MAX_PAGE_SIZE)
                    .from(page * CommandLineApp.MAX_PAGE_SIZE))
            .filter(new FileFilter().projectKey(new BasicStringFilterProperty().$eq(projectKey)));
    return parent.buildFileClient().searchFiles(query).getItems();
  }
}
