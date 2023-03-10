package org.ek9lang.compiler.support;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 */
public class ResolvedDirective extends ResolutionDirective {

  public ResolvedDirective(final DirectiveSpec spec) {
    super(spec);
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.Resolved;
  }
}
