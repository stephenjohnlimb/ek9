package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for for-in loops: for item in collection.
 * <p>
 * Reuses while loop infrastructure with iterator setup.
 * </p>
 * <p>
 * Follows WhileStatementGenerator pattern with 4 scopes:
 * - Outer scope (guards + iterator)
 * - Whole loop scope
 * - Condition iteration scope (hasNext)
 * - Body iteration scope (next + user body)
 * </p>
 */
public final class ForInGenerator extends AbstractGenerator
    implements Function<EK9Parser.ForStatementExpressionContext, List<IRInstr>> {

  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final GeneratorSet generators;

  ForInGenerator(final IRGenerationContext stackContext,
                 final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.ForStatementExpressionContext ctx) {

    final var forLoopCtx = ctx.forLoop();
    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Check guards (not implemented yet)
    if (forLoopCtx.preFlowStatement() != null) {
      throw new CompilerException("For-in loop guards not yet implemented");
    }

    // Get squirreled pattern
    final var forSymbol = getRecordedSymbolOrException(ctx);
    final var pattern = forSymbol.getSquirrelledData(CommonValues.FOR_ITERATION_PATTERN);
    if (pattern == null) {
      throw new CompilerException("No iteration pattern squirreled for for-loop");
    }

    // SCOPE 1: Outer scope (matches while - for guards)
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

    // Setup: Generate iterator variable based on pattern
    final var collectionSymbol = getRecordedSymbolOrException(forLoopCtx.expression());
    final var collectionType = (IAggregateSymbol) symbolTypeOrException.apply(collectionSymbol);

    final String iteratorVar;
    final IAggregateSymbol iteratorType;

    if (CommonValues.ITERATOR_METHOD.toString().equals(pattern)) {
      // Pattern A: _temp = collection.iterator()
      var iteratorSetup = generateIteratorCall(forLoopCtx, collectionType, instructions);
      iteratorVar = iteratorSetup.variable();
      iteratorType = iteratorSetup.type();
    } else if (CommonValues.DIRECT_ITERATION.toString().equals(pattern)) {
      // Pattern B: Use collection directly
      var collectionSetup = evaluateCollectionExpression(forLoopCtx, collectionType, instructions);
      iteratorVar = collectionSetup.variable();
      iteratorType = collectionSetup.type();
    } else {
      throw new CompilerException("Invalid iteration pattern: " + pattern);
    }

    // SCOPE 2: Whole loop scope (matches while)
    final var wholeLoopScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(wholeLoopScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(wholeLoopScopeId, debugInfo));

    // Declare loop variable in loop scope for proper lifetime management
    final var loopVarSymbol = getRecordedSymbolOrException(forLoopCtx);
    final var loopVarType = symbolTypeOrException.apply(loopVarSymbol);
    final var loopVarName = loopVarSymbol.getName();
    instructions.add(MemoryInstr.reference(loopVarName, loopVarType.getFullyQualifiedName(), debugInfo));

    // Register loop variable to loop scope once (establishes ownership)
    // Variable doesn't have value yet, but SCOPE_EXIT will release it at loop end
    instructions.add(ScopeInstr.register(loopVarName, wholeLoopScopeId, debugInfo));

    // Generate while loop condition and body
    final var conditionCase = generateIterationConditionAndBody(
        forLoopCtx, iteratorVar, iteratorType, ctx.instructionBlock(), debugInfo);

    // Create WHILE_LOOP ControlFlowChainDetails
    final var whileDetails = ControlFlowChainDetails.createWhileLoop(
        List.of(conditionCase),
        debugInfo,
        wholeLoopScopeId
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(whileDetails));

    // Exit scopes
    instructions.add(ScopeInstr.exit(wholeLoopScopeId, debugInfo));
    stackContext.exitScope();

    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Helper record for iterator setup results.
   */
  private record IteratorSetup(String variable, IAggregateSymbol type) {
  }

  /**
   * Pattern A: collection.iterator()
   * Generate iterator() call and return iterator variable + type.
   */
  private IteratorSetup generateIteratorCall(
      final EK9Parser.ForLoopContext forLoopCtx,
      final IAggregateSymbol collectionType,
      final List<IRInstr> instructions) {

    // Create expression-specific debug info for accurate source location tracking
    final var debugInfo = stackContext.createDebugInfo(forLoopCtx.expression());

    // Evaluate collection with memory management
    final var collectionVar = stackContext.generateTempName();
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(
            new ExprProcessingDetails(forLoopCtx.expression(),
                new VariableDetails(collectionVar, debugInfo))),
        new VariableDetails(collectionVar, debugInfo)
    ));

    // Call iterator() - resolve return type first
    final var iteratorMethod = collectionType.resolve(new MethodSymbolSearch("iterator"))
        .orElseThrow(() -> new CompilerException("iterator() not found"));
    final var iteratorReturnType = (IAggregateSymbol) symbolTypeOrException.apply(iteratorMethod);

    // Call iterator()
    final var iteratorVar = stackContext.generateTempName();
    final var callContext = CallContext.forUnaryOperation(
        collectionType,
        "iterator",
        collectionVar,
        iteratorReturnType,
        stackContext.currentScopeId());

    final var callDetails = generators.callDetailsBuilder.apply(callContext);

    // Build iterator() call with memory management
    final var iteratorInstructions = new ArrayList<>(callDetails.allInstructions());
    iteratorInstructions.add(CallInstr.call(
        iteratorVar,
        debugInfo,
        callDetails.callDetails()));

    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> iteratorInstructions,
        new VariableDetails(iteratorVar, debugInfo)
    ));

    return new IteratorSetup(iteratorVar, iteratorReturnType);
  }

  /**
   * Pattern B: Collection IS iterator.
   * Evaluate collection expression and return collection variable + type.
   */
  private IteratorSetup evaluateCollectionExpression(
      final EK9Parser.ForLoopContext forLoopCtx,
      final IAggregateSymbol collectionType,
      final List<IRInstr> instructions) {

    final var debugInfo = stackContext.createDebugInfo(forLoopCtx.expression());
    final var collectionVar = stackContext.generateTempName();

    instructions.addAll(generators.exprGenerator.apply(
        new ExprProcessingDetails(forLoopCtx.expression(),
            new VariableDetails(collectionVar, debugInfo))));

    return new IteratorSetup(collectionVar, collectionType);
  }

  /**
   * Generate condition (hasNext) and body (next + user code).
   * EXACTLY follows WhileStatementGenerator pattern with 2 iteration scopes.
   */
  private ConditionCaseDetails generateIterationConditionAndBody(
      final EK9Parser.ForLoopContext forLoopCtx,
      final String iteratorVar,
      final IAggregateSymbol iteratorType,
      final EK9Parser.InstructionBlockContext bodyCtx,
      final DebugInfo debugInfo) {

    // SCOPE 3: Condition iteration scope (enters/exits each iteration)
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);

    // Generate: iteratorVar.hasNext()
    final var conditionEvaluation = new ArrayList<IRInstr>();
    conditionEvaluation.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // Call hasNext()
    final var hasNextResult = stackContext.generateTempName();
    final var hasNextContext = CallContext.forUnaryOperation(
        iteratorType,
        "hasNext",
        iteratorVar,
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        stackContext.currentScopeId());

    final var hasNextDetails = generators.callDetailsBuilder.apply(hasNextContext);

    // Build hasNext() call with memory management
    final var hasNextInstructions = new ArrayList<>(hasNextDetails.allInstructions());
    hasNextInstructions.add(CallInstr.call(
        hasNextResult,
        debugInfo,
        hasNextDetails.callDetails()));

    conditionEvaluation.addAll(generators.variableMemoryManagement.apply(
        () -> hasNextInstructions,
        new VariableDetails(hasNextResult, debugInfo)
    ));

    // Convert to primitive boolean
    final var primitiveCondition = stackContext.generateTempName();
    conditionEvaluation.addAll(generators.primitiveBooleanExtractor.apply(
        new BooleanExtractionParams(hasNextResult, primitiveCondition, debugInfo)));

    conditionEvaluation.add(ScopeInstr.exit(conditionScopeId, debugInfo));
    stackContext.exitScope();

    // SCOPE 4: Body iteration scope (enters/exits each iteration)
    final var bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyScopeId, debugInfo, IRFrameType.BLOCK);

    // Generate body: loopVar = iteratorVar.next(); [user code]
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(bodyScopeId, debugInfo));

    // loopVar = iteratorVar.next()
    bodyEvaluation.addAll(
        generateLoopVariableBinding(forLoopCtx, iteratorVar, iteratorType, debugInfo));

    // User body statements
    for (var stmt : bodyCtx.blockStatement()) {
      bodyEvaluation.addAll(generators.blockStmtGenerator.apply(stmt));
    }

    bodyEvaluation.add(ScopeInstr.exit(bodyScopeId, debugInfo));
    stackContext.exitScope();

    // Create ConditionCaseDetails (matches while loop structure)
    return ConditionCaseDetails.createExpression(
        conditionScopeId,
        conditionEvaluation,
        hasNextResult,       // EK9 Boolean
        primitiveCondition,  // primitive boolean
        bodyEvaluation,
        null                 // No result (statement form)
    );
  }

  /**
   * Generate: loopVar = iteratorVar.next()
   */
  private List<IRInstr> generateLoopVariableBinding(
      final EK9Parser.ForLoopContext forLoopCtx,
      final String iteratorVar,
      final IAggregateSymbol iteratorType,
      final DebugInfo debugInfo) {

    // Get loop variable symbol and type
    // Note: Loop variable is recorded against ForLoopContext in Phase 1, not against identifier
    final var loopVarSymbol = getRecordedSymbolOrException(forLoopCtx);
    final var loopVarType = symbolTypeOrException.apply(loopVarSymbol);

    // Call next()
    final var nextResult = stackContext.generateTempName();
    final var nextContext = CallContext.forUnaryOperation(
        iteratorType,
        "next",
        iteratorVar,
        loopVarType,
        stackContext.currentScopeId());

    final var nextDetails = generators.callDetailsBuilder.apply(nextContext);

    // Build next() call with memory management
    final var nextInstructions = new ArrayList<>(nextDetails.allInstructions());
    nextInstructions.add(CallInstr.call(
        nextResult,
        debugInfo,
        nextDetails.callDetails()));

    final var instructions = new ArrayList<>(generators.variableMemoryManagement.apply(
        () -> nextInstructions,
        new VariableDetails(nextResult, debugInfo)
    ));

    // Bind to loop variable using reassignment pattern
    // RELEASE is NULL-safe (verified in LLVM backend ek9_arc_runtime.c:49-54)
    // First iteration: variable is NULL/uninitialized, RELEASE safely returns
    // Variable already registered to loop scope at declaration
    final var loopVarName = loopVarSymbol.getName();
    instructions.add(MemoryInstr.release(loopVarName, debugInfo));
    instructions.add(MemoryInstr.store(loopVarName, nextResult, debugInfo));
    instructions.add(MemoryInstr.retain(loopVarName, debugInfo));

    return instructions;
  }
}
