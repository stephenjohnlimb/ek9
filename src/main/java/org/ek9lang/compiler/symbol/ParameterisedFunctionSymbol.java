package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Given a parameterised symbol and the symbol used to use as the parameter we are in effect saying:
 * Given myFunction<T>
 * Given SomeObjectType
 * We are saying myFunction<SomeObjectType>
 * This class is probably one of biggest 'mind fucks' you'll ever have.
 * generics of generics with parameters.
 * <p>
 * TODO maybe refactor this and the ParameterisedTypeSymbol to pull out common stuff.
 */
public class ParameterisedFunctionSymbol extends FunctionSymbol implements ParameterisedSymbol
{

	//This is the function that can be parameterised
	private final FunctionSymbol parameterisableSymbol;

	//This is what it is parameterised with - so this could be common with ParameterisedTypeSymbol.
	private final List<ISymbol> parameterSymbols = new ArrayList<>();

	private boolean variablesAndMethodsHydrated = false;

	public ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, Optional<ISymbol> parameterSymbol, IScope enclosingScope)
	{
		this(parameterisableSymbol, parameterSymbol.stream().collect(Collectors.toList()), enclosingScope);
	}

	public ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, List<ISymbol> parameterSymbols, IScope enclosingScope)
	{
		this(parameterisableSymbol, enclosingScope);
		parameterSymbols.forEach(this::addParameterSymbol);
		this.setReturningSymbol(parameterisableSymbol.getReturningSymbol());
		parameterisationComplete();
	}

	private ParameterisedFunctionSymbol(FunctionSymbol parameterisableSymbol, IScope enclosingScope)
	{
		super("", enclosingScope); //Gets set below
		AssertValue.checkNotNull("parameterisableSymbol cannot be null", parameterisableSymbol);

		if(!parameterisableSymbol.isGenericInNature() || !parameterisableSymbol.isATemplateFunction())
			throw new IllegalArgumentException("parameterisableSymbol must be parameterised");

		this.parameterisableSymbol = parameterisableSymbol;
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
	public boolean isSymbolTypeMatch(ISymbol symbolType)
	{
		//So was this type created by the type we are checking
		if(parameterisableSymbol.isExactSameType(symbolType))
		{
			//So this might also be considered equivalent
			//But lets check that the parameterised types are a match
			return CommonParameterisedTypeDetails.doSymbolsMatch(parameterSymbols, parameterisableSymbol.getParameterisedTypes());
		}
		return super.isSymbolTypeMatch(symbolType);
	}

	/**
	 * So once you have created this object and added all the parameters you want to
	 * Call this so that the full name of the concrete parameterised generic type is manifest.
	 */
	private void parameterisationComplete()
	{
		super.setName(CommonParameterisedTypeDetails.getInternalNameFor(parameterisableSymbol, parameterSymbols));
		this.setModuleScope(parameterisableSymbol.getModuleScope());
		if(this.isGenericInNature())
			super.setCategory(SymbolCategory.TEMPLATE_FUNCTION);
		else
			super.setCategory(SymbolCategory.FUNCTION);
		super.setGenus(SymbolGenus.FUNCTION);
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
	 *
	 * @param toClone The symbol to Clone.
	 * @return A new symbol or symbol passed in if no generic parameter replacement is required.
	 */
	private ISymbol cloneSymbolWithNewType(ISymbol toClone)
	{
		//TODO refactor - don't clone the code!
		if(toClone instanceof VariableSymbol willClone)
		{
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
	 *
	 * @param typeToResolve The type to resolve.
	 * @return The new resolved symbol.
	 */
	private Optional<ISymbol> resolveWithNewType(Optional<ISymbol> typeToResolve)
	{
		if(typeToResolve.isPresent() && typeToResolve.get() instanceof IAggregateSymbol aggregate)
		{
			if(aggregate.isATemplateType())
			{
				//So the aggregate here is the generic template definition.
				//We must ensure we marry up the generic parameters here - for example we might have 2 params S and T
				//But the aggregate we are lookup might just have one the 'T'. So we need to deal with this.
				List<ISymbol> lookupParameterSymbols = new ArrayList<>();
				for(ISymbol symbol : aggregate.getParameterisedTypes())
				{
					//So given a T or whatever we need to find which index position it is in
					//Then we can use that same index position to get the equiv symbol from what parameters have been applied.
					int index = CommonParameterisedTypeDetails.getIndexOfType(parameterisableSymbol, Optional.of(symbol));
					if(index < 0)
					{
						throw new RuntimeException("Unable to find symbol [" + symbol + "]");
					}
					ISymbol resolvedSymbol = parameterSymbols.get(index);
					lookupParameterSymbols.add(resolvedSymbol);
				}
				//So now we have a generic type and a set of actual parameters we can resolve it!
				String parameterisedTypeSymbolName = CommonParameterisedTypeDetails.getInternalNameFor(aggregate, lookupParameterSymbols);
				TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
				return this.resolve(search);
			}
			else if(aggregate.isGenericTypeParameter())
			{
				//So if this is a T or and S or a P for example we need to know
				//What actual symbol to use from the parameterSymbols we have been parameterised with.
				int index = CommonParameterisedTypeDetails.getIndexOfType(parameterisableSymbol, typeToResolve);
				ISymbol resolvedSymbol = parameterSymbols.get(index);

				return Optional.of(resolvedSymbol);
			}
			else if(aggregate.isGenericInNature() && aggregate instanceof ParameterisedFunctionSymbol asParameterisedTypeSymbol)
			{
				//Now this is a  bastard because we need to clone one of these classes we're already in.
				//But it might be a List of T or a real List of Integer for example.

				//So the thing we are to clone it itself a generic aggregate.
				//System.out.println("Need to Clone a generic aggregate [" + aggregate + "]");
				List<ISymbol> lookupParameterSymbols = new ArrayList<>();
				//Now it may really be a concrete one or it too could be something like a List of P

				for(ISymbol symbol : asParameterisedTypeSymbol.parameterSymbols)
				{
					//So lets see might be a K or V for example but we must be able to find that type in this object	
					if(symbol.isGenericTypeParameter())
					{
						int index = CommonParameterisedTypeDetails.getIndexOfType(parameterisableSymbol, Optional.of(symbol));
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

				String parameterisedTypeSymbolName = CommonParameterisedTypeDetails.getInternalNameFor(asParameterisedTypeSymbol.parameterisableSymbol, lookupParameterSymbols);
				TypeSymbolSearch search = new TypeSymbolSearch(parameterisedTypeSymbolName);
				return this.resolve(search);
			}

		}
		return typeToResolve;
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
		String nameOfFunction = parameterisableSymbol.getName() + getAnyGenericParamsAsFriendlyNames();
		StringBuilder buffer = new StringBuilder();
		buffer.append(getAccessModifier()).append(" ");
		if(getReturningSymbol() != null)
			buffer.append(getSymbolTypeAsString(getReturningSymbol().getType()));
		else
			buffer.append("Unknown");

		buffer.append(" <- ").append(nameOfFunction);
		buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(getSymbolsForThisScope(), true));
		return buffer.toString();
	}
}
