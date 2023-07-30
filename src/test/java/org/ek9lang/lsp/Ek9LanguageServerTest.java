package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

final class Ek9LanguageServerTest {

  final static String validSource = "SinglePackage.ek9";
  final static String relativePathToValidSource = "/examples/constructs/packages/";
  final static String invalidSource = "unevenIndentation.ek9";
  final static String relativePathToInvalidSource = "/badExamples/basics/";
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
    sourceFileSupport.copyFileToTestCWD(relativePathToValidSource, validSource);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnotics.
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
    var sourceFile = sourceFileSupport.copyFileToTestCWD(relativePathToValidSource, validSource);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnotics.
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
    var completionItem = result.getLeft().get(0);
    assertNotNull(completionItem);
    assertEquals("module", completionItem.getLabel());

    var hoverResult = languageServer
        .getTextDocumentService().hover(new HoverParams(new TextDocumentIdentifier(sourceFile.toURI().toString()),
            new Position(1, 9)));

    var hover = hoverResult.get();
    assertNotNull(hover);
    assertEquals("Module declaration block, https://www.ek9.io/structure.html#module",
        hover.getContents().getRight().getValue());
    languageServer.shutdown();
  }

  @Test
  void testChangedAndDeletedFileEvent() {
    var theSourceFile = sourceFileSupport.copyFileToTestCWD(relativePathToValidSource, validSource);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //As SinglePackage.ek9 is valid we'd expect zero length error diagnotics.
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
    sourceFileSupport.copyFileToTestCWD(relativePathToInvalidSource, invalidSource);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //Now we'd expect errors
    assertOddNumberOfSpacesError(client);

    languageServer.shutdown();
  }

  @Test
  void testInvalidEk9SourceFileEvents() {
    var file = sourceFileSupport.copyFileToTestCWD(relativePathToInvalidSource, invalidSource);

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

  private void assertOddNumberOfSpacesError(final SimulatedLspClient client) {
    client.getLastDiagnostics().ifPresent(diagnostics -> {
      assertEquals(1, diagnostics.getDiagnostics().size());
      var theError = diagnostics.getDiagnostics().get(0);
      assertEquals("Odd number of spaces for indentation", theError.getMessage());
    });
  }

  private void assertNoErrors(final SimulatedLspClient client) {
    client.getLastDiagnostics()
        .ifPresent(diagnostics -> assertEquals(0, diagnostics.getDiagnostics().size()));
  }

  @Test
  void testDifferentMessagesWithValidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(relativePathToValidSource, validSource);

    final Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    final SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    Map<MessageType, Consumer<String>> messageTypes =
        Map.of(MessageType.Warning, languageServer::sendWarningBackToClient, MessageType.Log,
            languageServer::sendLogBackToClient, MessageType.Info,
            languageServer::sendInfoBackToClient, MessageType.Error,
            languageServer::sendErrorBackToClient);

    messageTypes.forEach((key, value) -> {
      value.accept("A Message");
      client.getLastMessage().ifPresentOrElse(message -> assertEquals(key, message.getType()),
          () -> fail("Expecting message"));
    });

    languageServer.shutdown();
  }

  private static class SimulatedLspClient implements LanguageClient {

    private Optional<PublishDiagnosticsParams> lastDiagnostics = Optional.empty();

    private Optional<MessageParams> lastMessage = Optional.empty();

    @Override
    public void telemetryEvent(Object object) {

    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
      lastDiagnostics = Optional.of(diagnostics);
    }

    public Optional<PublishDiagnosticsParams> getLastDiagnostics() {
      return lastDiagnostics;
    }

    @Override
    public void showMessage(MessageParams messageParams) {
      System.out.println(messageParams);

    }

    public Optional<MessageParams> getLastMessage() {
      return lastMessage;
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
        ShowMessageRequestParams requestParams) {
      return null;
    }

    @Override
    public void logMessage(MessageParams message) {
      lastMessage = Optional.of(message);
    }
  }

}
