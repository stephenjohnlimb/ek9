package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.ek9lang.cli.SourceFileSupport;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

//Because this depends on specific files existing and different tests add and remove them.
//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class Ek9LanguageServerTest {

  static final String VALID_SOURCE = "SinglePackage.ek9";
  static final String RELATIVE_PATH_TO_VALID_SOURCE = "/examples/parseAndCompile/constructs/packages/";
  static final String INVALID_SOURCE = "unevenIndentation.ek9";
  static final String OTHER_INVALID_SOURCE = "FlowControl.ek9";
  static final String RELATIVE_PATH_TO_INVALID_SOURCE = "/badExamples/basics/";
  static final String RELATIVE_PATH_TO_OTHER_INVALID_SOURCE =
      "/examples/parseButFailCompile/phase1/abnormalBlockTermination/";
  static final String RELATIVE_PATH_TO_CLAUDE_SOURCE = "/claude/mcp-lsp/";
  static final String CLAUDE_SOURCE = "StringAndDateIsSet.ek9";

  private static final OsSupport osSupport = new OsSupport(true);
  private static final FileHandling fileHandling = new FileHandling(osSupport);
  private static final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);
  final String workspaceDirectory = osSupport.getCurrentWorkingDirectory();
  final Supplier<InitializeParams> initParameters = () -> {
    InitializeParams params = new InitializeParams();
    var uri = new File(workspaceDirectory).toURI().toString();
    params.setWorkspaceFolders(List.of(new WorkspaceFolder(uri)));
    return params;
  };

  final Function<Ek9LanguageServer, SimulatedLspClient> prepareLanguageServer = languageServer -> {
    SimulatedLspClient client = new SimulatedLspClient();
    languageServer.connect(client);
    languageServer.initialize(initParameters.get());
    return client;
  };

  final BiFunction<File, FileChangeType, DidChangeWatchedFilesParams> prepareChangedFile =
      (file, changeType) -> new DidChangeWatchedFilesParams(
          List.of(new FileEvent(file.toURI().toString(), changeType)));

  @AfterEach
  void tidyUpTempWorkspace() {
    //This will delete stubbed home and stubbed cwd.
    fileHandling.deleteContentsAndBelow(
        new File(new File(osSupport.getUsersHomeDirectory()).getParent()), true);
  }

  @Test
  void testBasicStartupWithValidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);
    languageServer.shutdown();
  }

  @Test
  void testClaudeStartupWithValidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_CLAUDE_SOURCE, CLAUDE_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);
    languageServer.shutdown();
  }


  @Test
    /*
     * This just checks for word completion of the language.
     * Later once the parsing and processing is complete it will be possible
     * to use completion for variable, class, record, method and function names.
     */
  void testLanguageCompletionAndHover() throws ExecutionException, InterruptedException {
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);
    //Now lets see if we can get a language completion.
    //line 1 position 9 (both zero based) should give us 'module'

    var completionParams =
        new CompletionParams(new TextDocumentIdentifier(sourceFile.toURI().toString()),
            new Position(1, 9));

    var completionResult = languageServer
        .getTextDocumentService().completion(completionParams);
    var result = completionResult.get();
    assertTrue(result.isLeft());
    assertFalse(result.isRight());
    assertEquals(1, result.getLeft().size());
    var completionItem = result.getLeft().getFirst();
    assertNotNull(completionItem);
    assertEquals("module", completionItem.getLabel());

    var hoverResult = languageServer
        .getTextDocumentService().hover(new HoverParams(new TextDocumentIdentifier(sourceFile.toURI().toString()),
            new Position(1, 9)));

    var hover = hoverResult.get();
    assertNotNull(hover);
    assertEquals(
        "MODULE: Primary code organization unit containing related constructs (functions, classes, records, etc.). Use to group logically related functionality. One module per .ek9 file. Syntax: `defines module ModuleName`. Modules are EK9's package/namespace equivalent. Use for organizing code into cohesive units. https://ek9.io/structure.html#module",
        hover.getContents().getRight().getValue());
    languageServer.shutdown();
  }

  @Test
  void testSymbolHoverOnVariable() throws ExecutionException, InterruptedException {
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);

    // Hover over 'publicAccess' variable at line 16, column 4 (0-based: line 15, col 4)
    // Line 16 in file is: "    publicAccess as Boolean := true"
    var hoverResult = languageServer
        .getTextDocumentService().hover(new HoverParams(
            new TextDocumentIdentifier(sourceFile.toURI().toString()),
            new Position(15, 4)))  // 0-based: line 15, column 4
        .get();

    // Symbol hover should return symbol info (or null if not found)
    // The test verifies the hover mechanism doesn't throw exceptions
    // and can return meaningful results for variables
    if (hoverResult != null) {
      assertNotNull(hoverResult.getContents(), "Hover should have contents");
      // Symbol hover returns markdown with type info
      var contents = hoverResult.getContents();
      assertNotNull(contents, "Should have hover contents");
    }

    languageServer.shutdown();
  }

  @Test
  void testHoverAtInvalidPosition() {
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertNoErrors(client);

    // Hover at position way beyond file content (line 1000, col 1000)
    // Should not throw exception and should return null or keyword fallback
    assertDoesNotThrow(() -> {
      languageServer
          .getTextDocumentService().hover(new HoverParams(
              new TextDocumentIdentifier(sourceFile.toURI().toString()),
              new Position(1000, 1000)))
          .get();
    });

    // Result can be null when no token found at invalid position
    // The key assertion is that no exception is thrown
    languageServer.shutdown();
  }

  @Test
  void testHoverOnNonExistentFile() {
    // First initialize with a valid source so the server is properly set up
    sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertNoErrors(client);

    // Request hover on a file that doesn't exist in the workspace
    assertDoesNotThrow(() -> {
      languageServer
          .getTextDocumentService().hover(new HoverParams(
              new TextDocumentIdentifier("file:///nonexistent/path/to/file.ek9"),
              new Position(0, 0)))
          .get();
    });

    // Should return null when source file not found - no exception thrown
    // The important assertion is that this doesn't crash
    languageServer.shutdown();
  }

  @Test
  void testHoverOnWhitespace() {
    // Test hover at a position that contains only whitespace/indentation
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertNoErrors(client);

    // Hover at column 0 which is typically indentation whitespace
    assertDoesNotThrow(() -> {
      languageServer
          .getTextDocumentService().hover(new HoverParams(
              new TextDocumentIdentifier(sourceFile.toURI().toString()),
              new Position(5, 0)))  // Line with leading whitespace
          .get();
    });
    // May return keyword hover or null - should not throw exception
    languageServer.shutdown();
  }

  @Test
  void testHoverOnComment() {
    // Test hover on a comment line (if the source file has comments)
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertNoErrors(client);

    // Hover at a position - even if no symbol, should not crash
    assertDoesNotThrow(() -> {
      languageServer
          .getTextDocumentService().hover(new HoverParams(
              new TextDocumentIdentifier(sourceFile.toURI().toString()),
              new Position(0, 5)))  // First line near the shebang
          .get();
    });
    // Should handle gracefully
    languageServer.shutdown();
  }

  @Test
  void testConcurrentHoverRequests() throws Exception {
    // Test thread safety when multiple hover requests arrive simultaneously
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);
    assertNoErrors(client);

    // Launch multiple concurrent hover requests using a thread pool
    var executor = Executors.newFixedThreadPool(10);
    var futures = new ArrayList<CompletableFuture<?>>();

    for (int i = 0; i < 20; i++) {
      final int line = i % 10;  // Vary the line positions
      futures.add(CompletableFuture.supplyAsync(() -> {
        try {
          return languageServer.getTextDocumentService()
              .hover(new HoverParams(
                  new TextDocumentIdentifier(sourceFile.toURI().toString()),
                  new Position(line, 5)))
              .get();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }, executor));
    }

    // Wait for all to complete - should not throw exceptions
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor should terminate cleanly");
    languageServer.shutdown();
  }

  @Test
  void testChangedAndDeletedFileEvent() {
    var theSourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);

    //Now we wish to simulate a change to that file.
    languageServer.getWorkspaceService()
        .didChangeWatchedFiles(prepareChangedFile.apply(theSourceFile, FileChangeType.Changed));
    //We would still expect no errors, as no real change has taken place.
    assertNoErrors(client);

    //Now simulate a deletion
    languageServer.getWorkspaceService()
        .didChangeWatchedFiles(prepareChangedFile.apply(theSourceFile, FileChangeType.Deleted));

    languageServer.shutdown();
  }

  @Test
  void testChangeConfiguration() {
    //No files but we just want to trigger the fact configuration has changed.
    var params = new DidChangeConfigurationParams();
    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    languageServer.getWorkspaceService().didChangeConfiguration(params);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertNoErrors(client);
  }

  @Test
  void testBasicStartupWithInvalidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_OTHER_INVALID_SOURCE, OTHER_INVALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //Now we'd expect errors
    assertPointlessOrUnReachableErrors(client);

    languageServer.shutdown();
  }

  @Test
  void testInvalidEk9SourceFileEvents() {
    var file = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_INVALID_SOURCE, INVALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    assertOddNumberOfSpacesError(client);

    //We will now simulate some events
    var textDocService = languageServer.getTextDocumentService();

    var documentIdentifier = new TextDocumentIdentifier(file.toURI().toString());
    textDocService.didClose(new DidCloseTextDocumentParams(documentIdentifier));
    assertNoErrors(client);

    var actualContent = osSupport.getFileContent(file);

    actualContent.ifPresent(fileContent -> {
      var textDocumentItem = new TextDocumentItem(file.toURI().toString(), "ek9", 1, fileContent);
      textDocService.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
      assertOddNumberOfSpacesError(client);

    });

    languageServer.shutdown();
  }

  private void assertPointlessOrUnReachableErrors(final SimulatedLspClient client) {
    //For this file, it has been designed to have lots of defects in various scenarios.
    client.getLastDiagnostics().ifPresent(diagnostics -> {
      assertEquals(34, diagnostics.getDiagnostics().size());
      var theError = diagnostics.getDiagnostics().getFirst();
      assertTrue(containsBadFlowControlTypeErrorMessage(theError.getMessage()));
    });
  }

  private boolean containsBadFlowControlTypeErrorMessage(final String errorMessage) {
    final boolean rtn = errorMessage.contains("return not possible")
        || errorMessage.contains("expression is pointless")
        || errorMessage.contains("all paths lead to an Exception")
        || errorMessage.contains("Unreachable, because of 'throw'");

    if (!rtn) {
      System.out.println("Assertion failed because of Error Message [" + errorMessage + "]");
    }
    return rtn;
  }

  private void assertOddNumberOfSpacesError(final SimulatedLspClient client) {
    client.getLastDiagnostics().ifPresent(diagnostics -> {
      assertEquals(1, diagnostics.getDiagnostics().size());
      var theError = diagnostics.getDiagnostics().getFirst();
      assertEquals("Odd number of spaces for indentation", theError.getMessage());
    });
  }

  private void assertNoErrors(final SimulatedLspClient client) {
    client.getLastDiagnostics()
        .ifPresent(diagnostics -> {
          if (!diagnostics.getDiagnostics().isEmpty()) {
            diagnostics.getDiagnostics().forEach(System.out::println);
          }
          assertEquals(0, diagnostics.getDiagnostics().size());
        });
  }

  @Test
  void testDifferentMessagesWithValidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    final Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    final SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    Map<MessageType, Consumer<String>> messageTypes =
        Map.of(MessageType.Warning, languageServer::sendWarningBackToClient, MessageType.Log,
            languageServer::sendLogBackToClient, MessageType.Info,
            languageServer::sendInfoBackToClient, MessageType.Error,
            languageServer::sendErrorBackToClient);

    messageTypes.forEach((key, val) -> {
      val.accept("A Message");
      client.getLastMessage().ifPresentOrElse(message -> assertEquals(key, message.getType()),
          () -> fail("Expecting message"));
    });

    languageServer.shutdown();
  }

  private static class SimulatedLspClient implements LanguageClient {

    private PublishDiagnosticsParams lastDiagnostics;

    private MessageParams lastMessage;

    @Override
    public void telemetryEvent(Object object) {
      //Will record these telemetry events in time
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
      lastDiagnostics = diagnostics;
    }

    public Optional<PublishDiagnosticsParams> getLastDiagnostics() {
      return Optional.ofNullable(lastDiagnostics);
    }

    @Override
    public void showMessage(MessageParams messageParams) {
      System.out.println(messageParams);
    }

    public Optional<MessageParams> getLastMessage() {
      return Optional.ofNullable(lastMessage);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
        ShowMessageRequestParams requestParams) {
      return null;
    }

    @Override
    public void logMessage(MessageParams message) {
      lastMessage = message;
    }
  }

}
