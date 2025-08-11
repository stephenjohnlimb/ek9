package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.Field;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.Operation;

/**
 * Just to enable viewing and printing of nodes.
 * There will be other visitors including those for code generation.
 * But this is focussed on enabling the viewing of node structures to ensure
 * the right structures are being created.
 */
public class NodePrinter implements INodeVisitor {

  private final SymbolSignatureExtractor symbolSignatureGenerator = new SymbolSignatureExtractor();

  @Override
  public void visit(final IRConstruct construct) {

    System.out.printf("Construct: %s%n", construct.getSignatureQualifiedName());
    
    // Print field declarations first
    construct.getFields().forEach(field -> field.accept(this));
    
    // Then print operations
    construct.getOperations().forEach(operation -> operation.accept(this));
  }

  @Override
  public void visit(final Field field) {
    final var debugLocation = field.getDebugInfo() != null ? field.getDebugInfo() : "";
    System.out.printf("Field: %s, %s  %s%n", field.getName(), field.getTypeName(), debugLocation);
  }

  @Override
  public void visit(final Operation operation) {

    final var symbol = operation.getSymbol();
    final var debugLocation = operation.getDebugInfo() != null ? operation.getDebugInfo() : "";
    final var operationSignature = symbolSignatureGenerator.apply(symbol);
    System.out.printf("Operation: %s  %s%n", operationSignature, debugLocation);
    // Using BasicBlock IR
    operation.getBody().accept(this);

  }

  // New IR visitor methods
  @Override
  public void visit(final BasicBlockInstr basicBlock) {
    System.out.printf("BasicBlock: %s%n", basicBlock.getLabel());
    for (IRInstr instruction : basicBlock.getInstructions()) {
      instruction.accept(this);
    }

    if (!basicBlock.getSuccessors().isEmpty()) {
      System.out.printf("  Successors: %s%n",
          basicBlock.getSuccessors().stream()
              .map(BasicBlockInstr::getLabel)
              .toList());
    }
  }

  @Override
  public void visit(final IRInstr irInstruction) {
    System.out.printf("IRInstruction: %s%n", irInstruction);
  }
}
