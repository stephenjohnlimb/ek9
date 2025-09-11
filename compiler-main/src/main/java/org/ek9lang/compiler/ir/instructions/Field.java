package org.ek9lang.compiler.ir.instructions;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Represents a field/property declaration in an EK9 construct (class, record, component, etc.).
 * Note that this also include 'capture' variables in dynamic classes and dynamic functions.
 * <p>
 * Fields are structural declarations that define the data members of a construct,
 * separate from the behavioral operations (methods). They map directly to:
 * - JVM: Field declarations in .class files
 * - LLVM: Member declarations in struct types
 * </p>
 * <p>
 * Unlike REFERENCE instructions which declare runtime variable storage,
 * Field IR represents compile-time structural metadata about the construct's layout.
 * </p>
 */
public final class Field implements INode {

  private final ISymbol symbol;
  private final String name;
  private final String typeName;
  private final DebugInfo debugInfo;

  /**
   * Create a new Field IR node.
   *
   * @param symbol    The VariableSymbol representing this property field
   * @param name      The field name (e.g., "aField")
   * @param typeName  The fully qualified type name (e.g., "org.ek9.lang::String")
   * @param debugInfo Source location information
   */
  public Field(final ISymbol symbol, final String name, final String typeName, final DebugInfo debugInfo) {
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    AssertValue.checkNotNull("Field name cannot be null", name);
    AssertValue.checkNotNull("Type name cannot be null", typeName);

    this.symbol = symbol;
    this.name = name;
    this.typeName = typeName;
    this.debugInfo = debugInfo;
  }

  /**
   * Get the symbol representing this field.
   *
   * @return The underlying VariableSymbol
   */
  public ISymbol getSymbol() {
    return symbol;
  }

  /**
   * Get the field name.
   *
   * @return The field name (e.g., "aField")
   */
  public String getName() {
    return name;
  }

  /**
   * Get the fully qualified type name.
   *
   * @return The type name (e.g., "org.ek9.lang::String")
   */
  public String getTypeName() {
    return typeName;
  }

  /**
   * Get debug information for this field.
   *
   * @return Debug info containing source location, or null if not available
   */
  public DebugInfo getDebugInfo() {
    return debugInfo;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Field{"
        + "name='" + name + '\''
        + ", typeName='" + typeName + '\''
        + ", symbol=" + symbol
        + '}';
  }
}