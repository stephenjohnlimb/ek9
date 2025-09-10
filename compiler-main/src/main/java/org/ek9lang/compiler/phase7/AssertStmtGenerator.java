package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

final class AssertStmtGenerator extends AbstractGenerator
    implements BiFunction<EK9Parser.AssertStatementContext, String, List<IRInstr>> {

  private final CallDetailsForIsTrue callDetailsForTrue = new CallDetailsForIsTrue();

  AssertStmtGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssertStatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    //This deals with generating the instructions for the expression
    final var expressionGenerator = new ExprInstrGenerator(stackContext);
    //This deals with calling the above, but then retaining/recording the appropriate symbol for memory management
    final var processor = new RecordExprProcessing(expressionGenerator);

    final var assertStmtDebugInfo = debugInfoCreator.apply(new Ek9Token(ctx.ASSERT().getSymbol()));

    final var rhsExprResult = stackContext.generateTempName();

    final var exprDetails = new ExprProcessingDetails(ctx.expression(),
        new VariableDetails(rhsExprResult, new BasicDetails(scopeId, assertStmtDebugInfo)));

    final var instructions = new ArrayList<>(processor.apply(exprDetails));

    // Call the _true() method to get primitive boolean (true if set AND true)
    final var rhsResult = stackContext.generateTempName();
    final var callDetails = callDetailsForTrue.apply(rhsExprResult);
    instructions.add(CallInstr.call(rhsResult, assertStmtDebugInfo, callDetails));

    // Assert on the primitive boolean result - back-end will then implement that.
    instructions.add(BranchInstr.assertValue(rhsResult, assertStmtDebugInfo));

    return instructions;
  }
}
