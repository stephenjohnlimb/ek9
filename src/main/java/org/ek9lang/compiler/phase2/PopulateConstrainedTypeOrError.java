package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.AggregateHasPureConstruction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * For Constrained Types, focus on checking it is possible to constrain the type and if so then
 * clones the appropriate methods/operators over and alters the types as appropriate.
 * Note that it must also ensure that any constructors on the Constraining type if pure are also marked pure.
 * Once one constructor is pure all constructors must be pure.
 */
class PopulateConstrainedTypeOrError extends RuleSupport implements BiConsumer<AggregateSymbol, ISymbol> {
  final AggregateFactory aggregateFactory;

  final List<String> methodNamesToAlsoRetrainOldSignature =
      List.of("matches", "contains", "<", "<=", ">", ">=", "==", "<>");

  //These are a method names where we can alter the incoming argument type.
  final List<String> methodNamesWithAlterableArgumentTypes =
      List.of(":=:", ":^:", ":~:", "matches", "contains", "and", "or", "xor",
          "<", "<=", ">", ">=", "==", "<>", "<=>", "<~>");

  //These are the method names where we can alter the return type - methods are those with one incoming argument
  final List<String> methodNamesWithAlterableReturnTypes =
      List.of("+", "-", "*", "/", "^", ">>", "<<", "and", "or", "xor");

  //These are the method names of methods with no input arguments where we must not alter the return type.
  final List<String> methodNamesWithNonAlterableReturnTypes =
      List.of("length", "abs", "#?");

  final AggregateHasPureConstruction aggregateHasPureConstruction = new AggregateHasPureConstruction();

  PopulateConstrainedTypeOrError(final SymbolsAndScopes symbolsAndScopes,
                                 final AggregateFactory aggregateFactory,
                                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.aggregateFactory = aggregateFactory;

  }

  @Override
  public void accept(final AggregateSymbol newType, final ISymbol constrainedType) {

    if (constrainedType == null) {
      //Already will have detected and emitted type not resolved.
      return;
    }

    final var isBoolean = constrainedType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Boolean());
    final var isJson = constrainedType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Json());
    final var isType = constrainedType.getCategory().equals(ISymbol.SymbolCategory.TYPE);
    final var isAcceptableGenus = constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS_CONSTRAINED)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.RECORD);
    final var isAbstract = constrainedType.isMarkedAbstract();

    if (!isType || !isAcceptableGenus || isAbstract || isBoolean || isJson) {
      emitCannotConstrainTypeError(newType.getSourceToken(), constrainedType);
      return;
    }

    if (!(constrainedType instanceof IAggregateSymbol constrainingAggregate)) {
      //Already will have detected and emitted type not resolved.
      return;
    }

    //Now we can safely cast and clone over the methods
    cloneMethodsAndOperators(newType, constrainingAggregate);

    //Also need to ensure that constructors take the purity of the constraining type.
    if (aggregateHasPureConstruction.test(constrainingAggregate)) {
      newType.getConstructors().forEach(method -> method.setMarkedPure(true));
    }

  }

  private void cloneMethodsAndOperators(final AggregateSymbol newType, final IAggregateSymbol constrainedType) {


    aggregateFactory.addConstructor(newType, new VariableSymbol("arg", constrainedType));
    final var candidates =
        constrainedType.getAllNonAbstractMethods().stream().filter(MethodSymbol::isNotConstructor).toList();
    for (var method : candidates) {
      if (method.isOperator()) {
        if (method.getCallParameters().isEmpty()) {
          //Then it is a no argument operator and will have a return
          processNoArgOperator(method, newType, constrainedType);
        } else {
          //It is an operator that takes arguments and may also have a return
          processArgOperator(method, newType, constrainedType);
        }

      } else {
        //For non operators - just clone method and leave type as they are.
        var clonedMethod = method.clone(newType);
        newType.define(clonedMethod);
      }

    }
  }

  private void processNoArgOperator(final MethodSymbol method,
                                    final AggregateSymbol newType,
                                    final IAggregateSymbol constrainedType) {

    final var clonedMethod = method.clone(newType);
    if (!methodNamesWithNonAlterableReturnTypes.contains(clonedMethod.getName())) {
      adjustReturnType(clonedMethod, newType, constrainedType);
    }
    newType.define(clonedMethod);

  }

  private void processArgOperator(final MethodSymbol method,
                                  final AggregateSymbol newType,
                                  final IAggregateSymbol constrainedType) {

    final var clonedMethod = method.clone(newType);

    //Now we always alter any types in the input if they match the constrained type
    if (methodNamesWithAlterableReturnTypes.contains(clonedMethod.getName())) {
      adjustReturnType(clonedMethod, newType, constrainedType);
    }

    if (methodNamesWithAlterableArgumentTypes.contains(clonedMethod.getName())) {
      final var typeAdjusted = adjustArgumentType(clonedMethod, newType, constrainedType);
      //For some specific methods we duplicate the method but with the old type
      //This is useful for constrained types.
      if (typeAdjusted && methodNamesToAlsoRetrainOldSignature.contains(method.getName())) {
        newType.define(method.clone(newType));
      }
    }

    newType.define(clonedMethod);

  }

  private boolean adjustArgumentType(final MethodSymbol clonedMethod,
                                     final AggregateSymbol newType,
                                     final IAggregateSymbol constrainedType) {

    var rtn = false;
    for (var argument : clonedMethod.getCallParameters()) {
      if (argument.getType().isPresent() && argument.getType().get().isExactSameType(constrainedType)) {
        rtn = true;
        argument.setType(newType);
      }
    }
    return rtn;
  }

  private void adjustReturnType(final MethodSymbol clonedMethod,
                                final AggregateSymbol newType,
                                final IAggregateSymbol constrainedType) {

    if (clonedMethod.isReturningSymbolPresent() && clonedMethod.getReturningSymbol().getType().isPresent()) {
      final var currentType = clonedMethod.getReturningSymbol().getType().get();
      if (currentType.isExactSameType(constrainedType)) {
        final var returningSymbol = (VariableSymbol) clonedMethod.getReturningSymbol();
        returningSymbol.setType(newType);
        clonedMethod.setReturningSymbol(returningSymbol);
      }
    }

  }

  private void emitCannotConstrainTypeError(final IToken lineToken, final ISymbol argument) {

    argument.getType().ifPresent(argType -> {
      final var msg =
          "'" + argument.getFriendlyName() + "' is a '" + argType.getCategory() + "/" + argType.getGenus() + "':";
      errorListener.semanticError(lineToken, msg, ErrorListener.SemanticClassification.TYPE_CANNOT_BE_CONSTRAINED);
    });

  }
}