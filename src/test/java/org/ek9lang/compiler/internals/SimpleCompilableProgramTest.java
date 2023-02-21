package org.ek9lang.compiler.internals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * There is quite a bit on linkage (cross dependency) between:
 * CompilableProgram, ParsedModules, ParsedModule and ModuleScope.
 * So this is a bit beyond a unit test - but I hate mocks.
 * Maybe I could do a bit better here!
 * These four classes provide the overall structure for where to put and hold
 * all the artefacts that are built up during the first half of the compilation process.
 * i.e. just before (and with a bit of cross over into) the generation of the internal
 * intermediate representation.
 * This test is just the first initial simple test to ensure these hang together reasonably well.
 */
class SimpleCompilableProgramTest {

  private static final Supplier<CompilableSource> validEk9Source = new HelloWorldSupplier();

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext =
      () -> new SharedThreadContext<>(new CompilableProgram());

  @Test
  void brokenParseModuleConstructionNullSource() {
    assertThrows(IllegalArgumentException.class, () -> new ParsedModule(null, sharedContext.get()));
  }

  @Test
  void brokenAdditionOfParsedModule() {
    //We do this because the main compiler is designed to use multiple threads and access to this
    //central object has to be controlled with a reentrant lock (avoid synchronised methods).
    var sharedThreadContext = sharedContext.get();

    ParsedModule module = new ParsedModule(validEk9Source.get(), sharedContext.get());
    assertNotNull(module);

    //So use the shared context as we would in real code to access and add the module.
    //But let's try adding without being parsed.
    sharedThreadContext.accept(
        compilableProgram -> assertThrows(IllegalArgumentException.class, () -> compilableProgram.add(module)));
  }

  @Test
  void checkParsedModuleIdentification() {
    final var source = validEk9Source.get();
    final ParsedModule module = new ParsedModule(source, sharedContext.get());

    assertTrue(module.isForThisCompilableSource(source));
    assertEquals(source, module.getSource());

    assertEquals(source.hashCode(), module.getSource().hashCode());
  }

  @Test
  void checkParsedModuleEquality() {
    final var source = validEk9Source.get();
    final var compilationUnitContext = source.prepareToParse().parse();

    //use the same source and the parsed modules should be considered the same.
    final ParsedModule module1 = new ParsedModule(source, sharedContext.get());
    final var moduleScope1 = module1.acceptCompilationUnitContext(compilationUnitContext);
    assertNotNull(moduleScope1);

    final ParsedModule module2 = new ParsedModule(source, sharedContext.get());
    final var moduleScope2 = module2.acceptCompilationUnitContext(compilationUnitContext);
    assertNotNull(moduleScope2);

    assertEquals(module1, module2);
    assertEquals(module1.hashCode(), module2.hashCode());
    assertEquals(module1.toString(), module2.toString());
    assertEquals(moduleScope1, moduleScope2);
    assertEquals(moduleScope1.hashCode(), moduleScope2.hashCode());
  }

  /**
   * OK now we get down to it.
   * Get the ek9 source code.
   * Parse it.
   * Get the main Parse AST compilationUnitContext
   * Put that into the ParsedModule, and it is then initialised and will return a ModuleScope.
   * Also try adding more than once and removing.
   * The nature of compilation is the visitor and the building up on mutable state (hence the multiphase creation).
   */
  @Test
  void parsingAndAdditionOfParsedModule() {
    var sharedThreadContext = sharedContext.get();

    //Effectively this is the first stages of compilation. That's tested elsewhere.
    //We just need the compilationUnitContext for the parsed module.
    final var source = validEk9Source.get();
    final var compilationUnitContext = source.prepareToParse().parse();
    //just ensure it has parsed OK.
    assertNotNull(compilationUnitContext);

    final ParsedModule module = new ParsedModule(source, sharedContext.get());
    final var moduleScope = module.acceptCompilationUnitContext(compilationUnitContext);
    assertNotNull(moduleScope);
    //These should tie up.
    assertEquals(moduleScope.getScopeName(), module.getScopeName());
    assertEquals(moduleScope.getScopeName(), module.getModuleName());

    sharedThreadContext.accept(compilableProgram -> {
      compilableProgram.add(module);
      assertEquals(1, compilableProgram.getParsedModules(module.getModuleName()).size());
    });

    sharedThreadContext.accept(
        compilableProgram -> assertThrows(CompilerException.class, () -> compilableProgram.add(module)));

    //Now we will remove that parsed source
    sharedThreadContext.accept(compilableProgram -> {
      compilableProgram.remove(module);
      assertEquals(0, compilableProgram.getParsedModules(module.getModuleName()).size());
    });
  }
}
