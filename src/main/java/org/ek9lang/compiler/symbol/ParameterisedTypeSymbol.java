package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.Digest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Given a parameterised symbol and the symbol used to use as the parameter we are in effect saying:
 * Given List<T>
 * Given SomeObjectType
 * We are saying List<SomeObjectType>
 * This class is probably one of biggest 'mind fucks' you'll ever have.
 * generics of generics with parameters.
 *
 * TODO maybe refactor this and the ParameterisedFunctionSymbol to pull out common stuff.
 */
public class ParameterisedTypeSymbol extends AggregateSymbol implements ParameterisedSymbol
{	
	//This is the class that can be parameterised
	//Now you need to check if this symbol type was reverse engineered as you will need to treat it in a different manner maybe.
	private AggregateSymbol parameterisableSymbol;
	
	//This is what it is parameterised with.
	private List<ISymbol> parameterSymbols = new ArrayList<ISymbol>();
	
	private boolean variablesAndMethodsHydrated = false;
	
	/**
	 * So this is where we make a real internal name as a real type.
	 * @return the internal name to be used by combining the parameterisableSymbol with the parameters provided.
	 */
	public static String getInternalNameFor(IAggregateSymbol parameterisableSymbol, List<ISymbol> parameterSymbols)
	{
		return getEK9InternalNameFor(parameterisableSymbol, parameterSymbols);	
	}
	
	private static String getEK9InternalNameFor(IAggregateSymbol parameterisableSymbol, List<ISymbol> parameterSymbols)
	{
		StringBuffer rtn = new StringBuffer("_");
		rtn.append(parameterisableSymbol.getName());
		rtn.append("_");
		//Also include but as fully qualified in the hash to ensure unique across modules with same type name
		//i.e net.customer.List and some.other.List - will both prefix with _List but hash will differ.
		StringBuffer buffer = new StringBuffer(parameterisableSymbol.getFullyQualifiedName());
		for(ISymbol symbol : parameterSymbols)
		{
			buffer.append("_");
			buffer.append(symbol.getFullyQualifiedName());
		}
		rtn.append(Digest.digest(buffer.toString()).toString());
		
		return rtn.toString();
	}
	
