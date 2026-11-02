package org.camunda.community.hub.download;

import static org.camunda.community.hub.download.Downloader.Mode.CANONICAL_PATH;
import static org.camunda.community.hub.download.Downloader.Mode.SIMPLE_PATH;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.camunda.community.hub.cli.CommandLineApp;
import org.camunda.community.hub.client.api.FileApi;
import org.camunda.community.hub.client.dto.BasicStringFilterProperty;
import org.camunda.community.hub.client.dto.FileFilter;
import org.camunda.community.hub.client.dto.FileResult;
import org.camunda.community.hub.client.dto.FileSearchQuery;
import org.camunda.community.hub.client.dto.SearchQueryPage;
import org.camunda.community.hub.client.invoker.ApiException;

public class Downloader {

  private final FileApi fileApi;
  private final File folder;

  public Downloader(FileApi fileApi, File folder) {
    this.fileApi = Objects.requireNonNull(fileApi);
    this.folder = Objects.requireNonNull(folder);
  }

  public void downloadProject(String projectKey, Mode mode) throws ApiException {
    List<FileResult> files = findFiles(projectKey);

    for (FileResult file : files) {
      FileResult fileWithContent = getFile(file.getFileKey());
      if (SIMPLE_PATH.equals(mode)) {
        writeFile(fileWithContent.getContent(), fileWithContent.getSimplePath());
      } else if (CANONICAL_PATH.equals(mode)) {
        writeFile(fileWithContent.getContent(), getCanonicalPath(fileWithContent));
      } else {
        throw new IllegalArgumentException("Unknown mode: " + mode);
      }
    }
  }

  private List<FileResult> findFiles(String projectKey) throws ApiException {
    List<FileResult> result = new ArrayList<>();
    int from = 0;
    while (true) {
      FileSearchQuery query =
          new FileSearchQuery()
              .filter(new FileFilter().projectKey(new BasicStringFilterProperty().$eq(projectKey)))
              .page(new SearchQueryPage().limit(CommandLineApp.MAX_PAGE_SIZE).from(from));
      List<FileResult> items = fileApi.searchFiles(query).getItems();
      if (items == null || items.isEmpty()) {
        return result;
      }
      result.addAll(items);
      from += CommandLineApp.MAX_PAGE_SIZE;
    }
  }

  private String getCanonicalPath(FileResult file) {
    String fileName =
        String.format(
            "%s[%s].%s", file.getName(), file.getFileKey(), toExtension(file.getType()));
    String folderPath = file.getCanonicalPath();
    return folderPath == null || folderPath.isEmpty() ? fileName : folderPath + "/" + fileName;
  }

  private String toExtension(FileResult.TypeEnum type) {
    return switch (type) {
      case ELEMENT_TEMPLATE -> "json";
      case MARKDOWN -> "md";
      default -> type.getValue();
    };
  }

  private void writeFile(String content, String fileName) {
    System.out.println("Writing: " + fileName);
    File outputFile = new File(folder, fileName);

    try {
      FileUtils.write(outputFile, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public FileResult getFile(String fileKey) throws ApiException {
    return fileApi.getFile(fileKey);
  }

  public enum Mode {
    SIMPLE_PATH,
    CANONICAL_PATH
  }
}
