package org.ek9lang.compiler.backend.jvm;

import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized ASM generator for ScopeInstr processing.
 * Handles SCOPE_ENTER, SCOPE_EXIT, and SCOPE_REGISTER operations.
 * <p>
 * While JVM doesn't need explicit scope management for memory (GC handles that),
 * scopes are critical for:
 * - LocalVariableTable generation (debug information)
 * - Variable lifetime tracking for debuggers
 * - Proper variable visibility in nested blocks (if/else/switch/for/try)
 * </p>
 */
public final class ScopeInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<ScopeInstr> {

  public ScopeInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                final OutputVisitor outputVisitor,
                                final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a scope operation instruction.
   * Uses ScopeInstr opcode to determine the specific operation.
   */
  @Override
  public void accept(final ScopeInstr scopeInstr) {
    AssertValue.checkNotNull("ScopeInstr cannot be null", scopeInstr);

    switch (scopeInstr.getOpcode()) {
      case SCOPE_ENTER -> handleScopeEnter(scopeInstr);
      case SCOPE_EXIT -> handleScopeExit(scopeInstr);
      case SCOPE_REGISTER -> handleScopeRegister(scopeInstr);
      default -> throw new IllegalArgumentException("Unsupported scope opcode: " + scopeInstr.getOpcode());
    }
  }

  /**
   * Handle SCOPE_ENTER instruction: Mark the start of a variable scope.
   * Creates a label at this bytecode position to mark where variables become valid.
   */
  private void handleScopeEnter(final ScopeInstr scopeInstr) {
    final String scopeId = scopeInstr.getScopeId();
    if (scopeId == null) {
      throw new IllegalArgumentException("SCOPE_ENTER requires scope ID");
    }

    // Create a new scope info if it doesn't exist
    final var scopeInfo = getMethodContext().scopeMap.computeIfAbsent(scopeId, ScopeInfo::new);

    // Create and visit a label to mark scope start
    final Label startLabel = new Label();
    scopeInfo.startLabel = startLabel;
    getCurrentMethodVisitor().visitLabel(startLabel);

    // Note: We do NOT generate debug info (line numbers) for scope labels
    // Scopes are structural markers for LocalVariableTable, not executable instructions
  }

  /**
   * Handle SCOPE_EXIT instruction: Mark the end of a variable scope.
   * Creates a label at this bytecode position to mark where variables become invalid.
   */
  private void handleScopeExit(final ScopeInstr scopeInstr) {
    final String scopeId = scopeInstr.getScopeId();
    if (scopeId == null) {
      throw new IllegalArgumentException("SCOPE_EXIT requires scope ID");
    }

    // SCOPE_EXIT must find existing scope from SCOPE_ENTER
    final var scopeInfo = getMethodContext().scopeMap.get(scopeId);
    if (scopeInfo == null) {
      throw new org.ek9lang.core.CompilerException(
          "SCOPE_EXIT for scope '" + scopeId + "' without matching SCOPE_ENTER. "
          + "This indicates a bug in IR generation.");
    }

    // Create and visit a label to mark scope end
    final Label endLabel = new Label();
    scopeInfo.endLabel = endLabel;
    getCurrentMethodVisitor().visitLabel(endLabel);

    // Note: We do NOT generate debug info (line numbers) for scope labels
    // Scopes are structural markers for LocalVariableTable, not executable instructions
  }

  /**
   * Handle SCOPE_REGISTER instruction: Associate a variable with a scope.
   * Updates the variable's scope ID in its metadata for later LocalVariableTable generation.
   * <p>
   * SCOPE_REGISTER happens when a variable actually references an object (after STORE),
   * not at REFERENCE time (variable declaration). This associates the variable with the
   * scope for LocalVariableTable generation.
   * </p>
   */
  private void handleScopeRegister(final ScopeInstr scopeInstr) {
    final String objectName = scopeInstr.getObject();
    final String scopeId = scopeInstr.getScopeId();

    if (objectName == null || scopeId == null) {
      throw new IllegalArgumentException("SCOPE_REGISTER requires object name and scope ID");
    }

    // Update variable metadata with scope information
    // This links the variable to its scope for LocalVariableTable generation
    final var varInfo = getMethodContext().localVariableMetadata.get(objectName);
    if (varInfo != null) {
      // Variable metadata exists - create new LocalVariableInfo with scope ID
      // LocalVariableInfo is immutable, so we need to create a new instance
      final var updatedVarInfo = new AbstractAsmGenerator.LocalVariableInfo(
          varInfo.name,
          varInfo.typeDescriptor,
          scopeId
      );
      // Update slot from original varInfo if it was already set
      updatedVarInfo.slot = varInfo.slot;
      getMethodContext().localVariableMetadata.put(objectName, updatedVarInfo);
    }

    // Note: We do NOT generate debug info (line numbers) for SCOPE_REGISTER
    // This is a metadata instruction that doesn't correspond to executable code
  }
}
