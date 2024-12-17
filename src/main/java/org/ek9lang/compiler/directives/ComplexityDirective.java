package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@Complexity: PRE_IR_CHECKS: FUNCTION: "SomeFunction": "12"
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
