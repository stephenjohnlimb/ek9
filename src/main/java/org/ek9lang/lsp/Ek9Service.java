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
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.internals.Workspace;
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

  protected Workspace getWorkspace() {
    return getLanguageServer().getWorkspaceService().getEk9WorkSpace();
  }

  protected TokenResult getNearestToken(TextDocumentPositionParams params) {
    TokenResult rtn = new TokenResult();
    String uri = getFilename(params.getTextDocument());
    if (getWorkspace().isSourcePresent(uri)) {
      int line = params.getPosition().getLine() + 1;
      int charPos = params.getPosition().getCharacter();
      rtn = getWorkspace().getSource(uri).nearestToken(line, charPos);
      //Try a bit further back.
      if (!rtn.isPresent()) {
        rtn = getWorkspace().getSource(uri).nearestToken(line, charPos - 1);
      }
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

  protected void reportOnCompiledSource(ErrorListener errorListener) {
    Logger.debug("Reporting on " + errorListener.getGeneralIdentifierOfSource());

    clearOldCompiledDiagnostics(errorListener.getGeneralIdentifierOfSource());
    PublishDiagnosticsParams sourceDiagnostics =
        diagnosticExtractor.getErrorDiagnostics(errorListener);
    sendDiagnostics(sourceDiagnostics);
  }

  protected void clearOldCompiledDiagnostics(String generalIdentifierOfSource) {
    if (generalIdentifierOfSource != null) {
      PublishDiagnosticsParams clearedDiagnostics =
          diagnosticExtractor.getEmptyDiagnostics(generalIdentifierOfSource);
      sendDiagnostics(clearedDiagnostics);
    }
  }

  /**
   * This is how to send compiler errors back.
   *
   * @param diagnostics The set of diagnostics to be returned to the user.
   */
  public void sendDiagnostics(final PublishDiagnosticsParams diagnostics) {
    getLanguageServer().getClient().ifPresent(client -> client.publishDiagnostics(diagnostics));
  }

  public void sendWarningBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Warning, message));
  }

  public void sendErrorBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Error, message));
  }

  public void sendInfoBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Info, message));
  }

  public void sendLogBackToClient(String message) {
    sendLogMessageBackToClient(new MessageParams(MessageType.Log, message));
  }

  private void sendLogMessageBackToClient(MessageParams message) {
    getLanguageServer().getClient().ifPresent(client -> client.logMessage(message));
  }
}
