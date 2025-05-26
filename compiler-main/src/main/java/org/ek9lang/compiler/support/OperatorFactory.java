package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.DEFAULTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Designed to gather aspect of operator population.
 */
public class OperatorFactory {

  private final AggregateManipulator aggregateManipulator;

  public OperatorFactory(final AggregateManipulator aggregateManipulator) {

    this.aggregateManipulator = aggregateManipulator;

  }

  void addEnumerationMethods(final AggregateSymbol enumerationSymbol) {

    final var booleanType = aggregateManipulator.resolveBoolean(enumerationSymbol);
    final var integerType = aggregateManipulator.resolveInteger(enumerationSymbol);
    final var stringType = aggregateManipulator.resolveString(enumerationSymbol);
    final var jsonType = aggregateManipulator.resolveJson(enumerationSymbol);

    //Some reasonable operations

    aggregateManipulator.addComparatorOperator(enumerationSymbol, "<=>", integerType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, "==", booleanType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, "<>", booleanType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, "<", booleanType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, ">", booleanType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, "<=", booleanType);
    aggregateManipulator.addComparatorOperator(enumerationSymbol, ">=", booleanType);

    //For an enumeration we provide implementations for comparisons against strings as well.
    //We can convert to the enumeration and then just compare.
    stringType.ifPresent(string -> {
      var argType = (IAggregateSymbol) string;
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, "<=>", integerType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, "==", booleanType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, "<>", booleanType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, "<", booleanType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, ">", booleanType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, "<=", booleanType);
      aggregateManipulator.addComparatorOperator(enumerationSymbol, argType, ">=", booleanType);
    });

    //isSet
    aggregateManipulator.addPurePublicSimpleOperator(enumerationSymbol, "?", booleanType);
    //Now a _string $ operator
    aggregateManipulator.addPurePublicSimpleOperator(enumerationSymbol, "$", stringType);
    //Promote to String operator
    aggregateManipulator.addPurePublicSimpleOperator(enumerationSymbol, "#^", stringType);

    //To JSON operator
    aggregateManipulator.addPurePublicSimpleOperator(enumerationSymbol, "$$", jsonType);
    //hash code
    aggregateManipulator.addPurePublicSimpleOperator(enumerationSymbol, "#?", integerType);

    //First and last
    aggregateManipulator.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#<");
    aggregateManipulator.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#>");

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

    final var integerType = aggregateManipulator.resolveInteger(aggregate);
    final var stringType = aggregateManipulator.resolveString(aggregate);
    final var booleanType = aggregateManipulator.resolveBoolean(aggregate);
    final var jsonType = aggregateManipulator.resolveJson(aggregate);
    final var rtn = switch (operator) {
      case "<", "<=", ">", ">=", "==", "<>":
        yield aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, operator, booleanType);
      case "<=>":
        yield aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, operator, integerType);
      case "?":
        yield aggregateManipulator.createPurePublicSimpleOperator(aggregate, operator, booleanType);
      case "$":
        yield aggregateManipulator.createPurePublicSimpleOperator(aggregate, operator, stringType);
      case "$$":
        yield aggregateManipulator.createPurePublicSimpleOperator(aggregate, operator, jsonType);
      case "#?":
        yield aggregateManipulator.createPurePublicSimpleOperator(aggregate, operator, integerType);
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

    final var integerType = aggregateManipulator.resolveInteger(aggregate);
    final var booleanType = aggregateManipulator.resolveBoolean(aggregate);

    final var theDefaultOperators = getAllPossibleDefaultOperators(aggregate);
    final var additionalOperators = new ArrayList<>(Arrays.asList(
        aggregateManipulator.createToJsonSimpleOperator(aggregate),
        //Cannot default the promote operator
        aggregateManipulator.createPurePublicReturnSameTypeMethod(aggregate, "#<"),
        aggregateManipulator.createPurePublicReturnSameTypeMethod(aggregate, "#>"),
        aggregateManipulator.createPurePublicReturnSameTypeMethod(aggregate, "~"),
        aggregateManipulator.createPurePublicReturnSameTypeMethod(aggregate, "abs"),
        //This is the unary minus
        aggregateManipulator.createPurePublicReturnSameTypeMethod(aggregate, "-"),
        aggregateManipulator.createPurePublicSimpleOperator(aggregate, "empty", booleanType),
        aggregateManipulator.createPurePublicSimpleOperator(aggregate, "length", integerType),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, ">>", Optional.of(aggregate)),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<<", Optional.of(aggregate)),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "and", Optional.of(aggregate)),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "or", Optional.of(aggregate)),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "xor", Optional.of(aggregate)),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "mod", integerType),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "rem", integerType),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "contains", booleanType),
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "matches", booleanType),
        aggregateManipulator.createPurePublicSimpleOperator(aggregate, "open", Optional.empty()),
        aggregateManipulator.createPurePublicSimpleOperator(aggregate, "close", Optional.empty()),

        //So for operators these will deal in the same type.
        aggregateManipulator.createOperator(aggregate, "+", true),
        aggregateManipulator.createOperator(aggregate, "-", true),
        aggregateManipulator.createOperator(aggregate, "*", true),
        aggregateManipulator.createOperator(aggregate, "/", true),

        //Mutator type operators
        aggregateManipulator.createOperator(aggregate, ":~:", false),
        aggregateManipulator.createOperator(aggregate, ":^:", false),
        aggregateManipulator.createOperator(aggregate, ":=:", false),
        aggregateManipulator.createOperator(aggregate, "|", false),
        aggregateManipulator.createOperator(aggregate, "+=", false),
        aggregateManipulator.createOperator(aggregate, "-=", false),
        aggregateManipulator.createOperator(aggregate, "*=", false),
        aggregateManipulator.createOperator(aggregate, "/=", false),
        aggregateManipulator.createMutatorOperator(aggregate, "++"),
        aggregateManipulator.createMutatorOperator(aggregate, "--"),

        //fuzzy compare
        aggregateManipulator.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<~>", integerType)));

    final List<MethodSymbol> rtn = new ArrayList<>(theDefaultOperators.size() + additionalOperators.size());
    rtn.addAll(theDefaultOperators);
    rtn.addAll(additionalOperators);

    return rtn;
  }
}
