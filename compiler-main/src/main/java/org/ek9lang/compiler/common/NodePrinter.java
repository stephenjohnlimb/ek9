package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.Assignment;
import org.ek9lang.compiler.ir.BasicBlock;
import org.ek9lang.compiler.ir.ChainedAccess;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.Instructions;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.VariableDecl;

/**
 * Just to enable viewing and printing of nodes.
 * There will be other visitors including those for code generation.
 * But this is focussed on enabling the viewing of node structures to ensure
 * the right structures are being created.
 */
public class NodePrinter implements INodeVisitor {

  @Override
  public void visit(final IRConstruct construct) {

    System.out.printf("Construct: %s%n", construct.getFullyQualifiedName());
    construct.getOperations().forEach(operation -> operation.accept(this));
  }

  @Override
  public void visit(final Operation operation) {
    System.out.printf("Start Method: %s %s %s%n", operation.getReturn().getReturnType(), operation.getOperationName(),
        operation.getParameters());

    // Using BasicBlock IR
    operation.getBody().accept(this);

    System.out.printf("End Method: %s %s %s%n", operation.getReturn().getReturnType(), operation.getOperationName(),
        operation.getParameters());
  }

  @Override
  public void visit(final Instructions instructions) {
    instructions.getItems().forEach(item -> item.accept(this));
  }

  @Override
  public void visit(final VariableDecl variableDecl) {
    System.out.printf("Variable Declaration: %s%n", variableDecl.toString());
  }

  @Override
  public void visit(final Assignment assignment) {
    System.out.printf("Assignment Declaration: %s%n", assignment.toString());
  }

  @Override
  public void visit(final ChainedAccess chainedAccess) {
    System.out.printf("ChainedAccess Declaration: %s%n", chainedAccess.toString());
  }

  // New IR visitor methods
  @Override
  public void visit(final BasicBlock basicBlock) {
    System.out.printf("BasicBlock: %s%n", basicBlock.getLabel());
    for (IRInstruction instruction : basicBlock.getInstructions()) {
      System.out.printf("  %s%n", instruction);
    }

    if (!basicBlock.getSuccessors().isEmpty()) {
      System.out.printf("  Successors: %s%n",
          basicBlock.getSuccessors().stream()
              .map(BasicBlock::getLabel)
              .toList());
    }
  }

  @Override
  public void visit(final IRInstruction irInstruction) {
    System.out.printf("IRInstruction: %s%n", irInstruction);
  }
}
