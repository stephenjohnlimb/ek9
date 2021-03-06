package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.core.utils.Digest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Yes it's a long name.
 * The idea is to type and get the common stuff out of
 * ParameterisedTypeSymbol and ParameterisedFunctionSymbol.
 * <p>
 * Also general for of Symbol type stuff
 */
public class CommonParameterisedTypeDetails
{

	/**
	 * Could be either a ParameterisedTypeSymbol or a ParameterisedFunctionSymbol
	 * Builds a unique name based on what it has been parameterised with.
	 */
	public static String getInternalNameFor(ISymbol parameterisableSymbol, List<ISymbol> parameterSymbols)
	{
		return getEK9InternalNameFor(parameterisableSymbol, parameterSymbols);
	}

	private static String getEK9InternalNameFor(ISymbol parameterisableSymbol, List<ISymbol> parameterSymbols)
	{
		var toDigest = parameterisableSymbol.getFullyQualifiedName() + parameterSymbols
				.stream()
				.map(ISymbol::getFullyQualifiedName)
				.collect(Collectors.joining("_"));

		return "_" + parameterisableSymbol.getName() + "_" + Digest.digest(toDigest);
	}

	public static boolean doSymbolsMatch(List<ISymbol> list1, List<ISymbol> list2)
	{
		if(list1 != null && list2 != null && list1.size() == list2.size())
		{
			for(int i = 0; i < list1.size(); i++)
				if(!list1.get(i).isExactSameType(list2.get(i)))
					return false;
			return true;
		}
		return false;
	}

	public static String asCommaSeparated(List<ISymbol> params, boolean includeBraces)
	{
		var commaSeparated = params.stream().map(ISymbol::getFriendlyName).collect(Collectors.joining(", "));
		if(!includeBraces)
			return commaSeparated;
		return "(" + commaSeparated + ")";
	}

	/**
	 * For the type passed in - a T or and S whatever we need to know its index.
	 * From this we can look at what this has been parameterised with and use that type.
	 *
	 * @param theType The generic definition parameter i.e S, or T
	 * @return The index or -1 if not found.
	 */
	public static int getIndexOfType(ScopedSymbol parameterisableSymbol, Optional<ISymbol> theType)
	{
		if(theType.isPresent())
		{
			for(int i = 0; i < parameterisableSymbol.getParameterisedTypes().size(); i++)
			{
				if(parameterisableSymbol.getParameterisedTypes().get(i).isExactSameType(theType.get()))
					return i;
			}
		}
		return -1;
	}
}
