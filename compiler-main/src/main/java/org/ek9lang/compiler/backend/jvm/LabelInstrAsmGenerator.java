package org.ek9lang.compiler.backend.jvm;

import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized ASM generator for LabelInstr processing.
 * Handles LABEL operations - marks instruction positions as branch targets
 * for control flow operations like BRANCH_TRUE, BRANCH_FALSE, and BRANCH.
 */
public final class LabelInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<LabelInstr> {

  public LabelInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                final OutputVisitor outputVisitor,
                                final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM label placement for a label instruction.
   * Uses the label cache in MethodContext to ensure the same JVM Label object
   * is used for both label definition and branch targets.
   */
  @Override
  public void accept(final LabelInstr labelInstr) {
    AssertValue.checkNotNull("LabelInstr cannot be null", labelInstr);

    // Generate debug info if available
    labelInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    final var labelName = labelInstr.getLabelName();
    if (labelName == null || labelName.isEmpty()) {
      throw new CompilerException("LABEL instruction requires label name");
    }

    // Get or create the JVM label (reused by branch instructions)
    final Label jvmLabel = getOrCreateLabel(labelName);

    // Place the label at this instruction position
    getCurrentMethodVisitor().visitLabel(jvmLabel);
  }
}
