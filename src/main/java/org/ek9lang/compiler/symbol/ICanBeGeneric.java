package org.ek9lang.compiler.symbol;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Really a Marker interface for generic classes and generic functions.
 */
public interface ICanBeGeneric extends ISymbol {

  /**
   * Used to keep track of any parameterised types used in a generic type that use some or
   * all of the generic parameters.
   */
  List<ParameterisedTypeSymbol> getParameterisedTypeReferences();

  /**
   * Keep track of parameterised functions used.
   */
  List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences();

  void addParameterisedFunctionReference(ParameterisedFunctionSymbol parameterisedFunctionReference);

  void addParameterisedTypeReference(ParameterisedTypeSymbol parameterisedTypeReference);

  void addParameterType(AggregateSymbol parameterisedType);

  List<ISymbol> getAnyGenericParameters();

  List<ISymbol> getParameterTypes();

  void setContextForParameterisedType(ParserRuleContext ctx);

}
