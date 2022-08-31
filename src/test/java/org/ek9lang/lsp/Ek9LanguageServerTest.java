package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.ek9lang.cli.SourceFileSupport;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

final class Ek9LanguageServerTest {
  private static final OsSupport osSupport = new OsSupport(true);
  private static final FileHandling fileHandling = new FileHandling(osSupport);
  private static final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  final String workspaceDirectory = osSupport.getCurrentWorkingDirectory();

  final static String validSource = "SinglePackage.ek9";
  final static String relativePathToValidSource = "/examples/constructs/packages/";

  final static String invalidSource = "unevenIndentation.ek9";
  final static String relativePathToInvalidSource = "/badExamples/basics/";

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

  @AfterAll
  static void tidyUpTempWorkspace() {
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
    client.getLastDiagnostics().ifPresent(diagnostics -> assertEquals(0, diagnostics.getDiagnostics().size()));

    languageServer.shutdown();
  }

  @Test
  void testBasicStartupWithInvalidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(relativePathToInvalidSource, invalidSource);

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    //Now we'd expect errors
    client.getLastDiagnostics().ifPresent(diagnostics -> {
      assertEquals(1, diagnostics.getDiagnostics().size());
      var theError = diagnostics.getDiagnostics().get(0);
      assertEquals("Odd number of spaces for indentation", theError.getMessage());
    });

    languageServer.shutdown();
  }

  @Test
  void testDifferentMessagesWithValidEk9Source() {
    sourceFileSupport.copyFileToTestCWD(relativePathToValidSource, validSource);

    final Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    final SimulatedLspClient client = prepareLanguageServer.apply(languageServer);

    Map<MessageType, Consumer<String>> messageTypes = Map.of(
        MessageType.Warning, languageServer::sendWarningBackToClient,
        MessageType.Log, languageServer::sendLogBackToClient,
        MessageType.Info, languageServer::sendInfoBackToClient,
        MessageType.Error, languageServer::sendErrorBackToClient
    );

    messageTypes.forEach((key, value) -> {
      value.accept("A Message");
      client.getLastMessage()
          .ifPresentOrElse(message -> assertEquals(key, message.getType()),
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
