package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@NotResolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of Integer"
 */
public class NotResolvedDirective extends ResolutionDirective {

  public NotResolvedDirective(final DirectiveSpec spec) {
    super(spec);
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.NotResolved;
  }
}
