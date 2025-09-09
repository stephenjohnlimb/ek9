package org.ek9lang.compiler.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.common.AggregateHasPureConstruction;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Support for taking one aggregate and manipulating the methods and the like to be applied
 * to another aggregate.
 * This is typically used in the phase 3 where we have one aggregate with a set of methods
 * and what another aggregate to have those methods but in some cases we want to alter the
 * return types via covariance.
 * It also has more general uses in creating operators and methods for specific types or
 * generic types of T.
 * This is a factory of sorts, not in the pure OO sense; but still a factory.
 */
public class AggregateManipulator {
  public static final int MAX_ARGUMENTS_PER_CALL = 20;

  public static final String PRIVATE = "private";
  public static final String PROTECTED = "protected";
  public static final String PUBLIC = "public";
  public static final String PARAM = "param";

  private final OperatorFactory operatorFactory;
  private final AggregateHasPureConstruction aggregateHasPureConstruction = new AggregateHasPureConstruction();
  /**
   * Typically used when the module is not a core ek9 module.
   * This is so we can bootstrap ek9 but then once the types are defined we can use them quickly.
   * So this can remain null in some situations.
   */
  private Ek9Types ek9Types;

  public AggregateManipulator() {
    operatorFactory = new OperatorFactory(this);
  }

  public AggregateManipulator(final Ek9Types ek9Types) {

    this();
    this.ek9Types = ek9Types;

  }

  public Ek9Types getEk9Types() {

    return ek9Types;
  }

  /**
   * Add a synthetic constructor, if a constructor is not present.
   * Also adds a constructor with all the properties as well (if not present).
   */
  public void addSyntheticConstructorIfRequired(final IAggregateSymbol aggregateSymbol) {

    //Do not create for generic types, that is done elsewhere.
    if (!aggregateSymbol.getCategory().equals(SymbolCategory.TEMPLATE_TYPE)) {

      //Default constructor
      addSyntheticConstructorIfRequired(aggregateSymbol, new ArrayList<>());

      //Constructor with all properties on the aggregate, max of twenty.
      List<ISymbol> propertiesOfAggregate = aggregateSymbol.getProperties();
      if (propertiesOfAggregate.size() < MAX_ARGUMENTS_PER_CALL) {
        addSyntheticConstructorIfRequired(aggregateSymbol, propertiesOfAggregate);
      }
    }
  }

  /**
   * Add a synthetic constructor, if a constructor is not present.
   */
  public void addSyntheticConstructorIfRequired(final IAggregateSymbol aggregateSymbol,
                                                final List<ISymbol> constructorArguments) {

    addConstructorIfRequired(aggregateSymbol, constructorArguments, true);

  }

  /**
   * Add a new constructor if not present, marked as synthetic.
   */
  public void addConstructorIfRequired(final IAggregateSymbol aggregateSymbol,
                                       final List<ISymbol> constructorArguments,
                                       final boolean synthetic) {

    final var symbolSearch = new MethodSymbolSearch(aggregateSymbol.getName());
    symbolSearch.setTypeParameters(constructorArguments);

    final var results = new MethodSymbolSearchResult();
    final var resolvedMethods = aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(symbolSearch, results);

    if (!resolvedMethods.isSingleBestMatchPresent() && !resolvedMethods.isAmbiguous()) {
      //If there are other constructors defined, and we provide a built-in one then we need to ensure we make it
      //pure if any of the others are pure.
      final var needsPure = aggregateHasPureConstruction.test(aggregateSymbol);
      addConstructor(aggregateSymbol, constructorArguments, synthetic, needsPure);
    }

  }

  /**
   * Create a new constructor for the aggregate with no params.
   *
   * @param t The aggregate type to add the constructor to.
   */
  public MethodSymbol addConstructor(final IAggregateSymbol t) {

    final var constructor = new MethodSymbol(t.getName(), t);
    constructor.setConstructor(true);
    constructor.setType(t);
    t.define(constructor);

    return constructor;
  }

  public void addConstructor(final IAggregateSymbol aggregateSymbol,
                             final List<ISymbol> constructorArguments,
                             final boolean synthetic,
                             final boolean markedPure) {
    final var newConstructor = new MethodSymbol(aggregateSymbol.getName(), aggregateSymbol);

    newConstructor.setMarkedPure(markedPure);
    newConstructor.setConstructor(true);
    newConstructor.setInitialisedBy(aggregateSymbol.getSourceToken());
    newConstructor.setSourceToken(aggregateSymbol.getSourceToken());
    newConstructor.setSynthetic(synthetic);
    newConstructor.setType(aggregateSymbol);

    newConstructor.setCallParameters(constructorArguments);
    aggregateSymbol.define(newConstructor);
  }

