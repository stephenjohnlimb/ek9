package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.ShortCircuitAndGenerator;
import org.ek9lang.compiler.phase7.support.ShortCircuitOrGenerator;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for expressions.
 * <p>
 * This is the real backbone of processing and is very big! It is also recursive calling upon
 * expression (sometimes directly and sometimes indirectly).<br>
 * The ANTLR grammar follows:
 * </p>
 * <pre>
 *   expression
 *     : expression op=(INC | DEC | BANG)
 *     | op=SUB expression
 *     | expression op=QUESTION
 *     | op=TOJSON expression
 *     | op=DOLLAR expression
 *     | op=PROMOTE expression
 *     | op=LENGTH OF? expression
 *     | op=PREFIX expression
 *     | op=SUFFIX expression
 *     | op=HASHCODE expression
 *     | op=ABS OF? expression
 *     | op=SQRT OF? expression
 *     | &lt;assoc=right&gt; left=expression coalescing=(CHECK | ELVIS) right=expression
 *     | primary
 *     | call
 *     | objectAccessExpression
 *     | list
 *     | dict
 *     | expression IS? neg=NOT? op=EMPTY
 *     | op=(NOT | TILDE) expression
 *     | left=expression op=CARET right=expression
 *     | left=expression op=(DIV | MUL | MOD | REM ) NL? right=expression
 *     | left=expression op=(ADD | SUB) NL? right=expression
 *     | left=expression op=(SHFTL | SHFTR) right=expression
 *     | left=expression op=(CMP | FUZ) NL? right=expression
 *     | left=expression op=(LE | GE | GT | LT) NL? right=expression
 *     | left=expression op=(EQUAL | NOTEQUAL | NOTEQUAL2) NL? right=expression
 *     | &lt;assoc=right&gt; left=expression coalescing_equality=(COALESCE_LE | COALESCE_GE | COALESCE_GT | COALESCE_LT) right=expression
 *     | left=expression neg=NOT? op=MATCHES right=expression
 *     | left=expression neg=NOT? op=CONTAINS right=expression
 *     | left=expression IS? neg=NOT? IN right=expression
 *     | left=expression op=(AND | XOR | OR) NL? right=expression
 *     | expression IS? neg=NOT? IN range
 *     | &lt;assoc=right&gt; control=expression LEFT_ARROW ternaryPart ternary=(COLON|ELSE) ternaryPart
 *     ;
 * </pre>
 */
final class ExprInstrGenerator extends AbstractGenerator {

  private static final OperatorMap operatorMap = new OperatorMap();

  private final EK9Parser.ExpressionContext antlrCtx;
  private final String initialScopeId;
  private final ObjectAccessInstrGenerator objectAccessCreator;
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final RecordExprProcessing recordExprProcessing;
  private final ShortCircuitAndGenerator shortCircuitAndGenerator;
  private final ShortCircuitOrGenerator shortCircuitOrGenerator;

