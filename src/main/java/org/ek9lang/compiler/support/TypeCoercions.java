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
  private static final TypeCoercions instance = new TypeCoercions();

  public static TypeCoercions get() {

    return instance;
  }

  /**
   * Can the 'from' token type be coerced to the 'to' type.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public boolean isCoercible(final Optional<ISymbol> from, final Optional<ISymbol> to) {
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
  public boolean isCoercible(final ISymbol from, final ISymbol to) {

    AssertValue.checkNotNull("Coercion from cannot be null", from);
    AssertValue.checkNotNull("Coercion to cannot be null", to);

    if (from instanceof IAggregateSymbol fromAggregate) {
      //that is the promotion symbol
      final var promoteMethod = fromAggregate.resolve(new MethodSymbolSearch("#^"));

      return promoteMethod
          .flatMap(ISymbol::getType)
          .map(type -> type.isAssignableTo(to))
          .orElse(false);
    }

    return false;
  }
}
