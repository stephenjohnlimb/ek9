package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;


/**
 * Checks on the assignment during a declaration.
 * <pre>
 *   identifier AS? typeDef QUESTION? (ASSIGN | ASSIGN2 | COLON | MERGE | ASSIGN_UNSET) assignmentExpression
 *   identifier LEFT_ARROW assignmentExpression
 * </pre>
 */
final class CheckVariableAssignmentDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.VariableDeclarationContext> {

  private final CheckTypesCompatible checkTypesCompatible;
  private final CheckAssignment checkAssignment;

  /**
   * Create a new checker of variable assignments when variables are being declared.
   */
  CheckVariableAssignmentDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypesCompatible = new CheckTypesCompatible(symbolAndScopeManagement, errorListener);
    this.checkAssignment = new CheckAssignment(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {

    //Note that we use the untyped check to access the symbols
    //This is because for assignments we can use explicit or inferred type (see grammar).

    var varSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Expecting a variable symbol to be present Line: "
        + ctx.start.getLine(), varSymbol);

    var exprSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx.assignmentExpression());
    //Eventually we should always get an expression here (though it may not be 'typed')

    //Now it should have been typed if it has a typeDef - if not then that's an error in previous phases.
    //So we just accept that previous errors will have been reported.
    if (ctx.typeDef() != null) {
      if (processTypeDefUse(ctx, varSymbol, exprSymbol)) {
        checkTypeCompatibility(ctx, varSymbol, exprSymbol);
      }
      checkAssignment.accept(new Ek9Token(ctx.op), varSymbol);
    } else {
      processInferredType(ctx, varSymbol, exprSymbol);
    }
  }

  /**
   * So if not already typed and the typedef is recorded then we can use that type.
   * True if type compatibility check is still required.
   */
  private boolean processTypeDefUse(final EK9Parser.VariableDeclarationContext ctx,
                                    final ISymbol varSymbol,
                                    final ISymbol exprSymbol) {

    if (varSymbol.getType().isEmpty()) {
      var theType = symbolAndScopeManagement.getRecordedSymbol(ctx.typeDef());
      if (theType != null) {
        varSymbol.setType(theType);
      }
    }

    if (areTheSameType(varSymbol, exprSymbol)) {
      return false;
    }

    if (exprSymbol != null && exprSymbol.getType().isPresent() && varSymbol.getType().isPresent()) {
      checkAndUpdateIfLhsIsParameterised(varSymbol.getType().get(), exprSymbol, exprSymbol.getType().get());
    }
    return true;
  }

  private void checkAndUpdateIfLhsIsParameterised(ISymbol lhsType, ISymbol rhs, ISymbol rhsType) {

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

  /**
   * When declaring variables without inference, we must check that what has been explicitly declared is
   * compatible with the right hand side (i.e. the expression).
   */
  private void checkTypeCompatibility(final EK9Parser.VariableDeclarationContext ctx,
                                      final ISymbol varSymbol,
                                      final ISymbol exprSymbol) {
    var data = new TypeCompatibilityData(new Ek9Token(ctx.start), varSymbol, exprSymbol);
    checkTypesCompatible.accept(data);
  }

  private void processInferredType(final EK9Parser.VariableDeclarationContext ctx,
                                   final ISymbol varSymbol, final ISymbol exprSymbol) {
    if (varSymbol.getType().isEmpty() && exprSymbol != null && exprSymbol.getType().isPresent()) {
      varSymbol.setType(exprSymbol.getType());
      if (exprSymbol.getType().get().isGenericInNature()) {
        errorListener.semanticError(ctx.start, "", GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED);
      }
    }
  }
}
