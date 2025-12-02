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
import java.util.stream.Stream;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
  static final String RELATIVE_PATH_TO_LARGE_SOURCE =
      "/examples/bytecodeGeneration/comprehensiveNestedControlFlow/";
  static final String LARGE_SOURCE = "comprehensiveNestedControlFlow.ek9";

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
            new Position(1, 9))).get();

    assertNotNull(hoverResult);
    assertEquals(
        "MODULE: Primary code organization unit containing related constructs (functions, classes, records, etc.). Use to group logically related functionality. One module per .ek9 file. Syntax: `defines module ModuleName`. Modules are EK9's package/namespace equivalent. Use for organizing code into cohesive units. https://ek9.io/structure.html#module",
        hoverResult.getContents().getRight().getValue());
    languageServer.shutdown();
  }

  @Test
  void testSymbolHoverOnVariable() throws ExecutionException, InterruptedException {
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnostics.
    assertNoErrors(client);

    var hoverResult = languageServer
        .getTextDocumentService().hover(new HoverParams(
            new TextDocumentIdentifier(sourceFile.toURI().toString()),
            new Position(15, 4)))
        .get();
    assertNotNull(hoverResult);

    assertNotNull(hoverResult.getContents(), "Hover should have contents");
    // Symbol hover returns markdown with type info
    var contents = hoverResult.getContents();
    assertNotNull(contents, "Should have hover contents");

    languageServer.shutdown();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("hoverEdgeCases")
  void testHoverEdgeCases(String description, String fileUriOverride, int line, int col) {
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_VALID_SOURCE, VALID_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);
    assertNoErrors(client);

    // Use override URI if provided, otherwise use the real source file
    String fileUri = fileUriOverride != null ? fileUriOverride : sourceFile.toURI().toString();

    assertDoesNotThrow(() -> languageServer.getTextDocumentService()
            .hover(new HoverParams(new TextDocumentIdentifier(fileUri), new Position(line, col)))
            .get(),
        "Hover should not throw for: " + description);

    languageServer.shutdown();
  }

  private static Stream<Arguments> hoverEdgeCases() {
    return Stream.of(
        Arguments.of("invalid position beyond file", null, 1000, 1000),
        Arguments.of("non-existent file", "file:///nonexistent/path/to/file.ek9", 0, 0),
        Arguments.of("whitespace position", null, 5, 0),
        Arguments.of("comment/shebang line", null, 0, 5)
    );
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
  void testLargeFileHoverPerformance() throws Exception {
    // Test hover performance on a large file (1238 lines)
    var sourceFile = sourceFileSupport.copyFileToTestCWD(RELATIVE_PATH_TO_LARGE_SOURCE, LARGE_SOURCE);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);
    assertNoErrors(client);

    System.out.println("=== Large File Hover Performance Test ===");
    System.out.println("File: " + LARGE_SOURCE + " (1238 lines)");
    System.out.println("Note: Lines 35-1140 are @BYTECODE directive, actual code starts at line 1141");

    // Test hover at various positions:
    // - Lines in @BYTECODE directive (large embedded text) - tests traversal speed
    // - Lines in actual EK9 program code (line 1141+) - tests symbol lookup
    // Line/Column pairs: line (0-indexed), column (0-indexed)
    int[][] testPositions = {
        {0, 0},      // #!ek9 shebang
        {49, 10},    // Inside @BYTECODE directive
        {199, 10},   // Deep inside @BYTECODE
        {499, 10},   // Middle of @BYTECODE
        {799, 10},   // Later in @BYTECODE
        {1140, 6},   // Line 1141: "    ComprehensiveNestedControlFlow()" - function name
        {1145, 8},   // Line 1146: "      stdout <- Stdout()" - stdout variable
        {1162, 8},   // Line 1163: "      counter <- 0" - counter variable
        {1171, 11},  // Line 1172: "        if counter mod 2 == 0" - counter
        {1211, 8},   // Line 1212: "      stdout.println(...)" - stdout
        {1228, 9},   // Line 1229: "      if grandTotal > 200..." - grandTotal
    };

    long totalTime = 0;
    int foundCount = 0;
    for (int[] pos : testPositions) {
      int line = pos[0];
      int col = pos[1];
      long start = System.nanoTime();

      var hoverResult = languageServer.getTextDocumentService()
          .hover(new HoverParams(
              new TextDocumentIdentifier(sourceFile.toURI().toString()),
              new Position(line, col)))
          .get(10, TimeUnit.SECONDS);

      long durationNs = System.nanoTime() - start;
      long durationMs = durationNs / 1_000_000;
      totalTime += durationMs;

      String resultInfo;
      if (hoverResult != null && hoverResult.getContents() != null) {
        foundCount++;
        resultInfo = "SYMBOL";
      } else if (hoverResult != null) {
        resultInfo = "hover (no content)";
      } else {
        resultInfo = "null";
      }
      System.out.printf("  Line %4d col %2d: %4d ms (%s)%n", line + 1, col, durationMs, resultInfo);

      // Each hover should complete within 2 seconds
      assertTrue(durationMs < 2000,
          "Hover at line " + (line + 1) + " col " + col + " took too long: " + durationMs + "ms");
    }

    System.out.printf("  Symbols found: %d of %d%n", foundCount, testPositions.length);
    System.out.printf("  Average:   %4d ms%n", totalTime / testPositions.length);
    System.out.printf("  Total:     %4d ms for %d hovers%n", totalTime, testPositions.length);

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
