package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used as a base for a range of constructs that can be expressions.
 */
abstract class PossibleExpressionConstruct extends TypedSymbolAccess {
  private final GetGuardVariable getGuardVariable;

  protected PossibleExpressionConstruct(final SymbolAndScopeManagement symbolAndScopeManagement,
                                        final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.getGuardVariable = new GetGuardVariable(symbolAndScopeManagement, errorListener);
  }

  protected boolean isVariableInitialisedInScopes(final CodeFlowAnalyzer analyzer,
                                                  final ISymbol variable,
                                                  final List<IScope> scopes) {
    if (scopes.isEmpty()) {
      return false;
    }

    return scopes.stream()
        .allMatch(scope -> analyzer.doesSymbolMeetAcceptableCriteria(variable, scope));
  }

  protected Optional<ISymbol> getGuardExpressionVariable(final EK9Parser.PreFlowStatementContext ctx) {
    if (ctx != null) {
      return getGuardVariable.apply(ctx);
    }
    return Optional.empty();
  }

  protected Optional<ISymbol> getGuardExpressionVariable(final EK9Parser.PreFlowAndControlContext ctx) {

    if (ctx != null) {
      return getGuardExpressionVariable(ctx.preFlowStatement());
    }
    return Optional.empty();

  }

  /**
   * Now when there is a preflow control, it may mean that a variable is initialised.
   */
  protected void processPossibleGuardInitialisation(final CodeFlowAnalyzer analyzer,
                                                    final ISymbol guardVariable,
                                                    final ParserRuleContext ctx) {

    var scope = symbolAndScopeManagement.getRecordedScope(ctx);

    boolean initialised = analyzer.doesSymbolMeetAcceptableCriteria(guardVariable, scope);
    if (initialised) {
      final var outerScope = symbolAndScopeManagement.getTopScope();
      analyzer.markSymbolAsMeetingAcceptableCriteria(guardVariable, outerScope);
    }

  }

  protected void pullUpAcceptableCriteriaToHigherScope(final CodeFlowAnalyzer analyzer,
                                                       List<IScope> allAppropriateBlocks,
                                                       final IScope outerScope) {

    var unInitialisedVariables = analyzer.getSymbolsNotMeetingAcceptableCriteria(outerScope);
    for (var variable : unInitialisedVariables) {
      if (isVariableInitialisedInScopes(analyzer, variable, allAppropriateBlocks)) {
        analyzer.markSymbolAsMeetingAcceptableCriteria(variable, outerScope);
      }
    }

  }

  protected void checkReturningVariableOrError(final EK9Parser.ReturningParamContext ctx,
                                               final IScope switchScope, boolean noGuardExpression) {

    Consumer<ISymbol> errorIssuer = variable -> errorListener.semanticError(ctx.LEFT_ARROW().getSymbol(),
        "'" + variable.getName() + "':", RETURN_NOT_ALWAYS_INITIALISED);

    if (ctx != null) {
      //Note that the returning variable is registered against the switch scope.
      var returningVariable = symbolAndScopeManagement.getRecordedSymbol(ctx);
      if (!symbolAndScopeManagement.isVariableInitialised(returningVariable, switchScope)) {
        errorIssuer.accept(returningVariable);
      }
      if (noGuardExpression) {
        return;
      }
      //But if there is a guard and the return variable has not been initialised then we issue error
      //As the guard may cause the whole expression not to run and only the return value will be used as is.
      if (ctx.variableOnlyDeclaration() != null && ctx.variableOnlyDeclaration().QUESTION() != null) {
        errorIssuer.accept(returningVariable);
      }
    }

  }
}
