package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.core.AssertValue;

/**
 * Pulls out the common code for variable and variable-only declarations. DRY.
 */
abstract class AbstractVariableDeclGenerator extends AbstractGenerator {

  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

  AbstractVariableDeclGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  public List<IRInstr> getDeclInstrs(final ParseTree ctx) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var variableSymbol = getRecordedSymbolOrException(ctx);
    final var instructions = new ArrayList<IRInstr>();

    final var variableName = variableNameForIR.apply(variableSymbol);
    final var variableTypeName = typeNameOrException.apply(variableSymbol);
    final var variableDebugInfo = stackContext.createDebugInfo(variableSymbol.getSourceToken());

    instructions.add(MemoryInstr.reference(variableName, variableTypeName, variableDebugInfo));

    return instructions;
  }
}
