package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.Assignment;
import org.ek9lang.compiler.ir.Block;
import org.ek9lang.compiler.ir.ChainedAccess;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.compiler.ir.Instructions;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.VariableDecl;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Just to enable viewing and printing of nodes.
 * There will be other visitors including those for code generation.
 * But this is focussed on enabling the viewing of node structures to ensure
 * the right structures are being created.
 */
public class NodePrinter implements INodeVisitor {

  @Override
  public void visit(final Construct construct) {
    //TODO pretty print just the details of the construct
    if (construct.getSymbol().getGenus() == SymbolGenus.PROGRAM) {
      System.out.println("Program: would create static main entry point and call to _main");
    }

    System.out.printf("Construct: %s\n", construct.getFullyQualifiedName());
    construct.getOperations().forEach(operation -> operation.accept(this));
  }

  @Override
  public void visit(final Operation operation) {
    System.out.printf("Start Method: %s %s %s\n", operation.getReturn().getReturnType(), operation.getOperationName(),
        operation.getParameters());

    operation.getBody().accept(this);

    System.out.printf("End Method: %s %s %s\n", operation.getReturn().getReturnType(), operation.getOperationName(),
        operation.getParameters());
  }

  @Override
  public void visit(final Block block) {
    System.out.printf("Start Block Marker: %s\n", block.getStart());
    block.getItems().forEach(item -> item.accept(this));
    System.out.printf("End Block Marker: %s\n", block.getEnd());
  }

  @Override
  public void visit(final Instructions instructions) {
    instructions.getItems().forEach(item -> item.accept(this));
  }

  @Override
  public void visit(final VariableDecl variableDecl) {
    System.out.printf("Variable Declaration: %s\n", variableDecl.toString());
  }

  @Override
  public void visit(final Assignment assignment) {
    System.out.printf("Assignment Declaration: %s\n", assignment.toString());
  }

  @Override
  public void visit(final ChainedAccess chainedAccess) {
    System.out.printf("ChainedAccess Declaration: %s\n", chainedAccess.toString());
  }
}
