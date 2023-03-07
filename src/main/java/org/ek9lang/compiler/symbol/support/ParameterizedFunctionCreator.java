package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;

/**
 * Creates a parameterized function from a generic function and a list of parameterizing types.
 */
public class ParameterizedFunctionCreator
    implements BiFunction<FunctionSymbol, List<ISymbol>, ParameterisedFunctionSymbol> {

  @Override
  public ParameterisedFunctionSymbol apply(FunctionSymbol genericFunction, List<ISymbol> parameterizingTypes) {
    //Use the same module scope as the generic type as there is where the new type will reside.
    var scope = genericFunction.getModuleScope();
    var theFunction = new ParameterisedFunctionSymbol(genericFunction, parameterizingTypes, scope);
    //Use the same source token for this as the generic function.
    theFunction.setModuleScope(genericFunction.getModuleScope());
    theFunction.setParsedModule(genericFunction.getParsedModule());
    theFunction.setSourceToken(genericFunction.getSourceToken());

    //Consider any return type replacement as this stage?
    //i.e. Supplier of type T
    //       <- r as T?
    //This is more complex than it looks and needs to be done in later compiler phases
    //Because it could be returning a List of Optional of DictEntry of (Integer T)!
    return theFunction;
  }
}
