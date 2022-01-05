package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.Optional;

/**
 * Holds the coercions we can make from and to on types.
 *
 * The main driver and extensibility on this is the #^ promotion operator.
 * If there is a promotion operator that can return a compatible type then it can be coerced.
 *
 * But any type can only have one promotion operator. i.e. from Integer to Float for example.
 * An Integer could not also have a promotion to String or something else as well.
 */
public class TypeCoercions
{
	private static TypeCoercions instance = new TypeCoercions();
	
	public static TypeCoercions get()
	{
		return instance;
	}
	
	public TypeCoercions()
	{
		
	}
	
	public boolean isCoercible(Optional<ISymbol> from, Optional<ISymbol> to)
	{
		if(!from.isPresent() || !to.isPresent())
			return false;

		AssertValue.checkNotNull("Coercion from cannot be null", from);
		AssertValue.checkNotNull("Coercion to cannot be null", to);
		
		return isCoercible(from.get(), to.get());
	}
	
	public boolean isCoercible(ISymbol from, ISymbol to)
	{
		AssertValue.checkNotNull("Coercion from cannot be null", from);
		AssertValue.checkNotNull("Coercion to cannot be null", to);

		if(from instanceof IAggregateSymbol)
		{
			IAggregateSymbol fromAggregate = (IAggregateSymbol)from;
			Optional<ISymbol> promoteMethod = fromAggregate.resolve(new MethodSymbolSearch("#^")); //that is the promotion symbol
			if(promoteMethod.isPresent())
			{
				Optional<ISymbol> allowed = promoteMethod.get().getType();
				if(allowed.get().isAssignableTo(to))
					return true;
				//now only allow one level of promotion/coercion.
			}
		}
		return false;
	}
}
