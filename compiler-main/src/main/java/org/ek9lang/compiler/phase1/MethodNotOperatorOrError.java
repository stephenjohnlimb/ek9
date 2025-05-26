package org.ek9lang.compiler.phase1;

import java.util.Set;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Just checks that named operator methods:
 * mod, rem, abs, sqrt, open, close, contains, matches, empty, length
 * are not being used as just method names.
 * They are reserved for operators and the semantics that go with those operators.
 * While they could be used with different arguments/parameters - this might be confusing.
 */
final class MethodNotOperatorOrError extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final Set<String> namedOperators = Set.of(
      "and", "or", "xor", "mod", "rem", "abs", "sqrt", "open", "close", "contains", "matches", "empty", "length");

  MethodNotOperatorOrError(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol,
                     final EK9Parser.MethodDeclarationContext methodDeclarationContext) {

    if (namedOperators.contains(methodSymbol.getName())) {
      //Then even if the method does have a different set of arguments we cannot allow it.
      //Just too confusing to have operators and methods with the same name.
      final var msg = "method named '" + methodSymbol.getName() + "' conflicts with operator semantics:";
      errorListener.semanticError(methodSymbol.getSourceToken(), msg,
          ErrorListener.SemanticClassification.OPERATOR_NAME_USED_AS_METHOD);
    }

  }
}
