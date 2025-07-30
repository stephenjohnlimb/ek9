package org.ek9lang.lsp;

import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.Logger;

/**
 * Base service for EK9 language compilation.
 */
abstract class Ek9Service {
  protected final PathExtractor pathExtractor = new PathExtractor();
  private final ErrorsToDiagnostics diagnosticExtractor = new ErrorsToDiagnostics();
  private final Ek9LanguageServer languageServer;

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

  protected CompilerFlags getCompilerFlags() {
    return getLanguageServer().getCompilerConfig();
  }


  protected Ek9CompilerService getCompilerService() {
    return getLanguageServer().getCompilerService();
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

    return pathExtractor.apply(textDocument.getUri()).toString();
  }

  protected String getFilename(final TextDocumentItem textDocument) {

    return pathExtractor.apply(textDocument.getUri()).toString();
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

    Logger.debugf("Sending back diagnostics %d\n", diagnostics.getDiagnostics().size());
    getLanguageServer().getClient().ifPresent(client -> client.publishDiagnostics(diagnostics));

  }
}
