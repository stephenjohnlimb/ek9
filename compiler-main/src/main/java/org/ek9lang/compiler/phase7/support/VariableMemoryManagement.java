package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;

/**
 * Calls a supplier of main processing instructions and then adds in the memory management.
 * STACK-BASED: Uses IRGenerationContext to get current scope ID from stack.
 */
public final class VariableMemoryManagement
    implements BiFunction<Supplier<List<IRInstr>>, VariableDetails, List<IRInstr>> {

  private final IRGenerationContext stackContext;

  public VariableMemoryManagement(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
    this.stackContext = stackContext;
  }

  @Override
  public List<IRInstr> apply(final Supplier<List<IRInstr>> mainProcessing, final VariableDetails target) {

    final var instructions = mainProcessing.get();
    instructions.add(MemoryInstr.retain(target.resultVariable(), target.debugInfo()));
    instructions.add(
        ScopeInstr.register(target.resultVariable(), stackContext.currentScopeId(), target.debugInfo()));

    return instructions;
  }
}
