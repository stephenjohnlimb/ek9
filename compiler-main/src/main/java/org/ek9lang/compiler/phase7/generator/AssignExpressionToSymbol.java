package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

final class AssignExpressionToSymbol extends AbstractGenerator
    implements BiFunction<ISymbol, EK9Parser.AssignmentExpressionContext, List<IRInstr>> {

  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final Function<String, List<IRInstr>> assignmentGenerator;
  private final boolean release;
  private final VariableMemoryManagement variableMemoryManagement;

  AssignExpressionToSymbol(final IRGenerationContext stackContext,
                           final VariableMemoryManagement variableMemoryManagement,
                           final boolean release,
                           final Function<String, List<IRInstr>> assignmentGenerator) {
    super(stackContext);
    this.assignmentGenerator = assignmentGenerator;
    this.release = release;
    this.variableMemoryManagement = variableMemoryManagement;
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
    final var rhsVariableDetails = new VariableDetails(rhsResult, rhsExprDebugInfo);

    final var instructions = variableMemoryManagement
        .apply(() -> assignmentGenerator.apply(rhsResult), rhsVariableDetails);

    //Now before we can assign - we may need to release (depending on use).
    if (release) {
      instructions.add(MemoryInstr.release(lhsVariableName, rhsExprDebugInfo));
    }

    instructions.add(MemoryInstr.store(lhsVariableName, rhsResult, rhsExprDebugInfo));

    // ARC FIX: Use 'release' parameter to distinguish declaration from reassignment.
    // Declaration (release=false): RETAIN + SCOPE_REGISTER (variable ownership established)
    // Reassignment (release=true): RETAIN only (variable already registered to declaring scope)
    // CRITICAL: Fields are owned by the object, not the scope, so they get RETAIN but NO SCOPE_REGISTER.
    // See EK9_ARC_MEMORY_MANAGEMENT.md for comprehensive ARC documentation.
    if (!release) {
      if (lhsSymbol.isPropertyField()) {
        // Field declaration: RETAIN only (field lifetime managed by object, not scope)
        // Fields are released when the object is destroyed, not when i_init exits
        instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
      } else {
        // Local variable declaration: RETAIN + SCOPE_REGISTER (scope owns the variable)
        final var lhsVariableDetails = new VariableDetails(lhsVariableName, rhsExprDebugInfo);
        variableMemoryManagement.apply(() -> instructions, lhsVariableDetails);
      }
    } else {
      // Reassignment: Just RETAIN, variable already registered to declaring scope
      instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
    }

    return instructions;
  }
}
