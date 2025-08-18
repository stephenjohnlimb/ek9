package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.CallDetailsForTrue;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.core.AssertValue;

final class AssertStmtGenerator extends AbstractGenerator
    implements BiFunction<EK9Parser.AssertStatementContext, String, List<IRInstr>> {

  private final CallDetailsForTrue callDetailsForTrue = new CallDetailsForTrue();

  AssertStmtGenerator(final IRContext context) {
    super(context);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssertStatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    final var expressionGenerator = new ExprInstrGenerator(context, ctx.expression(), scopeId);
    // Evaluate the assert expression

    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx.expression());
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    final var rhsExprResult = context.generateTempName();
    //Get the instructions for what we are to assert.
    final var instructions = new ArrayList<>(expressionGenerator.apply(rhsExprResult));

    //We will get an EK9 Boolean back from this, so need to manage it
    instructions.add(MemoryInstr.retain(rhsExprResult, debugInfo));
    instructions.add(ScopeInstr.register(rhsExprResult, scopeId, debugInfo));

    // Call the _true() method to get primitive boolean (true if set AND true)
    final var rhsResult = context.generateTempName();
    final var callDetails = callDetailsForTrue.apply(rhsExprResult);
    instructions.add(CallInstr.call(rhsResult, debugInfo, callDetails));

    // Assert on the primitive boolean result - back-end will then implement that.
    instructions.add(BranchInstr.assertValue(rhsResult, debugInfo));

    return instructions;
  }
}
