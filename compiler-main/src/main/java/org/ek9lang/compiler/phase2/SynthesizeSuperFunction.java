package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.TemplateFunctionSymbolSearch;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Looks at the function and if it has no super function, attempts to work out what would be the
 * best super function generic type it could implement.
 * <p>
 * Also ensures that purity is retained.
 * So for some abstract generic functions like UnaryOperator of T for example the function must be pure.
 * i.e. is it really a Supplier of T and Consumer of T, etc.
 * </p>
 * <p>
 * The reason for this, is that it enables just functions to be defined but then accept them via their
 * generic function signature. Less coding for the EK9 developer.
 * </p>
 */
final class SynthesizeSuperFunction implements Consumer<FunctionSymbol> {
  private final SymbolsAndScopes symbolsAndScopes;
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ParameterisedLocator parameterisedLocator;

  SynthesizeSuperFunction(final SymbolsAndScopes symbolsAndScopes,
                          final SymbolFactory symbolFactory,
                          final ErrorListener errorListener) {

    this.symbolsAndScopes = symbolsAndScopes;
    this.parameterisedLocator =
        new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);

  }

  @Override
  public void accept(final FunctionSymbol function) {

    //Should get called if already has a super.
    if (function.getSuperFunction().isEmpty() && checkParametersHaveTypes(function)) {
      if (function.isMarkedPure()) {
        trySuppliersAndConsumers(function);
        tryPredicatesAndFunctions(function);
      } else {
        tryProducersAndAccessors(function);
        tryAssessorsAndRoutines(function);
      }
    }
    //Now it might not have been possible to set the super function to one of the idiomatic types
    //Like consumer etc. So set it to 'Any'.
    if (function.getSuperFunction().isEmpty()) {
      function.setSuperFunction(symbolsAndScopes.getEk9Types().ek9Any());
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

  private void trySuppliersAndConsumers(final FunctionSymbol function) {

    //Check if candidate could be a supplier
    if (function.getCallParameters().isEmpty() && isUsableReturnTypePresent(function)) {
      function.getReturningSymbol().getType().ifPresent(
          t -> processParameterisedType(function, "org.ek9.lang::Supplier", List.of(t))
      );

      return;
    }

    //The argument types will be needed by both of these super options.
    final var types = symbolTypeExtractor.apply(function.getCallParameters());

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

  private void tryProducersAndAccessors(final FunctionSymbol function) {

    //Check if candidate could be a producer
    if (function.getCallParameters().isEmpty() && isUsableReturnTypePresent(function)) {
      function.getReturningSymbol().getType().ifPresent(
          t -> processParameterisedType(function, "org.ek9.lang::Producer", List.of(t))
      );

      return;
    }

    //The argument types will be needed by both of these super options.
    final var types = symbolTypeExtractor.apply(function.getCallParameters());

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

  private void tryPredicatesAndFunctions(final FunctionSymbol function) {

    //Cannot be any of the possible stock generic types.
    if (!isUsableReturnTypePresent(function)) {
      return;
    }

    final var types = symbolTypeExtractor.apply(function.getCallParameters());
    final var returnType = getReturnType(function);

    if (types.size() == 1) {
      final var paramAndReturnTypeSame = types.getFirst().isExactSameType(returnType);

      if (isReturnTypeBoolean(function)) {
        //Then it is a predicate
        processParameterisedType(function, "org.ek9.lang::Predicate", types);
      } else if (paramAndReturnTypeSame) {
        //Then it is Unary
        processParameterisedType(function, "org.ek9.lang::UnaryOperator", types);
      } else {
        //It is a Function
        final var functionTypes = List.of(types.getFirst(), returnType);
        processParameterisedType(function, "org.ek9.lang::Function", functionTypes);
      }

    } else if (types.size() == 2) {
      final var paramTypesSame = types.get(0).isExactSameType(types.get(1));

      if (isReturnTypeInteger(function) && paramTypesSame) {
        //Then it is a comparator - if the type of both arguments are the same
        final var comparatorType = List.of(types.getFirst());
        processParameterisedType(function, "org.ek9.lang::Comparator", comparatorType);
      } else if (isReturnTypeBoolean(function)) {
        //Then it is a bi-predicate
        processParameterisedType(function, "org.ek9.lang::BiPredicate", types);
      } else {
        //It is a bi function.
        final var functionTypes = List.of(types.get(0), types.get(1), returnType);
        processParameterisedType(function, "org.ek9.lang::BiFunction", functionTypes);
      }
    }
  }

  private void tryAssessorsAndRoutines(final FunctionSymbol function) {

    //Cannot be any of the possible stock generic types.
    if (!isUsableReturnTypePresent(function)) {
      return;
    }

    final var types = symbolTypeExtractor.apply(function.getCallParameters());
    final var returnType = getReturnType(function);

    if (types.size() == 1) {

      if (isReturnTypeBoolean(function)) {
        //Then it is an assessor
        processParameterisedType(function, "org.ek9.lang::Assessor", types);
      } else {
        //It is a Routine
        final var functionTypes = List.of(types.getFirst(), returnType);
        processParameterisedType(function, "org.ek9.lang::Routine", functionTypes);
      } //There is no non-pure Unary operation.

    } else if (types.size() == 2) {

      if (isReturnTypeBoolean(function)) {
        //Then it is a bi-assessor
        processParameterisedType(function, "org.ek9.lang::BiAssessor", types);
      } else {
        //It is a bi routine.
        final var functionTypes = List.of(types.get(0), types.get(1), returnType);
        processParameterisedType(function, "org.ek9.lang::BiRoutine", functionTypes);
      }
      //There is no non-pure comparator.
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
        && !symbolsAndScopes.getEk9Types().ek9Void().equals(getReturnType(function));

  }

  private boolean isReturnTypeBoolean(final FunctionSymbol function) {
    return symbolsAndScopes.getEk9Types().ek9Boolean().equals(getReturnType(function));
  }

  private boolean isReturnTypeInteger(final FunctionSymbol function) {
    return symbolsAndScopes.getEk9Types().ek9Integer().equals(getReturnType(function));
  }

  private void processParameterisedType(final FunctionSymbol function,
                                        final String genericTypeName,
                                        final List<ISymbol> typeArguments) {

    final var genericType = function.resolve(new TemplateFunctionSymbolSearch(genericTypeName));
    if (genericType.isEmpty()) {
      throw new CompilerException("Must be possible to resolve built-in generic type [" + genericTypeName);
    }

    final var data = new ParameterisedTypeData(function.getSourceToken(), genericType.get(), typeArguments);
    final var resolvedParameterizedType = parameterisedLocator.apply(data);

    resolvedParameterizedType.ifPresentOrElse(newType -> {
          //When processing built-in - do not want to match self!
          if (!newType.isExactSameType(function)) {
            function.setSuperFunction((FunctionSymbol) newType);
          }
        },
        () -> {
          throw new CompilerException("Must be possible to parameterized type [" + genericTypeName);
        }
    );


  }

}
