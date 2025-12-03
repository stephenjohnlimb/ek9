package org.ek9lang.compiler.common;

import java.io.PrintWriter;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.Field;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;

/**
 * Just to enable viewing and printing of nodes.
 * There will be other visitors including those for code generation.
 * But this is focussed on enabling the viewing of node structures to ensure
 * the right structures are being created.
 */
public class NodePrinter implements INodeVisitor {

  private final PrintWriter printWriter;
  private final SymbolSignatureExtractor symbolSignatureGenerator = new SymbolSignatureExtractor();

  public NodePrinter(final PrintWriter printWriter) {
    this.printWriter = printWriter;
  }

  @Override
  public void visit(final IRConstruct construct) {

    printWriter.printf("ConstructDfn: %s%n", construct.getSignatureQualifiedName());

    // Print program entry point first (if present)
    construct.getProgramEntryPoint().ifPresent(programEntryPoint -> programEntryPoint.accept(this));

    // Print field declarations
    construct.getFields().forEach(field -> field.accept(this));

    // Then print operations
    construct.getOperations().forEach(operation -> operation.accept(this));
  }

  @Override
  public void visit(final Field field) {
    final var debugLocation = field.getDebugInfo() != null ? field.getDebugInfo() : "";
    printWriter.printf("Field: %s, %s  %s%n", field.getName(), field.getTypeName(), debugLocation);
  }

  @Override
  public void visit(final OperationInstr operation) {

    final var symbol = operation.getSymbol();
    final var debugLocation = operation.getDebugInfo() != null ? operation.getDebugInfo() : "";
    final var operationSignature = symbolSignatureGenerator.apply(symbol);
    printWriter.printf("OperationDfn: %s  %s%n", operationSignature, debugLocation);
    // Using BasicBlock IR - abstract operations have no body
    final var body = operation.getBody();
    if (body != null) {
      body.accept(this);
    }

  }

  // New IR visitor methods
  @Override
  public void visit(final BasicBlockInstr basicBlock) {
    printWriter.printf("BasicBlock: %s%n", basicBlock.getLabel());
    for (IRInstr instruction : basicBlock.getInstructions()) {
      instruction.accept(this);
    }

    if (!basicBlock.getSuccessors().isEmpty()) {
      printWriter.printf("  Successors: %s%n",
          basicBlock.getSuccessors().stream()
              .map(BasicBlockInstr::getLabel)
              .toList());
    }
  }

  @Override
  public void visit(final IRInstr irInstruction) {
    printWriter.printf("%s%n", irInstruction);
  }

  /**
   * Visit ProgramEntryPointInstr to display the program entry point block.
   */
  public void visit(final ProgramEntryPointInstr programEntryPoint) {
    printWriter.printf("%s%n", programEntryPoint);
  }
}
