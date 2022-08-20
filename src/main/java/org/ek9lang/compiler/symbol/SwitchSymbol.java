package org.ek9lang.compiler.symbol;

/**
 * EK9 switch statement - this can effectively return a value if it is configured with returning
 * part.
 * When generating out put we need this to create its own block so variables inside are hidden
 * from later scopes.
 * So as we have a returning part (optional) we need a scope to put it in (i.e. this scope) this
 * then means that when we use the case parts of the switch we can find that returning variable
 * - but also check of local variables declared in the case block against this switch scope and
 * any outer scopes.
 * Finally, when coming to generate the output - this symbol will be able to supply the outer
 * variable to set the result to.
 */
public class SwitchSymbol extends ControlSymbol {
  public SwitchSymbol(IScope enclosingScope) {
    super("Switch", enclosingScope);
  }

  @Override
  public SwitchSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoSwitchSymbol(new SwitchSymbol(withParentAsAppropriate));
  }

  protected SwitchSymbol cloneIntoSwitchSymbol(SwitchSymbol newCopy) {
    super.cloneIntoControlSymbol(newCopy);
    return newCopy;
  }
}