	public ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol, List<ISymbol> parameterSymbols, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		parameterSymbols.forEach(parameterSymbol -> addParameterSymbol(parameterSymbol));
		parameterisationComplete();
	}
	
	public ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol, Optional<ISymbol> parameterSymbol, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		addParameterSymbol(parameterSymbol);
		parameterisationComplete();
	}
	
	public ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol, Optional<ISymbol> parameterSymbol1, Optional<ISymbol> parameterSymbol2, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		addParameterSymbol(parameterSymbol1);
		addParameterSymbol(parameterSymbol2);
		parameterisationComplete();
	}
	
	private ParameterisedTypeSymbol(AggregateSymbol parameterisableSymbol, IScope enclosingScope)
	{		
		super("", enclosingScope); //Gets set below
		AssertValue.checkNotNull("parameterisableSymbol cannot be null", parameterisableSymbol);
		
		if(!parameterisableSymbol.isGenericInNature() || !parameterisableSymbol.isATemplateType())
			throw new IllegalArgumentException("parameterisableSymbol must be parameterised");
		
		this.parameterisableSymbol = parameterisableSymbol;
		//Put is in the same module as where the generic type has been defined.
		//Note that this could and probably will be a system on in many cases list List and Optional.
		this.setModuleScope(parameterisableSymbol.getModuleScope());
		super.setCategory(SymbolCategory.TYPE);
		//take note if source from reverse engineering.
		setReversedEngineeredToEK9(parameterisableSymbol.isReversedEngineeredToEK9());
		
		setProduceFullyQualifiedName(parameterisableSymbol.getProduceFullyQualifiedName());
		setEk9Core(parameterisableSymbol.isEk9Core());
	}	
	
	@Override
	public boolean isOpenForExtension()
	{
		//Whatever the parameterisable symbol was defined to be.
		return parameterisableSymbol.isOpenForExtension();
	}

	public AggregateSymbol getParameterisableSymbol()
	{
		return parameterisableSymbol;
	}

	public List<ISymbol> getParameterSymbols()
	{
		return parameterSymbols;
	}

	public boolean isVariablesAndMethodsHydrated()
	{
		return variablesAndMethodsHydrated;
	}

	@Override
	public boolean isAParameterisedType()
	{
		return true;
	}
	
	@Override
	public boolean isGenericInNature()
	{
		//These can also be generic definitions when initially parsed.
		//But when given a set of valid parameter are no longer generic aggregates.
		for(ISymbol param :parameterSymbols)
		{
			if(param.isGenericTypeParameter())
				return true;
		}
		return false;
	}
	
	@Override
	public boolean isSymbolTypeMatch(ISymbol symbolType)
	{
		//So was this type created by the type we are checking
		if(parameterisableSymbol.isExactSameType(symbolType))
		{
			//So this might also be considered equivalent
			//But lets check that the parameterised types are a match
			return doSymbolsMatch(parameterSymbols, parameterisableSymbol.getParameterisedTypes());
		}
		return super.isSymbolTypeMatch(symbolType);
	}

	private boolean doSymbolsMatch(List<ISymbol> list1, List<ISymbol> list2)
	{
		if(list1.size() == list2.size())
		{
			for(int i=0; i<list1.size(); i++)
				if(!list1.get(i).isExactSameType(list2.get(i)))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Some parameterised types have been parameterised with S and T. This is done for use
	 * within the symbol tables and when defining other generic types.
	 * But these cannot actually be generated. So we do need to be able to distinguish between
	 * those that can be really used and those that are just used with other generic classes.
	 * @return true if just conceptual and cannot be generated, false if can actually be made manifest and used.
	 */
	public boolean isConceptualParameterisedType()
	{
		for(ISymbol param : parameterSymbols)
		{
			if(param.isGenericTypeParameter())
				return true;
		}
		return false;
	}
	public ParameterisedTypeSymbol addParameterSymbol(Optional<ISymbol> parameterSymbol)
	{
		AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);		
		addParameterSymbol(parameterSymbol.get());
		return this;
	}
	
	public ParameterisedTypeSymbol addParameterSymbol(ISymbol parameterSymbol)
	{
		AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);
		
		parameterSymbols.add(parameterSymbol);
		
		return this;
	}
	
	/**
	 * So once you have created this object and added all the parameters you want to
	 * Call this so that the full name of the concrete parameterised generic type is manifest.
	 */
	private void parameterisationComplete()
	{
		super.setName(getInternalNameFor(parameterisableSymbol, parameterSymbols));
	}
	
	/**
	 * Need to go through this parameterised type and workout and return if any new paramterised types are required.
	 * So for example Optional<OO> might use a Iterator<II> so if we create a new Optional<V> it means we also need an Iterator<V>
	 * @param global 
	 * @return The list of parameterised types this parameterised type depends on
	 */
	public void establishDependentParameterisedTypes(IScope global)
	{
		for(ISymbol symbol : parameterisableSymbol.getSymbolsForThisScope())
		{
			//So if method or variable get it's return type and resolve with new
			//lets see if it needs creating or if it already exists.
			
			ResolutionResult result = resolveWithNewType(symbol.getType());
			if(!result.wasResolved)
			{
				//System.out.println("Return Type [" + result.symbol.get() + "] did not exist needs creating");
				global.define(result.symbol.get());
				if(result.symbol.get() instanceof ParameterisedTypeSymbol)
					((ParameterisedTypeSymbol)result.symbol.get()).establishDependentParameterisedTypes(global);
			}
			if(symbol.isAMethod())
			{
				MethodSymbol method = (MethodSymbol)symbol;
				method.getSymbolsForThisScope().forEach(param -> {
					ResolutionResult paramResult = resolveWithNewType(param.getType());
					if(!paramResult.wasResolved)
					{
						ISymbol s = paramResult.symbol.get();
						//System.out.println("Param Type [" + s.getFriendlyName() + "] did not exist needs creating");
						global.define(paramResult.symbol.get());
						if(paramResult.symbol.get() instanceof ParameterisedTypeSymbol)
							((ParameterisedTypeSymbol)paramResult.symbol.get()).establishDependentParameterisedTypes(global);
					}
				});
			}
		}
	}
	/**
	 * After phase one all the basic types and method signatures are set up for the types we can use for templates.
	 * But now we need to create the parameterised types an setup the specific signatures with the actual types.
	 */
	public void initialSetupVariablesAndMethods()
	{
		
		//Collect up all these defs in a single symbol table not per module - but more like
		//a global symbol table just for templates that have been turned into types by having
		//parameters provided.
		//then you can get back to cloning the properties and methods from the parameterisableSymbol
		//and replacing the T's and S's etc with these actual parameterSymbols types.
		//Then it's just like you've written the same code with different parameter types.
		
		//Then you can go to the resolve phase and you'll have concrete types in place.
		//Just remember in the symbols we don't really deal with bodies as such - that is the IR phase
		//Symbol phase we are really checking all these methods, classes and properties exist and are of the right type.
		//In the expression phase just checking all those add up ok.
		//But only in the IR phase do we really pull together the contents of the methods into Nodes.
		//So that's where we will also need to pull together the template type, concrete parameters to create a full correct concrete IR
		
		//First off lets go through symbols and create copies but with the applied concrete types.
		
		for(ISymbol symbol : parameterisableSymbol.getSymbolsForThisScope())
		{
			//Going in
			//System.out.println("G: {" + this.getFriendlyName() + "}: [" + symbol.getFriendlyName() + "]");
			ISymbol replacementSymbol = cloneSymbolWithNewType(symbol);
			//Coming out
			//System.out.println("C: {" + this.getFriendlyName() + "}: [" + replacementSymbol.getFriendlyName() + "]");
			this.define(replacementSymbol);
		}
		
		//We have now hydrated variables and methods they should now be resolvable.
		variablesAndMethodsHydrated = true;
	}

	/**
	 * Clones any symbols that employ generic parameters. For non generic uses the same symbol.
	 * @param toClone The symbol to Clone.
	 * @return A new symbol or symbol passed in if no generic parameter replacement is required.
	 */
	private ISymbol cloneSymbolWithNewType(ISymbol toClone)
	{
		//TODO refcator - don't clone the code!
		if(toClone instanceof VariableSymbol)
		{
			VariableSymbol willClone = (VariableSymbol)toClone;
			Optional<ISymbol> fromType = willClone.getType();
			Optional<ISymbol> newType = resolveWithNewType(fromType).symbol;
			//System.out.println("variable type from [" + fromType.get() + "] to [" + newType.get() + "]"); 
			VariableSymbol rtn = willClone.clone(null);
			rtn.setType(newType);			
			//So mimic the location of the source
			rtn.setSourceToken(toClone.getSourceToken());
			return rtn;
		}
		else if(toClone instanceof MethodSymbol)
		{
			//Need to review all the method cloning as there is duplication in the MethodSymbol itself.
			MethodSymbol willClone = (MethodSymbol)toClone;
			Optional<ISymbol> fromType = willClone.getType();
			Optional<ISymbol> newType = resolveWithNewType(fromType).symbol;
			String useMethodName = willClone.getName();
			
			//Shit man what about the constructor methods!
			//Don't we need to change their names to match the new class name
			//This is not Java - this is ek9 - the constructor method for aggregate Optional
			//is still marked as a constructor but keeps its old name! That way we can still resolve it in ek9
			//Only at generation time to Java does that get changed.
			//But see how templateValidator has to deal with this.
			
			MethodSymbol rtn = new MethodSymbol(useMethodName, this);
			//System.out.println("Method cloning [" + willClone + "] [" + willClone.getName() + "]  constructor [" + willClone.isConstructor() + "]");

			//System.out.println("method type from [" + fromType.get() + "] to [" + newType.get() + "]"); 
			
			rtn.setType(newType);
			rtn.setOverride(willClone.isOverride());
			rtn.setAccessModifier(willClone.getAccessModifier());
			rtn.setConstructor(willClone.isConstructor());
			rtn.setOperator(willClone.isOperator());
			rtn.setMarkedPure(willClone.isMarkedPure());
			rtn.setUsedAsProxyForDelegate(willClone.getUsedAsProxyForDelegate());
			rtn.setParameterisedWrappingRequired(willClone.isParameterisedWrappingRequired());
			rtn.setEk9ReturnsThis(willClone.isEk9ReturnsThis());
			for(ISymbol symbol : willClone.getSymbolsForThisScope())
			{
				ISymbol clonedParam = cloneSymbolWithNewType(symbol);
				//System.out.println("Cloning of method [" + willClone.getName() + "] [" + symbol + "] - [" + clonedParam + "]");
				rtn.define(clonedParam);
			}
			
			//what about rtn and type
			ISymbol rtnSymbol = willClone.getReturningSymbol();
			if(rtnSymbol != null)
			{
				ISymbol replacementSymbol = cloneSymbolWithNewType(rtnSymbol);
				rtn.setReturningSymbol(replacementSymbol);
			}
			//System.out.println("Method cloned [" + rtn + "] [" + rtn.getName() + "] constructor [" + rtn.isConstructor() + "]");

			return rtn;
		}
		throw new RuntimeException("We can only clone variables and methods in generic type templates not [" + toClone + "]");
	}
	
	/**
	 * Resolve the type could be concrete or could be As S or T type generic.
	 * @param typeToResolve The type to resolve.
	 * @return The new resolved symbol.
	 */
	private ResolutionResult resolveWithNewType(Optional<ISymbol> typeToResolve)
	{
		//System.out.println("resolveWithNewType [" + typeToResolve.get() + "]");
		if(typeToResolve.isPresent() && typeToResolve.get() instanceof IAggregateSymbol)
		{
			AggregateSymbol aggregate = (AggregateSymbol)typeToResolve.get();
			if(aggregate.isATemplateType())
			{
				//So the aggregate here is the generic template definition.
				//We must ensure we marry up the generic parameters here - for example we might have 2 params S and T
				//But the aggregate we are lookup might just have one the 'T'. So we need to deal with this.
				List<ISymbol> lookupParameterSymbols = new ArrayList<ISymbol>();
				for(ISymbol symbol : aggregate.getParameterisedTypes())
				{
					//So given a T or whatever we need to find which index position it is in
					//Then we can use that same index position to get the equiv symbol from what parameters have been applied.
					int index = getIndexOfType(Optional.of(symbol));
					if(index < 0)
					{
						//Just for debug.
						getIndexOfType(Optional.of(symbol));
						throw new RuntimeException("Unable to find symbol [" + symbol + "] in [" + aggregate + "] in [" + this + "]");
					}
					ISymbol resolvedSymbol = parameterSymbols.get(index);
					lookupParameterSymbols.add(resolvedSymbol);
				}
				//So now we have a generic type and a set of actual parameters we can resolve it!
				String parameterisedTypeSymbolName = getInternalNameFor(aggregate, lookupParameterSymbols);
				TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
				ResolutionResult rtn = new ResolutionResult();
				rtn.symbol = this.resolve(search);				
				rtn.wasResolved = rtn.symbol.isPresent(); 
				//So if not resolved then make one.
				if(!rtn.wasResolved)
					rtn.symbol = Optional.of(new ParameterisedTypeSymbol(aggregate, lookupParameterSymbols, this.getEnclosingScope()));
				//System.out.println("Looking for [" + parameterisedTypeSymbolName + "] resolved is [" + resolvedSymbol.isPresent() + "]");
				
				return rtn;
			}
			else if(aggregate.isGenericTypeParameter())
			{
				//So if this is a T or and S or a P for example we need to know
				//What actual symbol to use from the parameterSymbols we have been parameterised with.
				int index = getIndexOfType(typeToResolve);
				ISymbol resolvedSymbol = parameterSymbols.get(index);
				ResolutionResult rtn = new ResolutionResult();
				rtn.symbol = Optional.of(resolvedSymbol);
				rtn.wasResolved = rtn.symbol.isPresent(); 
				return rtn;
			}
			else if(aggregate.isGenericInNature() && aggregate instanceof ParameterisedTypeSymbol)
			{
				//Now this is a  bastard because we need to clone one of these classes we're already in.
				//But it might be a List of T or a real List of Integer for example.
				
				//So the thing we are to clone it itself a generic aggregate.
				//System.out.println("Need to Clone a generic aggregate [" + aggregate + "]");
				ParameterisedTypeSymbol asParameterisedTypeSymbol = (ParameterisedTypeSymbol)aggregate;
				List<ISymbol> lookupParameterSymbols = new ArrayList<ISymbol>();
				//Now it may really be a concrete one or it too could be something like a List of P
				
				for(ISymbol symbol : asParameterisedTypeSymbol.parameterSymbols)
				{
					//So lets see might be a K or V for example but we must be able to find that type in this object	
					if(symbol.isGenericTypeParameter())
					{
						int index = getIndexOfType(Optional.of(symbol));
						if(index < 0)
						{
							throw new RuntimeException("Unable to find symbol [" + symbol + "]");
						}
						ISymbol resolvedSymbol = parameterSymbols.get(index);
						lookupParameterSymbols.add(resolvedSymbol);
					}
					else
					{
						//Ah no it is a concrete type so just use that.
						lookupParameterSymbols.add(symbol);
					}
				}
				
				String parameterisedTypeSymbolName = getInternalNameFor(asParameterisedTypeSymbol.parameterisableSymbol, lookupParameterSymbols);
				TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
				ResolutionResult rtn = new ResolutionResult();
				rtn.symbol = this.resolve(search);				
				rtn.wasResolved = rtn.symbol.isPresent(); 
				//So if not resolved then make one.
				if(!rtn.wasResolved)
					rtn.symbol = Optional.of(new ParameterisedTypeSymbol(asParameterisedTypeSymbol.parameterisableSymbol, lookupParameterSymbols, this.getEnclosingScope()));
				return rtn;
			}
		}
		ResolutionResult rtn = new ResolutionResult();
		rtn.symbol = typeToResolve;
		rtn.wasResolved = true;
		return rtn;
	}
	
	/**
	 * For the type passed in - a T or and S whatever we need to know it's index.
	 * From this we can look at what this has been parameterised with and use that type.
	 * @param theType The generic definition parameter i.e S, or T
	 * @return The index or -1 if not found.
	 */
	private int getIndexOfType(Optional<ISymbol> theType)
	{
		if(theType.isPresent())
		{
			for(int i=0; i < parameterisableSymbol.getParameterisedTypes().size(); i++)
			{
				ISymbol pType = parameterisableSymbol.getParameterisedTypes().get(i);
				if(pType.isExactSameType(theType.get()))
					return i;
			}
		}
		return -1;
	}
	
	@Override
	public Optional<ISymbol> getType()
	{
		//This is also a type
		return Optional.of(this);
	}
	
	@Override
	public String getFriendlyName()
	{
		StringBuffer buffer = new StringBuffer(parameterisableSymbol.getName());
		buffer.append(" of ");
		boolean first = true;
		
		for(ISymbol symbol : parameterSymbols)
		{
			if(!first)
				buffer.append(", ");
			first = false;
			buffer.append(symbol);
		}
		return buffer.toString();
	}


	@Override
	public ISymbol setType(Optional<ISymbol> type)
	{
		throw new RuntimeException("Cannot alter ParameterisedTypeSymbol types", new UnsupportedOperationException());
	}	
	
	/**
	 * So now here when it comes to being assignable the generic parameterisable type has to be the same.
	 * And the parameters it has been parameterised with also have to be the same and match.
	 *
	 * Only then do we consider it to be assignable via a weight.
	 * 
	 * This might be a bit over simplified, but at least it is simple and straightforward.
	 * We are not currently considering extending or inheritance with generic templates types - they are already complex enough.
	 */
	@Override
	public double getAssignableWeightTo(ISymbol s)
	{
		return getUnCoercedAssignableWeightTo(s);
	}
	
	public double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		//Now because we've hashed the class and parameter signature we can do a very quick check here.
		//Plus we dont allow any types of coersion or super class matching.
		if(this.getName().equals(s.getName()))
			return 0.0;

		return -1000.0;
	}
	
	@Override
	public ScopeType getScopeType()
	{
		return ScopeType.AGGREGATE;
	}

	private static class ResolutionResult
	{		
		/**
		 * The Type that would be needed.
		 * Not is may already exist and was resolved OK
		 * See was Resolved below.
		 */
		Optional<ISymbol> symbol = Optional.ofNullable(null);
		
		/**
		 * So the result may well be set, but in the case of PAramterisedTypeSymbols
		 * it maybe that this is what we need - but it has not yet been created. 
		 */
		boolean wasResolved = false;
	}
}