  /**
   * Add another constructor to type t, but passing in an s as the value in the construction.
   *
   * @param t The aggregate type to add the constructor to.
   * @param s The argument - arg with a symbol type to be passed in as a construction parameter.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addConstructor(final AggregateSymbol t, final Optional<ISymbol> s) {

    if (s.isPresent()) {
      return addConstructor(t, s.get());
    }

    return addConstructor(t);
  }

  /**
   * Add a constructor to the type aggregate with a particular parameter.
   */
  public MethodSymbol addConstructor(final AggregateSymbol t, final ISymbol s) {

    final var constructor = addConstructor(t);
    constructor.define(s);

    return constructor;
  }

  /**
   * Takes the 'from' aggregate obtains all the methods that are not abstract that have a return
   * the type 'from' and adds them to the 'to' aggregate. It makes new constructors for each of
   * the constructors the 'from' has.
   * So take not note all methods get copied across only those with return type of 'from' which
   * is then transformed in to return type 'to'.
   * In general this method is used for defining types that extend another type, but we just want
   * constructors and co-variance returns.
   * Though it could be used in conjunction with other methods to alter the to aggregate.
   *
   * @param from The aggregate to get the methods from
   * @param to   The aggregate to add the methods to.
   */
  public void addNonAbstractMethods(final IAggregateSymbol from, final IAggregateSymbol to) {

    for (MethodSymbol method : from.getAllNonAbstractMethods()) {
      method.getType().ifPresentOrElse(methodType -> {
        if (method.isConstructor() && methodType.isExactSameType(from)) {
          final var newMethod = new MethodSymbol(to.getName(), Optional.of(to), to).setConstructor(true);
          final var params = method.getCallParameters();

          newMethod.setCallParameters(params);
          to.define(newMethod);
        } else {
          if (methodType.isExactSameType(from) && !method.isMarkedNoClone()) {
            //So we have a method that returns the same type - we need to modify this and add it in.
            final var newMethod = cloneMethodWithNewType(method, to);
            to.define(newMethod);
          }
        }
      }, () -> {
        throw new CompilerException("Method return type must be set for use to copy methods over");
      });

    }
  }

  public MethodSymbol cloneMethodWithNewType(final MethodSymbol method, final IAggregateSymbol to) {

    return method.clone(to, to);
  }


  /**
   * Create a generic parameter of specific name.
   * We use the name createGenericT, so it is obvious what we are doing here.
   * These are used in generic classes/functions, and we provide a number of operators
   * that a developer would reasonably expect. So our generic types and functions will compile
   * and all will look OK. But when it comes to use with a concrete type we have to check the actual
   * operators that are supported by those concrete types - this is done in the IR phase.
   * Where are the generic type/functions get checked in the resolve phase.
   *
   * @param name          - The name of the generic type parameter
   * @param genericParent - The fully qualified name of the generic function or class this 'T' is defined for.
   * @param scope         - The scope it should go in.
   */
  public AggregateSymbol createGenericT(final String name, String genericParent, final IScope scope) {

    final var t = new AggregateSymbol(name, scope);
    t.setConceptualTypeParameter(true);
    t.putSquirrelledData(CommonValues.GENERIC_PARENT, genericParent);

    //Do not add any methods yet - wait until later phases.
    //This is especially true when the 'T' is constrained.
    return t;
  }

  /**
   * This is the idea where a 'T' is constrained to only be a type or a subtype of that type.
   */
  public void updateToConstrainBy(final IAggregateSymbol t, final IAggregateSymbol constrainingType) {

    //So firstly lets make the T a subtype of the constraining type
    t.setSuperAggregate(constrainingType);
    //Now whatever constructors the constraining type has we will clone and add those in.
    //This should enable construction with the same arguments (if there are constructors)
    //This can then just be implemented as a direct call to the super.
    constrainingType.getAllNonAbstractMethodsInThisScopeOnly().stream()
        .filter(MethodSymbol::isConstructor)
        .forEach(constructor -> {
          var clonedConstructor = constructor.clone(t);
          clonedConstructor.setName(t.getName());
          clonedConstructor.setSourceToken(t.getSourceToken());
          clonedConstructor.setType(t);
          t.define(clonedConstructor);
        });

  }

  /**
   * Provides all the possible default operators.
   * Does not add them to the aggregate, but does create them with the aggregate as the enclosing scope.
   */
  public List<MethodSymbol> getAllPossibleDefaultOperators(final IAggregateSymbol aggregate) {

    return operatorFactory.getAllPossibleDefaultOperators(aggregate);
  }

  /**
   * If the operator provided can be defaulted then a Method symbol with the correct signature will
   * be returned. Otherwise, the return Optional will be empty (normally indicating an error).
   */
  public Optional<MethodSymbol> getDefaultOperator(final IAggregateSymbol aggregate, final String operator) {

    return Optional.ofNullable(operatorFactory.getDefaultOperator(aggregate, operator));
  }

