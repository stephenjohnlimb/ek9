package org.ek9lang.compiler.symbols.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.main.resolvedefine.GeneralTypeResolver;
import org.ek9lang.compiler.main.resolvedefine.SymbolSearchConfiguration;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Because lots of checks are needed to resolve generic types this consumer will
 * do the assertions. It uses a flexible Resolver.
 */
public class GenericsSymbolCheck implements Consumer<SymbolSearchConfiguration> {

  private final GeneralTypeResolver resolver;
  private final boolean expectPresent;

  private final ISymbol.SymbolCategory categoryIfPresent;

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent, final ISymbol.SymbolCategory categoryIfPresent) {
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    this.resolver = new GeneralTypeResolver(scope);
    this.expectPresent = expectPresent;
    this.categoryIfPresent = categoryIfPresent;
  }

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent) {
    this(program, moduleName, expectPresent, ISymbol.SymbolCategory.TYPE);
  }

  @Override
  public void accept(final SymbolSearchConfiguration toResolve) {

    var resolved = resolver.apply(toResolve);
    assertEquals(expectPresent, resolved.isPresent(), "Unable to resolve: " + toResolve);
    if (expectPresent && resolved.isPresent()) {
      assertEquals(categoryIfPresent, resolved.get().getCategory());
    }
  }
}
