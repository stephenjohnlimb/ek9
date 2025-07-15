package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.AggregateHasPureConstruction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * For Constrained Types, focus on checking it is possible to constrain the type and if so then
 * clones the appropriate methods/operators over and alters the types as appropriate.
 * Note that it must also ensure that any constructors on the Constraining type if pure are also marked pure.
 * Once one constructor is pure all constructors must be pure.
 */
class PopulateConstrainedTypeOrError extends RuleSupport implements BiConsumer<AggregateSymbol, ISymbol> {
  final AggregateManipulator aggregateManipulator;

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
                                 final AggregateManipulator aggregateManipulator,
                                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.aggregateManipulator = aggregateManipulator;

  }

  @Override
  public void accept(final AggregateSymbol newType, final ISymbol constrainedType) {

    if (constrainedType == null) {
      //Already will have detected and emitted type not resolved.
      return;
    }

    final var isBoolean = constrainedType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Boolean());
    final var isJson = constrainedType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Json());
    final var isType = constrainedType.getCategory().equals(SymbolCategory.TYPE);
    final var isAcceptableGenus = isGenusAcceptable(constrainedType.getGenus());
    final var isAbstract = constrainedType.isMarkedAbstract();

    if (!isType || !isAcceptableGenus || isAbstract || isBoolean || isJson) {
      emitCannotConstrainTypeError(newType.getSourceToken(), constrainedType);
      return;
    }

    if (!(constrainedType instanceof IAggregateSymbol constrainingAggregate)) {
      //Already will have detected and emitted type not resolved.
      return;
    }

    setSuperAggregate(newType);
    //Now we can safely cast and clone over the methods
    cloneMethodsAndOperators(newType, constrainingAggregate);

    //Also need to ensure that constructors take the purity of the constraining type.
    if (aggregateHasPureConstruction.test(constrainingAggregate)) {
      newType.getConstructors().forEach(method -> method.setMarkedPure(true));
    }

  }

  private boolean isGenusAcceptable(final SymbolGenus genus) {

    return genus.equals(SymbolGenus.CLASS)
        || genus.equals(SymbolGenus.CLASS_CONSTRAINED)
        || genus.equals(SymbolGenus.CLASS_ENUMERATION)
        || genus.equals(SymbolGenus.RECORD);

  }

  private void setSuperAggregate(final AggregateSymbol newType) {

    newType.setSuperAggregate(symbolsAndScopes.getEk9Types().ek9Any());

  }

  private void cloneMethodsAndOperators(final AggregateSymbol newType, final IAggregateSymbol constrainedType) {

    aggregateManipulator.addConstructor(newType, new VariableSymbol("arg", constrainedType));

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
        addMethodIfNotAlreadyPresent(clonedMethod, newType);
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
    addMethodIfNotAlreadyPresent(clonedMethod, newType);

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
        addMethodIfNotAlreadyPresent(method.clone(newType), newType);

      }
    }

    addMethodIfNotAlreadyPresent(clonedMethod, newType);
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

  /**
   * Now because it is possible to use constructs that have supers, we may find that an existing method has
   * been overridden, we only want to add the method with matching signatures once.
   */
  private void addMethodIfNotAlreadyPresent(final MethodSymbol clonedMethod, final AggregateSymbol newType) {
    var results = newType.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch(clonedMethod),
        new MethodSymbolSearchResult());
    if (results.isEmpty()) {
      newType.define(clonedMethod);
    }
  }

  private void adjustReturnType(final MethodSymbol clonedMethod,
                                final AggregateSymbol newType,
                                final IAggregateSymbol constrainedType) {

    if (clonedMethod.isReturningSymbolPresent() && clonedMethod.getReturningSymbol().getType().isPresent()) {
      final var currentType = clonedMethod.getReturningSymbol().getType().get();
      if (currentType.isExactSameType(constrainedType)) {
        final var returningSymbol = clonedMethod.getReturningSymbol();
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