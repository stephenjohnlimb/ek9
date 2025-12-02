package org.ek9lang.lsp;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.core.Glob;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;

/**
 * The Language Server Implementation into the modular EK9 compiler.
 */
final class Ek9LanguageServer implements IEk9LanguageServer {

  private final PathExtractor pathExtractor = new PathExtractor();

  private final OsSupport osSupport;
  private final Ek9CompilerConfig compilerConfig;
  private final Ek9TextDocumentService textDocumentService;
  private final Ek9WorkspaceService workspaceService;
  private final Ek9CompilerService compilerService;

  private LanguageClient client;
  //To be used when the application exits, set to zero on shutdown by client.
  private int errorCode = 1;

  /**
   * Uses part of the compiler as a plugin language server.
   */
  Ek9LanguageServer(final OsSupport osSupport) {

    //TODO as compiler has more implementation move phase to IR_ANALYSIS.
    this.osSupport = osSupport;
    this.compilerService = new Ek9CompilerService(this);
    this.textDocumentService = new Ek9TextDocumentService(this);
    this.workspaceService = new Ek9WorkspaceService(this);
    this.compilerConfig = new Ek9CompilerConfig(CompilationPhase.PRE_IR_CHECKS);

  }

  @Override
  public void connect(final LanguageClient client) {

    if (client != null) {
      Logger.debug("EK9: connect from client");
    } else {
      Logger.debug("EK9: Client connect but client was null");
    }

    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {

    final var initializeResult = new InitializeResult(new ServerCapabilities());
    Logger.debug("EK9: initialize");

    if (params != null) {
      final var folders = params.getWorkspaceFolders();
      if (folders != null) {
        folders.forEach(folder -> {
          final var path = pathExtractor.apply(folder.getUri());

          final var searchCondition = new Glob("**.ek9");
          final var fileList = osSupport.getFilesRecursivelyFrom(path.toFile(), searchCondition);

          Logger.debug("EK9: Found " + fileList.size() + " files");
          fileList.forEach(file -> getWorkspaceService().getWorkspace().addSource(file));
        });
      }
      compilerService.recompileWorkSpace();
    } else {
      Logger.debug("EK9: Initialised with no parameters");
    }

    final var completionOptions = new CompletionOptions();
    initializeResult.getCapabilities().setCompletionProvider(completionOptions);

    initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
    initializeResult.getCapabilities().setHoverProvider(true);
    initializeResult.getCapabilities().setDefinitionProvider(true);
    initializeResult.getCapabilities().setDeclarationProvider(true);
    initializeResult.getCapabilities().setReferencesProvider(true);

    return CompletableFuture.supplyAsync(() -> initializeResult);
  }

  Optional<LanguageClient> getClient() {

    return Optional.ofNullable(client);
  }

  Ek9CompilerConfig getCompilerConfig() {

    return compilerConfig;
  }

  @Override
  public CompletableFuture<Object> shutdown() {

    Logger.debug("EK9: Shutdown");
    errorCode = 0;

    return null;
  }

  @Override
  public void exit() {

    Logger.debug("EK9: Exit");
    System.exit(errorCode);
  }

  @Override
  public Ek9TextDocumentService getTextDocumentService() {

    Logger.debug("EK9: getTextDocumentService");

    return textDocumentService;
  }

  @Override
  public Ek9WorkspaceService getWorkspaceService() {

    return workspaceService;
  }

  public Ek9CompilerService getCompilerService() {
    return compilerService;
  }

  void sendWarningBackToClient(final String message) {

    sendLogMessageBackToClient(new MessageParams(MessageType.Warning, message));

  }

  void sendErrorBackToClient(final String message) {

    sendLogMessageBackToClient(new MessageParams(MessageType.Error, message));

  }

  void sendInfoBackToClient(final String message) {

    sendLogMessageBackToClient(new MessageParams(MessageType.Info, message));

  }

  void sendLogBackToClient(final String message) {

    sendLogMessageBackToClient(new MessageParams(MessageType.Log, message));

  }

  void sendLogMessageBackToClient(final MessageParams message) {

    client.logMessage(message);

  }
}
