package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.ek9lang.cli.SourceFileSupport;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class Ek9LanguageServerTest {
  private static final OsSupport osSupport = new OsSupport(true);
  private static final FileHandling fileHandling = new FileHandling(osSupport);
  private static final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  final String workspaceDirectory = osSupport.getCurrentWorkingDirectory();

  @BeforeAll
  static void setupTempWorkspace() {
    String sourceName = "SinglePackage.ek9";
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
  }

  @AfterAll
  static void tidyUpTempWorkspace() {
    //This will delete stubbed home and stubbed cwd.
    fileHandling.deleteContentsAndBelow(
        new File(new File(osSupport.getUsersHomeDirectory()).getParent()), true);
  }

  @Test
  void testBasicStartup() {
    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    assertNotNull(languageServer);

    languageServer.initialize(newInitializeParams(workspaceDirectory));


  }

  private InitializeParams newInitializeParams(String tempWorkspaceDirectory) {
    InitializeParams params = new InitializeParams();
    var uri = new File(tempWorkspaceDirectory).toURI().toString();
    params.setWorkspaceFolders(List.of(new WorkspaceFolder(uri)));
    return params;
  }

}
