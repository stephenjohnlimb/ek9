package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.AggregateSupport;
import org.ek9lang.compiler.symbol.support.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.Digest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Given a parameterised symbol and the symbol used to use as the parameter we are in effect saying:
 * Given myFunction<T>
 * Given SomeObjectType
 * We are saying myFunction<SomeObjectType>
 * 
 * TODO maybe refactor this and the ParameterisedTypeSymbol to pull out common stuff.
 */
public class ParameterisedFunctionSymbol extends FunctionSymbol implements ParameterisedSymbol, SymbolType
{
	private AggregateSupport aggregateSupport = new AggregateSupport();
	
	//This is the function that can be parameterised
	private FunctionSymbol parameterisableSymbol;
	
	//This is what it is parameterised with - so this could be common with ParameterisedTypeSymbol.
	private List<ISymbol> parameterSymbols = new ArrayList<ISymbol>();
	
	private boolean variablesAndMethodsHydrated = false;
	
	/**
	 * So this is where we make a real internal name as a real type.
	 * @return the internal name to be used by combining the parameterisableSymbol with the parameters provided.
	 */
	public static String getInternalNameFor(FunctionSymbol parameterisableSymbol, List<ISymbol> parameterSymbols)
	{
		StringBuffer rtn = new StringBuffer("_");
		rtn.append(parameterisableSymbol.getName());
		rtn.append("_");
		//Also include but as fully qualified in the hash to ensure unique across modules with same type name
		//i.e net.customer.myFunction and some.other.myFunction - will both prefix with _myFunction but hash will differ.
		StringBuffer buffer = new StringBuffer(parameterisableSymbol.getFullyQualifiedName());
		for(ISymbol symbol : parameterSymbols)
		{
			buffer.append("_");
			buffer.append(symbol.getFullyQualifiedName());
		}
		rtn.append(Digest.digest(buffer.toString()).toString());
		
		return rtn.toString();
	}
	
