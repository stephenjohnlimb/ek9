package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.core.AssertValue;

/**
 * Pulls out the common code for variable and variable-only declarations. DRY.
 */
abstract class AbstractVariableDeclGenerator extends AbstractGenerator {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

  AbstractVariableDeclGenerator(final IRContext context) {
    super(context);
  }

  public List<IRInstr> getDeclInstrs(final ParseTree ctx,
                                     final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    final var variableSymbol = getRecordedSymbolOrException(ctx);
    final var instructions = new ArrayList<IRInstr>();

    final var variableName = variableNameForIR.apply(variableSymbol);
    final var variableTypeName = typeNameOrException.apply(variableSymbol);
    final var variableDebugInfo = debugInfoCreator.apply(variableSymbol.getSourceToken());

    instructions.add(MemoryInstr.reference(variableName, variableTypeName, variableDebugInfo));

    return instructions;
  }
}
