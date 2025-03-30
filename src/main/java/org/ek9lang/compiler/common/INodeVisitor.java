package org.ek9lang.compiler.common;

import org.ek9lang.compiler.ir.Argument;
import org.ek9lang.compiler.ir.Assignment;
import org.ek9lang.compiler.ir.Block;
import org.ek9lang.compiler.ir.Call;
import org.ek9lang.compiler.ir.ChainedAccess;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.compiler.ir.ConstructorCall;
import org.ek9lang.compiler.ir.Expression;
import org.ek9lang.compiler.ir.Instructions;
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

  default void visit(final Argument argument) {
  }

  default void visit(final Assignment assignment) {
  }

  default void visit(final Block block) {
  }

  default void visit(final Call call) {
  }

  default void visit(final ChainedAccess chainedAccess) {
  }

  default void visit(final Construct construct) {
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

}
