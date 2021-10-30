/**
 * 
 */
package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is typically a 'class' or an interface type where it can include the definitions of new
 * properties. These can then be made accessible via the symbol table to its
 * methods.
 * 
 * But note there is also a single super 'AggregateScopedSymbol' that maps to a
 * super class so here the resolve can access variables in the super class.
 * 
 * i.e we only support single inheritance - but see AggregateWithTraitsSymbol for 'traits' support.
 * 
 * I've also added in a mechanism to parameterise the aggregate like Java generics.
 * 
 * In general the resolution would be able to resolve variables in the enclosing
 * scope because that will be module level in ZEN, there are new global types
 * and global constants that can appear to exist in the package (module) level
 * scope.
 * 
 * So for example it is possible to reference a 'constant' or a 'type' that is
 * defined in the same module or if another module is drawn in through
 * 'references'.
 * 
 * In reality (when creating the Java code) the constants and types can just be
 * public statics on a class. For example module the.mod that defines Constants
 * could have a class called the.mod._Constants with public final statics
 * defined.
 * 
 * So there are two ways to resolve references first is module scope and second
 * is super class type scope.
 *
 */
public class AggregateSymbol extends ScopedSymbol implements SymbolType, IAggregateSymbol
{
	//This is the module this aggregate has been defined in.
	private IScope moduleScope;
	
	/**
	 * Just used in commented output really - just handy to understand the origin of the aggregate
	 * as some are synthetic.
	 */
	private String aggregateDescription;
	
	//This this actually a 'T' itself - we will need to know this.
	private boolean genericTypeParameter;
	
	/** If there is a method that acts as a dispatcher then this aggregate is also a dispatcher.. */
	private boolean markedAsDispatcher = false;
	
	/**
	 * Might always be null if a base 'class' or 'interface'.
	 */
	private Optional<IAggregateSymbol> superAggregateScopedSymbol = Optional.ofNullable(null);

	/**
	 * Also keep a back pointer to the direct subclasses.
	 */
	private List<IAggregateSymbol> subAggregateScopedSymbols = new ArrayList<IAggregateSymbol>();
	
	/*
	 * Was the aggregate marked as abstract in the source code.
	 */
	private boolean markedAbstract = false;
	
	private boolean forceVirtual = false;

	private boolean injectable = false;
	
	private boolean openForExtension = false;
	
	private Optional<String> pipeSinkType = Optional.ofNullable(null);
	
	private Optional<String> pipeSourceType = Optional.ofNullable(null);

	/**
	 * For dynamic classes we can capture variables from the enclosing scope(s) and pull them in
	 * We can then hold and access them in the dynamic class even when the class has moved out of the original scope.
	 * i.e. a sort of closure over a variable - but has to be declared and is not implicit like java/other languages.
	 */
	private Optional<LocalScope> capturedVariables = Optional.ofNullable(null);
	
	/**
	 * For reverse engineered aggregates we need to know if "_get" of object as itself
	 * is supported so we can create the IR output. 
	 */
	private boolean getOfSelfSupported = false;
	
