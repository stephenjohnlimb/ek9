package org.ek9lang.lsp;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.ek9lang.compiler.files.CompilableSource;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.utils.Logger;

/**
 * Base service for EK9 language compilation.
 */
public abstract class Ek9Service {
  private final ErrorsToDiagnostics diagnosticExtractor = new ErrorsToDiagnostics();
  private Ek9LanguageServer languageServer;

  protected Ek9Service() {

  }

  protected Ek9Service(Ek9LanguageServer languageServer) {
    //Keep a reference, so we can access client and send back messages.
    this.languageServer = languageServer;
  }

  protected Ek9LanguageServer getLanguageServer() {
    return languageServer;
  }

  protected void setLanguageServer(Ek9LanguageServer languageServer) {
    this.languageServer = languageServer;
  }

  protected Workspace getWorkspace() {
    return getLanguageServer().getWorkspaceService().getEk9WorkSpace();
  }

  protected TokenResult getNearestToken(TextDocumentPositionParams params) {
    TokenResult rtn = new TokenResult();
    String uri = getFilename(params.getTextDocument());
    if (getWorkspace().isSourcePresent(uri)) {
      int line = params.getPosition().getLine() + 1;
      int charPos = params.getPosition().getCharacter() + 1;
      rtn = getWorkspace().getSource(uri).nearestToken(line, charPos);
    }
    return rtn;
  }

  protected String getFilename(TextDocumentIdentifier textDocument) {
    return getPath(textDocument.getUri()).toString();
  }

  protected String getFilename(TextDocumentItem textDocument) {
    return getPath(textDocument.getUri()).toString();
  }

  protected Path getPath(String uri) {
    Path path = null;
    try {
      path = Paths.get(new URL(uri).toURI());
    } catch (URISyntaxException | MalformedURLException e) {
      //Consume leave as null
    }

    return path;
  }

  protected void reportOnCompiledSource(CompilableSource compilableSource) {
    Logger.error("Reporting on " + compilableSource.getFileName());

    clearOldCompiledDiagnostics(compilableSource);
    PublishDiagnosticsParams sourceDiagnostics =
        diagnosticExtractor.getErrorDiagnostics(compilableSource);
    sendDiagnostics(sourceDiagnostics);

  }

  protected void clearOldCompiledDiagnostics(CompilableSource compilableSource) {
    if (compilableSource != null) {
      PublishDiagnosticsParams clearedDiagnostics =
          diagnosticExtractor.getEmptyDiagnostics(compilableSource);
      sendDiagnostics(clearedDiagnostics);
    }
  }

  /**
   * This is how to send compiler errors back.
   *
   * @param diagnostics The set of diagnostics to be returned to the user.
   */
  public void sendDiagnostics(PublishDiagnosticsParams diagnostics) {
    getLanguageServer().getClient().publishDiagnostics(diagnostics);
  }

  protected void sendWarningBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Warning, message));
  }

  protected void sendErrorBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Error, message));
  }

  protected void sendInfoBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Info, message));
  }

  protected void sendLogBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Log, message));
  }

  private void sendLogMessageBackToClient(MessageParams message) {
    getLanguageServer().getClient().logMessage(message);
  }
}
