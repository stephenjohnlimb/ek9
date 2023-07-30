package org.ek9lang.compiler.directives;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * To be used in EK9 source code to assert that an error will be created.
 * Use this just before the statement that should cause a compiler error.
 * The first parameter is the phase that the error should be detected in
 * The second parameter is the type of error that should be generated.
 * {@code @Error:} SYMBOL_DEFINITION: RETURNING_REDUNDANT
 *
 * @see org.ek9lang.compiler.main.phases.CompilationPhase
 * @see org.ek9lang.compiler.errors.ErrorListener.SemanticClassification
 */
public class ErrorDirective implements Directive {
  private final CompilationPhase phase;
  private final Token directiveToken;
  private final ErrorListener.SemanticClassification classification;
  private final int lineNumber;

  /**
   * A new error style directive.
   */
  public ErrorDirective(final Token token, final CompilationPhase phase,
                        final ErrorListener.SemanticClassification classification,
                        final int lineNumber) {
    this.directiveToken = token;
    this.phase = phase;
    this.classification = classification;
    this.lineNumber = lineNumber;
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.Error;
  }


  public boolean isForPhase(final CompilationPhase phase) {
    return this.phase == phase;
  }

  public boolean isForClassification(ErrorListener.SemanticClassification classification) {
    return this.classification == classification;
  }

  public ErrorListener.SemanticClassification getClassification() {
    return classification;
  }

  @Override
  public Token getDirectiveToken() {
    return directiveToken;
  }

  @Override
  public int getAppliesToLineNumber() {
    return lineNumber;
  }

  @Override
  public String toString() {
    return type() + ": " + phase + ": " + classification + ": Line: " + lineNumber;
  }
}
