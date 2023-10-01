package org.ek9lang.compiler.symbols;

import java.io.Serial;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * While we don't add these in the scoped structures when compiling.
 * We do use these to augment the parse tree for the appropriate context.
 * We do this so that we can work out what type of result will be returned from an expression.
 * See methods like isExactSameType and isAssignableTo for type checking in the Symbol class.
 * The idea is to augment the parse tree with these expression symbols so that once we've been
 * through sufficient passes of the tree we have sufficient information to add in any
 * promotions/coercions and also check the type of parameters on operations are compatible.
 * We can then add in explicit IR type conversions if needed.
 * This information will also be used in the semantic analysis phase.
 */
public class ExpressionSymbol extends Symbol {

  @Serial
  private static final long serialVersionUID = 1L;

  //So could need to promote to get right type
  private boolean promotionRequired = false;
  //Or maybe just need to call the $ _string() operator (this the returning type has it).
  private boolean useStringOperator = false;

  //Now an Expression might just be really simple like the use of a constant value.
  //It may be important to know this when analysing the IR.
  private boolean declaredAsConstant = false;

  /**
   * Create a new expression based around an existing symbol.
   */
  public ExpressionSymbol(ISymbol symbol) {
    super(symbol.getName());

    super.setGenus(symbol.getGenus());
    //Also it will be the same type and category.
    setType(symbol.getType());
    setCategory(symbol.getCategory());

    //Need to clone over the initialised and constant ness
    setSourceToken(symbol.getSourceToken());
    setDeclaredAsConstant(symbol.isDeclaredAsConstant());

  }

  public ExpressionSymbol(String name) {
    super(name);
    super.setGenus(SymbolGenus.VALUE);
  }

  @Override
  public ExpressionSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoExpressionSymbol(new ExpressionSymbol(this.getName()));
  }

  protected ExpressionSymbol cloneIntoExpressionSymbol(ExpressionSymbol newCopy) {
    super.cloneIntoSymbol(newCopy);

    newCopy.setPromotionRequired(isPromotionRequired());
    newCopy.setUseStringOperator(isUseStringOperator());

    newCopy.setSourceToken(getSourceToken());
    newCopy.setDeclaredAsConstant(isDeclaredAsConstant());

    return newCopy;
  }

  @Override
  public String getFriendlyName() {
    StringBuilder buffer = new StringBuilder();

    getType().ifPresentOrElse(theType -> buffer.append(theType.getFriendlyName()),
        () -> buffer.append("Unknown"));

    buffer.append(" <- ").append(super.getName());

    return buffer.toString();
  }

  @Override
  public boolean isDeclaredAsConstant() {
    return declaredAsConstant;
  }

  public void setDeclaredAsConstant(boolean declaredAsConstant) {
    this.declaredAsConstant = declaredAsConstant;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  @Override
  public void setNotMutable() {
    throw new CompilerException(
        "Compiler error: Base ExpressionSymbol does not support mutability");
  }

  public boolean isPromotionRequired() {
    return promotionRequired;
  }

  public void setPromotionRequired(boolean promotionRequired) {
    this.promotionRequired = promotionRequired;
  }

  public boolean isUseStringOperator() {
    return useStringOperator;
  }

  public void setUseStringOperator(boolean useStringOperator) {
    this.useStringOperator = useStringOperator;
  }

  @Override
  public void setSourceToken(IToken sourceToken) {
    //This is also where this expression is initialised.
    //If the items making the expression have not been initialised that has to be checked
    //when the expression gets created.
    super.setSourceToken(sourceToken);
    setInitialisedBy(sourceToken);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof ExpressionSymbol that)
        && super.equals(o)
        && isPromotionRequired() == that.isPromotionRequired()
        && isUseStringOperator() == that.isUseStringOperator()
        && isDeclaredAsConstant() == that.isDeclaredAsConstant();
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (isPromotionRequired() ? 1 : 0);
    result = 31 * result + (isUseStringOperator() ? 1 : 0);
    result = 31 * result + (isDeclaredAsConstant() ? 1 : 0);
    return result;
  }
}
