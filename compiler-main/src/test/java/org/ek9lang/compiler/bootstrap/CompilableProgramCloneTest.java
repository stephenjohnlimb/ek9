package org.ek9lang.compiler.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.DeSerializer;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Serializer;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * As the EK9 compiler bootstrap actually parsed a 'sort of interface extern' version of EK9 built-in
 * source code just to get started, we want to parse this once and then effectively clone all that in memory stuff.
 * <br/>
 * That way we can save on reloading the basic built in and also library types (when those come in).
 * It also means that from an 'lsp' point of view it is possible to clear everything in a workspace out and
 * start over (but without the overhead of the initial bootstrap).
 * <br/>
 * For example, we may do the boostrap and load all dependant libraries (which as we use source may take some time),
 * then just focus on the EK9 code under development. We don't really want to be having to reparse all the bootstrap
 * and dependent library code over and over.
 * <br/>
 * We may find that actually we need to persist the basics in a tight binary form in time, i.e. built-in and
 * library processed EK9 source. Then just load it up again (if versions change, bit it and reparse).
 * But let's wait and see.
 * <br/>
 * For now lets just ensure that it is possible to create a compilable programs with various modules and clone it and
 * all the pats it uses.
 */
class CompilableProgramCloneTest {

  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(System.err::println);
    }
  };

  /**
   * Just make sure junit is ready to go.
   * Then effectively clone the compilableProgram by serializing and deserializing.
   * This Serialization will be used in the future to pre-prepare a 'compact' header
   * version of a library module. So plan is to parse and fully compile any dependencies,
   * produce the 'artefact' for that dependency and later 'link' to it.
   * But we will in effect need the 'interface' into that library - but we don't want to have to
   * reparse all its source again. So when we build the 'library' artefact we also build a
   * serialized version of the interface and save it in binary '*.ser' format.
   * Clearly if the compiler data structures change then the main lib source will need reparsing and
   * new artefact and *.ser interface file regenerating.
   */
  @Test
  void basicMinimalBootStrap() {
    //Firstly trigger the parsing and basic front end compilation of a minimal ek9 definition.
    var sharedContextOfCompilableProgram = createCompilableProgram();

    //Now check it has been correctly populated.
    sharedContextOfCompilableProgram.accept(this::assertCompilableProgram);

    var serializer = new Serializer();
    var justTheBytes = serializer.apply(sharedContextOfCompilableProgram);

    //OK now lets try and reload it.
    var deserializer = new DeSerializer();
    var reloadedProgram = deserializer.apply(justTheBytes);
    reloadedProgram.accept(this::assertCompilableProgram);
  }

  private SharedThreadContext<CompilableProgram> createCompilableProgram() {
    final Supplier<List<CompilableSource>> sourceSupplier =
        () -> List.of(new CompilableSource(Objects.requireNonNull(getClass().getResource(
            "/examples/bootstrap/org-ek9-lang.ek9")).getPath()));

    final var ek9BootStrap = new Ek9LanguageBootStrap(sourceSupplier, listener.get(),
        new CompilerReporter(false, true));
    assertNotNull(ek9BootStrap);

    //Now trigger the parsing and compiling (at least to the level needed) of that source.
    var sharedContextOfCompilableProgram = ek9BootStrap.get();
    assertNotNull(sharedContextOfCompilableProgram);

    return sharedContextOfCompilableProgram;
  }

  private void assertCompilableProgram(CompilableProgram program) {
    //Now we'd only expect a single module name - "org.ek9.lang"
    var moduleNames = program.getParsedModuleNames();
    assertEquals(1, moduleNames.size());

    //Now lets get that module
    assertEquals("org.ek9.lang", moduleNames.getFirst());

    assertModules(program.getParsedModules("org.ek9.lang"));
  }

  private void assertModules(final List<ParsedModule> modules) {
    //Now a module can be made from multiple files/modules, in this case there is only one
    assertNotNull(modules);
    assertFalse(modules.isEmpty());
    assertEquals(1, modules.size());

    var module = modules.getFirst();
    assertNotNull(module);

    var moduleScope = module.getModuleScope();
    assertModuleScope(moduleScope);

    //Now check the source object
    var compilableSource = module.getSource();
    assertCompilableSource(compilableSource);

  }

  private void assertModuleScope(final ModuleScope moduleScope) {
    assertNotNull(moduleScope);
    var justStringSymbol = moduleScope.resolve(new TypeSymbolSearch("String"));
    assertTrue(justStringSymbol.isPresent());
    justStringSymbol.ifPresent(stringSymbol -> {
      assertEquals("String", stringSymbol.getFriendlyName());
      assertStringMethods(stringSymbol);
    });
  }

  private void assertStringMethods(final ISymbol justStringSymbol) {
    if (justStringSymbol instanceof AggregateSymbol aggregate) {
      var methods = aggregate.getAllNonAbstractMethods();

      //Now expect 'Any' as the super of String constructor.

      assertEquals(3, methods.size());
      var defaultAnyConstructor = methods.getFirst();
      assertNotNull(defaultAnyConstructor);
      assertEquals(0, defaultAnyConstructor.getCallParameters().size());

      var defaultStringConstructor = methods.get(1);
      assertNotNull(defaultStringConstructor);
      assertEquals(0, defaultStringConstructor.getCallParameters().size());


      var copyConstructor = methods.get(2);
      assertNotNull(copyConstructor);
      var params = copyConstructor.getCallParameters();
      assertEquals(1, params.size());
      var arg0 = params.getFirst();
      assertNotNull(arg0);
      assertTrue(arg0.getType().isPresent());

    } else {
      fail("Expecting An Aggregate");
    }
  }

  private void assertCompilableSource(final CompilableSource compilableSource) {
    assertNotNull(compilableSource);
    assertTrue(compilableSource.getFileName().endsWith("examples/bootstrap/org-ek9-lang.ek9"));
    assertNotNull(compilableSource.getGeneralIdentifier());

    //We have not said this is a library and so this should be null.
    assertNull(compilableSource.getPackageModuleName());
    assertFalse(compilableSource.isDev());
    assertFalse(compilableSource.isLib());

    var checkTokenResult = compilableSource.nearestToken(12, 15);
    assertNotNull(checkTokenResult);

    //Check that we find the expected token text at this point.
    assertEquals("function", checkTokenResult.getToken().getText());
    //Now check the error listener
    assertErrorListener(compilableSource.getErrorListener());

  }

  private void assertErrorListener(final ErrorListener errorListener) {
    assertNotNull(errorListener);
    assertTrue(errorListener.isErrorFree());

  }

}
