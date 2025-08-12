package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to check that the IR generated matches what is expected.
 * <p>
 * Note use the 'back ticks' for the string as the IR is very likely to have double quotes used within it.
 * </p>
 * <pre>
 *  {@code @IR}: SIMPLE_IR_GENERATION: TYPE: "Example": `ConstructDfn: introduction::Example ...`
 *  {@code @IR}: SIMPLE_IR_GENERATION: FUNCTION: "TestFunction": `ConstructDfn: introduction::TestFunction ...`
 * </pre>
 */
public class IRDirective extends ResolutionDirective {

  /**
   * New directive checker for implements.
   */
  public IRDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.IR;
  }
}
