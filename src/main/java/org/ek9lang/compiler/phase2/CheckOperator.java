package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.support.SymbolFactory.DEFAULTED;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.AppropriateBodyOrError;
import org.ek9lang.compiler.common.ContextSupportsAbstractMethodOrError;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OverrideOrAbstractOrError;
import org.ek9lang.compiler.common.ProcessTraitMethodOrError;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TraitMethodAcceptableOrError;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks operators from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check. This does not check service operations that are marked as operators.
 */
final class CheckOperator extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.OperatorDeclarationContext> {

  private static final String OPERATOR_SEMANTICS = "operator semantics:";
  private final ProcessTraitMethodOrError processTraitMethodOrError;
  private final TraitMethodAcceptableOrError traitMethodAcceptableOrError;
  private final ContextSupportsAbstractMethodOrError contextSupportsAbstractMethodOrError;
  private final AppropriateBodyOrError appropriateBodyOrError;
  private final Map<String, Consumer<MethodSymbol>> operatorChecks;
  private final OverrideOrAbstractOrError overrideOrAbstractOrError;

  /**
   * Create a new operation checker.
   */
  CheckOperator(final SymbolAndScopeManagement symbolAndScopeManagement,
                final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.operatorChecks = populateOperatorChecks();

    traitMethodAcceptableOrError = new TraitMethodAcceptableOrError(errorListener);
    contextSupportsAbstractMethodOrError =
        new ContextSupportsAbstractMethodOrError(symbolAndScopeManagement, errorListener);
    processTraitMethodOrError = new ProcessTraitMethodOrError(errorListener);
    appropriateBodyOrError = new AppropriateBodyOrError(symbolAndScopeManagement, errorListener);
    overrideOrAbstractOrError = new OverrideOrAbstractOrError(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol, final EK9Parser.OperatorDeclarationContext ctx) {

    //Operator can be defaulted and so compiler will create implementation
    final var defaulted = "TRUE".equals(methodSymbol.getSquirrelledData(DEFAULTED));

    if (!defaulted) {
      operatorChecks.getOrDefault(ctx.operator().getText(), method -> {
      }).accept(methodSymbol);
    }

    if (ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext) {
      processTraitMethodOrError.accept(methodSymbol, ctx.operationDetails());
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      traitMethodAcceptableOrError.accept(methodSymbol, ctx.operationDetails());
    }

    contextSupportsAbstractMethodOrError.accept(methodSymbol, ctx);
    appropriateBodyOrError.accept(methodSymbol, ctx.operationDetails());
    overrideOrAbstractOrError.accept(methodSymbol);

  }

  private Map<String, Consumer<MethodSymbol>> populateOperatorChecks() {

    final Map<String, Consumer<MethodSymbol>> rtn = new HashMap<>();
    final Map<String, Consumer<MethodSymbol>> logicalOperatorChecks = Map.of(
        "<", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<=", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        ">", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        ">=", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "==", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<>", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<=>", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "<~>", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "!=", addPureCheck(this::emitBadNotEqualsOperator));

    final Map<String, Consumer<MethodSymbol>> simpleOperatorChecks = Map.of(
        "sqrt", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "!", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "?", addPureCheck(this::testAcceptNoArgumentsReturnBoolean),
        "~", addPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "-", addPureCheck(this::testMinusOperator),
        "+", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "*", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "/", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "^", addPureCheck(this::testAcceptOneArgumentsReturnAnyType)
    );

    final Map<String, Consumer<MethodSymbol>> noArgumentWithReturnChecks = Map.of(
        "#^", addPureCheck(this::testAcceptNoArgumentsReturnAnyTypeOtherThanSelf),
        "$$", addPureCheck(this::testAcceptNoArgumentsReturnJson),
        "$", addPureCheck(this::testAcceptNoArgumentsReturnString),
        "#?", addPureCheck(this::testAcceptNoArgumentsReturnInteger),
        "#<", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "#>", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "not", addPureCheck(this::emitBadNotOperator),
        "abs", addPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "empty", addPureCheck(this::testAcceptNoArgumentsReturnBoolean),
        "length", addPureCheck(this::testAcceptNoArgumentsReturnInteger)
    );

    final Map<String, Consumer<MethodSymbol>> oneArgumentWithReturnChecks = Map.of(
        ">>", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "<<", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "and", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "or", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "xor", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "mod", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "rem", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "contains", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "matches", addPureCheck(this::testAcceptOneArgumentsReturnBoolean)
    );

    final Map<String, Consumer<MethodSymbol>> noArgumentNoReturn = Map.of(
        "open", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "close", addPureCheck(this::testAcceptNoArgumentsNoReturn)
    );

    rtn.putAll(logicalOperatorChecks);
    rtn.putAll(simpleOperatorChecks);
    rtn.putAll(noArgumentWithReturnChecks);
    rtn.putAll(oneArgumentWithReturnChecks);
    rtn.putAll(noArgumentNoReturn);

    //Now for each of those operators tag on the is pure check.
    final Map<String, Consumer<MethodSymbol>> mutatorChecks = Map.of(
        ":~:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        ":^:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        ":=:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "|", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "+=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "-=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "*=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "/=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "++", addNonPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "--", addNonPureCheck(this::testAcceptNoArgumentsReturnConstructType)
    );

    rtn.putAll(mutatorChecks);

    return rtn;
  }

  private void testMinusOperator(final MethodSymbol methodSymbol) {

    if (methodSymbol.getCallParameters().isEmpty()) {
      testAcceptNoArgumentsReturnConstructType(methodSymbol);
    } else {
      testAcceptOneArgumentsReturnAnyType(methodSymbol);
    }

  }

  private Consumer<MethodSymbol> addPureCheck(final Consumer<MethodSymbol> check) {

    return check.andThen(this::testPure);
  }

  private Consumer<MethodSymbol> addNonPureCheck(final Consumer<MethodSymbol> check) {

    return check.andThen(this::testNotPure);
  }

  private void testAcceptNoArgumentsReturnAnyTypeOtherThanSelf(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    final var returnType = testAnyReturnType(methodSymbol);

    returnType.ifPresent(theType -> {
      if (methodSymbol.getParentScope() instanceof AggregateSymbol parentAggregate) {
        final var errorToken = methodSymbol.getReturningSymbol().getSourceToken();
        testNotSameType(errorToken, parentAggregate, theType);
      }
    });

  }

  private void testAcceptNoArgumentsReturnAnyType(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testAnyReturnType(methodSymbol);

  }

  private void testAcceptNoArgumentsReturnBoolean(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testReturnIsBoolean(methodSymbol);

  }

  private void testAcceptNoArgumentsReturnInteger(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testReturnIsInteger(methodSymbol);

  }

  private void testAcceptNoArgumentsReturnString(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testReturnType(methodSymbol, "String", ErrorListener.SemanticClassification.MUST_RETURN_STRING);

  }

  private void testAcceptNoArgumentsReturnJson(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testReturnType(methodSymbol, "JSON", ErrorListener.SemanticClassification.MUST_RETURN_JSON);

  }

  private void testAcceptOneArgumentsReturnInteger(final MethodSymbol methodSymbol) {

    testSingleArgument(methodSymbol);
    testReturnIsInteger(methodSymbol);

  }

  private void testAcceptOneArgumentsReturnBoolean(final MethodSymbol methodSymbol) {

    testSingleArgument(methodSymbol);
    testReturnIsBoolean(methodSymbol);

  }

  private void testReturnIsBoolean(final MethodSymbol methodSymbol) {

    testReturnType(methodSymbol, "Boolean", ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN);

  }

  private void testReturnIsInteger(MethodSymbol methodSymbol) {

    testReturnType(methodSymbol, "Integer", ErrorListener.SemanticClassification.MUST_RETURN_INTEGER);

  }

  private void testAcceptOneArgumentsReturnAnyType(final MethodSymbol methodSymbol) {

    testSingleArgument(methodSymbol);
    testAnyReturnType(methodSymbol);

  }

  private void testAcceptOneArgumentsNoReturn(final MethodSymbol methodSymbol) {

    testSingleArgument(methodSymbol);
    testNoReturn(methodSymbol);

  }

  private void testAcceptNoArgumentsNoReturn(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    testNoReturn(methodSymbol);

  }

  private void testNotSameType(final IToken token, final ISymbol s1, final ISymbol s2) {

    if (s1.isExactSameType(s2)) {
      errorListener.semanticError(token, OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.MUST_NOT_RETURN_SAME_TYPE);
    }

  }

  private void testPure(final MethodSymbol methodSymbol) {

    if (!methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_MUST_BE_PURE);
    }

  }

  private void testNotPure(final MethodSymbol methodSymbol) {

    if (methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_CANNOT_BE_PURE);
    }

  }

