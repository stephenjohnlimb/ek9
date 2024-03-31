package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.SymbolFactory.DEFAULTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Designed to gather aspect of operator population.
 */
public class OperatorFactory {

  private final AggregateFactory aggregateFactory;

  public OperatorFactory(final AggregateFactory aggregateFactory) {
    this.aggregateFactory = aggregateFactory;
  }

  void addEnumerationMethods(AggregateSymbol enumerationSymbol) {
    final Optional<ISymbol> booleanType = aggregateFactory.resolveBoolean(enumerationSymbol);
    final Optional<ISymbol> integerType = aggregateFactory.resolveInteger(enumerationSymbol);
    final Optional<ISymbol> stringType = aggregateFactory.resolveString(enumerationSymbol);
    final Optional<ISymbol> jsonType = aggregateFactory.resolveJson(enumerationSymbol);


    //Some reasonable operations

    aggregateFactory.addComparatorOperator(enumerationSymbol, "<=>", integerType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "==", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<>", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, ">", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<=", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, ">=", booleanType);

    //For an enumeration we provide implementations for comparisons against strings as well.
    //We can convert to the enumeration and then just compare.
    stringType.ifPresent(string -> {
      var argType = (IAggregateSymbol) string;
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, "<=>", integerType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, "==", booleanType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, "<>", booleanType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, "<", booleanType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, ">", booleanType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, "<=", booleanType);
      aggregateFactory.addComparatorOperator(enumerationSymbol, argType, ">=", booleanType);
    });

    //isSet
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "?", booleanType);
    //Now a _string $ operator
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "$", stringType);
    //Promote to String operator
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "#^", stringType);

    //To JSON operator
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "$$", jsonType);
    //hash code
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "#?", integerType);

    //First and last
    aggregateFactory.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#<");
    aggregateFactory.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#>");
  }

  List<MethodSymbol> getAllPossibleDefaultOperators(final IAggregateSymbol aggregate) {

    return new ArrayList<>(Arrays.asList(
        getDefaultOperator(aggregate, "<=>"),
        getDefaultOperator(aggregate, "=="),
        getDefaultOperator(aggregate, "<>"),
        getDefaultOperator(aggregate, "<"),
        getDefaultOperator(aggregate, "<="),
        getDefaultOperator(aggregate, ">"),
        getDefaultOperator(aggregate, ">="),
        getDefaultOperator(aggregate, "?"),
        getDefaultOperator(aggregate, "$"),
        getDefaultOperator(aggregate, "$$"),
        getDefaultOperator(aggregate, "#?")
    ));
  }

  MethodSymbol getDefaultOperator(final IAggregateSymbol aggregate, final String operator) {
    final Optional<ISymbol> integerType = aggregateFactory.resolveInteger(aggregate);
    final Optional<ISymbol> stringType = aggregateFactory.resolveString(aggregate);
    final Optional<ISymbol> booleanType = aggregateFactory.resolveBoolean(aggregate);
    final Optional<ISymbol> jsonType = aggregateFactory.resolveJson(aggregate);

    final var rtn = switch (operator) {
      case "<", "<=", ">", ">=", "==", "<>":
        yield aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, operator, booleanType);
      case "<=>":
        yield aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, operator, integerType);
      case "?":
        yield aggregateFactory.createPurePublicSimpleOperator(aggregate, operator, booleanType);
      case "$":
        yield aggregateFactory.createPurePublicSimpleOperator(aggregate, operator, stringType);
      case "$$":
        yield aggregateFactory.createPurePublicSimpleOperator(aggregate, operator, jsonType);
      case "#?":
        yield aggregateFactory.createPurePublicSimpleOperator(aggregate, operator, integerType);
      default:
        yield null;
    };
    if (rtn != null) {
      //It has been default and is a synthetic method (i.e. not one defined explicitly by the developer)
      rtn.putSquirrelledData(DEFAULTED, "TRUE");
      rtn.setSynthetic(true);
    }
    return rtn;
  }

  List<MethodSymbol> getAllPossibleSyntheticOperators(final IAggregateSymbol aggregate) {
    final Optional<ISymbol> integerType = aggregateFactory.resolveInteger(aggregate);
    final Optional<ISymbol> booleanType = aggregateFactory.resolveBoolean(aggregate);

    var theDefaultOperators = getAllPossibleDefaultOperators(aggregate);

    var additionalOperators = new ArrayList<>(Arrays.asList(
        aggregateFactory.createToJsonSimpleOperator(aggregate),

        //Cannot default the promote operator
        aggregateFactory.createPurePublicReturnSameTypeMethod(aggregate, "#<"),
        aggregateFactory.createPurePublicReturnSameTypeMethod(aggregate, "#>"),
        aggregateFactory.createPurePublicReturnSameTypeMethod(aggregate, "~"),
        aggregateFactory.createPurePublicReturnSameTypeMethod(aggregate, "abs"),
        //This is the unary minus
        aggregateFactory.createPurePublicReturnSameTypeMethod(aggregate, "-"),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "empty", booleanType),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "length", integerType),

        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, ">>", Optional.of(aggregate)),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<<", Optional.of(aggregate)),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "and", Optional.of(aggregate)),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "or", Optional.of(aggregate)),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "xor", Optional.of(aggregate)),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "mod", integerType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "rem", integerType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "contains", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "matches", booleanType),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "open", Optional.empty()),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "close", Optional.empty()),

        //Other operators

        //So for operators these will deal in the same type.
        aggregateFactory.createOperator(aggregate, "+", true),
        aggregateFactory.createOperator(aggregate, "-", true),
        aggregateFactory.createOperator(aggregate, "*", true),
        aggregateFactory.createOperator(aggregate, "/", true),

        //Mutator type operators
        aggregateFactory.createOperator(aggregate, ":~:", false),
        aggregateFactory.createOperator(aggregate, ":^:", false),
        aggregateFactory.createOperator(aggregate, ":=:", false),
        aggregateFactory.createOperator(aggregate, "|", false),
        aggregateFactory.createOperator(aggregate, "+=", false),
        aggregateFactory.createOperator(aggregate, "-=", false),
        aggregateFactory.createOperator(aggregate, "*=", false),
        aggregateFactory.createOperator(aggregate, "/=", false),
        aggregateFactory.createOperator(aggregate, "++", false),
        aggregateFactory.createOperator(aggregate, "--", false),

        //fuzzy compare
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<~>", integerType)));

    List<MethodSymbol> rtn = new ArrayList<>(theDefaultOperators.size() + additionalOperators.size());
    rtn.addAll(theDefaultOperators);
    rtn.addAll(additionalOperators);
    return rtn;
  }
}