	public AggregateSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
		super.setCategory(SymbolCategory.TYPE);
		super.setProduceFullyQualifiedName(true);
	}

	public AggregateSymbol(String name, Optional<ISymbol> type, IScope enclosingScope)
	{
		super(name, type, enclosingScope);
		super.setCategory(SymbolCategory.TYPE);
		super.setProduceFullyQualifiedName(true);
	}

	@Override
	public AggregateSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoAggregateSymbol(new AggregateSymbol(this.getName(), this.getType(), withParentAsAppropriate));
	}

	protected AggregateSymbol cloneIntoAggregateSymbol(AggregateSymbol newCopy)
	{
		super.cloneIntoScopeSymbol(newCopy);
		newCopy.moduleScope = moduleScope;
		if(aggregateDescription != null)
			newCopy.aggregateDescription = new String(aggregateDescription);

		newCopy.genericTypeParameter = genericTypeParameter;
		newCopy.markedAsDispatcher = markedAsDispatcher;

		if(superAggregateScopedSymbol.isPresent())
			newCopy.superAggregateScopedSymbol = Optional.of(superAggregateScopedSymbol.get());

		newCopy.subAggregateScopedSymbols.addAll(subAggregateScopedSymbols);
		newCopy.markedAbstract = markedAbstract;
		newCopy.forceVirtual = forceVirtual;
		newCopy.injectable = injectable;
		newCopy.openForExtension = openForExtension;

		if(pipeSinkType.isPresent())
			newCopy.pipeSinkType = Optional.of(pipeSinkType.get());

		if(pipeSourceType.isPresent())
			newCopy.pipeSourceType = Optional.of(pipeSourceType.get());

		if(capturedVariables.isPresent())
		{
			LocalScope newCaptureScope = new LocalScope("CaptureScope", getEnclosingScope());
			capturedVariables.get().cloneIntoLocalScope(newCaptureScope);
			newCopy.setCapturedVariables(newCaptureScope);
		}

		newCopy.getOfSelfSupported = getOfSelfSupported;

		return newCopy;
	}

	@Override
	public Optional<LocalScope> getCapturedVariables()
	{
		return capturedVariables;
	}

	@Override
	public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search)
	{
		Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
		return rtn;
	}

	public void setCapturedVariables(LocalScope capturedVariables)
	{
		this.capturedVariables = Optional.ofNullable(capturedVariables);
	}

	public void setCapturedVariablesVisibility(boolean isPublic)
	{
		if(capturedVariables.isPresent())
		{
			capturedVariables.get().getSymbolsForThisScope().forEach(symbol -> {
				if(symbol instanceof VariableSymbol)
				{
					((VariableSymbol)symbol).setPrivate(!isPublic);
				}
			});
		}
	}
	public void setCapturedVariables(Optional<LocalScope> capturedVariables)
	{
		this.capturedVariables = capturedVariables;
	}

	@Override
	public String getFriendlyName()
	{		
		return doGetFriendlyName();	
	}

	public String getAggregateDescription()
	{
		if(this.aggregateDescription == null)
			return this.getFriendlyName();
		return aggregateDescription;
	}

	public void setAggregateDescription(String aggregateDescription)
	{
		this.aggregateDescription = aggregateDescription;
	}
	
	@Override
	public boolean isGetOfSelfSupported()
	{
		return getOfSelfSupported;
	}

	@Override
	public void setGetOfSelfSupported(boolean getOfSelfSupported)
	{
		this.getOfSelfSupported = getOfSelfSupported;
	}

	public Optional<String> getPipeSinkType()
	{
		return pipeSinkType;
	}

	public void setPipeSinkType(Optional<String> pipeSinkType)
	{
		this.pipeSinkType = pipeSinkType;
	}

	public Optional<String> getPipeSourceType()
	{
		return pipeSourceType;
	}

	public void setPipeSourceType(Optional<String> pipeSourceType)
	{
		this.pipeSourceType = pipeSourceType;
	}
	
	public boolean isOpenForExtension()
	{
		return this.openForExtension;
	}
	
	public void setOpenForExtension(boolean open)
	{
		this.openForExtension = open;
	}
	
	/**
	 * Is this aggregate itself a generic sort of aggregate.
	 * @return boolean true if is parameterised 
	 */
	public boolean isGenericInNature()
	{
		return !this.getParameterisedTypes().isEmpty();
	}
	
	@Override
	public boolean isGenericTypeParameter()
	{
		return genericTypeParameter;
	}

	public void setGenericTypeParameter(boolean genericTypeParameter)
	{
		this.genericTypeParameter = genericTypeParameter;
	}

	public boolean isMarkedAsDispatcher()
	{
		return markedAsDispatcher;
	}

	public void setMarkedAsDispatcher(boolean markedAsDispatcher)
	{
		this.markedAsDispatcher = markedAsDispatcher;
	}
	
	public List<IAggregateSymbol> getSubAggregateScopedSymbols()
	{
		return subAggregateScopedSymbols;
	}
	
	public void addSubAggregateScopedSymbol(IAggregateSymbol sub)
	{
		if(!subAggregateScopedSymbols.contains(sub))
			subAggregateScopedSymbols.add(sub);
	}

	/**
	 * Gets all abstract methods in this aggregate and any super classes
	 * @return A list of all the methods marked as abstract.
	 */
	@Override
	public List<MethodSymbol> getAllAbstractMethods()
	{
		ArrayList<MethodSymbol> rtn = new ArrayList<MethodSymbol>();
		if(superAggregateScopedSymbol.isPresent())
			rtn.addAll(superAggregateScopedSymbol.get().getAllAbstractMethods());
			
		for(ISymbol sym: getSymbolsForThisScope())
			if(sym.isAMethod())
				if(((MethodSymbol)sym).isMarkedAbstract())
					rtn.add((MethodSymbol)sym);
		return rtn; 
	}
	
	/**
	 * Provides access to the properties on this aggregate - but only this aggregate.
	 * @return The list of properties.
	 */
	@Override
	public List<ISymbol> getProperties()
	{
		ArrayList<ISymbol> rtn = new ArrayList<ISymbol>();
		for(ISymbol sym: getSymbolsForThisScope())
			if(sym.isAVariable())
				rtn.add(sym);
		
		return rtn;
	}
		
	@Override
	public List<MethodSymbol> getConstructors()
	{
		ArrayList<MethodSymbol> rtn = new ArrayList<MethodSymbol>();
		for(ISymbol sym: getSymbolsForThisScope())
			if(sym.isAMethod())
				if(((MethodSymbol)sym).isConstructor())
					rtn.add((MethodSymbol)sym);
		return rtn;
	}

	@Override
	public List<MethodSymbol> getAllNonAbstractMethods()
	{
		ArrayList<MethodSymbol> rtn = new ArrayList<MethodSymbol>();
		if(superAggregateScopedSymbol.isPresent())
			rtn.addAll(superAggregateScopedSymbol.get().getAllNonAbstractMethods());
			
		rtn.addAll(getAllNonAbstractMethodsInThisScopeOnly());
		return rtn; 
	}
	
	public List<ISymbol> getAllPropertyFieldsInThisScopeOnly()
	{
		ArrayList<ISymbol> rtn = new ArrayList<ISymbol>();
		for(ISymbol sym: getSymbolsForThisScope())
			if(!sym.isAMethod()) //might have functions in there hung on as delegates
				rtn.add(sym);
		return rtn;
	}
	
	@Override
	public List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly()
	{
		ArrayList<MethodSymbol> rtn = new ArrayList<MethodSymbol>();
		for(ISymbol sym: getSymbolsForThisScope())
			if(sym.isAMethod())
				if(!((MethodSymbol)sym).isMarkedAbstract())
					rtn.add((MethodSymbol)sym);
		return rtn;
	}
	
	@Override
	public boolean isTraitImplemented(AggregateWithTraitsSymbol thisTraitSymbol)
	{
		if(superAggregateScopedSymbol.isPresent())
			return superAggregateScopedSymbol.get().isTraitImplemented(thisTraitSymbol);
		return false;
	}
	
	@Override
	public List<AggregateWithTraitsSymbol> getAllTraits()
	{
		//This has no traits but its super might.
		List<AggregateWithTraitsSymbol> rtn = new ArrayList<AggregateWithTraitsSymbol>();
		if(getSuperAggregateScopedSymbol().isPresent())
		{
			IAggregateSymbol theSuper = getSuperAggregateScopedSymbol().get();
			List<AggregateWithTraitsSymbol> superTraits = theSuper.getAllTraits();
			superTraits.forEach(trait -> {
				if(!rtn.contains(trait))
					rtn.add(trait);
			});
		}
		
		return rtn;
	}
	
	public IScope getModuleScope()
	{
		return moduleScope;
	}

	public void setModuleScope(Optional<IScope> moduleScope)
	{
		if(moduleScope.isPresent())
			this.moduleScope = moduleScope.get();
	}
	
	public void setModuleScope(IScope moduleScope)
	{
		this.moduleScope = moduleScope;
	}
	
	public boolean isMarkedAbstract()
	{
		return markedAbstract;
	}

	public void setMarkedAbstract(boolean markedAbstract)
	{
		this.markedAbstract = markedAbstract;
	}

	@Override
	public boolean isVirtual()
	{
		return forceVirtual;
	}
	
	@Override
	public boolean isInjectable()
	{
		return injectable;
	}

	public void setInjectable(boolean injectable)
	{
		this.injectable = injectable;
	}

	@Override
	public boolean isExtensionOfInjectable()
	{
		if(injectable)
			return true;
		if(superAggregateScopedSymbol.isPresent())
			return superAggregateScopedSymbol.get().isExtensionOfInjectable();
		return false;
	}
	
	@Override
	public void setVirtual(boolean virtual)
	{
		this.forceVirtual = virtual;
	}
	
	private double checkParameterisedTypesAssignable(List<ISymbol> listA, List<ISymbol> listB)
	{
		if(listA.size() != listB.size())
			return -1.0;
		
		double canAssign = 0.0;
		for(int i=0; i<listA.size(); i++)
		{
			canAssign += listA.get(i).getAssignableWeightTo(listB.get(i));
			if(canAssign < 0.0)
				return canAssign;
		}
		
		return canAssign;
	}
	
	@Override
	public double getAssignableWeightTo(ISymbol s)
	{
		//Here be dragons - looks simple but there are complexities in here with super types and generics
		
		//might not be able to be directly assignable - but not end of the world!
		//We might get a coersion match so weight might not be 0.0.
		double canAssign = super.getAssignableWeightTo(s);
		
		//Check if assignable as super
		if(superAggregateScopedSymbol.isPresent() && canAssign < 0.0)
		{
			//now we can check superclass matches. but add some weight because this did not match
			double weight = superAggregateScopedSymbol.get().getAssignableWeightTo(s);
			canAssign = 0.05 + weight;			
						
			if(canAssign < 0.0)
				return canAssign;			
		}
				
		if(canAssign >= 0.0)
		{
			if(s instanceof AggregateSymbol && canAssign >= 0.0)
			{
				AggregateSymbol toCheck = (AggregateSymbol)s;
				double paramterisedWeight = checkParameterisedTypesAssignable(getParameterisedTypes(), toCheck.getParameterisedTypes()); 
				canAssign += paramterisedWeight;
			}			
		}

		return canAssign;
	}
	
	public double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		double canAssign = super.getUnCoercedAssignableWeightTo(s);
		
		if(superAggregateScopedSymbol.isPresent() && canAssign < 0.0)
		{
			//now we can check superclass matches. but add some weight because this did not match
			double weight = superAggregateScopedSymbol.get().getUnCoercedAssignableWeightTo(s);
			canAssign = 0.05 + weight;			
						
			if(canAssign < 0.0)
				return canAssign;			
		}
				
		if(canAssign >= 0.0)
		{
			if(s instanceof AggregateSymbol && canAssign >= 0.0)
			{
				AggregateSymbol toCheck = (AggregateSymbol)s;
				double paramterisedWeight = checkParameterisedTypesAssignable(getParameterisedTypes(), toCheck.getParameterisedTypes()); 
				canAssign += paramterisedWeight;
			}			
		}

		return canAssign;
	}
	
	public AggregateSymbol addParameterisedType(AggregateSymbol parameterisedType)
	{
		super.addParameterisedType(parameterisedType);
		super.setCategory(SymbolCategory.TEMPLATE_TYPE);
		return this;
	}
	
	public void setSuperAggregateScopedSymbol(Optional<IAggregateSymbol> superAggregateScopedSymbol)
	{
		AssertValue.checkNotNull("Optional superAggregateScopedSymbol cannot be null", superAggregateScopedSymbol);
		this.superAggregateScopedSymbol = superAggregateScopedSymbol;
		if(superAggregateScopedSymbol.isPresent())
			superAggregateScopedSymbol.get().addSubAggregateScopedSymbol(this);
	}

	public void setSuperAggregateScopedSymbol(IAggregateSymbol baseSymbol)
	{
		setSuperAggregateScopedSymbol(Optional.ofNullable(baseSymbol));
	}
	
	public Optional<IAggregateSymbol> getSuperAggregateScopedSymbol()
	{
		return superAggregateScopedSymbol;	
	}

	public boolean hasImmediateSuper(IAggregateSymbol theSuper)
	{
		if(superAggregateScopedSymbol.isPresent())
			return superAggregateScopedSymbol.get().isExactSameType(theSuper);
		return false;
	}
	
	@Override
	public ISymbol setType(Optional<ISymbol> type)
	{
		//Used when cloning so type can be set if not already populated.
		if(this.isGenericTypeParameter())
			this.setSuperAggregateScopedSymbol((IAggregateSymbol)type.get());
		else if(super.getType().isPresent())
			throw new RuntimeException("You cannot set the type of an aggregate Symbol");
			
		return super.setType(type);
	}

	@Override
	public Optional<ISymbol> getType()
	{
		//This is also a type
		return Optional.of(this);
	}
	
	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		//System.out.println("Search '" + this.getName() + "' AggregateSymbol: " + search);
		
		MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);
		//Do supers first then do own
		if(superAggregateScopedSymbol.isPresent())
		{
			//So if we have a super then this will bee a peer of anything we already have passed in could be traits/interfaces
			buildResult = buildResult.mergePeerToNewResult(superAggregateScopedSymbol.get().resolveForAllMatchingMethods(search, new MethodSymbolSearchResult()));
			//System.out.println(this.getName() + ": AggregateSymbol super: " + buildResult);
		}
		
		MethodSymbolSearchResult res = resolveForAllMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());
		//System.out.println("Res '" + this.getName() + "' AggregateSymbol: " + search + ": " + res);
		buildResult = buildResult.overrideToNewResult(res);		
		
		//System.out.println("End Search '" + this.getName() + "' AggregateSymbol: " + search + ": " + buildResult);
		return buildResult;
	}
	
    public Optional<ISymbol> resolveMember(SymbolSearch search)
    {
        //For members we only look to our aggregate and super (unless we introduce traits later)
        Optional<ISymbol> rtn = resolveInThisScopeOnly(search);
        if(!rtn.isPresent() && superAggregateScopedSymbol.isPresent())
            rtn = superAggregateScopedSymbol.get().resolveMember(search);
        return rtn;
    }

	@Override
	public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search)
	{
		Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
		//But note we limit the search in the captured vars to that scope only - no looking up the enclosing scopes just the capture scope!
		if(!rtn.isPresent() && capturedVariables.isPresent())
			rtn = capturedVariables.get().resolveInThisScopeOnly(search);

		return rtn;
	}

	/**
	 * Resolve using super aggregate.
	 */
	protected Optional<ISymbol> resolveWithParentScope(SymbolSearch search)
	{
		Optional<ISymbol> rtn = Optional.ofNullable(null);
	
		//So we keep going back up the class hierarchy until no more supers then
		//When we get to the final aggregate we use the scopedSymbol local and back up to global symbol table.
		if(superAggregateScopedSymbol.isPresent())
			rtn = superAggregateScopedSymbol.get().resolve(search);
		
		if(!rtn.isPresent())
			rtn = super.resolve(search);
		
		return rtn;
	}
	
	/**
	 * So this is where we alter the mechanism of normal symbol resolution.
	 */
	@Override
	public Optional<ISymbol> resolve(SymbolSearch search)
	{
		Optional<ISymbol> rtn = Optional.ofNullable(null);
		//Now if this is a generic type class we might need to resolve the name of the type 'T' or 'S' or whatever for example		
		if(isGenericInNature() && search.getSearchType().equals(SymbolCategory.TYPE))
		{
			for(ISymbol parameterisedType : getParameterisedTypes())
			{
				if(parameterisedType.isAssignableTo(search.getNameAsSymbol()))
					rtn = Optional.of(parameterisedType);
			}
		}
		
		//Now see if it is defined in this aggregate.
		if(!rtn.isPresent())
			rtn = resolveInThisScopeOnly(search);
		//if not then resolve with parent scope which could be super or back up scope tree
		if(!rtn.isPresent())
			rtn = resolveWithParentScope(search);
		return rtn;
	}

	private String doGetFriendlyName()
	{
		StringBuffer buffer = new StringBuffer(super.getFriendlyName());
		buffer.append(getAnyGenericParamsAsFriendlyNames());
		return buffer.toString();
	}
	
	protected String getAnyGenericParamsAsFriendlyNames()
	{
		StringBuffer buffer = new StringBuffer();
		if(isGenericInNature())
		{
			buffer.append(" of ");
			boolean first = true;
			for(ISymbol symbol : this.getParameterisedTypes())
			{
				if(!first)
					buffer.append(", ");
				first = false;
				buffer.append(symbol.getFriendlyName());
			}
		}
		return buffer.toString();
	}
}
