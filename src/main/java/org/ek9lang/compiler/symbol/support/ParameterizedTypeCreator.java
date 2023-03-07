package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;

/**
 * Just created a Parameterized type from the generic type and the parameterizing parameters.
 */
public class ParameterizedTypeCreator implements BiFunction<AggregateSymbol, List<ISymbol>, ParameterisedTypeSymbol> {

  @Override
  public ParameterisedTypeSymbol apply(AggregateSymbol genericAggregateType, List<ISymbol> parameterizingTypes) {
    var scope = genericAggregateType.getModuleScope();
    var theType = new ParameterisedTypeSymbol(genericAggregateType, parameterizingTypes, scope);
    //Use the same source token for this as the generic type.
    theType.setModuleScope(genericAggregateType.getModuleScope());
    theType.setParsedModule(genericAggregateType.getParsedModule());
    theType.setSourceToken(genericAggregateType.getSourceToken());

    return theType;
  }
}
