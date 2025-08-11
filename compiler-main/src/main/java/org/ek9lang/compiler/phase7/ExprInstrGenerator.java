package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LiteralInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for expressions.
 * <p>
 * This is the real backbone of processing and is very big! It is also recursive calling upon
 * expression (sometimes directly and sometimes directly.<br>
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
final class ExprInstrGenerator {

  private final IRContext context;
  private final ObjectAccessInstrGenerator objectAccessCreator;
  private final DebugInfoCreator debugInfoCreator;

  ExprInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);

    this.context = context;
    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for expression.
   */
  public List<IRInstr> apply(final EK9Parser.ExpressionContext ctx,
                             final String resultVar,
                             final String scopeId) {
    AssertValue.checkNotNull("ExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("resultVar cannot be null", resultVar);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();

    // Handle postfix question operator: expression?
    if (ctx.op != null && "?".equals(ctx.op.getText())) {
      instructions.addAll(processQuestionOperator(ctx, resultVar, scopeId));
    } else if (ctx.call() != null) {

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
          final var callDetails = new CallDetails(typeName, typeName, "<init>",
              parameterTypes, typeName, List.of());

          instructions.add(CallInstr.constructor(resultVar, debugInfo, callDetails));

          // Add memory management for LLVM targets (no-ops on JVM)
          instructions.add(MemoryInstr.retain(resultVar, debugInfo));
          instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));
        }
      }
    } else if (ctx.objectAccessExpression() != null) {
      instructions.addAll(objectAccessCreator.apply(ctx.objectAccessExpression(), resultVar, scopeId));
    } else if (ctx.primary() != null) {
      // Handle primary expressions: literals, identifier references, parenthesized expressions
      instructions.addAll(processPrimaryExpression(ctx.primary(), resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Process primary expressions using symbol-driven approach.
   * Primary expressions include: literals, identifier references, parenthesized expressions.
   */
  private List<IRInstr> processPrimaryExpression(final EK9Parser.PrimaryContext ctx,
                                                 final String resultVar,
                                                 final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    if (ctx.literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.literal(), resultVar, scopeId));
    } else if (ctx.identifierReference() != null) {
      // Handle identifier references: variable names
      instructions.addAll(processIdentifierReference(ctx.identifierReference(), resultVar, scopeId));
    } else if (ctx.expression() != null) {
      // Handle parenthesized expressions: (expression)
      instructions.addAll(apply(ctx.expression(), resultVar, scopeId));
    }
    // primaryReference (THIS, SUPER) would be handled here too if needed

    return instructions;
  }

  /**
   * Process literal expressions using resolved symbol information.
   * This ensures we get correct type information, including decorated names for generic types.
   */
  private List<IRInstr> processLiteral(final EK9Parser.LiteralContext ctx,
                                       final String resultVar,
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
    instructions.add(LiteralInstr.literal(resultVar, literalValue, literalType, debugInfo));

    // Add memory management for EK9 object reference counting (consistent with constructor calls)
    instructions.add(MemoryInstr.retain(resultVar, debugInfo));
    instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));

    return instructions;
  }

  /**
   * Process identifier references using resolved symbol information.
   */
  private List<IRInstr> processIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx,
                                                   final String resultVar,
                                                   final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the resolved symbol for this identifier - phases 1-6 ensure all identifiers are resolved
    final var identifierSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Identifier symbol should be resolved by phases 1-6", identifierSymbol);

    // Load the variable using its resolved name (could be decorated for generic contexts)
    final var variableName = identifierSymbol.getName();

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(identifierSymbol);

    instructions.add(MemoryInstr.load(resultVar, variableName, debugInfo));

    return instructions;
  }

  /**
   * Process question operator: expression?
   * Generates _isSet() method call on the expression result.
   */
  private List<IRInstr> processQuestionOperator(final EK9Parser.ExpressionContext ctx,
                                                final String resultVar,
                                                final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Evaluate the expression that the ? operator is applied to
    final var tempExprResult = context.generateTempName();

    // ctx.expression(0) gets the first (and only) child expression for postfix operators
    if (!ctx.expression().isEmpty()) {
      instructions.addAll(apply(ctx.expression(0), tempExprResult, scopeId));
    }

    // Call _isSet() method on the expression result
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    // Generate method call: resultVar = tempExprResult._isSet()
    final var callDetails = new CallDetails(tempExprResult, "org.ek9.lang::Any",
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());

    instructions.add(CallInstr.operator(resultVar, debugInfo, callDetails));

    return instructions;
  }

}