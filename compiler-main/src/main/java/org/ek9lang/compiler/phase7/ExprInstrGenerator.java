package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.phase7.support.ConstructorCallProcessor;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.FunctionCallProcessor;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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
 * <p>
 * In time I'll probably need to break this up a bit more.
 * </p>
 */
final class ExprInstrGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final ObjectAccessInstrGenerator objectAccessCreator;
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final ShortCircuitAndGenerator shortCircuitAndGenerator;
  private final ShortCircuitOrGenerator shortCircuitOrGenerator;
  private final QuestionBlockGenerator questionBlockGenerator;
  private final UnaryOperationGenerator unaryOperationGenerator;
  private final BinaryOperationGenerator binaryOperationGenerator;
  private final ConstructorCallProcessor constructorCallProcessor;
  private final FunctionCallProcessor functionCallProcessor;

  ExprInstrGenerator(final IRContext context) {
    super(context);

    final RecordExprProcessing recordExprProcessing = new RecordExprProcessing(this::process);

    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
    this.shortCircuitAndGenerator = new ShortCircuitAndGenerator(context, recordExprProcessing);
    this.shortCircuitOrGenerator = new ShortCircuitOrGenerator(context, recordExprProcessing);
    this.questionBlockGenerator = new QuestionBlockGenerator(context, this::process);
    this.unaryOperationGenerator = new UnaryOperationGeneratorWithProcessor(context, this::process);
    this.binaryOperationGenerator = new BinaryOperationGeneratorWithProcessor(context, this::process);
    this.constructorCallProcessor = new ConstructorCallProcessor(context);
    this.functionCallProcessor = new FunctionCallProcessor(context);
  }

  /**
   * Generate IR instructions for expression.
   */
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    AssertValue.checkNotNull("Details cannot be null", details);

    return process(details);

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
    } else if (details.ctx().objectAccessExpression() != null) {
      return processObjectAccessExpression(details);
    }

    return processControlsOrStructures(details);

  }

  private List<IRInstr> processOperation(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    final var ctx = details.ctx();
    //Now while you may think these can just call the 'method' that is defined for the operator
    //There cases where 'pre-checks' and 'short circuits need to be applied.
    if (ctx.op.getType() == EK9Parser.QUESTION) {
      instructions.addAll(questionBlockGenerator.apply(details));
    } else if (ctx.op.getType() == EK9Parser.AND) {
      instructions.addAll(processAndExpression(details));
    } else if (ctx.op.getType() == EK9Parser.OR) {
      instructions.addAll(processOrExpression(details));
    } else {
      instructions.addAll(processGeneralOpExpression(details));
    }
    return instructions;
  }

  private Collection<? extends IRInstr> processGeneralOpExpression(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();
    if (details.ctx().expression().size() == 1) {
      //Looks like a unary operation, so delegate.
      instructions.addAll(unaryOperationGenerator.apply(details));
    } else {
      //Binary operation so delegate.
      instructions.addAll(binaryOperationGenerator.apply(details));
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
    final var exprResult = details.variableDetails().resultVariable();
    final var scopeId = details.variableDetails().basicDetails().scopeId();
    final var debugInfo = details.variableDetails().basicDetails().debugInfo();

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.primary().literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.primary().literal(), exprResult, scopeId));
    } else if (ctx.primary().identifierReference() != null) {
      instructions.addAll(processIdentifierReference(ctx.primary().identifierReference(), exprResult, debugInfo));
    } else if (ctx.primary().expression() != null && !ctx.primary().expression().isEmpty()) {
      instructions.addAll(
          process(new ExprProcessingDetails(ctx.primary().expression(), details.variableDetails())));
    } else if (ctx.primary().primaryReference() != null) {
      AssertValue.fail("PrimaryReference not yet Implemented");
    } else {
      AssertValue.fail("Unexpected path.");
    }

    return instructions;
  }

  private List<IRInstr> processCall(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var callContext = ctx.call();
    
    // Get the resolved symbol for the call
    final var callSymbol = getRecordedSymbolOrException(callContext);
    
    if (callSymbol instanceof CallSymbol resolvedCallSymbol) {
      final var toBeCalled = resolvedCallSymbol.getResolvedSymbolToCall();
      
      if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
        // Constructor calls: Use constructor call processor (no memory management for expression context)
        final var instructions = new ArrayList<IRInstr>();
        constructorCallProcessor.processConstructorCall(
            resolvedCallSymbol,
            callContext,
            details.variableDetails().resultVariable(),
            instructions,
            details.variableDetails().basicDetails().scopeId(),
            this::process,  // Expression processor function
            false           // No memory management for expression context
        );
        return instructions;
      } else if (toBeCalled instanceof org.ek9lang.compiler.symbols.FunctionSymbol) {
        // Function calls: Use unified function call processor with promotion support
        final var callProcessingDetails = org.ek9lang.compiler.phase7.support.CallProcessingDetails
            .forExpression(callContext, details.variableDetails());
        return functionCallProcessor.apply(callProcessingDetails, this::process);
      } else {
        throw new CompilerException("Unsupported call type: "
            + toBeCalled.getClass().getSimpleName() + " - " + toBeCalled);
      }
    } else {
      throw new org.ek9lang.core.CompilerException("Expected CallSymbol, but got: " + callSymbol.getClass().getSimpleName());
    }
  }

  private List<IRInstr> processObjectAccessExpression(final ExprProcessingDetails details) {
    return new ArrayList<>(objectAccessCreator
        .apply(details.ctx().objectAccessExpression(), details.variableDetails()));
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

    final var literalSymbol = getRecordedSymbolOrException(ctx);

    final var literalGenerator = new LiteralGenerator(context);
    return new ArrayList<>(literalGenerator.apply(new LiteralProcessingDetails(literalSymbol, rhsExprResult, scopeId)));
  }

  /**
   * Process identifier references using resolved symbol information.
   */
  private List<IRInstr> processIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx,
                                                   final String rhsExprResult, final DebugInfo debugInfo) {
    final var instructions = new ArrayList<IRInstr>();

    final var identifierSymbol = getRecordedSymbolOrException(ctx);

    // Load the variable using its resolved name (could be decorated for generic contexts)
    final var variableName = variableNameForIR.apply(identifierSymbol);

    instructions.add(MemoryInstr.load(rhsExprResult, variableName, debugInfo));

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