package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Because lots of checks are needed to resolve generic types this consumer will
 * do the assertions. It uses a flexible Resolver.
 */
public class GenericsSymbolCheck implements Consumer<SymbolSearchForTest> {

  private final ResolverForTesting resolver;
  private final boolean expectPresent;

  private final ISymbol.SymbolCategory categoryIfPresent;

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent, final ISymbol.SymbolCategory categoryIfPresent) {
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    this.resolver = new ResolverForTesting(scope);
    this.expectPresent = expectPresent;
    this.categoryIfPresent = categoryIfPresent;
  }

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent) {
    this(program, moduleName, expectPresent, ISymbol.SymbolCategory.TYPE);
  }

  @Override
  public void accept(final SymbolSearchForTest toResolve) {

    var resolved = resolver.apply(toResolve);
    assertEquals(expectPresent, resolved.isPresent(), "Unable to resolve: " + toResolve);
    if (expectPresent && resolved.isPresent()) {
      assertEquals(categoryIfPresent, resolved.get().getCategory());
    }
  }
}
