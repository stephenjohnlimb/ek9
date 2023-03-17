package org.ek9lang.compiler.symbol;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Really a Marker interface for generic classes and generic functions.
 */
public interface ICanBeGeneric extends ISymbol {

  Optional<PossibleGenericSymbol> getGenericType();

  /**
   * Used to keep track of any parameterised types/function used in a generic type that use some or
   * all of the generic parameters.
   */
  List<PossibleGenericSymbol> getParameterisedTypeReferences();

  void addParameterisedTypeReference(PossibleGenericSymbol parameterisedTypeReference);

  void addParameterType(ISymbol parameterisedType);

  List<ISymbol> getAnyGenericParameters();

  List<ISymbol> getParameterTypesOrArguments();

  void setContextForParameterisedType(ParserRuleContext ctx);

}
