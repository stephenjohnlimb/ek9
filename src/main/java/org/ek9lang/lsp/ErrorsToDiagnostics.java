package org.ek9lang.lsp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.core.Logger;

/**
 * Convert ErrorListener warnings and errors to LSP Diagnostics.
 */
final class ErrorsToDiagnostics {

  /**
   * Used as part of the language server to obtain an empty set of diagnostic information.
   */
  PublishDiagnosticsParams getEmptyDiagnostics(String generalIdentifierOfSource) {
    PublishDiagnosticsParams rtn = new PublishDiagnosticsParams();
    rtn.setUri(generalIdentifierOfSource);
    rtn.setDiagnostics(new ArrayList<>(0));

    return rtn;
  }

  /**
   * Used as part of the language server to convert errors in to diagnostic information.
   */
  PublishDiagnosticsParams getErrorDiagnostics(ErrorListener errorListener) {
    PublishDiagnosticsParams rtn = new PublishDiagnosticsParams();
    rtn.setUri(errorListener.getGeneralIdentifierOfSource());
    List<Diagnostic> diagnostics = new ArrayList<>(0);

    if (errorListener.hasWarnings()) {
      diagnostics.addAll(
          extractDiagnostics(errorListener.getWarnings(), DiagnosticSeverity.Warning));
    }

    if (errorListener.hasErrors()) {
      diagnostics.addAll(extractDiagnostics(errorListener.getErrors(), DiagnosticSeverity.Error));
    }

    rtn.setDiagnostics(diagnostics);

    Logger.debug(
        "Our URI [" + errorListener.getGeneralIdentifierOfSource() + "] "
            + diagnostics.size() + " diagnostics");
    return rtn;
  }

  private List<Diagnostic> extractDiagnostics(Iterator<ErrorListener.ErrorDetails> iter,
                                              DiagnosticSeverity severity) {
    List<Diagnostic> diagnostics = new ArrayList<>(0);

    //We only output the first syntax error per file because the others will be a cascade.
    boolean haveFirstSyntaxError = false;

    while (iter.hasNext()) {
      ErrorListener.ErrorDetails details = iter.next();
      boolean syntaxError =
          details.getClassification().equals(ErrorListener.ErrorClassification.SYNTAX_ERROR);
      if (!syntaxError || !haveFirstSyntaxError) {
        diagnostics.add(extractDiagnostic(severity, details));
      }
      haveFirstSyntaxError |= syntaxError;
    }

    return diagnostics;
  }

  private Diagnostic extractDiagnostic(DiagnosticSeverity severity,
                                       ErrorListener.ErrorDetails details) {
    Diagnostic d = new Diagnostic();

    d.setSeverity(severity);
    d.setMessage(details.getTypeOfError());

    //This should line up with the source in the LSP - as it is zero based.
    //Quite a few space/indent errors will report on the previous line.
    int lineNo = details.getLineNumber() - 1;
    int charPos = details.getPosition();

    Range r = new Range();
    //might see if we can improve this with length of token.
    r.setStart(new Position(lineNo, charPos));
    r.setEnd(new Position(lineNo, charPos + details.getTokenLength()));
    d.setRange(r);
    return d;
  }
}
