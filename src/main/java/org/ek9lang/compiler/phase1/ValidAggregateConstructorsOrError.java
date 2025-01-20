package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that if the aggregate is a generic class that the appropriate public constructors are in place.
 * <p>
 * This is important for generic classes as their construction can take multiple forms.
 * BUT, it is important that a 'public' default constructor is present on Generic types so that the
 * EK9 developer can write 'Optional() of String' for example.
 * </p>
 */
class ValidAggregateConstructorsOrError extends RuleSupport implements Consumer<ISymbol> {

  private final AggregateManipulator aggregateManipulator = new AggregateManipulator();

  protected ValidAggregateConstructorsOrError(SymbolsAndScopes symbolsAndScopes,
                                              ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final ISymbol possibleAggregate) {

    if (possibleAggregate instanceof IAggregateSymbol asGeneric
        && asGeneric.getCategory().equals(SymbolCategory.TEMPLATE_TYPE)) {

      processGenericAggregate(asGeneric);
    }

  }

  /**
   * It is possible to create synthetic constructors and also check that any EK9 developer constructors are valid.
   */
  private void processGenericAggregate(final IAggregateSymbol asGeneric) {

    final var constructors = asGeneric.getConstructors();

    if (constructors.isEmpty()) {
      //If no constructors have been created then we can automatically synthesize them.
      //First add the default constructor
      aggregateManipulator.addConstructor(asGeneric, List.of(), true, true);

      final var args = getConstructorArguments(asGeneric.getSourceToken(), asGeneric.getTypeParameterOrArguments());
      aggregateManipulator.addConstructor(asGeneric, args, true, true);

    } else {
      //There must always be two constructors for generic type.
      //So if the EK9 developer has created any constructors - they must create both.
      //They can just use 'default Constructor()' if they want - but must write the code.
      if (constructors.size() < 2) {
        emitIncorrectNumberOfConstructors(asGeneric);
      }
      //The default constructor (i.e. no arguments) and the constructor used for inferring types.
      for (var constructor : constructors) {
        checkConstructor(asGeneric, constructor);
      }
    }
  }

  private List<ISymbol> getConstructorArguments(final IToken sourceToken,
                                                final List<ISymbol> constructorArgumentTypes) {
    final var args = new ArrayList<ISymbol>();
    for (int i = 0; i < constructorArgumentTypes.size(); i++) {
      final var argumentName = "arg" + i;
      final var arg = new VariableSymbol(argumentName, constructorArgumentTypes.get(i));
      arg.setIncomingParameter(true);
      arg.setSourceToken(sourceToken);
      args.add(arg);
    }
    return args;
  }

  /**
   * Does basic checks of the constructor.
   */
  private void checkConstructor(final IAggregateSymbol asGeneric, final MethodSymbol methodSymbol) {

    final var requiredNumberOfParameterizingTypes = asGeneric.getTypeParameterOrArguments().size();

    if (!methodSymbol.isPublic()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "", GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC);
    }

    final var numParametersInConstructor = methodSymbol.getSymbolsForThisScope().size();

    //We must check the parameters in the constructor tie up with the parameter numbers for creation
    //We tie these two together unlike java so that we can implement inference - so a bit of a cost here.
    //We allow a default constructor with no parameters, but that means no type inference it will have
    //to be defined correctly on the lhs.
    if (numParametersInConstructor > 0) {
      if (numParametersInConstructor != requiredNumberOfParameterizingTypes) {
        errorListener.semanticError(methodSymbol.getSourceToken(), "",
            ErrorListener.SemanticClassification.GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE);
      } else {
        //The right number of arguments in the constructor but are they the right types and in the right order.
        argumentTypesMatchOrError(asGeneric, methodSymbol);
      }
    }

  }

  private void argumentTypesMatchOrError(final IAggregateSymbol asGeneric, final MethodSymbol methodSymbol) {
    final var genericTypeParameters = asGeneric.getTypeParameterOrArguments();

    final var constructorTypes = methodSymbol.getSymbolsForThisScope()
        .stream()
        .map(ISymbol::getType)
        .flatMap(Optional::stream)
        .toList();
    for (int i = 0; i < constructorTypes.size(); i++) {
      if (!genericTypeParameters.get(i).isExactSameType(constructorTypes.get(i))) {
        final var argumentInError = methodSymbol.getSymbolsForThisScope().get(i);
        final var msg = "wrt: '"
            + genericTypeParameters.get(i).getFriendlyName()
            + "' and '"
            + argumentInError.getFriendlyName()
            + "':";
        errorListener.semanticError(argumentInError.getSourceToken(), msg,
            GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES);
      }
    }
  }

  private void emitIncorrectNumberOfConstructors(final IAggregateSymbol aggregate) {
    errorListener.semanticError(aggregate.getSourceToken(), "", GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS);
  }
}
