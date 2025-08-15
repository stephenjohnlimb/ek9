package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.core.AssertValue;

final class AssertStmtGenerator extends AbstractGenerator
    implements BiFunction<EK9Parser.AssertStatementContext, String, List<IRInstr>> {


  AssertStmtGenerator(final IRContext context) {
    super(context);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssertStatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    final var expressionGenerator = new ExprInstrGenerator(context, ctx.expression(), scopeId);
    // Evaluate the assert expression
    final var rhsExprResult = context.generateTempName();
    final var instructions = new ArrayList<>(expressionGenerator.apply(rhsExprResult));

    // Call the _true() method to get primitive boolean (true if set AND true)
    final var rhsResult = context.generateTempName();
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx.expression());
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    final var booleanTypeName = getEk9BooleanFullyQualifiedName();
    final var callDetails = new CallDetails(rhsExprResult, booleanTypeName, IRConstants.TRUE_METHOD,
        List.of(), IRConstants.BOOLEAN, List.of());

    instructions.add(CallInstr.call(rhsResult, debugInfo, callDetails));

    // Assert on the primitive boolean result
    instructions.add(BranchInstr.assertValue(rhsResult, debugInfo));

    return instructions;
  }
}
