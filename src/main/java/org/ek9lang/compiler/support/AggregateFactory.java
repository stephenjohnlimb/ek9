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
public class AggregateFactory {
  private static final String PARAM = "param";
  public static final String EK9_LANG = "org.ek9.lang";
  public static final String EK9_MATH = "org.ek9.math";
  public static final String EK9_PATH = EK9_LANG + "::Path";
  public static final String EK9_VERSION = EK9_LANG + "::Version";
  public static final String EK9_REGEX = EK9_LANG + "::RegEx";
  public static final String EK9_MONEY = EK9_LANG + "::Money";
  public static final String EK9_COLOUR = EK9_LANG + "::Colour";
  public static final String EK9_RESOLUTION = EK9_LANG + "::Resolution";
  public static final String EK9_DIMENSION = EK9_LANG + "::Dimension";
  public static final String EK9_MILLISECOND = EK9_LANG + "::Millisecond";
  public static final String EK9_DURATION = EK9_LANG + "::Duration";
  public static final String EK9_DATETIME = EK9_LANG + "::DateTime";
  public static final String EK9_DATE = EK9_LANG + "::Date";
  public static final String EK9_TIME = EK9_LANG + "::Time";
  public static final String EK9_CHARACTER = EK9_LANG + "::Character";
  public static final String EK9_STRING = EK9_LANG + "::String";
  public static final String EK9_INTEGER = EK9_LANG + "::Integer";
  public static final String EK9_VOID = EK9_LANG + "::Void";
  public static final String EK9_FLOAT = EK9_LANG + "::Float";
  public static final String EK9_BOOLEAN = EK9_LANG + "::Boolean";
  public static final String EK9_BITS = EK9_LANG + "::Bits";
  public static final String EK9_JSON = EK9_LANG + "::JSON";
  public static final String EK9_HTTP_REQUEST = EK9_LANG + "::HTTPRequest";
  public static final String EK9_HTTP_RESPONSE = EK9_LANG + "::HTTPResponse";
  public static final String EK9_EXCEPTION = EK9_LANG + "::Exception";
  public static final String EK9_LIST = EK9_LANG + "::List";
  public static final String EK9_ITERATOR = EK9_LANG + "::Iterator";
  public static final String EK9_OPTIONAL = EK9_LANG + "::Optional";
  public static final String EK9_DICTIONARY = EK9_LANG + "::Dict";
  public static final String EK9_DICTIONARY_ENTRY = EK9_LANG + "::DictEntry";
  public static final String EK9_RESULT = EK9_LANG + "::Result";
  public static final String EK9_SUPPLIER = EK9_LANG + "::Supplier";
  public static final String EK9_CONSUMER = EK9_LANG + "::Consumer";
  public static final String EK9_FUNCTION = EK9_LANG + "::Function";
  public static final String EK9_UNARY_OPERATOR = EK9_LANG + "::UnaryOperator";
  public static final String EK9_PREDICATE = EK9_LANG + "::Predicate";
  public static final String EK9_COMPARATOR = EK9_LANG + "::Comparator";

  /**
   * Typically used when the module is not a core ek9 module.
   * This is so we can bootstrap ek9 but then once the types are defined we can use them quickly.
   * So this can remain null in some situations.
   */
  private Ek9Types ek9Types;

  private final OperatorFactory operatorFactory;

  private final AggregateHasPureConstruction aggregateHasPureConstruction = new AggregateHasPureConstruction();

  public AggregateFactory() {
    operatorFactory = new OperatorFactory(this);
  }

  public AggregateFactory(final Ek9Types ek9Types) {
    this();
    this.ek9Types = ek9Types;
  }

  /**
   * Add a synthetic constructor, if a constructor is not present.
   */
  public void addSyntheticConstructorIfRequired(IAggregateSymbol aggregateSymbol) {
    //Default constructor
    addSyntheticConstructorIfRequired(aggregateSymbol, new ArrayList<>());

    //Constructor with all properties on the aggregate.
    List<ISymbol> propertiesOfAggregate = aggregateSymbol.getProperties();
    addSyntheticConstructorIfRequired(aggregateSymbol, propertiesOfAggregate);
  }

  /**
   * Add a synthetic constructor, if a constructor is not present.
   */
  public void addSyntheticConstructorIfRequired(IAggregateSymbol aggregateSymbol,
                                                final List<ISymbol> constructorArguments) {
    addConstructorIfRequired(aggregateSymbol, constructorArguments, true);
  }