  private void testAcceptNoArgumentsReturnConstructType(final MethodSymbol methodSymbol) {

    testNoArguments(methodSymbol);
    final var parentScope = methodSymbol.getParentScope();
    final var returnType = testAnyReturnType(methodSymbol);

    returnType.ifPresent(theType -> {
      if (!theType.isExactSameType((ISymbol) parentScope)) {
        final var parentTypeName = parentScope.getFriendlyScopeName().startsWith("_Class")
            ? "DYNAMIC CLASS" : parentScope.getFriendlyScopeName();
        final var msg = "'" + theType.getFriendlyName() + "' is not '" + parentTypeName + "':";
        errorListener.semanticError(methodSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.MUST_RETURN_SAME_AS_CONSTRUCT_TYPE);
      }
    });

  }

  private Optional<ISymbol> testAnyReturnType(final MethodSymbol methodSymbol) {

    if (!methodSymbol.isReturningSymbolPresent() || isReturningVoidType(methodSymbol)) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.RETURNING_MISSING);
      return Optional.empty();
    }
    return methodSymbol.getType();

  }

  /**
   * If a returning symbol is present then its type must be Void.
   */
  private void testNoReturn(final MethodSymbol methodSymbol) {

    if (methodSymbol.isReturningSymbolPresent() && methodSymbol.getReturningSymbol().getType().isPresent()) {
      methodSymbol.getReturningSymbol().getType().ifPresent(theType -> {
        if (!symbolAndScopeManagement.getEk9Types().ek9Void().isExactSameType(theType)) {
          errorListener.semanticError(methodSymbol.getSourceToken(), "'" + theType.getFriendlyName() + "'",
              ErrorListener.SemanticClassification.RETURN_VALUE_NOT_SUPPORTED);
        }
      });
    }

  }

  private boolean isReturningVoidType(final MethodSymbol methodSymbol) {
    if (!methodSymbol.isReturningSymbolPresent()) {
      return false;
    }
    var possibleType = methodSymbol.getReturningSymbol().getType();
    return possibleType.filter(symbol -> symbolAndScopeManagement.getEk9Types().ek9Void().isExactSameType(symbol))
        .isPresent();

  }

  private void testReturnType(final MethodSymbol methodSymbol,
                              final String expectedType,
                              final ErrorListener.SemanticClassification errorIfInvalid) {

    var validReturnType = false;
    if (methodSymbol.isReturningSymbolPresent()) {
      final var theType = methodSymbol.resolve(new TypeSymbolSearch(expectedType));
      final var returningType = methodSymbol.getReturningSymbol().getType();
      if (returningType.isPresent() && theType.isPresent()) {
        validReturnType = returningType.get().isExactSameType(theType.get());
      }
    }

    if (!validReturnType) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS, errorIfInvalid);
    }

  }

  private void testNoArguments(final MethodSymbol methodSymbol) {

    if (!methodSymbol.getSymbolsForThisScope().isEmpty()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_MANY_ARGUMENTS);
    }

  }

  private void testSingleArgument(final MethodSymbol methodSymbol) {

    if (methodSymbol.getSymbolsForThisScope().isEmpty()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_FEW_ARGUMENTS);
    } else if (methodSymbol.getSymbolsForThisScope().size() > 1) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_MANY_ARGUMENTS);
    }

  }

  private void emitBadNotEqualsOperator(final MethodSymbol methodSymbol) {

    errorListener.semanticError(methodSymbol.getSourceToken(), "alternative:",
        ErrorListener.SemanticClassification.BAD_NOT_EQUAL_OPERATOR);

  }

  private void emitBadNotOperator(final MethodSymbol methodSymbol) {

    errorListener.semanticError(methodSymbol.getSourceToken(), "alternative:",
        ErrorListener.SemanticClassification.BAD_NOT_OPERATOR);

  }
}