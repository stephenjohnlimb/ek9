package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ParameterisedSymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.CompilerException;

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
                                                List<ISymbol> constructorArguments) {
    addConstructorIfRequired(aggregateSymbol, constructorArguments, true);
  }

  /**
   * Add a new constructor if not present, maybe marked as synthetic.
   */
  public void addConstructorIfRequired(IAggregateSymbol aggregateSymbol,
                                       List<ISymbol> constructorArguments, boolean synthetic) {
    MethodSymbolSearch symbolSearch = new MethodSymbolSearch(aggregateSymbol.getName());
    symbolSearch.setParameters(constructorArguments);
    Optional<ISymbol> resolvedConstructor = aggregateSymbol.resolveMember(symbolSearch);
    if (resolvedConstructor.isEmpty()) {
      MethodSymbol newConstructor = new MethodSymbol(aggregateSymbol.getName(), aggregateSymbol);
      newConstructor.setConstructor(true);
      newConstructor.setSynthetic(synthetic);
      newConstructor.setType(aggregateSymbol);
      newConstructor.setMethodParameters(constructorArguments);
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
   * @return The to aggregate but now with the methods added.
   */
  public IAggregateSymbol addNonAbstractMethods(IAggregateSymbol from, IAggregateSymbol to) {
    for (MethodSymbol method : from.getAllNonAbstractMethods()) {
      method.getType().ifPresentOrElse(methodType -> {
        if (method.isConstructor() && methodType.isExactSameType(from)) {
          MethodSymbol newMethod =
              new MethodSymbol(to.getName(), Optional.of(to), to).setConstructor(true);
          List<ISymbol> params = method.getMethodParameters();
          newMethod.setMethodParameters(params);
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
    return to;
  }

  public MethodSymbol cloneMethodWithNewType(MethodSymbol method, IAggregateSymbol to) {
    return method.clone(to, to);
  }

  /**
   * So our parameterisedSymbol could have a mix of real and abstract parameters and the
   * order could vary.
   * Our concreteSymbolType will have a set of real concrete parameters,
   * and we need to know how those map from S->String and T->Integer for example.
   */
  public List<ISymbol> getSuitableParameters(ParameterisedSymbol concreteSymbol,
                                             ParameterisedSymbol parameterisedSymbol) {

    List<ISymbol> appropriateParams = new ArrayList<>();
    parameterisedSymbol.getParameterSymbols().forEach(symbol -> {
      if (symbol.isGenericTypeParameter()) {
        List<ISymbol> concreteParameters = concreteSymbol.getParameterSymbols();
        List<ISymbol> genericParameters =
            concreteSymbol.getParameterisableSymbol().getParameterisedTypes();
        for (int i = 0; i < concreteParameters.size(); i++) {
          if (symbol.isExactSameType(genericParameters.get(i))) {
            appropriateParams.add(concreteParameters.get(i));
          }
        }
      } else {
        appropriateParams.add(symbol);
      }
    });

    return appropriateParams;
  }

  /**
   * Create a new constructor for the aggregate with no params.
   *
   * @param t The aggregate type to add the constructor to.
   */
  public MethodSymbol addConstructor(AggregateSymbol t) {
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
  public MethodSymbol addConstructor(AggregateSymbol t, Optional<ISymbol> s) {
    if (s.isPresent()) {
      return addConstructor(t, s.get());
    }
    return addConstructor(t);
  }

  /**
   * Add a constructor to the type aggregate with a particular parameter.
   */
  public MethodSymbol addConstructor(AggregateSymbol t, ISymbol s) {
    MethodSymbol constructor = addConstructor(t);
    constructor.define(s);
    return constructor;
  }

  public AggregateSymbol createTemplateGenericType(String name, IScope enclosingScope,
                                                   List<AggregateSymbol> teeSymbols) {
    return new AggregateSymbol(name, enclosingScope, teeSymbols);
  }

  public AggregateSymbol createTemplateGenericType(String name, IScope enclosingScope,
                                                   AggregateSymbol teeSymbol) {
    return new AggregateSymbol(name, enclosingScope, List.of(teeSymbol));
  }

  public FunctionSymbol createTemplateGenericFunction(String name, IScope enclosingScope,
                                                      AggregateSymbol teeSymbol) {
    return new FunctionSymbol(name, enclosingScope, List.of(teeSymbol));
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
  public AggregateSymbol createGenericT(String name, IScope scope) {
    AggregateSymbol t = new AggregateSymbol(name, scope);
    t.setGenericTypeParameter(true);

    addConstructor(t);

    final Optional<ISymbol> integerType = scope.resolve(new TypeSymbolSearch("Integer"));
    final Optional<ISymbol> stringType = scope.resolve(new TypeSymbolSearch("String"));
    final Optional<ISymbol> booleanType = scope.resolve(new TypeSymbolSearch("Boolean"));
    final Optional<ISymbol> voidType = scope.resolve(new TypeSymbolSearch("Void"));

    /*
     * Now make the '?', null check operator - this enables us to do null checking in
     * the template definition in the ek9 file
     */


    //isSet
    addPurePublicSimpleOperator(t, "?", booleanType);
    //Now a _string $ operator
    addPurePublicSimpleOperator(t, "$", stringType);
    //To JSON operator
    addPurePublicSimpleOperator(t, "$$", stringType);
    //hash code
    addPurePublicSimpleOperator(t, "#?", integerType);

    addPurePublicSimpleOperator(t, "empty", booleanType);
    addPurePublicSimpleOperator(t, "length", integerType);

    //First and last
    addPurePublicPrefixSuffixMethod(t, "#<");
    addPurePublicPrefixSuffixMethod(t, "#>");

    //negate operator
    addPurePublicPrefixSuffixMethod(t, "~");

    addPurePublicPrefixSuffixMethod(t, "++");
    addPurePublicPrefixSuffixMethod(t, "--");

    //Support automatic opening and closing of 'things'
    addPurePublicSimpleOperator(t, "open", voidType);
    addPurePublicSimpleOperator(t, "close", voidType);

    //Other operators

    //So for operators these will deal in the same type.
    addOperator(t, "+", true);
    addOperator(t, "-", true);
    addOperator(t, "*", true);
    addOperator(t, "/", true);

    //copy/clone
    addOperator(t, ":=:", true);

    addOperator(t, "|", false);
    addOperator(t, "+=", false);
    addOperator(t, "-=", false);
    addOperator(t, "*=", false);
    addOperator(t, "/=", false);

    //merge
    addOperator(t, ":~:", false);

    //replace
    addOperator(t, ":^:", false);

    addComparatorOperator(t, "==", booleanType);
    addComparatorOperator(t, "<>", booleanType);
    addComparatorOperator(t, "<", booleanType);
    addComparatorOperator(t, "<=", booleanType);
    addComparatorOperator(t, ">=", booleanType);
    addComparatorOperator(t, ">", booleanType);

    //compare
    addComparatorOperator(t, "<=>", integerType);
    //fuzzy compare
    addComparatorOperator(t, "<~>", integerType);

    return t;
  }

  /**
   * Just add a method to an aggregate with the name and parameters and return type.
   * The methodParameters can be empty if there are none.
   */
  public MethodSymbol addPublicMethod(AggregateSymbol clazz, String methodName,
                                      List<ISymbol> methodParameters,
                                      Optional<ISymbol> returnType) {
    MethodSymbol method = new MethodSymbol(methodName, clazz);
    method.setParsedModule(clazz.getParsedModule());
    methodParameters.forEach(method::define);
    clazz.define(method);
    method.setType(returnType);
    return method;
  }

  /**
   * Adds all the appropriate methods for a type that is an 'Enumeration'.
   * This allows a developer to use built-in methods on the 'Enumeration' type.
   */
  public void addEnumerationMethods(AggregateSymbol clazz) {
    final Optional<ISymbol> booleanType = clazz.resolve(new TypeSymbolSearch("Boolean"));
    final Optional<ISymbol> integerType = clazz.resolve(new TypeSymbolSearch("Integer"));
    final Optional<ISymbol> stringType = clazz.resolve(new TypeSymbolSearch("String"));
    //Some reasonable operations
    //compare
    addComparatorOperator(clazz, "<=>", integerType);
    addComparatorOperator(clazz, "==", booleanType);
    addComparatorOperator(clazz, "<>", booleanType);
    addComparatorOperator(clazz, "<", booleanType);
    addComparatorOperator(clazz, ">", booleanType);
    addComparatorOperator(clazz, "<=", booleanType);
    addComparatorOperator(clazz, ">=", booleanType);

    //isSet
    addPurePublicSimpleOperator(clazz, "?", booleanType);
    //Now a _string $ operator
    addPurePublicSimpleOperator(clazz, "$", stringType);
    //To JSON operator
    addPurePublicSimpleOperator(clazz, "$$", stringType);
    //hash code
    addPurePublicSimpleOperator(clazz, "#?", integerType);

    //First and last
    addPurePublicPrefixSuffixMethod(clazz, "#<");
    addPurePublicPrefixSuffixMethod(clazz, "#>");
  }

  private MethodSymbol addPurePublicPrefixSuffixMethod(AggregateSymbol clazz, String methodName) {
    return addPurePublicSimpleOperator(clazz, methodName, Optional.of(clazz));
  }

  /**
   * Adds a form of a comparison operator.
   */
  public MethodSymbol addComparatorOperator(AggregateSymbol clazz, String comparatorType,
                                            Optional<ISymbol> returnType) {
    MethodSymbol operator = addPurePublicSimpleOperator(clazz, comparatorType, returnType);
    operator.define(new VariableSymbol("param", clazz));
    return operator;
  }

  /**
   * Adds a simple operator, that is 'pure' (no side effects) and accepts no
   * parameters, but just returns a value.
   */
  public MethodSymbol addPurePublicSimpleOperator(AggregateSymbol clazz, String methodName,
                                                  Optional<ISymbol> returnType) {
    MethodSymbol method = new MethodSymbol(methodName, clazz);
    method.setReturningSymbol(new VariableSymbol("rtn", returnType));
    method.setParsedModule(clazz.getParsedModule());
    method.setAccessModifier("public");
    method.setMarkedPure(true);
    method.setOperator(true);
    method.setVirtual(false);
    clazz.define(method);
    return method;
  }

  /**
   * Add a normal operator, that accepts a type and the type name of the operator.
   * Can be pure +/-/* etc or a mutator like *=.
   */
  public MethodSymbol addOperator(AggregateSymbol clazz, String operatorType, boolean isPure) {
    VariableSymbol paramT = new VariableSymbol("param", clazz);

    MethodSymbol operator = new MethodSymbol(operatorType, clazz);
    operator.setParsedModule(clazz.getParsedModule());
    operator.setAccessModifier("public");
    operator.setMarkedPure(isPure);
    operator.setOperator(true);
    operator.setVirtual(false);

    operator.define(paramT);
    //returns the same type as itself
    operator.setType(clazz);
    clazz.define(operator);
    return operator;
  }
}
