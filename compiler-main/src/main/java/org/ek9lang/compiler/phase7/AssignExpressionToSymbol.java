package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

final class AssignExpressionToSymbol extends AbstractGenerator
    implements BiFunction<ISymbol, EK9Parser.AssignmentExpressionContext, List<IRInstr>> {

  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final String scopeId;
  private final boolean referenceAndRelease;

  AssignExpressionToSymbol(final IRContext context, final boolean referenceAndRelease, final String scopeId) {
    super(context);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);
    this.scopeId = scopeId;
    this.referenceAndRelease = referenceAndRelease;
  }

  @Override
  public List<IRInstr> apply(final ISymbol lhsSymbol,
                             final EK9Parser.AssignmentExpressionContext ctx) {

    AssertValue.checkNotNull("ISymbol cannot be null", lhsSymbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var lhsTypeName = typeNameOrException.apply(lhsSymbol);
    final var lhsVariableName = variableNameForIR.apply(lhsSymbol);
    final var lhsDebugInfo = debugInfoCreator.apply(lhsSymbol);
    final var assignmentExprInstrGenerator = new AssignmentExprInstrGenerator(context, scopeId);

    final var instructions = new ArrayList<IRInstr>();
    if (referenceAndRelease) {
      instructions.add(MemoryInstr.reference(lhsVariableName, lhsTypeName, lhsDebugInfo));
      instructions.add(MemoryInstr.release(lhsVariableName, lhsDebugInfo));
    }
    final var rhsResult = context.generateTempName();
    instructions.addAll(assignmentExprInstrGenerator.apply(ctx, rhsResult));

    instructions.add(MemoryInstr.store(lhsVariableName, rhsResult, lhsDebugInfo));
    instructions.add(MemoryInstr.retain(lhsVariableName, lhsDebugInfo));

    return instructions;
  }
}
