package org.ek9lang.compiler.phase2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.AppropriateBodyOrError;
import org.ek9lang.compiler.common.ContextSupportsAbstractMethodOrError;
import org.ek9lang.compiler.common.Defaulted;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OverrideOrAbstractOrError;
import org.ek9lang.compiler.common.ProcessTraitMethodOrError;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
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
final class ValidOperatorOrError extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.OperatorDeclarationContext> {

  private static final String OPERATOR_SEMANTICS = "operator semantics:";
  private final ProcessTraitMethodOrError processTraitMethodOrError;
  private final TraitMethodAcceptableOrError traitMethodAcceptableOrError;
  private final ContextSupportsAbstractMethodOrError contextSupportsAbstractMethodOrError;
  private final AppropriateBodyOrError appropriateBodyOrError;
  private final Map<String, Consumer<MethodSymbol>> operatorChecks;
  private final OverrideOrAbstractOrError overrideOrAbstractOrError;
  private final Defaulted defaulted = new Defaulted();

  /**
   * Create a new operation checker.
   */
  ValidOperatorOrError(final SymbolsAndScopes symbolsAndScopes,
                       final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.operatorChecks = populateOperatorChecks();

    traitMethodAcceptableOrError = new TraitMethodAcceptableOrError(errorListener);
    contextSupportsAbstractMethodOrError =
        new ContextSupportsAbstractMethodOrError(symbolsAndScopes, errorListener);
    processTraitMethodOrError = new ProcessTraitMethodOrError(errorListener);
    appropriateBodyOrError = new AppropriateBodyOrError(symbolsAndScopes, errorListener);
    overrideOrAbstractOrError = new OverrideOrAbstractOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol, final EK9Parser.OperatorDeclarationContext ctx) {


    if (!defaulted.test(methodSymbol)) {
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
        "<", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        "<=", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        ">", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        ">=", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        "==", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        "<>", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        "<=>", addPureCheck(this::oneArgumentReturnTypeIntegerOrError),
        "<~>", addPureCheck(this::oneArgumentReturnTypeIntegerOrError),
        "!=", addPureCheck(this::emitBadNotEqualsOperator));

    final Map<String, Consumer<MethodSymbol>> simpleOperatorChecks = Map.of(
        "sqrt", addPureCheck(this::noArgumentsReturnAnyTypeOrError),
        "!", addPureCheck(this::noArgumentsReturnAnyTypeOrError),
        "?", addPureCheck(this::noArgumentsReturnTypeBooleanOrError),
        "~", addPureCheck(this::noArgumentsReturnConstructTypeOrError),
        "-", addPureCheck(this::minusOperatorOrError),
        "+", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "*", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "/", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "^", addPureCheck(this::oneArgumentReturnAnyTypeOrError)
    );

    final Map<String, Consumer<MethodSymbol>> noArgumentWithReturnChecks = Map.of(
        "#^", addPureCheck(this::noArgumentsReturnAnyTypeOtherThanSelfOrError),
        "$$", addPureCheck(this::noArgumentsReturnTypeJsonOrError),
        "$", addPureCheck(this::noArgumentsReturnTypeStringOrError),
        "#?", addPureCheck(this::noArgumentsReturnTypeIntegerOrError),
        "#<", addPureCheck(this::noArgumentsReturnAnyTypeOrError),
        "#>", addPureCheck(this::noArgumentsReturnAnyTypeOrError),
        "not", addPureCheck(this::emitBadNotOperator),
        "abs", addPureCheck(this::noArgumentsReturnConstructTypeOrError),
        "empty", addPureCheck(this::noArgumentsReturnTypeBooleanOrError),
        "length", addPureCheck(this::noArgumentsReturnTypeIntegerOrError)
    );

    final Map<String, Consumer<MethodSymbol>> oneArgumentWithReturnChecks = Map.of(
        ">>", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "<<", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "and", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "or", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "xor", addPureCheck(this::oneArgumentReturnAnyTypeOrError),
        "mod", addPureCheck(this::oneArgumentReturnTypeIntegerOrError),
        "rem", addPureCheck(this::oneArgumentReturnTypeIntegerOrError),
        "contains", addPureCheck(this::oneArgumentReturnTypeBooleanOrError),
        "matches", addPureCheck(this::oneArgumentReturnTypeBooleanOrError)
    );

    final Map<String, Consumer<MethodSymbol>> noArgumentNoReturn = Map.of(
        "open", addPureCheck(this::noArgumentsReturnAnyTypeOrError),
        "close", addPureCheck(this::noArgumentsNoReturnOrError)
    );

    rtn.putAll(logicalOperatorChecks);
    rtn.putAll(simpleOperatorChecks);
    rtn.putAll(noArgumentWithReturnChecks);
    rtn.putAll(oneArgumentWithReturnChecks);
    rtn.putAll(noArgumentNoReturn);

    //Now for each of those operators tag on the is pure check.
    final Map<String, Consumer<MethodSymbol>> mutatorChecks = Map.of(
        ":~:", addNonPureCheck(this::oneArgumentNoReturnOrError),
        ":^:", addNonPureCheck(this::oneArgumentNoReturnOrError),
        ":=:", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "|", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "+=", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "-=", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "*=", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "/=", addNonPureCheck(this::oneArgumentNoReturnOrError),
        "++", addNonPureCheck(this::noArgumentsReturnConstructTypeOrError),
        "--", addNonPureCheck(this::noArgumentsReturnConstructTypeOrError)
    );

    rtn.putAll(mutatorChecks);

    return rtn;
  }

