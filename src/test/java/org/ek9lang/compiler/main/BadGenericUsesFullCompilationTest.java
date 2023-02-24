package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.GenericsSymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolSearchForTest;
import org.ek9lang.compiler.symbol.support.SymbolSearchMapFunction;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad generic uses of generics.
 */
class BadGenericUsesFullCompilationTest extends FullCompilationTest {

  public BadGenericUsesFullCompilationTest() {
    super("/examples/parseButFailCompile/badGenericUses");
  }

  private final SymbolSearchMapFunction mappingFunction = new SymbolSearchMapFunction();

  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(final boolean compilationResult, final int numberOfErrors,
                               final CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(13, numberOfErrors);
    assertEK9GeneratedGenericTypes(program);
    assertGenericUses(program);
  }

  private void assertEK9GeneratedGenericTypes(final CompilableProgram program) {

    var moduleName = "org.ek9.lang";
    var ek9 = program.getParsedModules(moduleName);
    assertNotNull(ek9);
    var typeChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TYPE);

    var listOfString = new SymbolSearchForTest("List", new SymbolSearchForTest("String"));
    var listOfFloat = new SymbolSearchForTest("List", new SymbolSearchForTest("Float"));

    var dictOfIntegerString = new SymbolSearchForTest("Dict",
        List.of(new SymbolSearchForTest("Integer"), new SymbolSearchForTest("String"))
    );

    var dictOfIntegerDate = new SymbolSearchForTest("Dict",
        List.of(new SymbolSearchForTest("Integer"), new SymbolSearchForTest("Date"))
    );

    checkExistsWith(typeChecker, List.of(listOfString, listOfFloat, dictOfIntegerString, dictOfIntegerDate));

  }

  private void assertGenericUses(final CompilableProgram program) {

    var moduleName = "incorrect.generic.uses";

    var uses = program.getParsedModules(moduleName);
    assertNotNull(uses);

    //Now also check for things that should have been defined
    var typeChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TYPE);
    var templateTypeChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TEMPLATE_TYPE);
    var functionChecker = new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.FUNCTION);
    var templateFunctionChecker =
        new GenericsSymbolCheck(program, moduleName, true, ISymbol.SymbolCategory.TEMPLATE_FUNCTION);

    checkExists(templateTypeChecker, List.of("GenericThing"));
    checkExists(templateFunctionChecker, List.of("AListener"));

    checkExists(functionChecker, List.of(
            "allowedDefaultConstructor",
            "allowedWithTypeInference",
            "allowedSpecificListSyntax",
            "allowedSpecificDictSyntax",
            "invalidPhase1IncorrectParenthesis1",
            "invalidGenericsUse",
            "invalidTooManyParameters",
            "invalidTooFewParameters",
            "validDynamicFunction",
            "invalidDynamicFunction",
            "validDynamicClass",
            "invalidDynamicClass"
        )
    );

    //Now the concrete parameterised types built from the generics.
    var genericThingOfInteger = new SymbolSearchForTest("GenericThing", new SymbolSearchForTest("Integer"));

    var genericThingOfDate = new SymbolSearchForTest("GenericThing", new SymbolSearchForTest("Date"));

    checkExistsWith(typeChecker, List.of(genericThingOfInteger,genericThingOfDate));
  }

  private void checkExists(final Consumer<SymbolSearchForTest> checker, final List<String> names) {
    checkExistsWith(checker, mappingFunction.apply(names));
  }

  private void checkExistsWith(final Consumer<SymbolSearchForTest> checker, final List<SymbolSearchForTest> searches) {
    searches.forEach(checker);
  }
}
