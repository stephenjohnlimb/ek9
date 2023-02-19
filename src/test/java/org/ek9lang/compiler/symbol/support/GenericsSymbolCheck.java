package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Because lots of checks are needed to resolve generic types this consumer will
 * do the assertions.
 */
public class GenericsSymbolCheck implements BiConsumer<String, List<String>> {

  private final GenericResolverForTesting genericResolver;
  private final boolean expectPresent;

  private final ISymbol.SymbolCategory categoryIfPresent;

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent, final ISymbol.SymbolCategory categoryIfPresent) {
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    this.genericResolver = new GenericResolverForTesting(scope);
    this.expectPresent = expectPresent;
    this.categoryIfPresent = categoryIfPresent;
  }

  public GenericsSymbolCheck(final CompilableProgram program, final String moduleName,
                             boolean expectPresent) {
    this(program, moduleName, expectPresent, ISymbol.SymbolCategory.TYPE);
  }

  @Override
  public void accept(final String genericName, List<String> parameters) {
    var resolved = genericResolver.apply(genericName, parameters);
    assertEquals(expectPresent, resolved.isPresent());
    if (expectPresent && resolved.isPresent()) {
      assertEquals(categoryIfPresent, resolved.get().getCategory());
    }
  }
}
