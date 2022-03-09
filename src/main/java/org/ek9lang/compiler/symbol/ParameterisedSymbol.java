package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.core.exception.AssertValue;

import java.util.List;
import java.util.Optional;

public interface ParameterisedSymbol extends IScope, ISymbol
{
	default ScopeType getScopeType() { return ScopeType.AGGREGATE; }
	
	List<ISymbol> getParameterSymbols();
	
	ScopedSymbol getParameterisableSymbol();

	//These will need to be implemented in the actual parameterised types/functions
	@Override
	default ISymbol clone(IScope withParentAsAppropriate) { return null; }

	@Override
	default boolean isMarkedPure() { return false; }

	/**
	 * Some parameterised types have been parameterised with S and T. This is done for use
	 * within the symbol tables and when defining other generic types.
	 * But these cannot actually be generated. So we do need to be able to distinguish between
	 * those that can be really used and those that are just used with other generic classes.
	 *
	 * @return true if just conceptual and cannot be generated. false when it can actually be made manifest and used.
	 */
	default boolean isConceptualParameterisedType()
	{
		return isGenericInNature();
	}

	@Override
	default boolean isGenericInNature()
	{
		//These can also be generic definitions when initially parsed.
		//But when given a set of valid parameter are no longer generic aggregates.
		return getParameterSymbols()
				.stream()
				.map(ISymbol::isGenericTypeParameter)
				.findFirst()
				.orElse(false);
	}

	@Override
	default Optional<ISymbol> getType()
	{
		//This is also a type
		return Optional.of(this);
	}

	@Override
	default ISymbol setType(Optional<ISymbol> type)
	{
		throw new RuntimeException("Cannot alter ParameterisedTypeSymbol types", new UnsupportedOperationException());
	}

	/**
	 * So now here when it comes to being assignable the generic parameterisable type has to be the same.
	 * And the parameters it has been parameterised with also have to be the same and match.
	 * <p>
	 * Only then do we consider it to be assignable via a weight.
	 * <p>
	 * This might be a bit over simplified, but at least it is simple and straightforward.
	 * We are not currently considering extending or inheritance with generic templates types - they are already complex enough.
	 */
	@Override
	default double getAssignableWeightTo(ISymbol s)
	{
		return getUnCoercedAssignableWeightTo(s);
	}

	@Override
	default double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		//Now because we've hashed the class and parameter signature we can do a very quick check here.
		//Plus we don't allow any types of coercion or super class matching.
		if(this.getName().equals(s.getName()))
			return 0.0;

		return -1000.0;
	}

	default ParameterisedSymbol addParameterSymbol(Optional<ISymbol> parameterSymbol)
	{
		AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);
		parameterSymbol.ifPresent(this::addParameterSymbol);
		return this;
	}

	default ParameterisedSymbol addParameterSymbol(ISymbol parameterSymbol)
	{
		AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);
		getParameterSymbols().add(parameterSymbol);
		return this;
	}

	default String optionalParenthesisParameterSymbolsAsCommaSeparated()
	{
		var params = getParameterSymbols();
		return CommonParameterisedTypeDetails.asCommaSeparated(params, params.size() > 1);
	}
}
