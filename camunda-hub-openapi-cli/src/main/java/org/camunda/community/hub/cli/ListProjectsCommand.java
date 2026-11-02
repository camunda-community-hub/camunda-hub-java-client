package org.camunda.community.hub.cli;

import org.camunda.community.hub.client.dto.ProjectSearchQuery;
import org.camunda.community.hub.client.dto.SearchQueryPage;
import picocli.CommandLine;

@CommandLine.Command(
    name = "listProjects",
    description = "Prints out projects in the organization.",
    mixinStandardHelpOptions = true)
public class ListProjectsCommand extends AbstractCommand {

  @CommandLine.Option(
      names = {"--page", "-pg"},
      description = "page number of the results to fetch, defaults to 0",
      scope = CommandLine.ScopeType.LOCAL)
  int page;

  @Override
  protected Object createResponse() throws Exception {
    ProjectSearchQuery query =
        new ProjectSearchQuery()
            .page(
                new SearchQueryPage()
                    .limit(CommandLineApp.MAX_PAGE_SIZE)
                    .from(page * CommandLineApp.MAX_PAGE_SIZE));
    return parent.buildProjectClient().searchProjects(query).getItems();
  }
}
