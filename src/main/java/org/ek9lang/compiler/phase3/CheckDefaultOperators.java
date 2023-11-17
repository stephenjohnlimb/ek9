package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_PROPERTY_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_SUPER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_THIS;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that operators that have been 'defaulted' could actually be implemented.
 * This is done by checking if any super has a suitable or compatible operators and also that
 * any properties/fields also have a suitable operator.
 * <br/>
 * All the equality checks really reply on a comparator (&lt;=&gt;) operator.
 * These are the operators that can be defaulted.
 * <pre>
 * "&lt;", "&lt;=", "&gt;", "&gt;=", "==", "&lt;&gt;":
 * "&lt;=&gt;":
 * "?":
 * "$":
 * "$$":
 * "#?":
 * </pre>
 */
final class CheckDefaultOperators extends TypedSymbolAccess implements Consumer<AggregateSymbol> {
  private final RetrieveDefaultedOperators retrieveDefaultedOperators = new RetrieveDefaultedOperators();
  private final CheckPropertyNames checkPropertyNames;

  CheckDefaultOperators(final SymbolAndScopeManagement symbolAndScopeManagement, final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkPropertyNames = new CheckPropertyNames(symbolAndScopeManagement, errorListener,
        CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {
    var operators = retrieveDefaultedOperators.apply(aggregateSymbol);

    checkIfPropertiesAreDelegates(aggregateSymbol, operators);

    //This will deal with all the less than, equal not equal greater than etc.
    checkComparator(aggregateSymbol, operators);

    //This deals with all the no param operators.
    checkUnaryOperators(aggregateSymbol, operators);

    checkToJsonSupportableIfRequired(aggregateSymbol, operators);
  }

  private void checkIfPropertiesAreDelegates(final AggregateSymbol aggregateSymbol,
                                             final List<MethodSymbol> operators) {


    final Consumer<ISymbol> propertyOperatorError = property -> operators.stream()
        .filter(operator -> !"?".equals(operator.getName()))
        .forEach(operator -> errorListener.semanticError(operator.getSourceToken(), "",
            FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS));

    aggregateSymbol.getProperties().stream()
        .filter(property -> property.getType().isPresent())
        .filter(property -> property.getType().get() instanceof FunctionSymbol)
        .forEach(propertyOperatorError);

  }

  private void checkComparator(final AggregateSymbol aggregateSymbol, final List<MethodSymbol> operators) {
    //First check if needs and equality check at all
    var requiresComparator = operators.stream().filter(this::isASortOfComparisonOperator).findFirst();

    //Now this means that we require the <=> on this aggregate and on any super.
    requiresComparator.ifPresent(method -> {
      checkForComparatorOrError(aggregateSymbol, method.getSourceToken(), MISSING_OPERATOR_IN_THIS);

      aggregateSymbol.getSuperAggregate().ifPresent(
          superAggregate -> checkForComparatorOrError(superAggregate, method.getSourceToken(),
              MISSING_OPERATOR_IN_SUPER));
    });
  }

  private void checkUnaryOperators(final AggregateSymbol aggregateSymbol, final List<MethodSymbol> operators) {

    //Get all the properties on this aggregate, we will need to check them for each operator.
    var properties = aggregateSymbol.getProperties();

    operators.stream().filter(this::isNotASortOfComparisonOperator).forEach(operator -> {

      //First if there is a super check it has the same operator - it must
      aggregateSymbol.getSuperAggregate()
          .ifPresent(superAggregate -> checkForOperatorOnPropertyTypeOrError(superAggregate, operator));

      //Now check each of the properties on this aggregate also has the operator.
      properties.forEach(property -> property.getType()
          .ifPresent(propertyType -> checkForOperatorOnPropertyTypeOrError(propertyType, property,
              operator)));
    });
  }

  private void checkForOperatorOnPropertyTypeOrError(final ISymbol type,
                                                     final ISymbol property,
                                                     final MethodSymbol operator) {
    if (type instanceof IAggregateSymbol aggregate) {
      checkForOperatorOnPropertyTypeOrError(aggregate, property, operator);
    }
  }

  private void checkForOperatorOnPropertyTypeOrError(final IAggregateSymbol aggregate,
                                                     final MethodSymbol operator) {
    var search = new MethodSymbolSearch(operator);

    if (aggregate.resolveInThisScopeOnly(search).isEmpty()) {
      emitMissingOperator(aggregate, operator.getSourceToken(), search, MISSING_OPERATOR_IN_SUPER);
    }
  }

  private void checkForOperatorOnPropertyTypeOrError(final IAggregateSymbol aggregate,
                                                     final ISymbol property,
                                                     final MethodSymbol operator) {
    var search = new MethodSymbolSearch(operator);

    if (aggregate.resolve(search).isEmpty()) {
      var msg = "Relating to '" + property.getFriendlyName() + "', it requires operator '" + search + "':";
      errorListener.semanticError(operator.getSourceToken(), msg, MISSING_OPERATOR_IN_PROPERTY_TYPE);
    }
  }

  private void checkToJsonSupportableIfRequired(final AggregateSymbol aggregateSymbol,
                                                final List<MethodSymbol> operators) {
    //Only is to JSON is required.
    operators
        .stream()
        .filter(operator -> "$$".equals(operator.getName()))
        .findFirst()
        .ifPresent(toJSON -> checkPropertyNames.accept(aggregateSymbol));
  }

  private boolean isNotASortOfComparisonOperator(final MethodSymbol operator) {
    return !isASortOfComparisonOperator(operator);
  }

  private boolean isASortOfComparisonOperator(final MethodSymbol operator) {
    return switch (operator.getName()) {
      case "<", "<=", ">", ">=", "==", "<>", "<=>":
        yield true;
      default:
        yield false;
    };
  }

  /**
   * Check for the <=> operator on the aggregate because we have an operator that needs is < <= >= > == <>.
   */
  private void checkForComparatorOrError(final IAggregateSymbol aggregate, final IToken sourceToken,
                                         final ErrorListener.SemanticClassification errorClassification) {
    var search = new MethodSymbolSearch("<=>").addTypeParameter(aggregate);

    if (aggregate.resolveInThisScopeOnly(search).isEmpty()) {
      emitMissingOperator(aggregate, sourceToken, search, errorClassification);
      //Then <=> is missing and a default implementation can not be created.
    }
  }

  private void emitMissingOperator(final IAggregateSymbol aggregateSymbol, final IToken sourceToken,
                                   final SymbolSearch operatorSearch,
                                   final ErrorListener.SemanticClassification errorClassification) {

    var msg = "Relating to '" + aggregateSymbol.getFriendlyName() + "' requires operator '" + operatorSearch + "':";
    errorListener.semanticError(sourceToken, msg, errorClassification);
  }
}
