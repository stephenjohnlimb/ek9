package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.ShouldRegisterVariableInScope;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

final class AssignExpressionToSymbol extends AbstractGenerator
    implements BiFunction<ISymbol, EK9Parser.AssignmentExpressionContext, List<IRInstr>> {

  private final ShouldRegisterVariableInScope shouldRegisterVariableInScope = new ShouldRegisterVariableInScope();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final Function<String, List<IRInstr>> assignmentGenerator;
  private final boolean referenceAndRelease;
  private final String scopeId;

  AssignExpressionToSymbol(final IRContext context,
                           final boolean referenceAndRelease,
                           final Function<String, List<IRInstr>> assignmentGenerator,
                           final String scopeId) {
    super(context);
    this.assignmentGenerator = assignmentGenerator;
    this.referenceAndRelease = referenceAndRelease;
    this.scopeId = scopeId;
  }

  @Override
  public List<IRInstr> apply(final ISymbol lhsSymbol,
                             final EK9Parser.AssignmentExpressionContext ctx) {

    AssertValue.checkNotNull("ISymbol cannot be null", lhsSymbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var lhsVariableName = variableNameForIR.apply(lhsSymbol);
    final var rhsExprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var rhsExprDebugInfo = debugInfoCreator.apply(rhsExprSymbol.getSourceToken());
    final var rhsResult = context.generateTempName();

    final var basicDetails = new BasicDetails(scopeId, rhsExprDebugInfo);
    final var instructions = new ArrayList<>(assignmentGenerator.apply(rhsResult));
    instructions.add(MemoryInstr.retain(rhsResult, rhsExprDebugInfo));
    instructions.add(ScopeInstr.register(rhsResult, basicDetails));

    //Now before we can assign - we may need to release (depending on use).
    if (referenceAndRelease) {
      instructions.add(MemoryInstr.release(lhsVariableName, rhsExprDebugInfo));
    }

    instructions.add(MemoryInstr.store(lhsVariableName, rhsResult, rhsExprDebugInfo));
    if (lhsSymbol.isPropertyField() || lhsSymbol.isReturningParameter()) {
      instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
    } else if (shouldRegisterVariableInScope.test(scopeId)) {
      instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
      instructions.add(ScopeInstr.register(lhsVariableName, basicDetails));
    }


    return instructions;
  }
}
