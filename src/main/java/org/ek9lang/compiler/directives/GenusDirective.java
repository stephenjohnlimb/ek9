package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@Genus: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeService": SERVICE_APPLICATION
 */
public class GenusDirective extends ResolutionDirective {

  public GenusDirective(final DirectiveSpec spec) {
    super(spec);
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.Genus;
  }
}
