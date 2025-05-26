package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Because lots of checks are needed to resolve non-generic types this consumer will
 * do the assertions.
 */
public class SymbolCheck implements Consumer<String> {

  private final SimpleResolverForTesting resolver;
  private final boolean expectPresent;

  private final SymbolCategory categoryIfPresent;

  public SymbolCheck(final CompilableProgram program, final String moduleName,
                     boolean thisScopeOnly,
                     boolean expectPresent, final SymbolCategory categoryIfPresent) {
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    this.resolver = new SimpleResolverForTesting(scope, thisScopeOnly);
    this.expectPresent = expectPresent;
    this.categoryIfPresent = categoryIfPresent;
  }

  @Override
  public void accept(final String nameToResolve) {
    var resolved = resolver.apply(nameToResolve);
    assertEquals(expectPresent, resolved.isPresent(), "Expecting " + nameToResolve);
    if (expectPresent && resolved.isPresent()) {
      assertEquals(categoryIfPresent, resolved.get().getCategory());
    }
  }
}
