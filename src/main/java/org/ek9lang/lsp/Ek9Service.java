package org.ek9lang.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.Logger;

/**
 * Base service for EK9 language compilation.
 */
abstract class Ek9Service {
  private final ErrorsToDiagnostics diagnosticExtractor = new ErrorsToDiagnostics();
  private Ek9LanguageServer languageServer;

  Ek9Service() {

  }

  Ek9Service(final Ek9LanguageServer languageServer) {

    //Keep a reference, so we can access client and send back messages.
    this.languageServer = languageServer;

  }

  protected Ek9LanguageServer getLanguageServer() {

    return languageServer;
  }

  protected Workspace getWorkspace() {

    return getLanguageServer().getWorkspaceService().getEk9WorkSpace();
  }

  protected TokenResult getNearestToken(final TextDocumentPositionParams params) {

    final var uri = getFilename(params.getTextDocument());

    var rtn = new TokenResult();
    if (getWorkspace().isSourcePresent(uri)) {
      final var line = params.getPosition().getLine() + 1;
      final var charPos = params.getPosition().getCharacter();
      rtn = getWorkspace().getSource(uri).nearestToken(line, charPos);
      //Try a bit further back.
      if (!rtn.isPresent()) {
        rtn = getWorkspace().getSource(uri).nearestToken(line, charPos - 1);
      }
    }

    return rtn;
  }

  protected String getFilename(final TextDocumentIdentifier textDocument) {

    return getPath(textDocument.getUri()).toString();
  }

  protected String getFilename(final TextDocumentItem textDocument) {

    return getPath(textDocument.getUri()).toString();
  }

  protected Path getPath(final String uri) {

    try {
      return Paths.get(new URI(uri));
    } catch (URISyntaxException e) {
      return null;
    }

  }

  protected void reportOnCompiledSource(final ErrorListener errorListener) {

    Logger.debug("Reporting on " + errorListener.getGeneralIdentifierOfSource());

    clearOldCompiledDiagnostics(errorListener.getGeneralIdentifierOfSource());
    final var sourceDiagnostics = diagnosticExtractor.getErrorDiagnostics(errorListener);
    sendDiagnostics(sourceDiagnostics);

  }

  protected void clearOldCompiledDiagnostics(final String generalIdentifierOfSource) {

    if (generalIdentifierOfSource != null) {
      final var clearedDiagnostics = diagnosticExtractor.getEmptyDiagnostics(generalIdentifierOfSource);
      sendDiagnostics(clearedDiagnostics);
    }
  }

  /**
   * This is how to send compiler errors back.
   *
   * @param diagnostics The set of diagnostics to be returned to the user.
   */
  void sendDiagnostics(final PublishDiagnosticsParams diagnostics) {

    getLanguageServer().getClient().ifPresent(client -> client.publishDiagnostics(diagnostics));

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

    getLanguageServer().getClient().ifPresent(client -> client.logMessage(message));

  }
}
