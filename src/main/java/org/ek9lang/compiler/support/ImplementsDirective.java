package org.ek9lang.compiler.support;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved as extending a type or function.
 * This can also work with TEMPLATE_TYPE and TEMPLATE_FUNCTION - but not METHODS (yet).
 * //@Implements: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeClass": "SomeBaseCase"
 * //@Implements: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeTrait": "SomeBaseTrait"
 * //@Implements: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "SomeClass": "SomeTrait"
 * //@Implements: EXPLICIT_TYPE_SYMBOL_DEFINITION: FUNCTION: "SomeFunction": "SomeAbstractFunction"
 */
public class ImplementsDirective extends ResolutionDirective {

  public ImplementsDirective(final DirectiveSpec spec) {
    super(spec);
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.Implements;
  }
}
