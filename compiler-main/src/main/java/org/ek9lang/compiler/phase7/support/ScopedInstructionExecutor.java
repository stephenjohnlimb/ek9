package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;

/**
 * CONCERN: Temporary scope lifecycle management.
 * RESPONSIBILITY: Execute instructions within auto-cleanup scope.
 * REUSABILITY: ALL generators (33+ usages across codebase).
 * <p>
 * Encapsulates the repetitive pattern:
 * 1. Generate scope ID
 * 2. Push scope onto stack (enterScope)
 * 3. Add SCOPE_ENTER instruction
 * 4. Execute instructions
 * 5. Add SCOPE_EXIT instruction
 * 6. Pop scope from stack (exitScope)
 * </p>
 * <p>
 * This ensures consistent scope lifecycle management across all generators,
 * eliminating bugs from manual scope handling and reducing code duplication.
 * </p>
 */
public final class ScopedInstructionExecutor {
  private final IRGenerationContext stackContext;

  public ScopedInstructionExecutor(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
  }

  /**
   * Execute instructions within temporary scope.
   * <p>
   * The scope is automatically created, pushed onto the stack, and cleaned up
   * after the instructions execute. This ensures proper memory management
   * through scope-based reference counting.
   * </p>
   *
   * @param instructionGenerator Supplier that generates the instructions to execute
   * @param debugInfo Debug information for scope instructions
   * @return Complete instruction list with SCOPE_ENTER, instructions, SCOPE_EXIT
   */
  public List<IRInstr> execute(
      final Supplier<List<IRInstr>> instructionGenerator,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);

    // Push scope onto stack
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);

    // Add SCOPE_ENTER instruction
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Execute user instructions
    instructions.addAll(instructionGenerator.get());

    // Add SCOPE_EXIT instruction
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));

    // Pop scope from stack
    stackContext.exitScope();

    return instructions;
  }
}