	public ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, List<ISymbol> parameterSymbols, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		parameterSymbols.forEach(parameterSymbol -> addParameterSymbol(parameterSymbol));
		parameterisationComplete();
	}
	
	public ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, Optional<ISymbol> parameterSymbol, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		addParameterSymbol(parameterSymbol);
		parameterisationComplete();
	}
	
	public ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, Optional<ISymbol> parameterSymbol1, Optional<ISymbol> parameterSymbol2, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		addParameterSymbol(parameterSymbol1);
		addParameterSymbol(parameterSymbol2);
		parameterisationComplete();
	}
	
	private ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, IScope enclosingScope)
	{		
		super("", enclosingScope); //Gets set below
		AssertValue.checkNotNull("parameterisableSymbol cannot be null", parameterisableSymbol);
		
		if(!parameterisableSymbol.isGenericInNature() || !parameterisableSymbol.isATemplateFunction())
			throw new IllegalArgumentException("parameterisableSymbol must be parameterised");
		
		this.parameterisableSymbol = parameterisableSymbol;
		
		this.setModuleScope(parameterisableSymbol.getModuleScope());
		super.setCategory(SymbolCategory.FUNCTION);
		super.setGenus(SymbolGenus.FUNCTION);
	}
	
	public FunctionSymbol getParameterisableSymbol()
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
	
	public ParameterisedFunctionSymbol addParameterSymbol(Optional<ISymbol> parameterSymbol)
	{
		AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);		
		addParameterSymbol(parameterSymbol.get());
		return this;
	}
	
	public ParameterisedFunctionSymbol addParameterSymbol(ISymbol parameterSymbol)
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
	 * After phase one all the basic types and method signatures are set up for the types we can use for templates.
	 * But now we need to create the parameterised types an setup the specific signatures with the actual types.
	 */
	public void initialSetupVariablesAndMethods()
	{	
		//System.out.println("Hydration of parameterised function");
		for(ISymbol symbol : parameterisableSymbol.getSymbolsForThisScope())
		{
			//System.out.println("Funct G: " + this.getFriendlyName() + ": [" + symbol.getFriendlyName() + "]");
			ISymbol replacementSymbol = cloneSymbolWithNewType(symbol);
			//System.out.println("Funct C: " + this.getFriendlyName() + ": [" + replacementSymbol.getFriendlyName() + "]");
			this.define(replacementSymbol);
		}
		//what about rtn and type
		ISymbol rtnSymbol = parameterisableSymbol.getReturningSymbol();
		if(rtnSymbol != null)
		{
			ISymbol replacementSymbol = cloneSymbolWithNewType(rtnSymbol);
			//System.out.println("Cloning function rtn [" + rtnSymbol + "] to [" + replacementSymbol + "]");
			this.setReturningSymbol(replacementSymbol);
		}
		//We do not make a note of the returning context as we need to define the returning type after replacement of the type.
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
		//TODO refactor - don't clone the code!
		if(toClone instanceof VariableSymbol)
		{
			VariableSymbol willClone = (VariableSymbol)toClone;
			Optional<ISymbol> fromType = willClone.getType();
			Optional<ISymbol> newType = resolveWithNewType(fromType);
			//System.out.println("variable type from [" + fromType.get() + "] to [" + newType.get() + "]");
			VariableSymbol rtn = willClone.clone(null);
			rtn.setType(newType);
			//So mimic the location of the source
			rtn.setSourceToken(toClone.getSourceToken());
			return rtn;
		}
		throw new RuntimeException("We can only clone variables in generic function templates not [" + toClone + "]");
	}
	
	/**
	 * Resolve the type could be concrete or could be As S or T type generic.
	 * @param typeToResolve The type to resolve.
	 * @return The new resolved symbol.
	 */
	private Optional<ISymbol> resolveWithNewType(Optional<ISymbol> typeToResolve)
	{
		if(typeToResolve.isPresent() && typeToResolve.get() instanceof IAggregateSymbol)
		{
			IAggregateSymbol aggregate = (IAggregateSymbol)typeToResolve.get();
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
						throw new RuntimeException("Unable to find symbol [" + symbol + "]");
					}
					ISymbol resolvedSymbol = parameterSymbols.get(index);
					lookupParameterSymbols.add(resolvedSymbol);
				}
				//So now we have a generic type and a set of actual parameters we can resolve it!
				String parameterisedTypeSymbolName = ParameterisedTypeSymbol.getInternalNameFor(aggregate, lookupParameterSymbols);
				TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
				return this.resolve(search);
			}
			else if(aggregate.isGenericTypeParameter())
			{
				//So if this is a T or and S or a P for example we need to know
				//What actual symbol to use from the parameterSymbols we have been parameterised with.
				int index = getIndexOfType(typeToResolve);
				ISymbol resolvedSymbol = parameterSymbols.get(index);
				
				return Optional.of(resolvedSymbol);
			}
			else if(aggregate.isGenericInNature() && aggregate instanceof ParameterisedFunctionSymbol)
			{
				//Now this is a  bastard because we need to clone one of these classes we're already in.
				//But it might be a List of T or a real List of Integer for example.
				
				//So the thing we are to clone it itself a generic aggregate.
				//System.out.println("Need to Clone a generic aggregate [" + aggregate + "]");
				ParameterisedFunctionSymbol asParameterisedTypeSymbol = (ParameterisedFunctionSymbol)aggregate;
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
				Optional<ISymbol> resolvedSymbol = this.resolve(search);
				return resolvedSymbol;
			}
				
		}
		return typeToResolve;
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
				if(parameterisableSymbol.getParameterisedTypes().get(i).isExactSameType(theType.get()))
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
		String nameOfFunction = parameterisableSymbol.getName() + " of " + parameterSymbolsAsCommaSeparated();
		StringBuffer buffer = new StringBuffer();
		if(getType().isPresent())
			buffer.append(nameOfFunction);
		else
			buffer.append("Unknown");
		
		buffer.append(" <- ").append(nameOfFunction);
		buffer.append("(");
		boolean first = true;
		for(ISymbol symbol : parameterSymbols)
		{
			if(!first)
				buffer.append(", ");
			buffer.append(symbol);
			first = false;
		}
		buffer.append(")");
		
		return buffer.toString();
	}
	
	private String parameterSymbolsAsCommaSeparated()
	{
		StringBuffer buffer = new StringBuffer();
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
		throw new RuntimeException("Cannot alter ParameterisedFunctionSymbol types", new UnsupportedOperationException());
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
}
