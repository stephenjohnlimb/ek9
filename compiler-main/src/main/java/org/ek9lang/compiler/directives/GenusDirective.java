package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * <pre>
 *  {@code @Genus}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeService": SERVICE_APPLICATION
 * </pre>
 */
public class GenusDirective extends ResolutionDirective {

  /**
   * New directive for genus checking.
   */
  public GenusDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.Genus;
  }
}