  private void minusOperatorOrError(final MethodSymbol methodSymbol) {

    if (methodSymbol.getCallParameters().isEmpty()) {
      noArgumentsReturnConstructTypeOrError(methodSymbol);
    } else {
      oneArgumentReturnAnyTypeOrError(methodSymbol);
    }

  }

  private Consumer<MethodSymbol> addPureCheck(final Consumer<MethodSymbol> check) {

    return check.andThen(this::isPureOrError);
  }

  private Consumer<MethodSymbol> addNonPureCheck(final Consumer<MethodSymbol> check) {

    return check.andThen(this::notPureOrError);
  }

  private void noArgumentsReturnAnyTypeOtherThanSelfOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    final var returnType = anyReturnTypeOrError(methodSymbol);

    returnType.ifPresent(theType -> {
      if (methodSymbol.getParentScope() instanceof AggregateSymbol parentAggregate) {
        final var errorToken = methodSymbol.getReturningSymbol().getSourceToken();
        notSameTypeOrError(errorToken, parentAggregate, theType);
      }
    });

  }

  private void noArgumentsReturnAnyTypeOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    anyReturnTypeOrError(methodSymbol);

  }

  private void noArgumentsReturnTypeBooleanOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    returnTypeIsBooleanOrError(methodSymbol);

  }

  private void noArgumentsReturnTypeIntegerOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    returnTypeIsIntegerOrError(methodSymbol);

  }

  private void noArgumentsReturnTypeStringOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    validReturnTypeOrError(methodSymbol, "String", ErrorListener.SemanticClassification.MUST_RETURN_STRING);

  }

  private void noArgumentsReturnTypeJsonOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    validReturnTypeOrError(methodSymbol, "JSON", ErrorListener.SemanticClassification.MUST_RETURN_JSON);

  }

  private void oneArgumentReturnTypeIntegerOrError(final MethodSymbol methodSymbol) {

    singleArgumentOrError(methodSymbol);
    returnTypeIsIntegerOrError(methodSymbol);

  }

  private void oneArgumentReturnTypeBooleanOrError(final MethodSymbol methodSymbol) {

    singleArgumentOrError(methodSymbol);
    returnTypeIsBooleanOrError(methodSymbol);

  }

  private void returnTypeIsBooleanOrError(final MethodSymbol methodSymbol) {

    validReturnTypeOrError(methodSymbol, "Boolean", ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN);

  }

  private void returnTypeIsIntegerOrError(MethodSymbol methodSymbol) {

    validReturnTypeOrError(methodSymbol, "Integer", ErrorListener.SemanticClassification.MUST_RETURN_INTEGER);

  }

  private void oneArgumentReturnAnyTypeOrError(final MethodSymbol methodSymbol) {

    singleArgumentOrError(methodSymbol);
    anyReturnTypeOrError(methodSymbol);

  }

  private void oneArgumentNoReturnOrError(final MethodSymbol methodSymbol) {

    singleArgumentOrError(methodSymbol);
    noReturnOrError(methodSymbol);

  }

  private void noArgumentsNoReturnOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    noReturnOrError(methodSymbol);

  }

  private void notSameTypeOrError(final IToken token, final ISymbol s1, final ISymbol s2) {

    if (s1.isExactSameType(s2)) {
      errorListener.semanticError(token, OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.MUST_NOT_RETURN_SAME_TYPE);
    }

  }

  private void isPureOrError(final MethodSymbol methodSymbol) {

    if (!methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_MUST_BE_PURE);
    }

  }

  private void notPureOrError(final MethodSymbol methodSymbol) {

    if (methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_CANNOT_BE_PURE);
    }

  }

  private void noArgumentsReturnConstructTypeOrError(final MethodSymbol methodSymbol) {

    noArgumentsOrError(methodSymbol);
    final var parentScope = methodSymbol.getParentScope();
    final var returnType = anyReturnTypeOrError(methodSymbol);

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

  private Optional<ISymbol> anyReturnTypeOrError(final MethodSymbol methodSymbol) {

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
  private void noReturnOrError(final MethodSymbol methodSymbol) {

    if (methodSymbol.isReturningSymbolPresent() && methodSymbol.getReturningSymbol().getType().isPresent()) {
      methodSymbol.getReturningSymbol().getType().ifPresent(theType -> {
        if (!symbolsAndScopes.getEk9Types().ek9Void().isExactSameType(theType)) {
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
    return possibleType.filter(symbol -> symbolsAndScopes.getEk9Types().ek9Void().isExactSameType(symbol))
        .isPresent();

  }

  private void validReturnTypeOrError(final MethodSymbol methodSymbol,
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

  private void noArgumentsOrError(final MethodSymbol methodSymbol) {

    if (!methodSymbol.getSymbolsForThisScope().isEmpty()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_MANY_ARGUMENTS);
    }

  }

  private void singleArgumentOrError(final MethodSymbol methodSymbol) {

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