  /**
   * Add a new constructor if not present, marked as synthetic.
   */
  public void addConstructorIfRequired(IAggregateSymbol aggregateSymbol,
                                       final List<ISymbol> constructorArguments,
                                       final boolean synthetic) {
    MethodSymbolSearch symbolSearch = new MethodSymbolSearch(aggregateSymbol.getName());
    symbolSearch.setTypeParameters(constructorArguments);
    var results = new MethodSymbolSearchResult();
    var resolvedMethods = aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(symbolSearch, results);
    if (!resolvedMethods.isSingleBestMatchPresent() && !resolvedMethods.isAmbiguous()) {
      //If there are other constructors defined, and we provide a built-in one then we need to ensure we make it
      //pure if any of the others are pure.
      var needsPure = aggregateHasPureConstruction.test(aggregateSymbol);

      MethodSymbol newConstructor = new MethodSymbol(aggregateSymbol.getName(), aggregateSymbol);
      newConstructor.setMarkedPure(needsPure);
      newConstructor.setConstructor(true);
      newConstructor.setInitialisedBy(aggregateSymbol.getSourceToken());
      newConstructor.setSourceToken(aggregateSymbol.getSourceToken());
      newConstructor.setSynthetic(synthetic);
      newConstructor.setType(aggregateSymbol);
      newConstructor.setCallParameters(constructorArguments);
      aggregateSymbol.define(newConstructor);
    }
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
  public void addNonAbstractMethods(final IAggregateSymbol from, IAggregateSymbol to) {
    for (MethodSymbol method : from.getAllNonAbstractMethods()) {
      method.getType().ifPresentOrElse(methodType -> {
        if (method.isConstructor() && methodType.isExactSameType(from)) {
          MethodSymbol newMethod = new MethodSymbol(to.getName(), Optional.of(to), to).setConstructor(true);
          List<ISymbol> params = method.getCallParameters();
          newMethod.setCallParameters(params);
          to.define(newMethod);
        } else {
          if (methodType.isExactSameType(from) && !method.isMarkedNoClone()) {
            //So we have a method that returns the same type - we need to modify this and add it in.
            MethodSymbol newMethod = cloneMethodWithNewType(method, to);
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
   * Create a new constructor for the aggregate with no params.
   *
   * @param t The aggregate type to add the constructor to.
   */
  public MethodSymbol addConstructor(IAggregateSymbol t) {
    MethodSymbol constructor = new MethodSymbol(t.getName(), t);
    constructor.setConstructor(true);
    constructor.setType(t);
    t.define(constructor);
    return constructor;
  }

  /**
   * Add another constructor to type t, but passing in an s as the value in the construction.
   *
   * @param t The aggregate type to add the constructor to.
   * @param s The argument - arg with a symbol type to be passed in as a construction parameter.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addConstructor(AggregateSymbol t, final Optional<ISymbol> s) {
    if (s.isPresent()) {
      return addConstructor(t, s.get());
    }
    return addConstructor(t);
  }

  /**
   * Add a constructor to the type aggregate with a particular parameter.
   */
  public MethodSymbol addConstructor(AggregateSymbol t, final ISymbol s) {
    MethodSymbol constructor = addConstructor(t);
    constructor.define(s);
    return constructor;
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
   * @param name  - The name of the generic type parameter
   * @param scope - The scope it should go in.
   */
  public AggregateSymbol createGenericT(final String name, final IScope scope) {
    AggregateSymbol t = new AggregateSymbol(name, scope);
    t.setConceptualTypeParameter(true);
    //Do not add any methods yet - wait until later phases.
    return t;
  }

  /**
   * This is the idea where a 'T' is constrained to only be a type or a subtype of that type.
   */
  public void updateToConstrainBy(IAggregateSymbol t, final IAggregateSymbol constrainingType) {

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
  public void addAllSyntheticOperators(IAggregateSymbol t) {
    var constructorMethod = addConstructor(t);
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
  public MethodSymbol addPublicMethod(AggregateSymbol clazz,
                                      final String methodName,
                                      final List<ISymbol> methodParameters,
                                      final Optional<ISymbol> returnType) {
    MethodSymbol method = new MethodSymbol(methodName, clazz);
    method.setParsedModule(clazz.getParsedModule());
    methodParameters.forEach(method::define);
    clazz.define(method);
    method.setReturningSymbol(new VariableSymbol("rtn", returnType));
    return method;
  }

  /**
   * Adds all the appropriate methods for a type that is an 'Enumeration'.
   * This allows a developer to use built-in methods on the 'Enumeration' type.
   */
  public void addEnumerationMethods(AggregateSymbol enumerationSymbol) {
    operatorFactory.addEnumerationMethods(enumerationSymbol);
  }

  public void addPurePublicReturnSameTypeMethod(IAggregateSymbol clazz, final String methodName) {
    var method = createPurePublicReturnSameTypeMethod(clazz, methodName);
    clazz.define(method);
  }

  public MethodSymbol createPurePublicReturnSameTypeMethod(IAggregateSymbol clazz, final String methodName) {
    return createPurePublicSimpleOperator(clazz, methodName, Optional.of(clazz));
  }

  /**
   * Adds a form of a comparison operator.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addComparatorOperator(IAggregateSymbol clazz,
                                            final IAggregateSymbol arg0,
                                            final String comparatorType,
                                            final Optional<ISymbol> returnType) {
    MethodSymbol operator = createPureAcceptArgumentOperatorAndReturnType(clazz, arg0, comparatorType, returnType);
    clazz.define(operator);
    return operator;
  }

  /**
   * Adds a form of a comparison operator.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol addComparatorOperator(IAggregateSymbol clazz, final String comparatorType,
                                            final Optional<ISymbol> returnType) {
    MethodSymbol operator = createPureAcceptSameTypeOperatorAndReturnType(clazz, comparatorType, returnType);
    clazz.define(operator);
    return operator;
  }

  /**
   * Creates a pure method with an argument the same as the main type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPureAcceptSameTypeOperatorAndReturnType(IAggregateSymbol clazz,
                                                                    final String comparatorType,
                                                                    final Optional<ISymbol> returnType) {
    MethodSymbol operator = createPurePublicSimpleOperator(clazz, comparatorType, returnType);
    //Always enable as a dispatcher, so that most specific type can be used.
    operator.setMarkedAsDispatcher(true);
    operator.define(new VariableSymbol(PARAM, clazz));
    return operator;
  }


  /**
   * Creates a pure method with an argument of a different type as the main type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPureAcceptArgumentOperatorAndReturnType(IAggregateSymbol clazz,
                                                                    final IAggregateSymbol arg0,
                                                                    final String comparatorType,
                                                                    final Optional<ISymbol> returnType) {
    MethodSymbol operator = createPurePublicSimpleOperator(clazz, comparatorType, returnType);
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
  public MethodSymbol addPurePublicSimpleOperator(IAggregateSymbol clazz,
                                                  final String methodName,
                                                  final Optional<ISymbol> returnType) {
    var method = createPurePublicSimpleOperator(clazz, methodName, returnType);
    clazz.define(method);
    return method;
  }

  /**
   * Create operator for json.
   */
  public MethodSymbol createToJsonSimpleOperator(final IAggregateSymbol aggregate) {
    final Optional<ISymbol> jsonType = resolveJson(aggregate);
    return createPurePublicSimpleOperator(aggregate, "$$", jsonType);
  }

  /**
   * Just creates a public operator with the name specified.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol createPurePublicSimpleOperator(final IAggregateSymbol clazz,
                                                     final String methodName,
                                                     final Optional<ISymbol> returnType) {

    MethodSymbol method = new MethodSymbol(methodName, clazz);
    if (returnType.isPresent()) {
      method.setReturningSymbol(new VariableSymbol("rtn", returnType));
    } else {
      method.setType(resolveVoid(clazz));
    }
    method.setParsedModule(clazz.getParsedModule());
    method.setAccessModifier("public");
    method.setMarkedPure(true);
    method.setOperator(true);
    return method;
  }

  /**
   * Create an operator of the name supplied.
   */
  public MethodSymbol createOperator(final IAggregateSymbol clazz,
                                     final String operatorType,
                                     final boolean isPure) {
    VariableSymbol paramT = new VariableSymbol(PARAM, clazz);

    MethodSymbol operator = new MethodSymbol(operatorType, clazz);
    operator.setParsedModule(clazz.getParsedModule());
    operator.setAccessModifier("public");
    operator.setMarkedPure(isPure);
    operator.setOperator(true);

    operator.define(paramT);
    //returns the same type as itself
    operator.setReturningSymbol(new VariableSymbol("rtn", clazz));
    return operator;
  }

  /**
   * Resolve Void from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveVoid(final IScope scope) {
    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Void());
    }
    return scope.resolve(new TypeSymbolSearch(EK9_VOID));
  }

  /**
   * Resolve Boolean from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveBoolean(final IScope scope) {
    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Boolean());
    }
    return scope.resolve(new TypeSymbolSearch(EK9_BOOLEAN));
  }

  /**
   * Resolve Integer from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveInteger(final IScope scope) {
    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Integer());
    }
    return scope.resolve(new TypeSymbolSearch(EK9_INTEGER));
  }

  /**
   * Resolve String from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveString(final IScope scope) {
    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9String());
    }
    return scope.resolve(new TypeSymbolSearch(EK9_STRING));
  }

  /**
   * Resolve JSON from cached ek9 types of full scope hierarchy resolution.
   */
  public Optional<ISymbol> resolveJson(final IScope scope) {
    if (ek9Types != null) {
      return Optional.of(ek9Types.ek9Json());
    }
    return scope.resolve(new TypeSymbolSearch(EK9_JSON));
  }
}
