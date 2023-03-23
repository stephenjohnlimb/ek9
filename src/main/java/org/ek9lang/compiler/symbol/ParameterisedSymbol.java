package org.ek9lang.compiler.symbol;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.support.ToCommaSeparated;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

/**
 * Models a Symbol that is capable of being parameterised.
 */
public interface ParameterisedSymbol extends IScopedSymbol {
  default ScopeType getScopeType() {
    return ScopeType.NON_BLOCK;
  }

  List<ISymbol> getParameterSymbols();

  ScopedSymbol getParameterisableSymbol();

  //These will need to be implemented in the actual parameterised types/functions
  @Override
  default IScopedSymbol clone(IScope withParentAsAppropriate) {
    return null;
  }

  @Override
  default boolean isMarkedPure() {
    return false;
  }

  /**
   * Some parameterised types have been parameterised with S and T. This is done for use
   * within the symbol tables and when defining other generic types.
   * But these cannot actually be generated. So we do need to be able to distinguish between
   * those that can be really used and those that are just used with other generic classes.
   *
   * @return true if just conceptual. false when it can actually be made manifest and used.
   */
  default boolean isConceptualParameterisedType() {
    return isGenericInNature();
  }

  @Override
  default boolean isGenericInNature() {
    //These can also be generic definitions when initially parsed.
    //But when given a set of valid parameter are no longer generic aggregates.
    return getParameterSymbols()
        .stream()
        .map(ISymbol::isConceptualTypeParameter)
        .findFirst()
        .orElse(false);
  }

  @Override
  default Optional<ISymbol> getType() {
    //This is also a type
    return Optional.of(this);
  }

  @Override
  default ISymbol setType(Optional<ISymbol> type) {
    throw new CompilerException("Cannot alter ParameterisedTypeSymbol types",
        new UnsupportedOperationException());
  }

  /**
   * So now here when it comes to being assignable the generic parameterisable type has to be
   * the same. The parameters it has been parameterised with also have to be the same and match.
   * Only then do we consider it to be assignable via a weight.
   * This might be a bit over simplified, but at least it is simple and straightforward.
   * We are not currently considering extending or inheritance with generic templates types
   * - they are already complex enough.
   */
  @Override
  default double getAssignableWeightTo(ISymbol s) {
    return getUnCoercedAssignableWeightTo(s);
  }

  @Override
  default double getUnCoercedAssignableWeightTo(ISymbol s) {
    //Now because we've hashed the class and parameter signature we can do a very quick check here.
    //Plus we don't allow any types of coercion or super class matching.
    if (this.getName().equals(s.getName())) {
      return 0.0;
    }

    return NOT_ASSIGNABLE;
  }

  /**
   * Adds parameterising symbol to this object, i.e. parameterises it.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default ParameterisedSymbol addParameterSymbol(Optional<ISymbol> parameterSymbol) {
    AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);
    parameterSymbol.ifPresent(this::addParameterSymbol);
    return this;
  }

  /**
   * Adds parameterising symbol to this object, i.e. parameterises it.
   */
  default void addParameterSymbol(ISymbol parameterSymbol) {
    AssertValue.checkNotNull("parameterSymbol cannot be null", parameterSymbol);
    getParameterSymbols().add(parameterSymbol);

  }

  /**
   * Get the parameters as a comma separated list.
   */
  default String optionalParenthesisParameterSymbolsAsCommaSeparated() {
    var params = getParameterSymbols();
    var toCommaSeparated = new ToCommaSeparated(params.size() > 1);

    return toCommaSeparated.apply(params);
  }
}
