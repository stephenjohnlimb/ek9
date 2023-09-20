package org.ek9lang.compiler.directives;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Just checks if there are any directives that relate to errors in the parsed module and tallies those up
 * with any errors held for the phase of compilation.
 */
public class ErrorDirectiveListener implements CompilationPhaseListener {
  @Override
  public void accept(CompilationEvent compilationEvent) {
    //Will be null until fully parsed.
    if (compilationEvent.parsedModule() != null) {
      //Only interested in error directives for this phase.
      var directives = compilationEvent.parsedModule().getDirectives(DirectiveType.Error, compilationEvent.phase());
      if (!directives.isEmpty()) {
        var errorListener = compilationEvent.source().getErrorListener();
        processDirectives(directives, errorListener);
      }
    }
  }

  /**
   * Tally up any error directives against the recorded errors.
   */
  private void processDirectives(List<Directive> directives, final ErrorListener errorListener) {
    //Get both in map form based line numbers.
    var directiveLineNumberLookup = toMap(directives);
    var errorLineNumberLookup = toMap(errorListener.getErrors());

    if (directiveLineNumberLookup.size() != errorLineNumberLookup.size()) {
      var msg = directiveLineNumberLookup.size() + " directives vs " + errorLineNumberLookup.size() + " errors,";
      errorListener.directiveError(new Ek9Token("Directives vs Errors"), msg,
          ErrorListener.SemanticClassification.DIRECTIVE_ERROR_MISMATCH);
    }

    var directiveLineNumbers = directiveLineNumberLookup.keySet().stream().sorted().toList();
    for (var directiveLineNumber : directiveLineNumbers) {
      var directive = (ErrorDirective) directiveLineNumberLookup.get(directiveLineNumber);
      var error = errorLineNumberLookup.get(directiveLineNumber);
      if (error == null) {
        var msg = "'" + directive.getClassification() + "' for line: " + directiveLineNumber;
        errorListener.directiveError(directive.getDirectiveToken(), msg,
            ErrorListener.SemanticClassification.ERROR_MISSING);
      } else {
        //Now remove from the list - any residuals mean we're missing a directive for an error.
        errorLineNumberLookup.remove(directiveLineNumber);

        //Check the type of error against the one expected in the directive
        if (!directive.isForClassification(error.getSemanticClassification())) {
          var msg = "'" + error.getSemanticClassification() + "' versus '" + directive + "'";
          errorListener.directiveError(directive.getDirectiveToken(), msg,
              ErrorListener.SemanticClassification.DIRECTIVE_WRONG_CLASSIFICATION);
        }
      }
    }

    //Now if there are any left in errors - they can't be in the directives because we've just looked.
    var errorLineNumbers = errorLineNumberLookup.keySet().stream().sorted().toList();

    for (var errorLineNumber : errorLineNumbers) {
      var error = errorLineNumberLookup.get(errorLineNumber);
      var msg = "as line: " + error.getLineNumber() + " has error: '" + error.getSemanticClassification() + "'";
      errorListener.directiveError(new Ek9Token("Expecting @Error directive"), msg,
          ErrorListener.SemanticClassification.DIRECTIVE_MISSING);
    }
  }

  /**
   * Just to make the lookups quicker.
   */
  private Map<Integer, ErrorListener.ErrorDetails> toMap(final Iterator<ErrorListener.ErrorDetails> iterator) {
    var map = new HashMap<Integer, ErrorListener.ErrorDetails>();
    while (iterator.hasNext()) {
      var error = iterator.next();
      map.put(error.getLineNumber(), error);
    }
    return map;
  }

  /**
   * Just to make the lookups quicker.
   */
  private Map<Integer, Directive> toMap(final List<Directive> directives) {
    var map = new HashMap<Integer, Directive>();
    directives.forEach(directive -> map.put(directive.getAppliesToLineNumber(), directive));
    return map;
  }
}
