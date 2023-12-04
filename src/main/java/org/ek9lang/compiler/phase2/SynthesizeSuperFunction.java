package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.TemplateFunctionSymbolSearch;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Looks that the function and if it has no super function, attempts to work out what would be the
 * best super function generic type it could implement. Also ensures that purity is retained.
 * So for some abstract generic functions like UnaryOperator of T for example the function must be pure.
 * i.e. is it really a Supplier of T and Consumer of T, etc.
 * <br/>
 * The reason for this, is that it enables just functions to be defined but then accept them via their
 * generic function signature. Less coding for the EK9 developer. But all function style templates are 'pure'.
 */
final class SynthesizeSuperFunction implements Consumer<FunctionSymbol> {
  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ParameterisedLocator parameterisedLocator;

  SynthesizeSuperFunction(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final SymbolFactory symbolFactory,
                          final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.parameterisedLocator =
        new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
  }

  @Override
  public void accept(FunctionSymbol function) {

    //Should get called if already has a super.
    if (function.getSuperFunction().isEmpty() && checkParametersHaveTypes(function)) {
      if (function.isMarkedPure()) {
        trySuppliersAndConsumers(function);
        tryPredicatesAndFunctions(function);
      } else {
        tryProducersAnAccessors(function);
      }
    }

  }

  /**
   * Because this is in an early phase, some types may be missing, they will be reported in this phase.
   * But this code does nor need to do any reporting as other code will detect those missing types.
   * We just need to be sure here before attempting to make super functions the param types are in place.
   */
  private boolean checkParametersHaveTypes(final FunctionSymbol function) {

    if (function.isReturningSymbolPresent() && function.getReturningSymbol().getType().isEmpty()) {
      return false;
    }
    return function.getCallParameters().stream().noneMatch(arg -> arg.getType().isEmpty());

  }

  private void trySuppliersAndConsumers(FunctionSymbol function) {

    //Check if candidate could be a supplier
    if (function.getCallParameters().isEmpty() && isUsableReturnTypePresent(function)) {
      function.getReturningSymbol().getType().ifPresent(
          t -> processParameterisedType(function, "org.ek9.lang::Supplier", List.of(t))
      );
      return;
    }

    //The argument types will be needed by both of these super options.
    var types = symbolTypeExtractor.apply(function.getCallParameters());

    //Now look to see if this could be a Consumer
    if (function.getCallParameters().size() == 1 && !isUsableReturnTypePresent(function)) {
      processParameterisedType(function, "org.ek9.lang::Consumer", types);
      return;
    }

    //Or a BiConsumer
    if (function.getCallParameters().size() == 2 && !isUsableReturnTypePresent(function)) {
      processParameterisedType(function, "org.ek9.lang::BiConsumer", types);
    }

  }

  private void tryProducersAnAccessors(FunctionSymbol function) {

    //Check if candidate could be a producer
    if (function.getCallParameters().isEmpty() && isUsableReturnTypePresent(function)) {
      function.getReturningSymbol().getType().ifPresent(
          t -> processParameterisedType(function, "org.ek9.lang::Producer", List.of(t))
      );
      return;
    }

    //The argument types will be needed by both of these super options.
    var types = symbolTypeExtractor.apply(function.getCallParameters());

    //Now look to see if this could be aa Accessor
    if (function.getCallParameters().size() == 1 && !isUsableReturnTypePresent(function)) {
      processParameterisedType(function, "org.ek9.lang::Acceptor", types);
      return;
    }

    //Or a BiAccessor
    if (function.getCallParameters().size() == 2 && !isUsableReturnTypePresent(function)) {
      processParameterisedType(function, "org.ek9.lang::BiAcceptor", types);
    }

  }

  private void tryPredicatesAndFunctions(FunctionSymbol function) {

    //Cannot be any of the possible stock generic types.
    if (!isUsableReturnTypePresent(function)) {
      return;
    }

    var types = symbolTypeExtractor.apply(function.getCallParameters());
    var returnType = getReturnType(function);

    if (types.size() == 1) {
      var paramAndReturnTypeSame = types.get(0).isExactSameType(returnType);
      if (isReturnTypeBoolean(function)) {
        //Then it is a predicate
        processParameterisedType(function, "org.ek9.lang::Predicate", types);
      } else if (paramAndReturnTypeSame) {
        //Then it is Unary
        processParameterisedType(function, "org.ek9.lang::UnaryOperator", types);
      } else {
        //It is a Function
        var functionTypes = List.of(types.get(0), returnType);
        processParameterisedType(function, "org.ek9.lang::Function", functionTypes);
      }
    } else if (types.size() == 2) {
      var paramTypesSame = types.get(0).isExactSameType(types.get(1));
      if (isReturnTypeInteger(function) && paramTypesSame) {
        //Then it is a comparator - if the type of both arguments are the same
        var comparatorType = List.of(types.get(0));
        processParameterisedType(function, "org.ek9.lang::Comparator", comparatorType);
      } else if (isReturnTypeBoolean(function)) {
        //Then it is a bi-predicate
        processParameterisedType(function, "org.ek9.lang::BiPredicate", types);
      } else {
        //It is a bi function.
        var functionTypes = List.of(types.get(0), types.get(1), returnType);
        processParameterisedType(function, "org.ek9.lang::BiFunction", functionTypes);
      }
    }

  }

  private ISymbol getReturnType(final FunctionSymbol function) {
    return function.getReturningSymbol().getType().orElse(null);
  }

  private boolean isUsableReturnTypePresent(final FunctionSymbol function) {

    if (!function.isReturningSymbolPresent()) {
      return false;
    }

    //Void is not a usable return type for any sort of function.
    return function.getReturningSymbol().getType().isPresent()
        && !symbolAndScopeManagement.getEk9Types().ek9Void().equals(getReturnType(function));

  }

  private boolean isReturnTypeBoolean(final FunctionSymbol function) {
    return symbolAndScopeManagement.getEk9Types().ek9Boolean().equals(getReturnType(function));
  }

  private boolean isReturnTypeInteger(final FunctionSymbol function) {
    return symbolAndScopeManagement.getEk9Types().ek9Integer().equals(getReturnType(function));
  }

  private void processParameterisedType(final FunctionSymbol function,
                                        final String genericTypeName,
                                        final List<ISymbol> typeArguments) {

    var genericType = function.resolve(new TemplateFunctionSymbolSearch(genericTypeName));
    if (genericType.isEmpty()) {
      throw new CompilerException("Must be possible to resolve build in generic type [" + genericTypeName);
    }
    var data = new ParameterisedTypeData(function.getSourceToken(), genericType.get(), typeArguments);
    var resolvedParameterizedType = parameterisedLocator.apply(data);
    if (resolvedParameterizedType.isEmpty()) {
      throw new CompilerException("Must be possible to parameterized type [" + genericTypeName);
    }
    var superFunction = (FunctionSymbol) resolvedParameterizedType.get();
    function.setSuperFunction(superFunction);
  }
}
