package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.SymbolMatcher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents some type of method that exists on an aggregate type scope.
 * Or it could just be a function type concept at the module level.
 * <p>
 * This could be an 'operation' that exists on a model type aggregate for
 * example. Or it may be one of the storage operations.
 */
public class MethodSymbol extends ScopedSymbol
{
	/**
	 * Keep separate variable for what we are returning because we need its name and type.
	 */
	private ISymbol returningSymbol;

	//Just used internally to check for method signature matching
	private final SymbolMatcher matcher = new SymbolMatcher();

	/**
	 * So has the developer indicated that this method is an overriding method.
	 */
	private boolean override = false;
	/**
	 * By default, access to methods is public unless otherwise modified.
	 */
	private String accessModifier = "public";

	/**
	 * Is this a constructor method or just a normal method
	 */
	private boolean constructor = false;

	/**
	 * Is this an operator like := or < etc.
	 */
	private boolean operator = false;

	/**
	 * Was it marked abstract in the source code.
	 */
	private boolean markedAbstract = false;

	/**
	 * We may be interested to know and may restrict some operation if this function is marked as pure.
	 */
	private boolean markedPure = false;

	private boolean markedAsDispatcher = false;

	/**
	 * We may or may not alter if it was abstract by setting this virtual value.
	 */
	private boolean virtual = false;

	/**
	 * Should this method be cloned during a clone operation like for type defines.
	 */
	private boolean markedNoClone = false;

	/**
	 * Is this method a synthetic one, ie typically for constructors the compiler can indicate
	 * this method should be created in code generation.
	 */
	private boolean synthetic = false;

	/**
	 * Really just used for reverse engineered methods from Java we need to know if the method returns this or not.
	 */
	private boolean ek9ReturnsThis = false;

	private String usedAsProxyForDelegate = null;

	/**
	 * When returning a values and this method is in a generic parameterised class does the return value
	 * need wrapping up or can it be returned directly.
	 */
	private boolean parameterisedWrappingRequired = false;

