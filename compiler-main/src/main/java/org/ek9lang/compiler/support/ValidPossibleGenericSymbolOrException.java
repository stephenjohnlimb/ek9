package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Does a few checks on a PossibleGenericSymbol in terms of checking types and the generic type it is configured with.
 * At the moment untyped arguments and returns are consider a compiler error and an exception is thrown.
 * The reason for this, is that before a generic type can be parameterised it must have all its types known.
 */
public final class ValidPossibleGenericSymbolOrException implements Consumer<PossibleGenericSymbol> {

  @SuppressWarnings("checkstyle:PatternVariableName")
  @Override
  public void accept(final PossibleGenericSymbol possibleGenericSymbol) {

    //mainly just preconditions
    AssertValue.checkNotNull("parameterisedSymbol cannot be null", possibleGenericSymbol);
    AssertValue.checkFalse("parameterisedSymbol does not have any type arguments",
        possibleGenericSymbol.getTypeParameterOrArguments().isEmpty());

    final var theGenericType = possibleGenericSymbol.getGenericType();

    AssertValue.checkTrue("parameterisedSymbol does not have a generic type",
        theGenericType.isPresent());
    AssertValue.checkFalse("theGenericType within the parameterisedSymbol does not have any type arguments",
        theGenericType.get().getTypeParameterOrArguments().isEmpty());

    switch (possibleGenericSymbol) {
      case FunctionSymbol _ -> checkFunction(theGenericType.get());
      case AggregateSymbol _ -> checkAggregate(theGenericType.get());
      default -> throw new CompilerException("Only expecting functions and aggregates.");
    }
  }

  private void checkFunction(final PossibleGenericSymbol theGenericType) {

    final var genericFunction = (FunctionSymbol) theGenericType;
    if (genericFunction.getReturningSymbol().getType().isEmpty()) {
      final var msg = String.format("Function return type must be set [%s] [%s]",
          genericFunction, genericFunction.getSourceToken());

      throw new CompilerException(msg);
    }
    checkCallParameters(genericFunction, genericFunction.getCallParameters());

  }

  private void checkAggregate(final PossibleGenericSymbol theGenericType) {

    if (theGenericType instanceof AggregateSymbol genericAggregate) {
      checkMethods(genericAggregate);
    } else {
      throw new CompilerException("Compiler Error, not castable [" + theGenericType + "]");
    }
  }

  private void checkMethods(final AggregateSymbol genericAggregate) {
    genericAggregate.getAllMethods().forEach(method -> {
      if (!method.isConstructor() && (!method.isReturningSymbolPresent() || method.getReturningSymbol().getType()
          .isEmpty())) {
        //Not sure if this is just a developer error yet. or a broken compiler.
        final var msg = String.format("No returning symbol on [%s] method [%s] [%s]",
            genericAggregate, method, method.getSourceToken());
        throw new CompilerException(msg);
      }
      checkCallParameters(method, method.getCallParameters());
    });
  }

  private void checkCallParameters(final ISymbol symbol, final List<ISymbol> callParameters) {
    callParameters.forEach(arg -> {
      if (arg.getType().isEmpty()) {
        final var msg = String.format("Argument type must be set on [%s] [%s] [%s]",
            symbol, arg, arg.getSourceToken());
        throw new CompilerException(msg);
      }
    });
  }
}
