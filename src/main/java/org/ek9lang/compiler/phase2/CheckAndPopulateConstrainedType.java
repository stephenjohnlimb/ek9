package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * For Constrained Types, focus on checking it is possible to constrain the type and if so then
 * clones the appropriate methods/operators over and alters the types as appropriate.
 */
class CheckAndPopulateConstrainedType extends RuleSupport implements BiConsumer<AggregateSymbol, ISymbol> {
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

  CheckAndPopulateConstrainedType(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final AggregateFactory aggregateFactory,
                                  final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.aggregateFactory = aggregateFactory;
  }

  @Override
  public void accept(AggregateSymbol newType, final ISymbol constrainedType) {
    if (constrainedType == null) {
      //Already will have detected and emitted type not resolved.
      return;
    }

    var isBoolean = constrainedType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Boolean());
    var isJson = constrainedType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Json());
    var isType = constrainedType.getCategory().equals(ISymbol.SymbolCategory.TYPE);
    var isAcceptableGenus = constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS_CONSTRAINED)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION)
        || constrainedType.getGenus().equals(ISymbol.SymbolGenus.RECORD);
    var isAbstract = constrainedType.isMarkedAbstract();

    if (!isType || !isAcceptableGenus || isAbstract || isBoolean || isJson) {
      emitCannotConstrainTypeError(newType.getSourceToken(), constrainedType);
      return;
    }

    //Now we can safely cast and clone over the methods
    cloneMethodsAndOperators(newType, (AggregateSymbol) constrainedType);
  }

  private void cloneMethodsAndOperators(AggregateSymbol newType, final AggregateSymbol constrainedType) {

    aggregateFactory.addConstructor(newType, constrainedType);
    var candidates =
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

  private void processNoArgOperator(final MethodSymbol method, AggregateSymbol newType,
                                    final AggregateSymbol constrainedType) {
    var clonedMethod = method.clone(newType);
    if (!methodNamesWithNonAlterableReturnTypes.contains(clonedMethod.getName())) {
      adjustReturnType(clonedMethod, newType, constrainedType);
    }
    newType.define(clonedMethod);
  }

  private void processArgOperator(final MethodSymbol method, AggregateSymbol newType,
                                  final AggregateSymbol constrainedType) {

    var clonedMethod = method.clone(newType);

    //Now we always alter any types in the input if they match the constrained type
    if (methodNamesWithAlterableReturnTypes.contains(clonedMethod.getName())) {
      adjustReturnType(clonedMethod, newType, constrainedType);
    }

    if (methodNamesWithAlterableArgumentTypes.contains(clonedMethod.getName())) {
      var typeAdjusted = adjustArgumentType(clonedMethod, newType, constrainedType);
      //For some specific methods we duplicate the method but with the old type
      //This is useful for constrained types.
      if (typeAdjusted && methodNamesToAlsoRetrainOldSignature.contains(method.getName())) {
        newType.define(method.clone(newType));
      }
    }

    newType.define(clonedMethod);
  }

  private boolean adjustArgumentType(MethodSymbol clonedMethod,
                                     final AggregateSymbol newType,
                                     final AggregateSymbol constrainedType) {
    var rtn = false;
    for (var argument : clonedMethod.getCallParameters()) {
      if (argument.getType().isPresent() && argument.getType().get().isExactSameType(constrainedType)) {
        rtn = true;
        argument.setType(newType);
      }
    }
    return rtn;
  }

  private void adjustReturnType(MethodSymbol clonedMethod,
                                final AggregateSymbol newType,
                                final AggregateSymbol constrainedType) {
    if (clonedMethod.isReturningSymbolPresent() && clonedMethod.getReturningSymbol().getType().isPresent()) {
      var currentType = clonedMethod.getReturningSymbol().getType().get();
      if (currentType.isExactSameType(constrainedType)) {
        var returningSymbol = (VariableSymbol) clonedMethod.getReturningSymbol();
        returningSymbol.setType(newType);
        clonedMethod.setReturningSymbol(returningSymbol);
      }
    }
  }

  private void emitCannotConstrainTypeError(final IToken lineToken, final ISymbol argument) {
    argument.getType().ifPresent(argType -> {
      var msg = "'" + argument.getFriendlyName() + "' is a '" + argType.getCategory() + "/" + argType.getGenus() + "':";
      errorListener.semanticError(lineToken, msg, ErrorListener.SemanticClassification.TYPE_CANNOT_BE_CONSTRAINED);
    });
  }
}
