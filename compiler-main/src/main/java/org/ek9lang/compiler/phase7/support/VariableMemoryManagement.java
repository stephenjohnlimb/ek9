package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;

/**
 * Calls a supplier of main processing instructions and then adds in the memory management.
 */
public final class VariableMemoryManagement
    implements BiFunction<Supplier<List<IRInstr>>, VariableDetails, List<IRInstr>> {

  @Override
  public List<IRInstr> apply(final Supplier<List<IRInstr>> mainProcessing, final VariableDetails target) {

    final var instructions = mainProcessing.get();
    instructions.add(MemoryInstr.retain(target.resultVariable(), target.basicDetails().debugInfo()));
    instructions.add(ScopeInstr.register(target.resultVariable(), target.basicDetails()));

    return instructions;
  }
}
