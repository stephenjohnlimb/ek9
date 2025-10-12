package org.ek9lang.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.symbols.AnyTypeSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.Test;

/**
 * Just tests the INodeVisitor NO-OP functionality.
 * Creates a Construct IR Node and uses the visitor to visit that.
 */
class NoOpNodeVisitorIRTest {
  //Because the interface has all default implementations it is possible to do this.
  final INodeVisitor underTest = new INodeVisitor() {
  };

  @Test
  void testEmptyConstruct() {
    final var global = new SymbolTable();
    final var any = new AnyTypeSymbol("Any", global);
    final var construct = new IRConstruct(any, "test.ek9");

    assertNotNull(construct.toString());
    underTest.visit(construct);

  }

}
