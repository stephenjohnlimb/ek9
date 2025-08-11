package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;

final class AssertStmtGenerator implements BiFunction<EK9Parser.AssertStatementContext, String, List<IRInstr>> {

  private final IRContext context;
  private final ExprInstrGenerator expressionGenerator;
  private final DebugInfoCreator debugInfoCreator;

  AssertStmtGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.expressionGenerator = new ExprInstrGenerator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssertStatementContext ctx, final String scopeId) {

    // Evaluate the assert expression
    final var tempExprResult = context.generateTempName();
    final var instructions = new ArrayList<>(expressionGenerator.apply(ctx.expression(), tempExprResult, scopeId));

    // Call the _true() method to get primitive boolean (true if set AND true)
    final var tempBoolResult = context.generateTempName();
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx.expression());
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    // Create method call with type information (_true() method on Boolean)
    final var booleanTypeName = context.getParsedModule().getEk9Types().ek9Boolean().getFullyQualifiedName();
    final var callDetails = new CallDetails(tempExprResult, booleanTypeName, "_true",
        List.of(), "boolean", List.of());

    instructions.add(CallInstr.call(tempBoolResult, debugInfo, callDetails));

    // Assert on the primitive boolean result
    instructions.add(BranchInstr.assertValue(tempBoolResult, debugInfo));

    return instructions;
  }
}
