package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_PROPERTY_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_SUPER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_OPERATOR_IN_THIS;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that operators that have been 'defaulted' could actually be implemented.
 * This is done by checking if any super has a suitable or compatible operators and also that
 * any properties/fields also have a suitable operator.
 * <br/>
 * All the equality checks really reply on a comparator (<=>) operator.
 * These are the operators that can be defaulted.
 * <pre>
 * "<", "<=", ">", ">=", "==", "<>":
 * "<=>":
 * "?":
 * "$":
 * "$$":
 * "#?":
 * </pre>
 */
final class CheckDefaultOperators extends RuleSupport implements Consumer<AggregateSymbol> {
  private final RetrieveDefaultedOperators retrieveDefaultedOperators = new RetrieveDefaultedOperators();

  CheckDefaultOperators(final SymbolAndScopeManagement symbolAndScopeManagement, final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {
    var operators = retrieveDefaultedOperators.apply(aggregateSymbol);

    //This will deal with all the less than, equal not equal greater than etc.
    checkComparator(aggregateSymbol, operators);

    //This deals with all the no param operators.
    checkUnaryOperators(aggregateSymbol, operators);

  }

  private void checkComparator(final AggregateSymbol aggregateSymbol, final List<MethodSymbol> operators) {
    //First check if needs and equality check at all
    var requiresComparator = operators.stream().filter(this::isASortOfComparisonOperator).findFirst();

    //Now this means that we require the <=> on this aggregate and on any super.
    requiresComparator.ifPresent(method -> {
      checkForComparatorOrError(aggregateSymbol, method.getSourceToken(), MISSING_OPERATOR_IN_THIS);

      aggregateSymbol.getSuperAggregateSymbol().ifPresent(
          superAggregate -> checkForComparatorOrError(superAggregate, method.getSourceToken(),
              MISSING_OPERATOR_IN_SUPER));
    });
  }

  private void checkUnaryOperators(final AggregateSymbol aggregateSymbol, final List<MethodSymbol> operators) {

    var properties = aggregateSymbol.getProperties();
    operators.stream().filter(this::isNotASortOfComparisonOperator).forEach(operator -> {
      //First if there is a super check it has the operator
      aggregateSymbol.getSuperAggregateSymbol()
          .ifPresent(superAggregate -> checkForOperatorOrError(superAggregate, operator));

      //Now check each of the properties on this aggregate also has the operator.
      properties.forEach(property -> property.getType()
          .ifPresent(propertyType -> checkForOperatorOrError((IAggregateSymbol) propertyType, property, operator)));
    });
  }

  private void checkForOperatorOrError(final IAggregateSymbol aggregate,
                                       final ISymbol property,
                                       final MethodSymbol operator) {
    var search = new MethodSymbolSearch(operator);

    if (aggregate.resolveInThisScopeOnly(search).isEmpty()) {
      var msg = "Relating to '" + property.getFriendlyName() + "', requires operator '" + search + "':";

      errorListener.semanticError(operator.getSourceToken(), msg, MISSING_OPERATOR_IN_PROPERTY_TYPE);
    }
  }

  private void checkForOperatorOrError(final IAggregateSymbol aggregate,
                                       final MethodSymbol operator) {
    var search = new MethodSymbolSearch(operator);

    if (aggregate.resolveInThisScopeOnly(search).isEmpty()) {
      emitMissingOperator(aggregate, operator.getSourceToken(), search, MISSING_OPERATOR_IN_SUPER);
    }
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
