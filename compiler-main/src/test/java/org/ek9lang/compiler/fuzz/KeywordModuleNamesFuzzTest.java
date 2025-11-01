package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests to verify keywords are allowed in module names (successful compilation test).
 * Tests that grammar change allowing keywords as module name segments works correctly.
 *
 * <p>Test corpus: fuzzCorpus/keywordModuleNames (8 test files)
 * Covers all 30 newly-allowed keywords:
 * - Access/Visibility: PRIVATE, PROTECTED, PUBLIC
 * - Type Relations: AS, OF, EXTENDS
 * - OOP: SUPER, THIS, ABSTRACT, OVERRIDE, OPERATOR
 * - Exceptions: TRY, CATCH, FINALLY, THROW
 * - Control Flow: IF, ELSE, SWITCH, CASE
 * - Packaging: PACKAGE, EXTERN
 * - Logic: AND, OR, NOT
 * - Testing: ASSERT, GIVEN, WHEN, THEN
 * - Loops: WHILE, FOR, DO
 * - Modifiers: DEFAULT, CONST, FINAL
 * - AOP: ASPECT
 *
 * <p>Expected behavior:
 * - All files should compile successfully (NO errors)
 * - Module names are opaque strings, keywords have no semantic meaning in this context
 * - Validates that developer ergonomics improvement works as intended
 *
 * <p>Test files:
 * 1. access_visibility_keywords.ek9 - PRIVATE, PROTECTED, PUBLIC
 * 2. oop_keywords.ek9 - SUPER, THIS, ABSTRACT, OVERRIDE, OPERATOR
 * 3. exception_handling_keywords.ek9 - TRY, CATCH, FINALLY, THROW
 * 4. control_flow_keywords.ek9 - IF, ELSE, SWITCH, CASE, WHILE, FOR, DO
 * 5. type_relations_keywords.ek9 - AS, OF, EXTENDS
 * 6. logic_testing_keywords.ek9 - AND, OR, NOT, ASSERT, GIVEN, WHEN, THEN
 * 7. modifiers_packaging_keywords.ek9 - PACKAGE, EXTERN, DEFAULT, CONST, FINAL, ASPECT
 * 8. all_keywords_comprehensive.ek9 - All 30 keywords in one module name
 */
class KeywordModuleNamesFuzzTest extends PhasesTest {

  public KeywordModuleNamesFuzzTest() {
    super("/examples/parseAndCompile/keywordModuleNames",
        List.of("com.security.private.public.protected",
            "org.framework.super.this.abstract.override.operator",
            "com.error.try.catch.finally.throw",
            "com.router.if.else.switch.case.while.for.do",
            "com.converters.as.of.extends",
            "com.test.and.or.not.assert.given.when.then",
            "org.tools.package.extern.default.const.final.aspect",
            "test.private.protected.public.as.of.extends.super.this.abstract.override.operator.try.catch.finally.throw.if.else.switch.case.package.extern.and.or.not.assert.given.when.then.while.for.do.default.const.final.aspect"),
        false,  // shouldFailCompilation = false (expect success!)
        false); // suppressReporting = false
  }

  @Test
  void testKeywordModuleNamesCompileSuccessfully() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final org.ek9lang.compiler.CompilableProgram program) {
    assertFalse(numberOfErrors > 0, "Expected zero errors, but got: " + numberOfErrors);
  }
}
