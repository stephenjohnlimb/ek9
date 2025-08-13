package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Provides basic interface for the EK9 internal @ type directives.
 * The idea is to accept this sort of thing.
 * These are used in development and testing, so that EK9 can be annotated as part of tests.
 * <br/>
 * <p>
 *   In general while you are looking to create code that compiles, during the development of the
 *   compiler we're actually looking to detect (accurately) code that does not compile.
 *   These directive embedded in EK9 source files make that testing much easier.
 * </p>
 * <p>
 *   This is especially true when a defect or missing rule in found in the EK9 compiler for some code
 *   that should actually fail to compile. That example code can be used as an example and the appropriate
 *   directive used to check for the error (clearly initially it won't trigger - dues to the bug).
 *   But when the existing rule is corrected or a new rule is added it acts as a regression test.
 * </p>
 * <p>
 *   The EK9 compiler code base has hundreds (and in the end probably thousands) of example source files,
 *   some of which should compile and others which <b>must not</b>compile.
 * </p>
 * <pre>
 *  {@code @Resolved}: SYMBOL_DEFINITION: TYPE: "Person"
 *  {@code @Resolved}: SYMBOL_DEFINITION: FUNCTION: "someThingElse"
 *  {@code @Resolved}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 *  {@code @Resolved}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of (Integer)"
 *  {@code @NotResolved}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of (Date)"
 *  {@code @Implements}: SYMBOL_DEFINITION: TYPE: "CType": "SomeGenericType of (Integer, String)"
 *  {@code @Genus}: FULL_RESOLUTION: TYPE: "ServiceApplication": "SERVICE_APPLICATION"
 * </pre>
 * <p>
 * Used in specific cases where we make changes to the complexity calculator
 * With this mechanism you can ensure that complexity calculations are working
 * </p>
 * <pre>
 *  {@code @Complexity}: PRE_IR_CHECKS: FUNCTION: "SomeFunction": 12
 *  {@code @Complexity}: PRE_IR_CHECKS: FUNCTION: "testNoArguments": 2
 *  {@code @IR}: IR_GENERATION: TYPE: "Example": `ConstructDfn: introduction::Example ...`
 *  {@code @Error}: PRE_IR_CHECKS: EXCESSIVE_COMPLEXITY
 * </pre>
 */
public interface Directive {
  /**
   * What s the type of the directive.
   */
  DirectiveType type();

  /**
   * Is it for the particular compiler phase.
   */
  boolean isForPhase(final CompilationPhase phase);

  /**
   * Which line number does this directive apply to.
   */
  int getAppliesToLineNumber();

  /**
   * Provide a token from the source, by default if not possible supply synthetic.
   */
  IToken getDirectiveToken();
}
