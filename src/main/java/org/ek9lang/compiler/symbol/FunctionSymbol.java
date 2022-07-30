package org.ek9lang.compiler.symbol;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

import java.util.List;
import java.util.Optional;

/**
 * Scope for functions that are part of a module.
 * <p>
 * While in ek9 these are just functions, when mapped to java we can implement in any way we like i.e. classes.
 * <p>
 * We need to ensure that any functions we extend have the same method signature.
 */
public class FunctionSymbol extends MethodSymbol implements ICanCaptureVariables
{
	//This is the module this function has been defined in.
	private IScope moduleScope;

	/**
	 * For Functions symbols we keep a handle on the context where the returning param (if any) was defined.
	 * We do this because with functions we allow the function name to be defined when implementing an abstract function
	 * without the need to refine all the parameter and returns. clearly this would not make sense for methods where you
	 * have overloading but for functions there is only one name for that function just the parameters and returns alter.
	 */
	private EK9Parser.ReturningParamContext returningParamContext;

	/**
	 * To be used when this function extends an abstract function.
	 * So we want the same method signature as the abstract function but this provides the implementation.
	 * It is sort of object-oriented but for functions.
	 */
	private Optional<FunctionSymbol> superFunctionSymbol = Optional.empty();

	/**
	 * For dynamic functions we can capture variables from the enclosing scope(s) and pull them in
	 * We can then hold and access them in the dynamic function even when the function has moved out of the original scope.
	 * i.e. a sort of closure over variables.
	 */
	private Optional<LocalScope> capturedVariables = Optional.empty();

