package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.CompilerException;

/**
 * Designed to capture the ASM specifics for byte code generation.
 * //TODO consider using this or other classes to generate byte code.
 */
final class AsmStructureCreator {


  private final ConstructTargetTuple constructTargetTuple;

  AsmStructureCreator(final ConstructTargetTuple constructTargetTuple, final INodeVisitor visitor) {

    this.constructTargetTuple = constructTargetTuple;
  }

  void processClass() {

    if (constructTargetTuple.construct().isProgram()) {
      processProgram();
      return;
    }
    throw new CompilerException("Constructs other than program not yet supported");
  }

  private void processProgram() {
    //Actually generate the ASM code from the IR.
  }


  byte[] getByteCode() {
    return new byte[0];
  }
}
