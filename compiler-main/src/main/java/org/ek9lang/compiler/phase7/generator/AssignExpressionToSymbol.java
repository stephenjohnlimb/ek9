package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ShouldRegisterVariableInScope;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

final class AssignExpressionToSymbol extends AbstractGenerator
    implements BiFunction<ISymbol, EK9Parser.AssignmentExpressionContext, List<IRInstr>> {

  private final ShouldRegisterVariableInScope shouldRegisterVariableInScope = new ShouldRegisterVariableInScope();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final Function<String, List<IRInstr>> assignmentGenerator;
  private final boolean release;
  private final String scopeId;
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  AssignExpressionToSymbol(final IRGenerationContext stackContext,
                           final boolean release,
                           final Function<String, List<IRInstr>> assignmentGenerator,
                           final String scopeId) {
    super(stackContext);
    this.assignmentGenerator = assignmentGenerator;
    this.release = release;
    this.scopeId = scopeId;
  }

  @Override
  public List<IRInstr> apply(final ISymbol lhsSymbol,
                             final EK9Parser.AssignmentExpressionContext ctx) {

    AssertValue.checkNotNull("ISymbol cannot be null", lhsSymbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var lhsVariableName = variableNameForIR.apply(lhsSymbol);
    final var rhsExprSymbol = getRecordedSymbolOrException(ctx);
    final var rhsExprDebugInfo = debugInfoCreator.apply(rhsExprSymbol.getSourceToken());

    final var rhsResult = stackContext.generateTempName();
    final var basicDetails = new BasicDetails(scopeId, rhsExprDebugInfo);
    final var rhsVariableDetails = new VariableDetails(rhsResult, basicDetails);

    final var instructions = variableMemoryManagement
        .apply(() -> assignmentGenerator.apply(rhsResult), rhsVariableDetails);

    //Now before we can assign - we may need to release (depending on use).
    if (release) {
      instructions.add(MemoryInstr.release(lhsVariableName, rhsExprDebugInfo));
    }

    instructions.add(MemoryInstr.store(lhsVariableName, rhsResult, rhsExprDebugInfo));
    if (lhsSymbol.isPropertyField() || lhsSymbol.isReturningParameter()) {
      instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
    } else if (shouldRegisterVariableInScope.test(scopeId)) {
      final var lhsVariableDetails = new VariableDetails(lhsVariableName, basicDetails);
      variableMemoryManagement.apply(() -> instructions, lhsVariableDetails);
    }

    return instructions;
  }
}
