package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;


/**
 * Checks on the assignment during a declaration and emits an error if not valid.
 * <pre>
 *   identifier AS? typeDef QUESTION? (ASSIGN | ASSIGN2 | COLON | MERGE | ASSIGN_UNSET) assignmentExpression
 *   identifier LEFT_ARROW assignmentExpression
 * </pre>
 */
final class VariableAssignmentOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.VariableDeclarationContext> {

  private final TypesCompatibleOrError typesCompatibleOrError;
  private final AssignmentOrError assignmentOrError;
  private final NoDelegateMethodClashesOrError noDelegateMethodClashesOrError;

  /**
   * Create a new checker of variable assignments when variables are being declared.
   */
  VariableAssignmentOrError(final SymbolsAndScopes symbolsAndScopes,
                            final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.typesCompatibleOrError = new TypesCompatibleOrError(symbolsAndScopes, errorListener);
    this.assignmentOrError = new AssignmentOrError(symbolsAndScopes, errorListener, true);
    this.noDelegateMethodClashesOrError = new NoDelegateMethodClashesOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {

    //Note that we use the untyped check to access the symbols
    //This is because for assignments we can use explicit or inferred type (see grammar).

    final var varSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Expecting a variable symbol to be present Line: "
        + ctx.start.getLine(), varSymbol);

    //We should always get an expression here (though it may not be 'typed')
    final var exprSymbol = symbolsAndScopes.getRecordedSymbol(ctx.assignmentExpression());

    //Now it should have been typed if it has a typeDef - if not then that's an error in previous phases.
    //So we just accept that previous errors will have been reported.
    if (ctx.typeDef() != null) {
      typeDefOrError(ctx, varSymbol, exprSymbol);
    } else {
      inferredTypeOrError(ctx, varSymbol, exprSymbol);
    }

    noDelegateMethodClashesOrError.accept(varSymbol);

  }

  private void typeDefOrError(final EK9Parser.VariableDeclarationContext ctx,
                              final ISymbol varSymbol,
                              final ISymbol exprSymbol) {
    typeDefUse(ctx, varSymbol, exprSymbol);
    typesCompatibleOrError.accept(new TypeCompatibilityData(new Ek9Token(ctx.start), varSymbol, exprSymbol));
    assignmentOrError.accept(new Ek9Token(ctx.op), varSymbol);
  }

  /**
   * So if not already typed and the typedef is recorded then we can use that type.
   */
  private void typeDefUse(final EK9Parser.VariableDeclarationContext ctx,
                          final ISymbol varSymbol,
                          final ISymbol exprSymbol) {

    if (varSymbol.getType().isEmpty()) {
      final var theType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
      if (theType != null) {
        varSymbol.setType(theType);
      }
    }

    if (areTheSameType(varSymbol, exprSymbol)) {
      return;
    }

    if (exprSymbol != null && exprSymbol.getType().isPresent() && varSymbol.getType().isPresent()) {
      updateIfLhsIsParameterised(varSymbol.getType().get(), exprSymbol, exprSymbol.getType().get());
    }

  }

  private void updateIfLhsIsParameterised(final ISymbol lhsType, final ISymbol rhs, final ISymbol rhsType) {

    //We must now consider this situation: "someVar List of Integer := List()"
    //In this situation we want to alter the type on the rhs to be 'List of Integer' not just 'List of T'.
    //This also applies to generic functions. It gives the ability to construct a type on the rhs of an expression
    //using just the generic type, but if the lhs is fully expressed we want to infer the lhs fully.
    if (lhsType instanceof PossibleGenericSymbol lhsMaybeParameterisedType
        && lhsMaybeParameterisedType.isParameterisedType()) {
      lhsMaybeParameterisedType.getGenericType().ifPresent(theGenericType -> {
        if (theGenericType.isExactSameType(rhsType)) {
          rhs.setType(lhsType);
        }
      });
    }
  }

  /**
   * Just a quick check to see if the lhs and rhs are the same types already.
   */
  private boolean areTheSameType(final ISymbol lhs,
                                 final ISymbol rhs) {

    if (lhs != null && rhs != null && lhs.getType().isPresent() && rhs.getType().isPresent()) {
      return lhs.getType().get().isExactSameType(rhs.getType().get());
    }

    return false;
  }

  private void inferredTypeOrError(final EK9Parser.VariableDeclarationContext ctx,
                                   final ISymbol varSymbol,
                                   final ISymbol exprSymbol) {

    if (varSymbol.getType().isEmpty() && exprSymbol != null && exprSymbol.getType().isPresent()) {
      varSymbol.setType(exprSymbol.getType());
      if (exprSymbol.getType().get().isGenericInNature()) {
        errorListener.semanticError(ctx.start, "", GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED);
      }
    }

  }
}
