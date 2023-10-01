package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;

/**
 * Models a variable.
 */
public class VariableSymbol extends Symbol {

  @Serial
  private static final long serialVersionUID = 1L;

  private boolean loopVariable = false;

  private boolean incomingParameter = false;

  private boolean returningParameter = false;

  /**
   * Limited scope of variables from other scopes
   * fields/properties on classes/components etc. are always private
   * Those on records are public and local variable a visible up the scope tree.
   * So it depends on where you are accessing the variable from.
   */
  private boolean isPrivate = false;

  private boolean isAggregatePropertyField = false;

  @SuppressWarnings("CopyConstructorMissesField")
  public VariableSymbol(VariableSymbol toCopy) {
    this(toCopy.getName(), toCopy.getType());
    this.setInitialisedBy(toCopy.getInitialisedBy());
  }

  public VariableSymbol(String name) {
    this(name, Optional.empty());
  }

  public VariableSymbol(String name, ISymbol type) {
    this(name, Optional.ofNullable(type));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public VariableSymbol(String name, Optional<ISymbol> type) {
    super(name, type);
    super.setGenus(SymbolGenus.VALUE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof VariableSymbol that)
        && super.equals(o)
        && isLoopVariable() == that.isLoopVariable()
        && isIncomingParameter() == that.isIncomingParameter()
        && isReturningParameter() == that.isReturningParameter()
        && isPrivate() == that.isPrivate()
        && isPropertyField() == that.isPropertyField();
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (isLoopVariable() ? 1 : 0);
    result = 31 * result + (isIncomingParameter() ? 1 : 0);
    result = 31 * result + (isReturningParameter() ? 1 : 0);
    result = 31 * result + (isPrivate() ? 1 : 0);
    result = 31 * result + (isPropertyField() ? 1 : 0);
    return result;
  }

  @Override
  public VariableSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoVariable(new VariableSymbol(this.getName(), this.getType()));
  }

  protected VariableSymbol cloneIntoVariable(VariableSymbol newCopy) {
    cloneIntoSymbol(newCopy);
    newCopy.loopVariable = this.loopVariable;
    newCopy.incomingParameter = this.incomingParameter;
    newCopy.returningParameter = this.returningParameter;
    newCopy.setPrivate(this.isPrivate);
    newCopy.setAggregatePropertyField(this.isAggregatePropertyField);
    return newCopy;
  }

  @Override
  public boolean isPropertyField() {
    return isAggregatePropertyField;
  }

  public void setAggregatePropertyField(boolean isAggregatePropertyField) {
    this.isAggregatePropertyField = isAggregatePropertyField;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  @Override
  public boolean isPublic() {
    return !isPrivate;
  }

  @Override
  public boolean isLoopVariable() {
    return loopVariable;
  }

  public void setLoopVariable(boolean asLoopVar) {
    loopVariable = asLoopVar;
  }

  @Override
  public boolean isIncomingParameter() {
    return incomingParameter;
  }

  public void setIncomingParameter(boolean incomingParameter) {
    this.incomingParameter = incomingParameter;
  }

  @Override
  public boolean isReturningParameter() {
    return returningParameter;
  }

  public void setReturningParameter(boolean returningParameter) {
    this.returningParameter = returningParameter;
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public String getFriendlyName() {
    StringBuilder rtn = new StringBuilder();
    if (this.isPrivate) {
      rtn.append("private ");
    }
    rtn.append(super.getFriendlyName());
    return rtn.toString();
  }
}