  /**
   * Add all possible synthetic operators to the aggregate.
   * This is useful for creation conceptual types like 'T'.
   * Because the semantics of the operators is fixed and known it means it is possible
   * to write code in a generic class that uses these operator.
   * Only when the generic type is parameterized with types are the operators on those types checked for existence.
   * If not present then errors of emitted.
   */
  public void addAllSyntheticOperators(final IAggregateSymbol t) {

    final var constructorMethod = addConstructor(t);

    constructorMethod.setSourceToken(t.getSourceToken());
    getAllPossibleSyntheticOperators(t).forEach(method -> {
      method.setSourceToken(t.getSourceToken());
      t.define(method);
    });

  }

  /**
   * Provides a list of all possible synthetic operators, this includes the possible default operators.
   * This is typically used when creating synthetic 'T' aggregates for generics/templates.
   */
  private List<MethodSymbol> getAllPossibleSyntheticOperators(final IAggregateSymbol aggregate) {

    return operatorFactory.getAllPossibleSyntheticOperators(aggregate);
  }

  /**
   * Just add a method to an aggregate with the name and parameters and return type.
   * The methodParameters can be empty if there are none.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addPublicMethod(final AggregateSymbol aggregate,
                                      final String methodName,
                                      final List<ISymbol> methodParameters,
                                      final Optional<ISymbol> returnType) {

    final var method = new MethodSymbol(methodName, aggregate);

    method.setParsedModule(aggregate.getParsedModule());
    methodParameters.forEach(method::define);
    aggregate.define(method);
    method.setReturningSymbol(new VariableSymbol("rtn", returnType));

    return method;
  }

  /**
   * Adds all the appropriate methods for a type that is an 'Enumeration'.
   * This allows a developer to use built-in methods on the 'Enumeration' type.
   */
  public void addEnumerationMethods(final AggregateSymbol enumerationSymbol) {

    operatorFactory.addEnumerationMethods(enumerationSymbol);
  }

  public void addPurePublicReturnSameTypeMethod(final IAggregateSymbol aggregate, final String methodName) {

    final var method = createPurePublicReturnSameTypeMethod(aggregate, methodName);

    aggregate.define(method);

  }

  public MethodSymbol createPurePublicReturnSameTypeMethod(final IAggregateSymbol aggregateSymbol,
                                                           final String methodName) {

    return createPurePublicSimpleOperator(aggregateSymbol, methodName, Optional.of(aggregateSymbol));
  }

  /**
   * Adds a form of a comparison operator.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void addComparatorOperator(IAggregateSymbol aggregateSymbol,
                                    final IAggregateSymbol arg0,
                                    final String comparatorType,
                                    final Optional<ISymbol> returnType) {

    final var operator = createPureArgumentOperatorAndReturnType(aggregateSymbol, arg0, comparatorType, returnType);

    aggregateSymbol.define(operator);

  }

  /**
   * Adds a form of a comparison operator.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addComparatorOperator(IAggregateSymbol aggregateSymbol,
                                            final String comparatorType,
                                            final Optional<ISymbol> returnType) {

    final var operator = createPureAcceptSameTypeOperatorAndReturnType(aggregateSymbol, comparatorType, returnType);

    aggregateSymbol.define(operator);

    return operator;
  }

  /**
   * Creates a pure method with an argument the same as the main type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPureAcceptSameTypeOperatorAndReturnType(IAggregateSymbol aggregateSymbol,
                                                                    final String comparatorType,
                                                                    final Optional<ISymbol> returnType) {

    final var operator = createPurePublicSimpleOperator(aggregateSymbol, comparatorType, returnType);

    //Always enable as a dispatcher, so that most specific type can be used.
    operator.setMarkedAsDispatcher(true);
    operator.define(new VariableSymbol(PARAM, aggregateSymbol));

    return operator;
  }


  /**
   * Creates a pure method with an argument of a different type as the main type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPureArgumentOperatorAndReturnType(IAggregateSymbol aggregateSymbol,
                                                              final IAggregateSymbol arg0,
                                                              final String comparatorType,
                                                              final Optional<ISymbol> returnType) {

    final var operator = createPurePublicSimpleOperator(aggregateSymbol, comparatorType, returnType);

    //Always enable as a dispatcher, so that most specific type can be used.
    operator.setMarkedAsDispatcher(true);
    operator.define(new VariableSymbol(PARAM, arg0));

    return operator;
  }

  /**
   * Adds a simple operator, that is 'pure' (no side effects) and accepts no
   * parameters, but just returns a value.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addPurePublicSimpleOperator(IAggregateSymbol aggregateSymbol,
                                                  final String methodName,
                                                  final Optional<ISymbol> returnType) {

    final var method = createPurePublicSimpleOperator(aggregateSymbol, methodName, returnType);

    aggregateSymbol.define(method);

    return method;
  }

  /**
   * Create operator for json.
   */
  public MethodSymbol createToJsonSimpleOperator(final IAggregateSymbol aggregateSymbol) {

    final var jsonType = resolveJson(aggregateSymbol);

    return createPurePublicSimpleOperator(aggregateSymbol, "$$", jsonType);
  }

