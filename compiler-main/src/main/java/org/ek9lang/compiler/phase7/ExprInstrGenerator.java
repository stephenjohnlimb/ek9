package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LiteralInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
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


  ExprInstrGenerator(final IRContext context,
                     final EK9Parser.ExpressionContext ctx,
                     final String initialScopeId) {
    super(context);
    AssertValue.checkNotNull("ExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("initialScopeId cannot be null", initialScopeId);
    this.antlrCtx = ctx;
    this.initialScopeId = initialScopeId;
    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
  }

  /**
   * Generate IR instructions for expression.
   */
  public List<IRInstr> apply(final String rhsExprResult) {
    AssertValue.checkNotNull("RhsExprResult cannot be null", rhsExprResult);

    //Now while it seems a bit pointless just to call another method here.
    //Actually some of the methods call back with new deeper contexts.
    //So this allows for recursion.
    return process(antlrCtx, rhsExprResult, initialScopeId);

  }

  private List<IRInstr> process(final EK9Parser.ExpressionContext ctx,
                                final String rhsExprResult,
                                final String scopeId) {

    //The idea here is that rather than have a giant 'if else' combo, the process is grouped.
    //This is just like earlier phases.

    if (ctx.op != null) {
      return processOperation(ctx, rhsExprResult, scopeId);
    } else if (ctx.coalescing != null) {
      return processCoalescing(ctx, rhsExprResult, scopeId);
    } else if (ctx.coalescing_equality != null) {
      return processCoalescingEquality(ctx, rhsExprResult, scopeId);
    } else if (ctx.primary() != null) {
      return processPrimary(ctx, rhsExprResult, scopeId);
    } else if (ctx.call() != null) {
      return processCall(ctx, rhsExprResult, scopeId);
    } else if (antlrCtx.objectAccessExpression() != null) {
      return processObjectAccessExpression(ctx, rhsExprResult, scopeId);
    }

    return processControlsOrStructures(ctx, rhsExprResult, scopeId);

  }

  private List<IRInstr> processOperation(final EK9Parser.ExpressionContext ctx,
                                         final String rhsExprResult,
                                         final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    //TODO you may find there is a more automated way to do this.
    //TODO The symbol and operator map may give a way to workout the Calldetails needed.

    // Handle postfix question operator: expression?
    if (ctx.op.getType() == EK9Parser.QUESTION) {
      instructions.addAll(processQuestionOperator(ctx, rhsExprResult, scopeId));
    } else {
      AssertValue.fail("Other Operations not yet implemented: " + ctx.op.getText());
    }
    return instructions;
  }

  private List<IRInstr> processCoalescing(final EK9Parser.ExpressionContext ctx,
                                          final String rhsExprResult,
                                          final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();
    AssertValue.fail("ProcessCoalescing not yet implemented");
    return instructions;
  }

  private List<IRInstr> processCoalescingEquality(final EK9Parser.ExpressionContext ctx,
                                                  final String rhsExprResult,
                                                  final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();
    AssertValue.fail("ProcessCoalescingEquality not yet implemented");
    return instructions;
  }

  /**
   * Process primary expressions using symbol-driven approach.
   * Primary expressions include: literals, identifier references, parenthesized expressions.
   */
  private List<IRInstr> processPrimary(final EK9Parser.ExpressionContext ctx,
                                       final String rhsExprResult,
                                       final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    if (ctx.primary().literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.primary().literal(), rhsExprResult, scopeId));
    } else if (ctx.primary().identifierReference() != null) {
      // Handle identifier references: variable names
      instructions.addAll(processIdentifierReference(ctx.primary().identifierReference(), rhsExprResult));
    } else if (ctx.expression() != null) {
      // Handle parenthesized expressions: (expression)
      //TODO I think I need a new temp variable here, but not sure.
      instructions.addAll(process(ctx, rhsExprResult, scopeId));
    } else {
      AssertValue.fail("PrimaryReference not yes Implemented");
    }

    return instructions;
  }

  private List<IRInstr> processCall(final EK9Parser.ExpressionContext ctx,
                                    final String rhsExprResult,
                                    final String scopeId) {

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

        instructions.add(CallInstr.constructor(rhsExprResult, debugInfo, callDetails));

        // Add memory management for LLVM targets (no-ops on JVM)
        instructions.add(MemoryInstr.retain(rhsExprResult, debugInfo));
        instructions.add(ScopeInstr.register(rhsExprResult, scopeId, debugInfo));
      } else {
        AssertValue.fail("Expecting method to have been resolved");
      }
    }
    return instructions;
  }

  private List<IRInstr> processObjectAccessExpression(final EK9Parser.ExpressionContext ctx,
                                                      final String rhsExprResult,
                                                      final String scopeId) {
    return new ArrayList<>(objectAccessCreator.apply(ctx.objectAccessExpression(), rhsExprResult, scopeId));

  }

  private List<IRInstr> processControlsOrStructures(final EK9Parser.ExpressionContext ctx,
                                                    final String rhsExprResult,
                                                    final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();
    AssertValue.fail("processControlsOrStructures not implemented");
    return instructions;
  }

  /**
   * Process literal expressions using resolved symbol information.
   * This ensures we get correct type information, including decorated names for generic types.
   */
  private List<IRInstr> processLiteral(final EK9Parser.LiteralContext ctx,
                                       final String rhsExprResult,
                                       final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the resolved symbol for this literal - phases 1-6 ensure all literals have resolved types
    final var literalSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Literal symbol should be resolved by phases 1-6", literalSymbol);

    // Get the type from the resolved symbol (could be decorated for generic contexts)
    final var literalType = literalSymbol.getType()
        .map(ISymbol::getFullyQualifiedName)
        .orElseThrow(() -> new RuntimeException("Literal should have resolved type by phase 7"));

    final var literalValue = literalSymbol.getName();

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(literalSymbol);

    // Create literal instruction with resolved type information
    instructions.add(LiteralInstr.literal(rhsExprResult, literalValue, literalType, debugInfo));

    // Add memory management for EK9 object reference counting (consistent with constructor calls)
    instructions.add(MemoryInstr.retain(rhsExprResult, debugInfo));
    instructions.add(ScopeInstr.register(rhsExprResult, scopeId, debugInfo));

    return instructions;
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
  private List<IRInstr> processQuestionOperator(final EK9Parser.ExpressionContext ctx,
                                                final String rhsExprResult,
                                                final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Evaluate the expression that the ? operator is applied to
    final var tempExprResult = context.generateTempName();

    // ctx.expression(0) gets the first (and only) child expression for postfix operators
    if (!ctx.expression().isEmpty()) {
      instructions.addAll(process(ctx.expression(0), tempExprResult, scopeId));
    }

    // Call _isSet() method on the expression result
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    final var typeName = typeNameOrException.apply(exprSymbol);
    final var methodName = operatorMap.getForward(ctx.op.getText());

    // Generate method call: rhsExprResult = tempExprResult._isSet()
    //TODO if there is only one expression then there are no arguments
    //For operators there is only ever zero or one argument and we can get the types
    //So I think this could be much more general.
    //Then we can use the SymbolSearch to get the Method and its return type.
    final var callDetails = new CallDetails(tempExprResult, typeName,
        methodName, List.of(), "org.ek9.lang::Boolean", List.of());

    instructions.add(CallInstr.operator(rhsExprResult, debugInfo, callDetails));

    return instructions;
  }

}