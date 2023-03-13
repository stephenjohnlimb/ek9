package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

/**
 * Given a parameterised symbol and the symbol used to use as the parameter we are in effect saying:
 * Given List of T
 * Given SomeObjectType
 * We are saying List of SomeObjectType
 * But SomeObjectType can also be a parameterised/able type!
 * TODO maybe refactor this and the ParameterisedFunctionSymbol to pull out common stuff.
 */
public class ParameterisedTypeSymbol extends AggregateSymbol implements ParameterisedSymbol {
  //This is the class that can be parameterised
  //Now you need to check if this symbol type was reverse engineered as you will need to treat
  //it in a different manner, maybe.
  private final AggregateSymbol parameterisableSymbol;

  //This is what it is parameterised with.
  private final List<ISymbol> parameterSymbols = new ArrayList<>();

  private boolean variablesAndMethodsHydrated = false;

  /**
   * Create a new type that is capable of being parameterised.
   */
  public ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol,
                                 List<ISymbol> parameterSymbols, IScope enclosingScope) {
    this(parameterisableSymbol, enclosingScope);
    parameterSymbols.forEach(this::addParameterSymbol);
    parameterisationComplete();
  }

  public ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol,
                                 Optional<ISymbol> parameterSymbol, IScope enclosingScope) {
    this(parameterisableSymbol, parameterSymbol.stream().toList(), enclosingScope);
  }

  private ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol, IScope enclosingScope) {
    super("", enclosingScope); //Gets set below
    AssertValue.checkNotNull("parameterisableSymbol cannot be null", parameterisableSymbol);

    if (!parameterisableSymbol.isGenericInNature() || !parameterisableSymbol.isTemplateType()) {
      throw new IllegalArgumentException("parameterisableSymbol must be parameterised");
    }

    this.parameterisableSymbol = parameterisableSymbol;
    //Put is in the same module as where the generic type has been defined.
    //Note that this could and probably will be a system on in many cases List and Optional.
    this.setModuleScope(parameterisableSymbol.getModuleScope());
    //Assume a TYPE, but see parameterisationComplete - because if any parameters are not concrete this is a TEMPLATE
    super.setCategory(SymbolCategory.TYPE);

    setEk9Core(parameterisableSymbol.isEk9Core());
  }

  @Override
  public boolean isParameterisedType() {
    return true;
  }

  @Override
  public boolean isOpenForExtension() {
    //Whatever the parameterisable symbol was defined to be.
    return parameterisableSymbol.isOpenForExtension();
  }

  public AggregateSymbol getParameterisableSymbol() {
    return parameterisableSymbol;
  }

  public List<ISymbol> getParameterSymbols() {
    return parameterSymbols;
  }

  public boolean isVariablesAndMethodsHydrated() {
    return variablesAndMethodsHydrated;
  }

  @Override
  public boolean isSymbolTypeMatch(ISymbol symbolType) {
    //So was this type created by the type we are checking
    if (parameterisableSymbol.isExactSameType(symbolType)) {
      //So this might also be considered equivalent
      //But let's check that the parameterised types are a match
      return CommonParameterisedTypeDetails.doSymbolsMatch(parameterSymbols,
          parameterisableSymbol.getParameterisedTypes());
    }
    return super.isSymbolTypeMatch(symbolType);
  }

  /**
   * So once you have created this object and added all the parameters you want to
   * Call this so that the full name of the concrete/conceptual parameterised generic type is manifest.
   */
  private void parameterisationComplete() {
    var numberOfParameters = this.parameterSymbols.size();
    var numberOfAvailableGenericParameters =
        this.parameterisableSymbol.getAnyGenericParameters().size();

    //There should have been a check in the compiler earlier than this but leave this in place for a while.
    if (numberOfParameters != numberOfAvailableGenericParameters) {
      throw new CompilerException("Compiler error mismatch in number of parameters");
    }

    super.setName(
        CommonParameterisedTypeDetails.getInternalNameFor(parameterisableSymbol, parameterSymbols));

    var isTemplateType = parameterSymbols.stream().anyMatch(ISymbol::isGenericTypeParameter);

    if (isTemplateType) {
      super.setCategory(SymbolCategory.TEMPLATE_TYPE);
    } else {
      super.setCategory(SymbolCategory.TYPE);
    }
    super.setGenus(SymbolGenus.TYPE);
  }

  /**
   * Need to go through this parameterised type and workout and return if any new paramterised
   * types are required.
   * So for example Optional of OO  might use a Iterator of II,
   * so if we create a new Optional of V  it means we also need an Iterator of V.
   */
  public void establishDependentParameterisedTypes(IScope global) {
    for (ISymbol symbol : parameterisableSymbol.getSymbolsForThisScope()) {
      //So if method or variable get it's return type and resolve with new
      //lets see if it needs creating or if it already exists.

      ResolutionResult result = resolveWithNewType(symbol.getType());
      if (!result.wasResolved && result.symbol.isPresent()) {
        ISymbol s = result.symbol.get();
        global.define(s);
        if (s instanceof ParameterisedTypeSymbol ps) {
          ps.establishDependentParameterisedTypes(global);
        }
      }
      if (symbol.isMethod()) {
        MethodSymbol method = (MethodSymbol) symbol;
        method.getSymbolsForThisScope().forEach(param -> {
          ResolutionResult paramResult = resolveWithNewType(param.getType());
          if (!paramResult.wasResolved && paramResult.symbol.isPresent()) {
            ISymbol s = paramResult.symbol.get();
            global.define(s);
            if (s instanceof ParameterisedTypeSymbol ps) {
              ps.establishDependentParameterisedTypes(global);
            }
          }
        });
      }
    }
  }

  /**
   * After phase one all the basic types and method signatures are set up for the types we can
   * use for templates.
   * But now we need to create the parameterised types and set up the specific signatures with
   * the actual types.
   */
  public void initialSetupVariablesAndMethods() {

    //Collect up all these definitions in a single symbol table not per module - but more like
    //a global symbol table just for templates that have been turned into types by having
    //parameters provided.
    //then you can get back to cloning the properties and methods from the parameterisableSymbol
    //and replacing the T's and S's etc. with these actual parameterSymbols types.
    //Then it's just like you've written the same code with different parameter types.

    //Then you can go to the resolve phase, and you'll have concrete types in place.
    //Just remember in the symbols we don't really deal with bodies as such - that is the IR phase
    //Symbol phase we are really checking all these methods, classes and properties exist and
    //are of the right type.
    //In the expression phase just checking all those add up ok.
    //But only in the IR phase do we really pull together the contents of the methods into Nodes.
    //So that's where we will also need to pull together the template type, concrete parameters
    //to create a full correct concrete IR

    //First off lets go through symbols and create copies but with the applied concrete types.

    parameterisableSymbol
        .getSymbolsForThisScope()
        .stream()
        .map(this::cloneSymbolWithNewType)
        .forEach(this::define);

    //We have now hydrated variables and methods they should now be resolvable.
    variablesAndMethodsHydrated = true;
  }

  /**
   * Clones any symbols that employ generic parameters. For non-generic uses the same symbol.
   *
   * @param toClone The symbol to Clone.
   * @return A new symbol or symbol passed in if no generic parameter replacement is required.
   */
  private ISymbol cloneSymbolWithNewType(ISymbol toClone) {
    //TODO refactor - don't clone the code!
    if (toClone instanceof VariableSymbol willClone) {
      Optional<ISymbol> fromType = willClone.getType();
      Optional<ISymbol> newType = resolveWithNewType(fromType).symbol;
      VariableSymbol rtn = willClone.clone(null);
      rtn.setType(newType);
      //So mimic the location of the source
      rtn.setSourceToken(toClone.getSourceToken());
      return rtn;
    } else if (toClone instanceof MethodSymbol willClone) {
      //Need to review all the method cloning as there is duplication in the MethodSymbol itself.
      Optional<ISymbol> fromType = willClone.getType();
      Optional<ISymbol> newType = resolveWithNewType(fromType).symbol;
      String useMethodName = willClone.getName();

      //Man, what about the constructor methods!
      //Don't we need to change their names to match the new class name
      //This is not Java - this is ek9 - the constructor method for aggregate Optional
      //is still marked as a constructor but keeps its old name! That way we can still resolve
      //it in ek9
      //Only at generation time to Java does that get changed.
      //But see how templateValidator has to deal with this.

      MethodSymbol rtn = new MethodSymbol(useMethodName, this);

      rtn.setType(newType);
      rtn.setOverride(willClone.isOverride());
      rtn.setAccessModifier(willClone.getAccessModifier());
      rtn.setConstructor(willClone.isConstructor());
      rtn.setOperator(willClone.isOperator());
      rtn.setMarkedPure(willClone.isMarkedPure());
      rtn.setUsedAsProxyForDelegate(willClone.getUsedAsProxyForDelegate());
      rtn.setEk9ReturnsThis(willClone.isEk9ReturnsThis());
      for (ISymbol symbol : willClone.getSymbolsForThisScope()) {
        ISymbol clonedParam = cloneSymbolWithNewType(symbol);
        rtn.define(clonedParam);
      }

      //what about rtn and type
      ISymbol rtnSymbol = willClone.getReturningSymbol();
      if (rtnSymbol != null) {
        ISymbol replacementSymbol = cloneSymbolWithNewType(rtnSymbol);
        rtn.setReturningSymbol(replacementSymbol);
      }

      return rtn;
    }
    throw new CompilerException(
        "We can only clone variables and methods in generic type templates not [" + toClone + "]");
  }

  /**
   * Resolve the type could be concrete or could be As S or T type generic.
   *
   * @param typeToResolve The type to resolve.
   * @return The new resolved symbol.
   */
  private ResolutionResult resolveWithNewType(Optional<ISymbol> typeToResolve) {
    if (typeToResolve.isPresent() && typeToResolve.get() instanceof IAggregateSymbol) {
      AggregateSymbol aggregate = (AggregateSymbol) typeToResolve.get();
      if (aggregate.isTemplateType()) {
        //So the aggregate here is the generic template definition.
        //We must ensure we marry up the generic parameters here - for example we might have
        //2 params S and T
        //But the aggregate we are lookup might just have one the 'T'. So we need to deal with this.
        List<ISymbol> lookupParameterSymbols = new ArrayList<>();
        for (ISymbol symbol : aggregate.getParameterisedTypes()) {
          //So given a T or whatever we need to find which index position it is in
          //Then we can use that same index position to get the equiv symbol from what parameters
          //have been applied.
          int index = getIndexOfType(Optional.of(symbol));
          if (index < 0) {
            //Just for debug.
            getIndexOfType(Optional.of(symbol));
            throw new CompilerException(
                "Unable to find symbol [" + symbol + "] in [" + aggregate + "] in [" + this + "]");
          }
          ISymbol resolvedSymbol = parameterSymbols.get(index);
          lookupParameterSymbols.add(resolvedSymbol);
        }
        //So now we have a generic type and a set of actual parameters we can resolve it!
        String parameterisedTypeSymbolName =
            CommonParameterisedTypeDetails.getInternalNameFor(aggregate, lookupParameterSymbols);
        TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
        ResolutionResult rtn = new ResolutionResult();
        rtn.symbol = this.resolve(search);
        rtn.wasResolved = rtn.symbol.isPresent();
        //So if not resolved then make one.
        if (!rtn.wasResolved) {
          rtn.symbol = Optional.of(new ParameterisedTypeSymbol(aggregate, lookupParameterSymbols,
              this.getEnclosingScope()));
        }

        return rtn;
      } else if (aggregate.isGenericTypeParameter()) {
        //So if this is a T or and S or a P for example we need to know
        //What actual symbol to use from the parameterSymbols we have been parameterised with.
        int index = getIndexOfType(typeToResolve);
        ISymbol resolvedSymbol = parameterSymbols.get(index);
        ResolutionResult rtn = new ResolutionResult();
        rtn.symbol = Optional.of(resolvedSymbol);
        rtn.wasResolved = true;
        return rtn;
      } else if (aggregate.isGenericInNature()
          && aggregate instanceof ParameterisedTypeSymbol asParameterisedTypeSymbol) {
        //Now this is a  bastard because we need to clone one of these classes we're already in.
        //But it might be a List of T or a real List of Integer for example.

        //So the thing we are to clone it itself a generic aggregate.
        List<ISymbol> lookupParameterSymbols = new ArrayList<>();
        //Now it may really be a concrete one, or it too could be something like a List of P

        for (ISymbol symbol : asParameterisedTypeSymbol.parameterSymbols) {
          //So lets see might be a K or V for example, but we must be able to find that type
          //in this object
          if (symbol.isGenericTypeParameter()) {
            int index = getIndexOfType(Optional.of(symbol));
            if (index < 0) {
              throw new CompilerException("Unable to find symbol [" + symbol + "]");
            }

            ISymbol resolvedSymbol = parameterSymbols.get(index);
            lookupParameterSymbols.add(resolvedSymbol);
          } else {
            //Ah, no it is a concrete type so just use that.
            lookupParameterSymbols.add(symbol);
          }
        }

        String parameterisedTypeSymbolName = CommonParameterisedTypeDetails.getInternalNameFor(
            asParameterisedTypeSymbol.parameterisableSymbol, lookupParameterSymbols);
        TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
        ResolutionResult rtn = new ResolutionResult();
        rtn.symbol = this.resolve(search);
        rtn.wasResolved = rtn.symbol.isPresent();
        //So if not resolved then make one.
        if (!rtn.wasResolved) {
          rtn.symbol = Optional.of(
              new ParameterisedTypeSymbol(asParameterisedTypeSymbol.parameterisableSymbol,
                  lookupParameterSymbols, this.getEnclosingScope()));
        }
        return rtn;
      }
    }
    ResolutionResult rtn = new ResolutionResult();
    rtn.symbol = typeToResolve;
    rtn.wasResolved = true;
    return rtn;
  }

  /**
   * For the type passed in - a T or and S whatever we need to know its index.
   * From this we can look at what this has been parameterised with and use that type.
   *
   * @param theType The generic definition parameter i.e S, or T
   * @return The index or -1 if not found.
   */
  private int getIndexOfType(Optional<ISymbol> theType) {
    if (theType.isPresent()) {
      for (int i = 0; i < parameterisableSymbol.getParameterisedTypes().size(); i++) {
        ISymbol paramType = parameterisableSymbol.getParameterisedTypes().get(i);
        if (paramType.isExactSameType(theType.get())) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public String getFriendlyName() {
    return parameterisableSymbol.getName() + " of "
        + optionalParenthesisParameterSymbolsAsCommaSeparated();
  }

  @Override
  public ScopeType getScopeType() {
    return ScopeType.NON_BLOCK;
  }

  private static class ResolutionResult {
    /**
     * The Type that would be needed.
     * Note is may already exist and was resolved OK
     * See was Resolved below.
     */
    Optional<ISymbol> symbol = Optional.empty();

    /**
     * So the result may well be set, but in the case of ParameterisedTypeSymbols
     * it maybe that this is what we need - but it has not yet been created.
     */
    boolean wasResolved = false;
  }
}