  /**
   * Just creates a public operator with the name specified.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPurePublicSimpleOperator(final IAggregateSymbol aggregateSymbol,
                                                     final String methodName,
                                                     final Optional<ISymbol> returnType) {

    final var method = new MethodSymbol(methodName, aggregateSymbol);

    if (returnType.isPresent()) {
      method.setReturningSymbol(new VariableSymbol("_rtn", returnType));
    } else {
      method.setType(resolveVoid(aggregateSymbol));
    }
    method.setParsedModule(aggregateSymbol.getParsedModule());
    method.setAccessModifier(PUBLIC);
    method.setMarkedPure(true);
    method.setOperator(true);

    return method;
  }

  /**
   * Create an operator of the name supplied.
   * The accepts a single argument of the same type as the aggregateSymbol
   * It also returns a value that is the same as the aggregateSymbol.
   */
  public MethodSymbol createOperator(final IAggregateSymbol aggregateSymbol,
                                     final String operatorType,
                                     final boolean isPure) {

    final var paramT = new VariableSymbol(PARAM, aggregateSymbol);
    final var operator = new MethodSymbol(operatorType, aggregateSymbol);

    operator.setParsedModule(aggregateSymbol.getParsedModule());
    operator.setAccessModifier(PUBLIC);
    operator.setMarkedPure(isPure);
    operator.setOperator(true);
    operator.define(paramT);
    //returns the same type as itself
    operator.setReturningSymbol(new VariableSymbol("_rtn", aggregateSymbol));

    return operator;
  }

  /**
   * Create an operator of the name supplied.
   * The accepts a single argument of the same type as the aggregateSymbol
   * But it returns Void -i.e. has no return value.
   */
  public MethodSymbol createMutatorOperatorReturnVoid(final IAggregateSymbol aggregateSymbol,
                                            final String operatorType) {

    final var paramT = new VariableSymbol(PARAM, aggregateSymbol);
    final var operator = new MethodSymbol(operatorType, aggregateSymbol);

    operator.setParsedModule(aggregateSymbol.getParsedModule());
    operator.setAccessModifier(PUBLIC);
    operator.setMarkedPure(false);
    operator.setOperator(true);
    operator.define(paramT);
    //returns Void - in ek9 syntax the return would just not be present.
    //But in code we actually add in a synthetic return.
    operator.setReturningSymbol(new VariableSymbol("_rtn", resolveVoid(aggregateSymbol)));

    return operator;
  }

  /**
   * Create an operator of the name supplied.
   * The accepts a single argument of the same type as the aggregateSymbol
   * It also returns a value that is the same as the aggregateSymbol.
   */
  public MethodSymbol createMutatorOperator(final IAggregateSymbol aggregateSymbol,
                                            final String operatorType) {

    final var operator = new MethodSymbol(operatorType, aggregateSymbol);

    operator.setParsedModule(aggregateSymbol.getParsedModule());
    operator.setAccessModifier(PUBLIC);
    operator.setMarkedPure(false);
    operator.setOperator(true);
    //returns the same type as itself
    operator.setReturningSymbol(new VariableSymbol("_rtn", aggregateSymbol));

    return operator;
  }

  /**
   * Resolve Void from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveVoid(final IScope scope) {

    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Void());
    }

    return scope.resolve(new TypeSymbolSearch(EK9TypeNames.EK9_VOID));
  }

  /**
   * Resolve Boolean from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveBoolean(final IScope scope) {

    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Boolean());
    }

    return scope.resolve(new TypeSymbolSearch(EK9TypeNames.EK9_BOOLEAN));
  }

  /**
   * Resolve Integer from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveInteger(final IScope scope) {

    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Integer());
    }

    return scope.resolve(new TypeSymbolSearch(EK9TypeNames.EK9_INTEGER));
  }

  /**
   * Resolve String from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveString(final IScope scope) {

    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9String());
    }

    return scope.resolve(new TypeSymbolSearch(EK9TypeNames.EK9_STRING));
  }

  /**
   * Resolve JSON from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveJson(final IScope scope) {

    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Json());
    }

    return scope.resolve(new TypeSymbolSearch(EK9TypeNames.EK9_JSON));
  }

}
