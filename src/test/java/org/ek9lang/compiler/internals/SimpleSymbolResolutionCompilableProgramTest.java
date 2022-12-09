package org.ek9lang.compiler.internals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.main.phases.definition.DefinitionPhase1Listener;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 *
 */
class SimpleSymbolResolutionCompilableProgramTest {

  private static final Supplier<CompilableSource> ek9BuiltInTypesSource = new Ek9BuiltinLangSupplier();

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
    if (errorListener.hasErrors()) {
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
    final var resolved = moduleScope.resolve(new TypeSymbolSearch("org.ek9.lang::String"));
    assertTrue(resolved.isEmpty());

    //Now lets visit that source and extract and load the types into the parsed module.
    DefinitionPhase1Listener listener = new DefinitionPhase1Listener(sharedContext.get(), module);
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(listener, compilationUnitContext);

    assertTrue(listener.getErrorListener().isErrorFree());
    //If we've managed all the listening correctly the scope stack should be empty
    //i.e. have we matched out enters and exits.
    assertTrue(listener.isScopeStackEmpty());

    //We should now find there some symbols defined.
    //I would expect new TypeSymbolSearch("org.ek9.lang::String") and new TypeSymbolSearch("String") to work.
    final var nowResolves = moduleScope.resolve(new TypeSymbolSearch("org.ek9.lang::String"));
    assertTrue(nowResolves.isPresent());
  }
}
