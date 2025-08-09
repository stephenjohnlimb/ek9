package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.Operation;

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

  default void visit(final Operation operation) {
  }

  // New IR instruction visitor methods
  default void visit(final BasicBlockInstr basicBlock) {
  }

  default void visit(final IRInstr irInstruction) {
  }

}
