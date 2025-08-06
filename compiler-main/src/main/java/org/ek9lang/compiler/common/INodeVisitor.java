package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.Argument;
import org.ek9lang.compiler.ir.Assignment;
import org.ek9lang.compiler.ir.BasicBlock;
import org.ek9lang.compiler.ir.Call;
import org.ek9lang.compiler.ir.ChainedAccess;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.ConstructorCall;
import org.ek9lang.compiler.ir.Expression;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.Instructions;
import org.ek9lang.compiler.ir.Literal;
import org.ek9lang.compiler.ir.Marker;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.Parameter;
import org.ek9lang.compiler.ir.Return;
import org.ek9lang.compiler.ir.Statement;
import org.ek9lang.compiler.ir.VariableDecl;
import org.ek9lang.compiler.ir.VariableRef;

/**
 * Used for double dispatch when visiting nodes.
 */
public interface INodeVisitor {

  /**
   * Entry point for visitors.
   */
  default void visit() {

  }

  default void visit(final Literal literal) {
  }

  default void visit(final Argument argument) {
  }

  default void visit(final Assignment assignment) {
  }

  default void visit(final Call call) {
  }

  default void visit(final ChainedAccess chainedAccess) {
  }

  default void visit(final IRConstruct construct) {
  }

  default void visit(final ConstructorCall constructorCall) {
  }

  default void visit(final Expression expression) {
  }

  default void visit(final Instructions instructions) {
  }

  default void visit(final Marker marker) {
  }

  default void visit(final Operation operation) {
  }

  default void visit(final Parameter parameter) {
  }

  default void visit(final Return returnValue) {
  }

  default void visit(final Statement statement) {
  }

  default void visit(final VariableDecl variableDecl) {
  }

  default void visit(final VariableRef variableRef) {
  }

  // New IR instruction visitor methods
  default void visit(final BasicBlock basicBlock) {
  }

  default void visit(final IRInstruction irInstruction) {
  }

}
