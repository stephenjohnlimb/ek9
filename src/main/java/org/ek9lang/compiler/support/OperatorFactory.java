package org.ek9lang.compiler.support;

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
    //compare
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<=>", integerType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "==", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<>", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, ">", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, "<=", booleanType);
    aggregateFactory.addComparatorOperator(enumerationSymbol, ">=", booleanType);

    //isSet
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "?", booleanType);
    //Now a _string $ operator
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "$", stringType);

    //To JSON operator
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "$$", jsonType);
    //hash code
    aggregateFactory.addPurePublicSimpleOperator(enumerationSymbol, "#?", integerType);

    //First and last
    aggregateFactory.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#<");
    aggregateFactory.addPurePublicReturnSameTypeMethod(enumerationSymbol, "#>");
  }

  List<MethodSymbol> getAllPossibleDefaultOperators(IAggregateSymbol aggregate) {
    final Optional<ISymbol> integerType = aggregateFactory.resolveInteger(aggregate);
    final Optional<ISymbol> stringType = aggregateFactory.resolveString(aggregate);
    final Optional<ISymbol> booleanType = aggregateFactory.resolveBoolean(aggregate);
    final Optional<ISymbol> jsonType = aggregateFactory.resolveJson(aggregate);

    return new ArrayList<>(Arrays.asList(
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<=", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, ">", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, ">=", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "==", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<>", booleanType),
        aggregateFactory.createPureAcceptSameTypeOperatorAndReturnType(aggregate, "<=>", integerType),

        aggregateFactory.createPurePublicSimpleOperator(aggregate, "?", booleanType),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "$", stringType),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "$$", jsonType),
        aggregateFactory.createPurePublicSimpleOperator(aggregate, "#?", integerType)));
  }

  List<MethodSymbol> getAllPossibleSyntheticOperators(IAggregateSymbol aggregate) {
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