	public FunctionSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
		super.setCategory(SymbolCategory.FUNCTION);
		super.setProduceFullyQualifiedName(true);
	}

	/**
	 * A function that can be parameterised, i.e. like a 'List of T'
	 * So the name would be 'List' and the parameterTypes would be a single aggregate of a conceptual T.
	 */
	public FunctionSymbol(String name, IScope enclosingScope, List<AggregateSymbol> parameterTypes)
	{
		this(name, enclosingScope);
		parameterTypes.forEach(this::addParameterisedType);
	}

	@Override
	public FunctionSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoFunctionSymbol(new FunctionSymbol(this.getName(), withParentAsAppropriate));
	}

	protected FunctionSymbol cloneIntoFunctionSymbol(FunctionSymbol newCopy)
	{
		super.cloneIntoMethodSymbol(newCopy);
		newCopy.setCategory(SymbolCategory.FUNCTION);
		newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
		newCopy.moduleScope = this.moduleScope;
		newCopy.returningParamContext = this.returningParamContext;
		superFunctionSymbol.ifPresent(functionSymbol -> newCopy.superFunctionSymbol = Optional.of(functionSymbol));

		if(capturedVariables.isPresent())
		{
			LocalScope newCaptureScope = new LocalScope("CaptureScope", getEnclosingScope());
			capturedVariables.get().cloneIntoLocalScope(newCaptureScope);
			newCopy.setCapturedVariables(newCaptureScope);
		}
		return newCopy;
	}

	/**
	 * Is this aggregate itself a generic sort of aggregate.
	 *
	 * @return boolean true if is parameterised
	 */
	@Override
	public boolean isGenericInNature()
	{
		return !this.getParameterisedTypes().isEmpty();
	}

	@Override
	public FunctionSymbol addParameterisedType(AggregateSymbol parameterisedType)
	{
		super.addParameterisedType(parameterisedType);
		super.setCategory(SymbolCategory.TEMPLATE_FUNCTION);
		return this;
	}

	public EK9Parser.ReturningParamContext getReturningParamContext()
	{
		return returningParamContext;
	}

	public void setReturningParamContext(EK9Parser.ReturningParamContext returningParamContext)
	{
		this.returningParamContext = returningParamContext;
	}

	public IScope getModuleScope()
	{
		return moduleScope;
	}

	public void setModuleScope(IScope moduleScope)
	{
		this.moduleScope = moduleScope;
	}

	public Optional<LocalScope> getCapturedVariables()
	{
		return capturedVariables;
	}

	public void setCapturedVariables(LocalScope capturedVariables)
	{
		setCapturedVariables(Optional.ofNullable(capturedVariables));
	}

	public void setCapturedVariables(Optional<LocalScope> capturedVariables)
	{
		this.capturedVariables = capturedVariables;
	}

	public void setCapturedVariablesVisibility(final boolean isPublic)
	{
		capturedVariables.ifPresent(localScope -> localScope.getSymbolsForThisScope().forEach(symbol -> {
			if(symbol instanceof VariableSymbol s)
				s.setPrivate(!isPublic);
		}));
	}

	public Optional<FunctionSymbol> getSuperFunctionSymbol()
	{
		return superFunctionSymbol;
	}

	public void setSuperFunctionSymbol(Optional<FunctionSymbol> superFunctionSymbol)
	{
		this.superFunctionSymbol = superFunctionSymbol;
	}

	@Override
	public void setReturningSymbol(ISymbol returningSymbol)
	{
		justSetReturningSymbol(returningSymbol);
	}

	@Override
	public double getAssignableWeightTo(ISymbol s)
	{
		return getUnCoercedAssignableWeightTo(s);
	}

	@Override
	public double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		double canAssign = super.getUnCoercedAssignableWeightTo(s);
		if(canAssign >= 0.0)
			return canAssign;

		//now we can check superclass matches. but add some weight because this did not match
		return superFunctionSymbol.map(value -> 0.05 + value.getUnCoercedAssignableWeightTo(s)).orElse(-1.0);
	}

	@Override
	public String getName()
	{
		return capturedVariables
				.map(scope -> "dynamic function" + CommonParameterisedTypeDetails.asCommaSeparated(scope.getSymbolsForThisScope(), true))
				.orElse(super.getName());
	}

	@Override
	public String getFriendlyScopeName()
	{
		return getFriendlyName();
	}

	@Override
	public String getFriendlyName()
	{
		Optional<ISymbol> returningSymbolType = getReturningSymbol() != null ? getReturningSymbol().getType() : Optional.empty();
		var	name = doGetFriendlyName(getName(), returningSymbolType) + getAnyGenericParamsAsFriendlyNames();

		return superFunctionSymbol.map(s -> name + " is " + s.getName()).orElse(name);
	}

	@Override
	public Optional<ISymbol> getType()
	{
		//Treat this as a type. To get result of call need to use:
		return Optional.of(this);
	}

	@Override
	public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search)
	{
		return super.resolveInThisScopeOnly(search);
	}

	@Override
	public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search)
	{
		//first check normally - i.e. the params on the function call and anything declared in the function body
		Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
		if(rtn.isEmpty() && capturedVariables.isPresent())
			rtn = capturedVariables.get().resolveInThisScopeOnly(search);

		return rtn;
	}

	@Override
	public Optional<ISymbol> resolve(SymbolSearch search)
	{
		//So a bit complex in how we resolve variables in functions
		//because we want to be able to resolve parametric types, general types, scope module items captured variables and method params.
		//But not stuff from the scope where the function as defined (captured gets passed-in).
		//But in some cases we need to resolve a generic type like T as it was used in a parent class or function!

		Optional<ISymbol> rtn = Optional.empty();
		//Now if this is a generic type class we might need to resolve the name of the type 'T' or 'S' or whatever for example		
		if(isGenericInNature() && search.getSearchType().equals(SymbolCategory.TYPE))
		{
			for(ISymbol parameterisedType : getParameterisedTypes())
			{
				if(parameterisedType.isAssignableTo(search.getNameAsSymbol()))
					rtn = Optional.of(parameterisedType);
			}
		}

		//But note we limit the search in the captured vars to that scope only - no looking up the enclosing scopes just the capture scope!
		if(rtn.isEmpty() && capturedVariables.isPresent())
			rtn = capturedVariables.get().resolveInThisScopeOnly(search);

		//Now here we must resolve some things but not others in specific ways
		if(rtn.isEmpty())
			rtn = super.resolveInThisScopeOnly(search); // check for parameters

		//check for general types
		if(rtn.isEmpty() && moduleScope != null)
			rtn = moduleScope.resolve(search);

		//only now do we check up the enclosing scope and for a generic function that could have a generic type defined but that is all we are allowed.
		//So if we need to resolve stuff in the enclosing scope and that scope is a class of a function that is generic then S T or whatever
		//well be resolvable; and we need that in this case.
		if(rtn.isEmpty())
			return super.getEnclosingScope().resolve(search);
		return rtn;
	}
}
