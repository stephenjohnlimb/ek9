package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * <pre>
 * {@code @Resolved}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 * </pre>
 */
public class ResolvedDirective extends ResolutionDirective {

  /**
   * Constructor of a directive for resolution.
   */
  public ResolvedDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.Resolved;
  }
}
