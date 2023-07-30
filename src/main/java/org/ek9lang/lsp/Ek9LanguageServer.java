package org.ek9lang.lsp;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.core.Glob;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;

/**
 * The Language Server Implementation into the modular EK9 compiler.
 */
public final class Ek9LanguageServer extends Ek9Service
    implements LanguageServer, LanguageClientAware {

  private final OsSupport osSupport;
  private final Ek9CompilerConfig compilerConfig;
  private final Ek9TextDocumentService textDocumentService;
  private final Ek9WorkspaceService workspaceService;
  private LanguageClient client;
  //To be used when the application exits, set to zero on shutdown by client.
  private int errorCode = 1;

  /**
   * Uses part of the compiler as a plugin language server.
   */
  public Ek9LanguageServer(final OsSupport osSupport) {
    this.osSupport = osSupport;
    this.textDocumentService = new Ek9TextDocumentService(this);
    this.workspaceService = new Ek9WorkspaceService(this);
    this.compilerConfig = new Ek9CompilerConfig(CompilationPhase.IR_ANALYSIS);
  }

  @Override
  protected Ek9LanguageServer getLanguageServer() {
    return this;
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    final InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

    List<WorkspaceFolder> folders = params.getWorkspaceFolders();
    if (folders != null) {
      folders.forEach(folder -> {
        Path path = getPath(folder.getUri());

        Glob searchCondition = new Glob("**.ek9");
        List<File> fileList = osSupport.getFilesRecursivelyFrom(path.toFile(), searchCondition);
        Logger.debug("Found " + fileList.size() + " files");
        fileList.forEach(file -> {
          //Use new JDK19 virtual threads for this.
          try {
            var compilableSource = getWorkspace().reParseSource(file.toPath());
            reportOnCompiledSource(compilableSource.getErrorListener());
          } catch (RuntimeException rex) {
            Logger.error("Failed to load and parse " + file.toString());
          }
        });
      });
    }

    // We have to have full documents
    initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);

    //Now tell client what capabilities this server supports.
    CompletionOptions completionOptions = new CompletionOptions();
    initializeResult.getCapabilities().setCompletionProvider(completionOptions);

    initializeResult.getCapabilities().setHoverProvider(true);
    initializeResult.getCapabilities().setDefinitionProvider(true);

    initializeResult.getCapabilities().setDeclarationProvider(true);
    initializeResult.getCapabilities().setReferencesProvider(true);

    return CompletableFuture.supplyAsync(() -> initializeResult);
  }

  public Optional<LanguageClient> getClient() {
    return Optional.ofNullable(client);
  }

  public Ek9CompilerConfig getCompilerConfig() {
    return compilerConfig;
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    errorCode = 0;
    return null;
  }

  @Override
  public void exit() {
    System.exit(errorCode);
  }

  @Override
  public Ek9TextDocumentService getTextDocumentService() {
    return textDocumentService;
  }

  @Override
  public Ek9WorkspaceService getWorkspaceService() {
    return workspaceService;
  }
}
