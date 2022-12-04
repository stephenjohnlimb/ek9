package org.ek9lang.compiler.internals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.main.phases.definition.BuiltinEk9Listener;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 *
 */
class SimpleSymbolResolutionCompilableProgramTest {

  private static final Supplier<CompilableSource> ek9BuiltInTypesSource = new Ek9BuiltinSupplier();

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext =
      () -> new SharedThreadContext<>(new CompilableProgram(List.of()));

  /**
   * Just for now, we will just inject a 'Type' ('org.ek9.lang.String') and check we can resolve it.
   * But we will need to define our bootstrap org.ek9.lang module and types later in development.
   */
  @Test
  void parsingAndAdditionOfParsedModule() {
    var sharedThreadContext = sharedContext.get();

    final var source = ek9BuiltInTypesSource.get();
    final var compilationUnitContext = source.prepareToParse().parse();
    final var errorListener = source.getErrorListener();

    //Ensure this has parsed.
    if(errorListener.hasErrors()) {
      errorListener.getErrors().forEachRemaining(System.err::println);
    }
    assertTrue(errorListener.isErrorFree());

    final ParsedModule module = new ParsedModule(source, sharedContext.get());
    final var moduleScope = module.acceptCompilationUnitContext(compilationUnitContext);

    assertEquals("org.ek9.lang", module.getModuleName());
    sharedThreadContext.accept(compilableProgram -> {
      compilableProgram.add(module);
      assertEquals(1, compilableProgram.getParsedModules(module.getModuleName()).size());
    });

    //Just be sure this String type cannot be found.
    final var resolved = moduleScope.resolve(new TypeSymbolSearch("String"));
    assertTrue(resolved.isEmpty());

    //Now lets visit that source and extract and load the types into the parsed module.
    BuiltinEk9Listener listener = new BuiltinEk9Listener(module);
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(listener, compilationUnitContext);

    //We should now find there some symbols defined.
    final var nowResolves = moduleScope.resolve(new TypeSymbolSearch("String"));
    assertTrue(nowResolves.isPresent());
  }
}
