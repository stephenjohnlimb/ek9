package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.Field;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;

/**
 * Used for double dispatch when visiting nodes.
 */
public interface INodeVisitor {

  /**
   * Entry point for visitors.
   */
  default void visit() {

  }

  default void visit(final IRConstruct construct) {
  }

  default void visit(final Field field) {
  }

  default void visit(final OperationInstr operation) {
  }

  // New IR instruction visitor methods
  default void visit(final BasicBlockInstr basicBlock) {
  }

  default void visit(final IRInstr irInstruction) {
  }

}