	public MethodSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
		super.setCategory(SymbolCategory.METHOD);
		super.setGenus(SymbolGenus.VALUE);
	}

	/**
	 * Typically used for cloning constructors.
	 */
	public MethodSymbol(String name, ISymbol type, IScope enclosingScope)
	{
		super(name, Optional.of(type), enclosingScope);
		super.setCategory(SymbolCategory.METHOD);
		super.setGenus(SymbolGenus.VALUE);
	}

	public MethodSymbol(String name, Optional<ISymbol> type, IScope enclosingScope)
	{
		super(name, type, enclosingScope);
		super.setCategory(SymbolCategory.METHOD);
		super.setGenus(SymbolGenus.VALUE);
	}

	public MethodSymbol clone(ISymbol withType, IScope withParentAsAppropriate)
	{
		return cloneIntoMethodSymbol(new MethodSymbol(this.getName(), withType, withParentAsAppropriate));
	}

	@Override
	public MethodSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoMethodSymbol(new MethodSymbol(this.getName(), this.getType(), withParentAsAppropriate));
	}

	protected MethodSymbol cloneIntoMethodSymbol(MethodSymbol newCopy)
	{
		super.cloneIntoScopeSymbol(newCopy);

		newCopy.setSourceToken(getSourceToken());
		if(isReturningSymbolPresent())
			newCopy.returningSymbol = this.returningSymbol.clone(newCopy);

		newCopy.override = this.override;
		newCopy.accessModifier = this.accessModifier;
		newCopy.constructor = this.constructor;
		newCopy.operator = this.operator;
		newCopy.markedAbstract = this.markedAbstract;
		newCopy.markedPure = this.markedPure;
		newCopy.markedAsDispatcher = this.markedAsDispatcher;
		newCopy.virtual = this.virtual;
		newCopy.markedNoClone = this.markedNoClone;
		newCopy.synthetic = this.synthetic;
		newCopy.ek9ReturnsThis = this.ek9ReturnsThis;
		newCopy.usedAsProxyForDelegate = this.usedAsProxyForDelegate;
		newCopy.parameterisedWrappingRequired = this.parameterisedWrappingRequired;

		return newCopy;
	}

	public boolean isSynthetic()
	{
		return synthetic;
	}

	public void setSynthetic(boolean synthetic)
	{
		this.synthetic = synthetic;
	}

	@Override
	public boolean isParameterisedWrappingRequired()
	{
		return parameterisedWrappingRequired;
	}

	public void setParameterisedWrappingRequired(boolean parameterisedWrappingRequired)
	{
		this.parameterisedWrappingRequired = parameterisedWrappingRequired;
	}

	public boolean isOverride()
	{
		return override;
	}

	public void setOverride(boolean override)
	{
		this.override = override;
	}

	public String getAccessModifier()
	{
		return accessModifier;
	}

	public void setAccessModifier(String accessModifier)
	{
		this.accessModifier = accessModifier;
	}

	public boolean isPrivate()
	{
		return accessModifier.equals("private");
	}

	public boolean isProtected()
	{
		return accessModifier.equals("protected");
	}

	public boolean isPublic()
	{
		return accessModifier.equals("public");
	}

	@Override
	public void define(ISymbol symbol)
	{
		super.define(symbol);
	}

	@Override
	public ISymbol setType(Optional<ISymbol> type)
	{
		return super.setType(type);
	}

	public boolean isUsedAsProxyForDelegate()
	{
		return usedAsProxyForDelegate != null;
	}

	public String getUsedAsProxyForDelegate()
	{
		return this.usedAsProxyForDelegate;
	}

	public void setUsedAsProxyForDelegate(String delegateName)
	{
		this.usedAsProxyForDelegate = delegateName;
		//now this also means a couple of other things
		this.setOverride(true);
		this.setMarkedAbstract(false);
		this.setVirtual(false);
	}

	public void setEk9ReturnsThis(boolean ek9ReturnsThis)
	{
		this.ek9ReturnsThis = ek9ReturnsThis;
	}

	public boolean isEk9ReturnsThis()
	{
		return ek9ReturnsThis;
	}

	public boolean isMarkedPure()
	{
		return markedPure;
	}

	public void setMarkedPure(boolean markedPure)
	{
		this.markedPure = markedPure;
	}

	public boolean isMarkedNoClone()
	{
		return markedNoClone;
	}

	public void setMarkedNoClone(boolean markedNoClone)
	{
		this.markedNoClone = markedNoClone;
	}

	public boolean isMarkedAsDispatcher()
	{
		return markedAsDispatcher;
	}

	public MethodSymbol setMarkedAsDispatcher(boolean markedAsDispatcher)
	{
		this.markedAsDispatcher = markedAsDispatcher;
		return this;
	}

	public boolean isMarkedAbstract()
	{
		return markedAbstract;
	}

	public void setMarkedAbstract(boolean markedAbstract)
	{
		this.markedAbstract = markedAbstract;
	}

	public void setVirtual(boolean virtual)
	{
		this.virtual = virtual;
	}

	public boolean isVirtual()
	{
		return virtual;
	}

	@Override
	public boolean isAConstant()
	{
		return true;
	}

	public boolean isConstructor()
	{
		return constructor;
	}

	public MethodSymbol setConstructor(boolean constructor)
	{
		this.constructor = constructor;
		return this;
	}

	public boolean isOperator()
	{
		return operator;
	}

	public MethodSymbol setOperator(boolean operator)
	{
		this.operator = operator;
		return this;
	}

	/**
	 * Some methods and functions have a named return symbol 'like rtn as String' for example.
	 * In other cases a method or a function will not return anything (We use 'Void') in the case as the 'type'.
	 * So when a Returning Symbol is set we use the type of the returning variable as the type return on the function/method.
	 */
	public boolean isReturningSymbolPresent()
	{
		return returningSymbol != null;
	}

	public ISymbol getReturningSymbol()
	{
		return returningSymbol;
	}

	protected void justSetReturningSymbol(ISymbol returningSymbol)
	{
		this.returningSymbol = returningSymbol;
	}

	public void setReturningSymbol(ISymbol returningSymbol)
	{
		justSetReturningSymbol(returningSymbol);

		if(returningSymbol != null)
			this.setType(returningSymbol.getType());
	}

	/**
	 * Does the signature of this method match that of the method passed in.
	 * Not the name of the method just the signature of the parameter types
	 * and special treatment for the return type - this can be coerced back or be a super type
	 */
	public boolean isSignatureMatchTo(MethodSymbol toMethod)
	{
		List<ISymbol> ourParams = this.getSymbolsForThisScope();
		List<ISymbol> theirParams = toMethod.getSymbolsForThisScope();
		double weight = matcher.getWeightOfParameterMatch(ourParams, theirParams);
		//must be exact match
		if(weight > 0.0 || weight < 0.0)
			return false;
		//System.out.println("parameter match weight is "+ weight);
		weight = matcher.getWeightOfMatch(this.getType(), toMethod.getType());

		//System.out.println("Return type weight is "+ weight);
		return !(weight < 0.0);
	}

	public boolean isParameterSignatureMatchTo(List<ISymbol> params)
	{
		List<ISymbol> ourParams = this.getSymbolsForThisScope();
		double weight = matcher.getWeightOfParameterMatch(ourParams, params);
		return !(weight < 0.0);
	}

	/**
	 * Added convenience method to make the parameters a bit more obvious
	 */
	public List<ISymbol> getMethodParameters()
	{
		return super.getSymbolsForThisScope();
	}

	/**
	 * Typically used when making synthetic methods you want to add in all the params from another method or something.
	 *
	 * @param params The parameters to pass to the method.
	 * @return this method - used for chaining.
	 */
	public MethodSymbol setMethodParameters(List<ISymbol> params)
	{
		for(ISymbol param : params)
			define(param);
		return this;
	}

	@Override
	public String getFriendlyName()
	{
		return doGetFriendlyName(this.getType());
	}

	protected String doGetFriendlyName(Optional<ISymbol> aType)
	{
		StringBuilder buffer = new StringBuilder();
		if(this.isOverride())
			buffer.append("override ");

		buffer.append(accessModifier).append(" ");

		buffer.append(getSymbolTypeAsString(aType));

		buffer.append(" <- ").append(super.getName());
		buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(getSymbolsForThisScope(), true));
		if(this.markedAbstract)
			buffer.append(" as abstract");
		return buffer.toString();
	}

	public String toString()
	{
		StringBuilder buffer = new StringBuilder();

		//for to string we want a bit of extra info here that we don't show end user. but good for debugging.
		if(this.virtual)
			buffer.append("virtual ");

		buffer.append(getFriendlyName());

		return buffer.toString();
	}
}
