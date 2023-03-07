package org.ek9lang.compiler.symbol;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Really a Marker interface for generic classes and generic functions.
 */
public interface ICanBeGeneric extends ISymbol {

  void addParameterisedFunctionReference(ParameterisedFunctionSymbol parameterisedFunctionReference);

  List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences();

  void addParameterisedTypeReference(ParameterisedTypeSymbol parameterisedTypeReference);

  List<ParameterisedTypeSymbol> getParameterisedTypeReferences();

  void addParameterisedType(AggregateSymbol parameterisedType);

  List<ISymbol> getAnyGenericParameters();

  List<ISymbol> getParameterisedTypes();

  void setContextForParameterisedType(ParserRuleContext ctx);

}
