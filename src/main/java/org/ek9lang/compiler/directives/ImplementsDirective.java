package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved as extending a type or function.
 * <pre>
 *  {@code @Implements}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeClass": "SomeBaseCase"
 *  {@code @Implements}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeTrait": "SomeBaseTrait"
 *  {@code @Implements}: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeClass": "SomeTrait"
 *  {@code @Implements}: EXPLICIT_TYPE_SYMBOL_DEFINITION: FUNCTION: "SomeFunction": "SomeAbstractFunction"
 * </pre>
 */
public class ImplementsDirective extends ResolutionDirective {

  /**
   * New directive checker for implements.
   */
  public ImplementsDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.Implements;
  }
}
