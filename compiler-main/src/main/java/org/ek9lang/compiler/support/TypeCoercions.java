package org.ek9lang.compiler.support;

import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Holds the coercions we can make from and to on types.
 * The main driver and extensibility on this is the #^ promotion operator.
 * If there is a promotion operator that can return a compatible type then it can be coerced.
 * But any type can only have one promotion operator. i.e. from Integer to Float for example.
 * An Integer could not also have a promotion to String or something else as well.
 */
public class TypeCoercions {

  private TypeCoercions() {
    //Just to stop instantiation.
  }

  /**
   * Can the 'from' token type be coerced to the 'to' type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static boolean isCoercible(final Optional<ISymbol> from, final Optional<ISymbol> to) {
    AssertValue.checkNotNull("Coercion from cannot be null", from);
    AssertValue.checkNotNull("Coercion to cannot be null", to);

    if (from.isEmpty() || to.isEmpty()) {
      return false;
    }

    return isCoercible(from.get(), to.get());
  }

  /**
   * Can the 'from' token type be coerced to the 'to' type.
   */
  public static boolean isCoercible(final ISymbol from, final ISymbol to) {

    AssertValue.checkNotNull("Coercion from cannot be null", from);
    AssertValue.checkNotNull("Coercion to cannot be null", to);

    if (from instanceof IAggregateSymbol fromAggregate) {
      //that is the promotion symbol
      final var promoteMethod = fromAggregate.resolve(new MethodSymbolSearch("#^"));

      //Now because we have promoted/coerced once, we avoid using 'isAssignable' because that will
      //effectively enable more coercions. We only want to trigger one coercion when trying to match types.
      //because it could be quite possible for several groups of types, to have a circular coercions if designed badly.
      //Plus is makes more sense as a developer, you can easily check one coercion, but allowing chaining of coercions
      //is likely to cause problems - Even one coercion is a bit risky, as it is sort of implicit and not that visible

      return promoteMethod
          .flatMap(ISymbol::getType)
          .map(type -> type.getUnCoercedAssignableCostTo(to))
          .stream()
          .anyMatch(cost -> cost >= SymbolMatcher.ZERO_COST);
    }

    return false;
  }
}