  ExprInstrGenerator(final IRContext context,
                     final EK9Parser.ExpressionContext ctx,
                     final String initialScopeId) {
    super(context);
    AssertValue.checkNotNull("ExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("initialScopeId cannot be null", initialScopeId);
    this.antlrCtx = ctx;
    this.initialScopeId = initialScopeId;
    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
    this.recordExprProcessing = new RecordExprProcessing(this::process);
    this.shortCircuitAndGenerator = new ShortCircuitAndGenerator(context, recordExprProcessing);
    this.shortCircuitOrGenerator = new ShortCircuitOrGenerator(context, recordExprProcessing);
  }

  /**
   * Generate IR instructions for expression.
   */
  public List<IRInstr> apply(final String rhsExprResult) {
    AssertValue.checkNotNull("RhsExprResult cannot be null", rhsExprResult);

    //Now while it seems a bit pointless just to call another method here.
    //Actually some of the methods call back with new deeper contexts.
    //So this allows for recursion.
    return process(new ExprProcessingDetails(antlrCtx, rhsExprResult, initialScopeId, null));

  }

  private List<IRInstr> process(final ExprProcessingDetails details) {

    //The idea here is that rather than have a giant 'if else' combo, the process is grouped.
    //This is just like earlier phases.
    final var ctx = details.ctx();
    if (ctx.op != null) {
      return processOperation(details);
    } else if (ctx.coalescing != null) {
      return processCoalescing(details);
    } else if (ctx.coalescing_equality != null) {
      return processCoalescingEquality(details);
    } else if (ctx.primary() != null) {
      return processPrimary(details);
    } else if (ctx.call() != null) {
      return processCall(details);
    } else if (antlrCtx.objectAccessExpression() != null) {
      return processObjectAccessExpression(details);
    }

    return processControlsOrStructures(details);

  }

  private List<IRInstr> processOperation(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    //TODO you may find there is a more automated way to do this.
    //TODO The symbol and operator map may give a way to workout the Calldetails needed.
    final var ctx = details.ctx();
    // Handle postfix question operator: expression?
    if (ctx.op.getType() == EK9Parser.QUESTION) {
      instructions.addAll(processQuestionOperator(details));
    } else if (ctx.op.getType() == EK9Parser.AND) {
      instructions.addAll(processAndExpression(details));
    } else if (ctx.op.getType() == EK9Parser.OR) {
      instructions.addAll(processOrExpression(details));
    } else {
      AssertValue.fail("Other Operations not yet implemented: " + ctx.op.getText());
    }
    return instructions;
  }

  private List<IRInstr> processCoalescing(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();
    AssertValue.fail("ProcessCoalescing not yet implemented");
    return instructions;
  }

  private List<IRInstr> processCoalescingEquality(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();
    AssertValue.fail("ProcessCoalescingEquality not yet implemented");
    return instructions;
  }

  /**
   * Process primary expressions using symbol-driven approach.
   * Primary expressions include: literals, identifier references, parenthesized expressions.
   */
  private List<IRInstr> processPrimary(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.exprResult();
    final var scopeId = details.scopeId();
    final var debugInfo = details.debugInfo();

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.primary().literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.primary().literal(), exprResult, scopeId));
    } else if (ctx.primary().identifierReference() != null) {
      instructions.addAll(processIdentifierReference(ctx.primary().identifierReference(), exprResult));
    } else if (ctx.primary().expression() != null && !ctx.primary().expression().isEmpty()) {
      instructions.addAll(
          process(new ExprProcessingDetails(ctx.primary().expression(), exprResult, scopeId, debugInfo)));
    } else if (ctx.primary().primaryReference() != null) {
      AssertValue.fail("PrimaryReference not yet Implemented");
    } else {
      AssertValue.fail("Unexpected path.");
    }

    return instructions;
  }

  private List<IRInstr> processCall(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.exprResult();
    final var scopeId = details.scopeId();

    final var instructions = new ArrayList<IRInstr>();

    // Get the resolved symbol for the call
    final var callSymbol = context.getParsedModule().getRecordedSymbol(ctx.call());

    if (callSymbol instanceof CallSymbol resolvedCallSymbol) {
      final var toBeCalled = resolvedCallSymbol.getResolvedSymbolToCall();

      if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
        // This is a constructor call
        final var parentScope = methodSymbol.getParentScope();
        final var typeName =
            (parentScope instanceof Symbol symbol) ? symbol.getFullyQualifiedName() : parentScope.toString();

        // Extract debug info if debugging instrumentation is enabled
        final var debugInfo = debugInfoCreator.apply(callSymbol);

        // Extract parameter types from constructor parameters
        final var parameterTypes = methodSymbol.getCallParameters().stream()
            .map(param -> param.getType().map(ISymbol::getFullyQualifiedName).orElse("org.ek9.lang::Any"))
            .toList();

        // Generate constructor call using actual resolved type name with complete type information
        final var callDetails = new CallDetails(typeName, typeName, IRConstants.INIT_METHOD,
            parameterTypes, typeName, List.of());

        instructions.add(CallInstr.constructor(exprResult, debugInfo, callDetails));

        // Add memory management for LLVM targets (no-ops on JVM)
        instructions.add(MemoryInstr.retain(exprResult, debugInfo));
        instructions.add(ScopeInstr.register(exprResult, scopeId, debugInfo));
      } else {
        AssertValue.fail("Expecting method to have been resolved");
      }
    }
    return instructions;
  }

  private List<IRInstr> processObjectAccessExpression(final ExprProcessingDetails details) {
    return new ArrayList<>(objectAccessCreator
        .apply(details.ctx().objectAccessExpression(), details.exprResult(), details.scopeId()));

  }

  private List<IRInstr> processControlsOrStructures(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    AssertValue.fail("processControlsOrStructures: Unsupported expression pattern");


    return instructions;
  }

  /**
   * Process literal expressions using resolved symbol information.
   * This ensures we get correct type information, including decorated names for generic types.
   */
  private List<IRInstr> processLiteral(final EK9Parser.LiteralContext ctx,
                                       final String rhsExprResult,
                                       final String scopeId) {

    // Get the resolved symbol for this literal - phases 1-6 ensure all literals have resolved types
    final var literalSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Literal symbol should be resolved by phases 1-6", literalSymbol);

    final var literalGenerator = new LiteralGenerator(context);
    return new ArrayList<>(literalGenerator.apply(new LiteralProcessingDetails(literalSymbol, rhsExprResult, scopeId)));
  }

  /**
   * Process identifier references using resolved symbol information.
   */
  private List<IRInstr> processIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx,
                                                   final String rhsExprResult) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the resolved symbol for this identifier - phases 1-6 ensure all identifiers are resolved
    final var identifierSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Identifier symbol should be resolved by phases 1-6", identifierSymbol);

    // Load the variable using its resolved name (could be decorated for generic contexts)
    final var variableName = variableNameForIR.apply(identifierSymbol);

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(identifierSymbol);

    instructions.add(MemoryInstr.load(rhsExprResult, variableName, debugInfo));

    return instructions;
  }

  /**
   * Process question operator: expression?
   * Generates _isSet() method call on the expression result.
   */
  private List<IRInstr> processQuestionOperator(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.exprResult();
    final var scopeId = details.scopeId();

    AssertValue.checkFalse("Must have expression present", ctx.expression().isEmpty());

    // Get debug information
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    // Evaluate the expression that the ? operator is applied to
    final var tempExprResult = context.generateTempName();

    final var instructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.expression(0), tempExprResult, scopeId, debugInfo)));

    // Call _isSet() method on the expression result
    final var typeName = typeNameOrException.apply(exprSymbol);
    final var methodName = operatorMap.getForward(ctx.op.getText());

    // Generate method call: exprResult = tempExprResult._isSet()
    //TODO if there is only one expression then there are no arguments
    //For operators there is only ever zero or one argument and we can get the types
    //So I think this could be much more general.
    //Then we can use the SymbolSearch to get the Method and its return type.
    final var callDetails = new CallDetails(tempExprResult, typeName,
        methodName, List.of(), "org.ek9.lang::Boolean", List.of());

    instructions.add(CallInstr.operator(exprResult, debugInfo, callDetails));

    return instructions;
  }

  /**
   * Process AND expression using high-level short-circuit instruction.
   * Pattern: left and right
   * Short-circuit: if left is false, result is false without evaluating right
   * Uses ShortCircuitInstr for backend-appropriate lowering.
   */
  private List<IRInstr> processAndExpression(final ExprProcessingDetails details) {
    return shortCircuitAndGenerator.apply(details);
  }

  /**
   * Process OR expression using high-level short-circuit instruction.
   * Pattern: left or right
   * Short-circuit: if left is true, result is true without evaluating right
   * Uses ShortCircuitInstr for backend-appropriate lowering.
   */
  private List<IRInstr> processOrExpression(final ExprProcessingDetails details) {
    return shortCircuitOrGenerator.apply(details);
  }

}