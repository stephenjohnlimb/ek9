package org.ek9lang.compiler.ir.instructions;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.common.SymbolSignatureExtractor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;

/**
 * In EK9 every construct becomes one of these nodes.
 * So that means class, trait, component, program, function - everything.
 * <p>
 * In the case of functions there is a single 'call' method, this has all the
 * parameters that would be supplied when other constructs use the function.
 * </p>
 * <p>
 * So this is more like a Function in Java - that is really just an object.
 * The reason for this is that in EK9 it is possible for functions to 'capture' variables
 * as properties (dynamic functions). But this whole mechanism makes sense even for normal
 * fixed named functions. It also means it is very easy to pass functions around as data.
 * </p>
 */
public final class IRConstruct implements INode {

  private final ISymbol symbol;
  private final List<Field> fields = new ArrayList<>();
  private final List<OperationInstr> operations = new ArrayList<>();

  private final SymbolSignatureExtractor symbolSignatureExtractor = new SymbolSignatureExtractor();

  public IRConstruct(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;

  }

  public boolean isForSymbol(final ISymbol toCheck) {
    //Has to be for the actual same object.
    return symbol == toCheck;
  }

  public String getFullyQualifiedName() {
    return symbol.getFullyQualifiedName();
  }

  /**
   * Returns signature-qualified name including parameter types and return type.
   * For functions and methods, this includes the complete signature to enable
   * method overloading resolution and target code generation.
   * <p>
   * Format: "module::construct(org.ek9.lang::ParamType1,org.ek9.lang::ParamType2)->org.ek9.lang::ReturnType"
   * </p>
   *
   * @return Signature-qualified name for overload resolution.
   */
  public String getSignatureQualifiedName() {
    return symbolSignatureExtractor.apply(symbol);
  }

  /**
   * Assess if this construct is a function or a general aggregate symbol type.
   *
   * @return true if the construct is just a function - otherwise false and it is an aggregate.
   */
  public boolean isFunction() {
    return symbol.isFunction();
  }

  public boolean isProgram() {
    return symbol.getGenus() == SymbolGenus.PROGRAM;
  }

  public void addField(final Field field) {
    AssertValue.checkNotNull("Field cannot be null", field);
    fields.add(field);
  }

  public void add(final OperationInstr operation) {
    AssertValue.checkNotNull("OperationInstr cannot be null", operation);
    operations.add(operation);
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  public List<Field> getFields() {
    return List.copyOf(fields);
  }

  public List<OperationInstr> getOperations() {
    return List.copyOf(operations);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Construct{" +
        "symbol=" + symbol +
        ", fields=" + fields +
        ", operations=" + operations +
        '}';
  }
}
