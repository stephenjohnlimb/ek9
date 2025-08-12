package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a specific class or function has a particular level of complexity.
 * This is mainly used to check that the complexity calculations in the compiler are working correctly.
 * EK9 limits the maximum amount of complexity in code to maximum levels - after which is emits errors to break the
 * compile phase. These are not warnings (that can be ignored), but are hard compiler errors.
 * <pre>
 *  {@code @Complexity: PRE_IR_CHECKS: FUNCTION: "SomeFunction": 12 }
 * </pre>
 */
public class ComplexityDirective extends ResolutionDirective {

  /**
   * The Directive to test for complexity values in EK9 instrumented code.
   */
  public ComplexityDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.Complexity;
  }
}
