package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallProcessingDetails;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.PrimaryReferenceProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
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
public final class ExprInstrGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final GeneratorSet generators;

  /**
   * Constructor accepting GeneratorSet for unified access to all generators.
   * Phase 3 Complete: Simplified from 10 parameters to 2.
   */
  ExprInstrGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
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
      instructions.addAll(generators.questionBlockGenerator.apply(details));
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
      instructions.addAll(generators.unaryOperationGenerator.apply(details));
    } else {
      //Binary operation so delegate.
      instructions.addAll(generators.binaryOperationGenerator.apply(details));
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
    final var debugInfo = details.variableDetails().debugInfo();

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.primary().literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.primary().literal(), exprResult));
    } else if (ctx.primary().identifierReference() != null) {
      instructions.addAll(processIdentifierReference(ctx.primary().identifierReference(), exprResult, debugInfo));
    } else if (ctx.primary().expression() != null && !ctx.primary().expression().isEmpty()) {
      instructions.addAll(
          process(new ExprProcessingDetails(ctx.primary().expression(), details.variableDetails())));
    } else if (ctx.primary().primaryReference() != null) {
      instructions.addAll(processPrimaryReference(ctx.primary().primaryReference(), exprResult));
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
        generators.constructorCallProcessor.processConstructorCall(
            resolvedCallSymbol,
            callContext,
            details.variableDetails().resultVariable(),
            instructions,
            this::process,  // Expression processor function
            false           // No memory management for expression context
        );
        return instructions;
      } else if (toBeCalled instanceof org.ek9lang.compiler.symbols.FunctionSymbol) {
        // Function calls: Use unified function call processor with promotion support
        final var callProcessingDetails = CallProcessingDetails
            .forExpression(callContext, details.variableDetails());
        return generators.functionCallProcessor.apply(callProcessingDetails, this::process);
      } else {
        throw new CompilerException("Unsupported call type: "
            + toBeCalled.getClass().getSimpleName() + " - " + toBeCalled);
      }
    } else {
      throw new CompilerException(
          "Expected CallSymbol, but got: " + callSymbol.getClass().getSimpleName());
    }
  }

  private List<IRInstr> processObjectAccessExpression(final ExprProcessingDetails details) {
    return new ArrayList<>(generators.objectAccessGenerator
        .apply(details.ctx().objectAccessExpression(), details.variableDetails()));
  }


  private List<IRInstr> processControlsOrStructures(final ExprProcessingDetails details) {
    final var ctx = details.ctx();

    if (ctx.list() != null) {
      return processList(details);
    } else if (ctx.dict() != null) {
      AssertValue.fail("Dict literals not yet implemented");
      return List.of();
    }

    AssertValue.fail("processControlsOrStructures: Unsupported expression pattern");
    return new ArrayList<>();
  }

  /**
   * Generate IR for list literal: ["elem1", "elem2", ...]
   * <p>
   * IR Pattern:
   * 1. CALL List&lt;T&gt;.&lt;init&gt;() → resultVariable
   * 2. RETAIN + SCOPE_REGISTER resultVariable
   * 3. For each element:
   * a. Evaluate expression → elementTemp
   * b. RETAIN + SCOPE_REGISTER elementTemp
   * c. CALL resultVariable._addAss(elementTemp)
   * </p>
   * <p>
   * Memory Management:
   * - List object retained and registered to current scope
   * - Each element retained and registered before _addAss call
   * - No LOAD/STORE between elements (direct operation on constructor result)
   * </p>
   */
  private List<IRInstr> processList(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();
    final var ctx = details.ctx();
    final var listContext = ctx.list();
    final var resultVariable = details.variableDetails().resultVariable();
    final var currentScopeId = stackContext.currentScopeId();

    // Get resolved list type from symbol table (includes decorated parameterized name)
    // List literals are represented as CallSymbols in phase 1, with the resolved
    // parameterized type (e.g., List<String>) set as the CallSymbol's TYPE in phase 3
    final var symbolFromContext = getRecordedSymbolOrException(listContext);
    final ISymbol listSymbol;

    if (symbolFromContext instanceof CallSymbol callSymbol) {
      // Extract the aggregate type from the CallSymbol's type field
      listSymbol = callSymbol.getType().orElseThrow(() ->
          new CompilerException("List literal has no resolved type"));
    } else {
      // Shouldn't happen for list literals, but handle gracefully
      listSymbol = symbolFromContext;
    }

    // Get fully qualified type name with module prefix (e.g., org.ek9.lang::_List_HASH)
    final var listTypeName = listSymbol.getFullyQualifiedName();

    // Debug info from list literal start token
    final var listDebugInfo = stackContext.createDebugInfo(listContext.getStart());

    // Step 1: Generate List<T> constructor call
    final var listConstructor = findDefaultConstructor(listSymbol);
    final var callMetaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    final var constructorMetaData = callMetaDataExtractor.apply(listConstructor);

    final var constructorCallDetails = new CallDetails(
        listTypeName,               // targetType (fully qualified: org.ek9.lang::_List_HASH)
        listTypeName,               // targetTypeName (fully qualified)
        IRConstants.INIT_METHOD,    // methodName: "<init>"
        List.of(),                  // parameterTypes (no params for default constructor)
        listTypeName,               // returnType (fully qualified)
        List.of(),                  // argumentVariables (no args)
        constructorMetaData,        // metaData (purity, complexity, effects)
        false                       // isMethod (false for constructors)
    );

    instructions.add(CallInstr.constructor(resultVariable, listDebugInfo, constructorCallDetails));

    // Step 2: Memory management for list object (must happen immediately after construction)
    instructions.add(MemoryInstr.retain(resultVariable, listDebugInfo));
    instructions.add(ScopeInstr.register(resultVariable, currentScopeId, listDebugInfo));

    // Step 3: Process each element expression
    final var expressions = listContext.expression();
    for (int i = 0; i < expressions.size(); i++) {
      final var exprCtx = expressions.get(i);

      // Generate temporary variable for element evaluation
      final var elementTemp = stackContext.generateTempName();
      final var elementDebugInfo = stackContext.createDebugInfo(exprCtx.getStart());
      final var elementDetails = new VariableDetails(elementTemp, elementDebugInfo);

      // Recursively evaluate element expression (handles literals, calls, etc.)
      final var elementProcessing = new ExprProcessingDetails(exprCtx, elementDetails);
      instructions.addAll(process(elementProcessing));

      // Get element type from resolved symbol
      final var elementSymbol = getRecordedSymbolOrException(exprCtx);
      final var elementType = elementSymbol.getType().orElseThrow(() ->
          new CompilerException("Element expression has no type: " + elementSymbol.getName()));
      final var elementTypeName = variableNameForIR.apply(elementType);

      // Memory management for element
      instructions.add(MemoryInstr.retain(elementTemp, elementDebugInfo));
      instructions.add(ScopeInstr.register(elementTemp, currentScopeId, elementDebugInfo));

      // Step 4: Generate _addAss call: list._addAss(element)
      // Use call resolution builder for proper method resolution with cost-based matching
      final var voidType = stackContext.getParsedModule().getEk9Types().ek9Void();
      final var callContext = CallContext.forBinaryOperation(
          listSymbol,      // Target type (List<String>)
          elementType,     // Argument type (String)
          voidType,        // Return type (Void for assignment operators)
          "_addAss",       // Method name
          resultVariable,  // Target variable (list instance)
          elementTemp,     // Argument variable (element)
          currentScopeId  // Current scope ID
      );

      // Use call details builder for method resolution and CallDetails creation
      final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

      // Add any promotion instructions generated by the builder
      instructions.addAll(callDetailsResult.allInstructions());

      // Add the _addAss operator call (returns Void, so no result variable)
      instructions.add(CallInstr.operator(null, elementDebugInfo, callDetailsResult.callDetails()));
    }

    // Result is the populated list in resultVariable
    return instructions;
  }

  /**
   * Find default (no-argument) constructor for aggregate type.
   *
   * @param typeSymbol The list type symbol (must be IAggregateSymbol)
   * @return The default constructor MethodSymbol
   * @throws CompilerException if not an aggregate or no default constructor found
   */
  private MethodSymbol findDefaultConstructor(final ISymbol typeSymbol) {
    if (!(typeSymbol instanceof IAggregateSymbol aggregate)) {
      throw new CompilerException("Expected aggregate type for list literal, got: "
          + typeSymbol.getClass().getSimpleName());
    }

    // Look for constructor with no parameters
    return aggregate.getConstructors().stream()
        .filter(ctor -> ctor.getCallParameters().isEmpty())
        .findFirst()
        .orElseThrow(() -> new CompilerException(
            "No default constructor found for list type: " + typeSymbol.getName()));
  }

  /**
   * Find _addAss(T) method on list type.
   * Uses name-based resolution to find the add-assignment operator method.
   *
   * @param listSymbol    The list type symbol
   * @param elementSymbol The element type symbol (for error messages)
   * @return The _addAss method symbol
   * @throws CompilerException if method not found
   */
  private MethodSymbol findAddAssMethod(final ISymbol listSymbol, final ISymbol elementSymbol) {
    if (!(listSymbol instanceof IAggregateSymbol aggregate)) {
      throw new CompilerException("Expected aggregate type for list, got: "
          + listSymbol.getClass().getSimpleName());
    }

    // Resolve _addAss method by name - operators are methods in EK9
    // For parameterized types, search all methods directly
    final var methods = aggregate.getAllMethods();
    for (var methodSymbol : methods) {
      if ("_addAss".equals(methodSymbol.getName())) {
        // Found a _addAss method - return the first one
        // (method resolution will handle parameter matching at call time)
        return methodSymbol;
      }
    }

    throw new CompilerException(
        "No _addAss method found on list type: " + listSymbol.getName()
            + " for element type: " + elementSymbol.getName());
  }

  /**
   * Process literal expressions using resolved symbol information.
   * This ensures we get correct type information, including decorated names for generic types.
   */
  private List<IRInstr> processLiteral(final EK9Parser.LiteralContext ctx,
                                       final String rhsExprResult) {

    final var literalSymbol = getRecordedSymbolOrException(ctx);

    final var literalGenerator = new LiteralGenerator(instructionBuilder);
    return new ArrayList<>(literalGenerator.apply(new LiteralProcessingDetails(literalSymbol, rhsExprResult)));
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
   * Process primary references (THIS and SUPER keywords).
   * <p>
   * MIGRATED TO STACK: PrimaryReferenceGenerator now creates debug info from stack context
   * instead of parameter threading. Method signature cleaned up to remove unused debugInfo parameter.
   * </p>
   */
  private List<IRInstr> processPrimaryReference(final EK9Parser.PrimaryReferenceContext ctx,
                                                final String rhsExprResult) {
    final var processingDetails = new PrimaryReferenceProcessingDetails(ctx, rhsExprResult);
    return generators.primaryReferenceGenerator.apply(processingDetails);
  }

  /**
   * Process AND expression using high-level short-circuit instruction.
   * Pattern: left and right
   * Short-circuit: if left is false, result is false without evaluating right
   * Uses ShortCircuitInstr for backend-appropriate lowering.
   */
  private List<IRInstr> processAndExpression(final ExprProcessingDetails details) {
    return generators.shortCircuitAndGenerator.apply(details);
  }

  /**
   * Process OR expression using high-level short-circuit instruction.
   * Pattern: left or right
   * Short-circuit: if left is true, result is true without evaluating right
   * Uses ShortCircuitInstr for backend-appropriate lowering.
   */
  private List<IRInstr> processOrExpression(final ExprProcessingDetails details) {
    return generators.shortCircuitOrGenerator.apply(details);
  }

}