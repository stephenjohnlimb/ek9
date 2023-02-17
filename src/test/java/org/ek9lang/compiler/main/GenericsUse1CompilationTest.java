package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.support.GenericTypeResolverForTesting;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just test generics simple declaration use compiles.
 * But also that the resulting parameterized symbols also get put into the correct module.
 */
class GenericsUse1CompilationTest extends FullCompilationTest {

  public GenericsUse1CompilationTest() {
    super("/examples/genericsUse1");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  /**
   * Checks only the stock ek9 stuff is present before parsing and compiling the source.
   * Checks that specific types that will be created during the compilation list Optional of String
   * do not exist yet as types.
   */
  protected void assertPreConditions(CompilableProgram program) {
    new SymbolCountCheck("org.ek9.lang", 66).test(program);

    var orgEk9LangScope = program.getParsedModules("org.ek9.lang").get(0).getModuleScope();
    GenericTypeResolverForTesting genericTypeResolver = new GenericTypeResolverForTesting(orgEk9LangScope);

    var resolvedGenericType = genericTypeResolver.apply("Optional", List.of("String"));
    assertTrue(resolvedGenericType.isEmpty());
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    //So we now expect more types as this ek9 source uses ek9 built in generic types.
    new SymbolCountCheck("org.ek9.lang", 70).test(program);

    //There are other types created but just check this one for now.
    var orgEk9LangScope = program.getParsedModules("org.ek9.lang").get(0).getModuleScope();
    GenericTypeResolverForTesting genericTypeResolver = new GenericTypeResolverForTesting(orgEk9LangScope);
    var resolvedGenericType = genericTypeResolver.apply("Optional", List.of("String"));
    assertTrue(resolvedGenericType.isPresent());

    new SymbolCountCheck(2, "simple.generics.use.one", 5).test(program);

    var justOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::JustOptional"));
    assertTrue(justOptionalSymbol.isPresent());

    var alsoJustOptionalSymbol =
        program.resolveByFullyQualifiedSearch(new FunctionSymbolSearch("simple.generics.use.one::AlsoJustOptional"));
    assertTrue(alsoJustOptionalSymbol.isPresent());

    if(alsoJustOptionalSymbol.get() instanceof FunctionSymbol alsoJustOptional) {
      var returningSymbol = alsoJustOptional.getReturningSymbol();
      assertTrue( returningSymbol.getType().isPresent());
      assertEquals(resolvedGenericType.get().getFullyQualifiedName(), returningSymbol.getType().get().getFullyQualifiedName());
    } else {
      fail("Expecting AlsoJustOptional to be a function");
    }
  }

}
