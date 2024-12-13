package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Because lots of checks are needed to resolve generic types this consumer will
 * do the assertions. It uses a flexible Resolver.
 */
public class GenericsSymbolCheck implements Consumer<SymbolSearchConfiguration> {

  private final GeneralTypeResolver resolver;
  private final boolean expectPresent;

  private final SymbolCategory categoryIfPresent;

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent, final SymbolCategory categoryIfPresent) {
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    this.resolver = new GeneralTypeResolver(scope);
    this.expectPresent = expectPresent;
    this.categoryIfPresent = categoryIfPresent;
  }

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent) {
    this(program, moduleName, expectPresent, SymbolCategory.TYPE);
  }

  @Override
  public void accept(final SymbolSearchConfiguration toResolve) {

    var resolved = resolver.apply(toResolve);
    assertEquals(expectPresent, resolved.isPresent(), "WRT: " + toResolve + " expect resolve: " + expectPresent);
    if (expectPresent && resolved.isPresent()) {
      assertEquals(categoryIfPresent, resolved.get().getCategory());
    }
  }
}
