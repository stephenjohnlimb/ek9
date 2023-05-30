package org.ek9lang.compiler.main.resolvedefine;

import static org.ek9lang.compiler.errors.ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.core.exception.AssertValue;


/**
 * Checks on the assignment during a declaration.
 */
public class CheckVariableAssignmentDeclaration implements Consumer<EK9Parser.VariableDeclarationContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;
  private final CheckTypesCompatible checkTypesCompatible;

  /**
   * Create a new checker of variable assignments when variables are being declared.
   */
  public CheckVariableAssignmentDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                                            final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
    this.checkTypesCompatible = new CheckTypesCompatible(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {
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

    //Once expressions are done might be able to remove this null check.
    if (exprSymbol != null && exprSymbol.getType().isPresent() && varSymbol.getType().isPresent()) {
      checkAndUpdateIfLhsIsParameterised(varSymbol.getType().get(), exprSymbol, exprSymbol.getType().get());
    }
    return true;
  }

  private void checkAndUpdateIfLhsIsParameterised(ISymbol lhsType, ISymbol rhs, ISymbol rhsType) {

    //We must now consider this situation: "someVar List of Integer := List()"
    //In this situation we want to alter the type on the rhs to be 'List of Integer' not just 'List of T'.

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
    var data = new TypeCompatibilityData(ctx.start, varSymbol, exprSymbol);
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
