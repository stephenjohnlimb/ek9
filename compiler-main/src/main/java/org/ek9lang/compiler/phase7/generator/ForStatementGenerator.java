package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.ForRangePolymorphicInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for for-range loops using FOR_RANGE_POLYMORPHIC instruction.
 * <p>
 * POLYMORPHIC DESIGN: Works with ANY type that implements required operators:
 * - &lt;=&gt; operator for direction detection and loop conditions
 * - ++ operator for increment (when no BY clause, ascending)
 * - -- operator for decrement (when no BY clause, descending)
 * - += operator (when BY clause present - works for both directions)
 * </p>
 * <p>
 * RUNTIME DIRECTION DETECTION: Uses start &lt;=&gt; end to determine:
 * - Negative result (-1): ascending range → use ++ or +=
 * - Positive result (1): descending range → use -- or +=
 * - Zero result (0): equal range → single iteration
 * </p>
 * <p>
 * FAIL-FAST ASSERTIONS: Validates all inputs are set to prevent silent bugs
 * in expression form where unset inputs could produce misleading results.
 * </p>
 * <p>
 * Range type is obtained from resolved symbols, NOT hardcoded.
 * Supports Integer, Float, Date, Duration, and custom types.
 * </p>
 * <p>
 * <b>NEW IR STRUCTURE:</b> Uses FOR_RANGE_POLYMORPHIC instruction which stores
 * user body ONCE (not 3x like nested CONTROL_FLOW_CHAIN approach).
 * Backends read body once and emit it multiple times for different directions.
 * </p>
 */
public final class ForStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.ForStatementExpressionContext, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final GeneratorSet generators;

  public ForStatementGenerator(final IRGenerationContext stackContext,
                                final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.ForStatementExpressionContext ctx) {
    AssertValue.checkNotNull("ForStatementExpressionContext cannot be null", ctx);

    if (ctx.forLoop() != null) {
      throw new CompilerException("For-in loops not yet implemented");
    }

    if (ctx.returningParam() != null) {
      throw new CompilerException("For loop expression form not yet implemented");
    }

    return generateForRangeLoop(ctx);
  }

  /**
   * Main orchestration method for for-range loop IR generation.
   * Generates FOR_RANGE_POLYMORPHIC instruction with initialization and body.
   * <p>
   * Scope structure (follows CONTROL_FLOW_CHAIN pattern):
   * </p>
   * <pre>
   *   Outer Scope: Loop wrapper for future guards
   *   Loop Scope: Initialization temps (start, end, by, direction, current)
   *   Body Scope: Body iteration temps, enters/exits each iteration
   * </pre>
   */
  private List<IRInstr> generateForRangeLoop(
      final EK9Parser.ForStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var forRangeCtx = ctx.forRange();

    if (forRangeCtx.preFlowStatement() != null) {
      throw new CompilerException("For loop guards not yet implemented");
    }

    // Get loop variable name
    final var loopVariableName = forRangeCtx.identifier(0).getText();

    // SCOPE 1: Enter loop outer scope (for future guards)
    // Matches if/else and while pattern for architectural consistency
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

    // SCOPE 2: Enter loop scope (for initialization temps)
    // This scope contains start, end, by, direction, current variables
    final var loopScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(loopScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(loopScopeId, debugInfo));

    // Phase 1: Generate initialization instructions (uses loopScopeId from currentScopeId())
    // MUST do this while loop scope is active, before entering body scope
    final var initializationData = generateInitialization(forRangeCtx, debugInfo);

    // SCOPE 3: Generate body scope ID (will be entered per iteration)
    // Body temps register to this scope and are released each iteration
    final var bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyScopeId, debugInfo, IRFrameType.BLOCK);

    // Phase 2: Generate body instructions with scope wrapping (uses bodyScopeId from currentScopeId())
    final var bodyInstructions = generateBodyInstructions(ctx, loopVariableName, debugInfo, bodyScopeId);

    // Exit body scope from context
    stackContext.exitScope();

    // Phase 3: Create dispatch cases with explicit IR
    final var dispatchCases = generateDispatchCases(
        initializationData,
        loopVariableName,
        debugInfo
    );

    // Phase 4: Create loop metadata
    final var loopMetadata = generateLoopMetadata(
        initializationData,
        loopVariableName
    );

    // Phase 5: Create scope metadata
    final var scopeMetadata = new ForRangePolymorphicInstr.ScopeMetadata(
        outerScopeId,
        loopScopeId,
        bodyScopeId
    );

    // Phase 6: Create the polymorphic loop instruction
    instructions.add(ForRangePolymorphicInstr.forRangePolymorphic(
        initializationData.instructions,
        dispatchCases,
        loopMetadata,
        bodyInstructions,
        scopeMetadata,
        debugInfo
    ));

    // Exit loop scope
    instructions.add(ScopeInstr.exit(loopScopeId, debugInfo));
    stackContext.exitScope();

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate initialization instructions for the loop.
   * This includes: evaluating start/end/by, asserting they're set, direction detection.
   */
  private InitializationData generateInitialization(
      final EK9Parser.ForRangeContext forRangeCtx,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Get range type from resolved symbols
    final var rangeSymbol = getRecordedSymbolOrException(forRangeCtx.range());
    final var rangeType = rangeSymbol.getType().orElseThrow(
        () -> new CompilerException("Range expression must have a type")
    );

    // Evaluate start expression (first expression in range)
    final var startTemp = stackContext.generateTempName();
    final var startResult = createTempVariable(debugInfo);
    // Add memory management for exprGenerator result (loop scope is on stack)
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(
            new ExprProcessingDetails(forRangeCtx.range().expression(0), startResult)
        ),
        startResult
    ));
    final var loadStartInstructions = new ArrayList<IRInstr>();
    loadStartInstructions.add(MemoryInstr.load(startTemp, startResult.resultVariable(), debugInfo));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> loadStartInstructions,
        new VariableDetails(startTemp, debugInfo)
    ));

    // Evaluate end expression (second expression in range)
    final var endTemp = stackContext.generateTempName();
    final var endResult = createTempVariable(debugInfo);
    // Add memory management for exprGenerator result (loop scope is on stack)
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(
            new ExprProcessingDetails(forRangeCtx.range().expression(1), endResult)
        ),
        endResult
    ));
    final var loadEndInstructions = new ArrayList<IRInstr>();
    loadEndInstructions.add(MemoryInstr.load(endTemp, endResult.resultVariable(), debugInfo));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> loadEndInstructions,
        new VariableDetails(endTemp, debugInfo)
    ));

    // Evaluate BY expression if present
    String byTemp = null;
    ISymbol byType = null;
    if (forRangeCtx.BY() != null) {
      byTemp = stackContext.generateTempName();

      if (forRangeCtx.literal() != null) {
        // BY with literal
        final var literalCtx = forRangeCtx.literal();
        final var bySymbol = getRecordedSymbolOrException(literalCtx);
        byType = bySymbol.getType().orElseThrow();

        final var byLiteralInstructions = new ArrayList<IRInstr>();
        byLiteralInstructions.add(LiteralInstr.literal(
            byTemp,
            literalCtx.getText(),
            byType.getFullyQualifiedName(),
            debugInfo
        ));
        instructions.addAll(generators.variableMemoryManagement.apply(
            () -> byLiteralInstructions,
            new VariableDetails(byTemp, debugInfo)
        ));
      } else {
        // BY with identifier
        final var identCtx = forRangeCtx.identifier(1);
        final var bySymbol = getRecordedSymbolOrException(identCtx);
        byType = bySymbol.getType().orElseThrow();

        final var byRefInstructions = new ArrayList<IRInstr>();
        byRefInstructions.add(MemoryInstr.reference(identCtx.getText(), byType.getFullyQualifiedName(), debugInfo));
        final var loadTemp = stackContext.generateTempName();
        // Load from identifier with memory management
        final var loadInstructions = new ArrayList<IRInstr>();
        loadInstructions.add(MemoryInstr.load(loadTemp, identCtx.getText(), debugInfo));
        byRefInstructions.addAll(generators.variableMemoryManagement.apply(
            () -> loadInstructions,
            new VariableDetails(loadTemp, debugInfo)
        ));
        byRefInstructions.add(MemoryInstr.store(byTemp, loadTemp, debugInfo));
        instructions.addAll(generators.variableMemoryManagement.apply(
            () -> byRefInstructions,
            new VariableDetails(byTemp, debugInfo)
        ));
      }
    }

    // Assert start is set
    instructions.addAll(generateIsSetAssertion(startTemp, rangeType, debugInfo));

    // Assert end is set
    instructions.addAll(generateIsSetAssertion(endTemp, rangeType, debugInfo));

    // Assert by is set (if present)
    if (byTemp != null) {
      instructions.addAll(generateIsSetAssertion(byTemp, byType, debugInfo));
    }

    // Calculate direction: direction = start <=> end
    final var directionTemp = stackContext.generateTempName();
    final var directionInstructions = new ArrayList<IRInstr>();

    final var cmpCallContext = CallContext.forBinaryOperation(
        rangeType,
        rangeType,
        stackContext.getParsedModule().getEk9Types().ek9Integer(),  // <=> returns Integer
        operatorMap.getForward("<=>"),
        startTemp,
        endTemp,
        stackContext.currentScopeId()
    );
    final var cmpCallResult = generators.callDetailsBuilder.apply(cmpCallContext);
    directionInstructions.addAll(cmpCallResult.allInstructions());
    directionInstructions.add(CallInstr.operator(directionTemp, debugInfo, cmpCallResult.callDetails()));

    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> directionInstructions,
        new VariableDetails(directionTemp, debugInfo)
    ));

    // Initialize current = start
    final var currentTemp = stackContext.generateTempName();
    final var currentInstructions = new ArrayList<IRInstr>();
    currentInstructions.add(MemoryInstr.load(currentTemp, startTemp, debugInfo));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> currentInstructions,
        new VariableDetails(currentTemp, debugInfo)
    ));

    return new InitializationData(
        instructions,
        startTemp,
        endTemp,
        byTemp,
        directionTemp,
        currentTemp,
        rangeType,
        byType
    );
  }

  /**
   * Generate instructions to assert a value is set (fail-fast).
   */
  private List<IRInstr> generateIsSetAssertion(
      final String valueTemp,
      final ISymbol valueType,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var isSetTemp = stackContext.generateTempName();

    final var isSetCallContext = CallContext.forUnaryOperation(
        valueType,
        "_isSet",
        valueTemp,
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        stackContext.currentScopeId()
    );
    final var isSetCallResult = generators.callDetailsBuilder.apply(isSetCallContext);

    // Add _isSet() call with memory management (Boolean object result)
    // Loop scope is on stack, so helper uses correct currentScopeId()
    final var isSetInstructions = new ArrayList<IRInstr>();
    isSetInstructions.addAll(isSetCallResult.allInstructions());
    isSetInstructions.add(CallInstr.operator(isSetTemp, debugInfo, isSetCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> isSetInstructions,
        new VariableDetails(isSetTemp, debugInfo)
    ));

    // Convert to primitive boolean and assert
    final var conversion = convertToPrimitiveBoolean(isSetTemp, debugInfo);
    final var primitiveCondition = conversion.addToInstructions(instructions);
    instructions.add(org.ek9lang.compiler.ir.instructions.BranchInstr.assertValue(
        primitiveCondition,
        debugInfo
    ));

    return instructions;
  }

  /**
   * Generate body instructions (user code) with scope management.
   * Body is wrapped with SCOPE_ENTER/EXIT for per-iteration memory management.
   * These are generated ONCE and stored in the FOR_RANGE_POLYMORPHIC instruction.
   * Backends will emit them multiple times (once per direction case).
   *
   * @param ctx              ForStatementExpressionContext
   * @param loopVariableName User's loop variable name (e.g., "i")
   * @param debugInfo        Debug information
   * @param bodyScopeId      Body iteration scope ID for SCOPE_ENTER/EXIT
   */
  private List<IRInstr> generateBodyInstructions(
      final EK9Parser.ForStatementExpressionContext ctx,
      final String loopVariableName,
      final DebugInfo debugInfo,
      final String bodyScopeId) {

    final var bodyInstructions = new ArrayList<IRInstr>();

    // Add SCOPE_ENTER at beginning (like WhileStatementGenerator line 126)
    // This scope enters/exits each iteration for tight memory management
    bodyInstructions.add(ScopeInstr.enter(bodyScopeId, debugInfo));

    // Process user's loop body (uses currentScopeId() = bodyScopeId)
    bodyInstructions.addAll(processBlockStatements(ctx.instructionBlock()));

    // Add SCOPE_EXIT at end (like WhileStatementGenerator line 128)
    // Automatically releases all temps registered to bodyScopeId
    bodyInstructions.add(ScopeInstr.exit(bodyScopeId, debugInfo));

    return bodyInstructions;
  }

  /**
   * Process block statements (delegates to block statement generator).
   */
  private List<IRInstr> processBlockStatements(
      final EK9Parser.InstructionBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    if (ctx != null && ctx.blockStatement() != null) {
      for (var blockStmtCtx : ctx.blockStatement()) {
        instructions.addAll(generators.blockStmtGenerator.apply(blockStmtCtx));
      }
    }

    return instructions;
  }

  /**
   * Generate dispatch cases with explicit IR for all three directions.
   */
  private ForRangePolymorphicInstr.DispatchCases generateDispatchCases(
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo) {

    final var ascendingCase = generateAscendingCase(initData, loopVariableName, debugInfo);
    final var descendingCase = generateDescendingCase(initData, loopVariableName, debugInfo);
    final var equalCase = generateEqualCase(loopVariableName, initData.currentTemp, debugInfo);

    return new ForRangePolymorphicInstr.DispatchCases(
        ascendingCase,
        descendingCase,
        equalCase
    );
  }

  /**
   * Generate ascending dispatch case (direction < 0).
   */
  private ForRangePolymorphicInstr.AscendingCase generateAscendingCase(
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo) {

    // Direction check: direction < 0
    final var directionCheck = generateDirectionLessThanZero(initData.directionTemp, debugInfo);
    final var directionPrimitive = extractPrimitiveVariable(directionCheck);

    // Loop condition: current <= end (using <=> for comparison)
    final var loopConditionTemplate = generateLoopCondition(
        initData.currentTemp,
        initData.endTemp,
        initData.rangeType,
        operatorMap.getForward("<="),
        debugInfo
    );
    final var loopConditionPrimitive = extractPrimitiveVariable(loopConditionTemplate);

    // Body setup: loopVariable = current
    final var loopBodySetup = generateBodySetup(loopVariableName, initData.currentTemp, debugInfo);

    // Loop increment: current++ or current += by
    final List<IRInstr> loopIncrement;
    if (initData.byTemp != null) {
      loopIncrement = generateIncrementBy(
          initData.currentTemp,
          initData.byTemp,
          initData.rangeType,
          initData.byType,
          debugInfo
      );
    } else {
      loopIncrement = generateIncrement(
          initData.currentTemp,
          initData.rangeType,
          operatorMap.getForward("++"),
          debugInfo
      );
    }

    return new ForRangePolymorphicInstr.AscendingCase(
        directionCheck,
        directionPrimitive,
        loopConditionTemplate,
        loopConditionPrimitive,
        loopBodySetup,
        loopIncrement
    );
  }

  /**
   * Generate descending dispatch case (direction > 0).
   */
  private ForRangePolymorphicInstr.DescendingCase generateDescendingCase(
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo) {

    // Direction check: direction > 0
    final var directionCheck = generateDirectionGreaterThanZero(initData.directionTemp, debugInfo);
    final var directionPrimitive = extractPrimitiveVariable(directionCheck);

    // Loop condition: current >= end (using <=> for comparison)
    final var loopConditionTemplate = generateLoopCondition(
        initData.currentTemp,
        initData.endTemp,
        initData.rangeType,
        operatorMap.getForward(">="),
        debugInfo
    );
    final var loopConditionPrimitive = extractPrimitiveVariable(loopConditionTemplate);

    // Body setup: loopVariable = current
    final var loopBodySetup = generateBodySetup(loopVariableName, initData.currentTemp, debugInfo);

    // Loop increment: current-- or current += by
    final List<IRInstr> loopIncrement;
    if (initData.byTemp != null) {
      loopIncrement = generateIncrementBy(
          initData.currentTemp,
          initData.byTemp,
          initData.rangeType,
          initData.byType,
          debugInfo
      );
    } else {
      loopIncrement = generateIncrement(
          initData.currentTemp,
          initData.rangeType,
          operatorMap.getForward("--"),
          debugInfo
      );
    }

    return new ForRangePolymorphicInstr.DescendingCase(
        directionCheck,
        directionPrimitive,
        loopConditionTemplate,
        loopConditionPrimitive,
        loopBodySetup,
        loopIncrement
    );
  }

  /**
   * Generate equal dispatch case (direction == 0, single iteration).
   */
  private ForRangePolymorphicInstr.EqualCase generateEqualCase(
      final String loopVariableName,
      final String currentTemp,
      final DebugInfo debugInfo) {

    // Body setup: loopVariable = current
    final var loopBodySetup = generateBodySetup(loopVariableName, currentTemp, debugInfo);

    return new ForRangePolymorphicInstr.EqualCase(
        loopBodySetup,
        true  // Single iteration flag
    );
  }

  /**
   * Generate direction check: direction < 0.
   * Returns IR sequence ending with primitive boolean result.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateDirectionLessThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var zeroTemp = stackContext.generateTempName();
    final var resultTemp = stackContext.generateTempName();

    // Create scope for direction check temps and push onto stack
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Load literal 0 with memory management (using helper)
    final var zeroType = stackContext.getParsedModule().getEk9Types().ek9Integer();
    final var zeroLiteralInstructions = new ArrayList<IRInstr>();
    zeroLiteralInstructions.add(LiteralInstr.literal(
        zeroTemp,
        "0",
        zeroType.getFullyQualifiedName(),
        debugInfo
    ));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> zeroLiteralInstructions,
        new VariableDetails(zeroTemp, debugInfo)
    ));

    // Call direction < 0 (returns Boolean object) with memory management (using helper)
    final var ltCallContext = CallContext.forBinaryOperation(
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        operatorMap.getForward("<"),
        directionTemp,
        zeroTemp,
        stackContext.currentScopeId()
    );
    final var ltCallResult = generators.callDetailsBuilder.apply(ltCallContext);
    final var ltCallInstructions = new ArrayList<IRInstr>();
    ltCallInstructions.addAll(ltCallResult.allInstructions());
    ltCallInstructions.add(CallInstr.operator(resultTemp, debugInfo, ltCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> ltCallInstructions,
        new VariableDetails(resultTemp, debugInfo)
    ));

    // Convert to primitive boolean (no memory management - primitive)
    final var conversion = convertToPrimitiveBoolean(resultTemp, debugInfo);
    conversion.addToInstructions(instructions);

    // Exit scope and pop from stack
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate direction check: direction > 0.
   * Returns IR sequence ending with primitive boolean result.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateDirectionGreaterThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var zeroTemp = stackContext.generateTempName();
    final var resultTemp = stackContext.generateTempName();

    // Create scope for direction check temps and push onto stack
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Load literal 0 with memory management (using helper)
    final var zeroType = stackContext.getParsedModule().getEk9Types().ek9Integer();
    final var zeroLiteralInstructions = new ArrayList<IRInstr>();
    zeroLiteralInstructions.add(LiteralInstr.literal(
        zeroTemp,
        "0",
        zeroType.getFullyQualifiedName(),
        debugInfo
    ));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> zeroLiteralInstructions,
        new VariableDetails(zeroTemp, debugInfo)
    ));

    // Call direction > 0 (returns Boolean object) with memory management (using helper)
    final var gtCallContext = CallContext.forBinaryOperation(
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        operatorMap.getForward(">"),
        directionTemp,
        zeroTemp,
        stackContext.currentScopeId()
    );
    final var gtCallResult = generators.callDetailsBuilder.apply(gtCallContext);
    final var gtCallInstructions = new ArrayList<IRInstr>();
    gtCallInstructions.addAll(gtCallResult.allInstructions());
    gtCallInstructions.add(CallInstr.operator(resultTemp, debugInfo, gtCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> gtCallInstructions,
        new VariableDetails(resultTemp, debugInfo)
    ));

    // Convert to primitive boolean (no memory management - primitive)
    final var conversion = convertToPrimitiveBoolean(resultTemp, debugInfo);
    conversion.addToInstructions(instructions);

    // Exit scope and pop from stack
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate loop condition: current (<=|>=) end.
   * Returns IR sequence ending with primitive boolean result.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateLoopCondition(
      final String currentTemp,
      final String endTemp,
      final ISymbol rangeType,
      final String comparisonOperator,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var cmpResultTemp = stackContext.generateTempName();
    final var zeroTemp = stackContext.generateTempName();
    final var conditionTemp = stackContext.generateTempName();

    // Create scope for loop condition temps and push onto stack
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Call current <=> end (returns Integer object) with memory management (using helper)
    final var cmpCallContext = CallContext.forBinaryOperation(
        rangeType,
        rangeType,
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        operatorMap.getForward("<=>"),
        currentTemp,
        endTemp,
        stackContext.currentScopeId()
    );
    final var cmpCallResult = generators.callDetailsBuilder.apply(cmpCallContext);
    final var cmpCallInstructions = new ArrayList<IRInstr>();
    cmpCallInstructions.addAll(cmpCallResult.allInstructions());
    cmpCallInstructions.add(CallInstr.operator(cmpResultTemp, debugInfo, cmpCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> cmpCallInstructions,
        new VariableDetails(cmpResultTemp, debugInfo)
    ));

    // Load literal 0 with memory management (using helper)
    final var zeroLiteralInstructions = new ArrayList<IRInstr>();
    zeroLiteralInstructions.add(LiteralInstr.literal(
        zeroTemp,
        "0",
        stackContext.getParsedModule().getEk9Types().ek9Integer().getFullyQualifiedName(),
        debugInfo
    ));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> zeroLiteralInstructions,
        new VariableDetails(zeroTemp, debugInfo)
    ));

    // Call cmpResult (<=|>=) 0 (returns Boolean object) with memory management (using helper)
    final var condCallContext = CallContext.forBinaryOperation(
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        comparisonOperator,
        cmpResultTemp,
        zeroTemp,
        stackContext.currentScopeId()
    );
    final var condCallResult = generators.callDetailsBuilder.apply(condCallContext);
    final var condCallInstructions = new ArrayList<IRInstr>();
    condCallInstructions.addAll(condCallResult.allInstructions());
    condCallInstructions.add(CallInstr.operator(conditionTemp, debugInfo, condCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> condCallInstructions,
        new VariableDetails(conditionTemp, debugInfo)
    ));

    // Convert to primitive boolean (no memory management - primitive)
    final var conversion = convertToPrimitiveBoolean(conditionTemp, debugInfo);
    conversion.addToInstructions(instructions);

    // Exit scope and pop from stack
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate body setup: loopVariable = current.
   */
  private List<IRInstr> generateBodySetup(
      final String loopVariableName,
      final String currentTemp,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    instructions.add(MemoryInstr.store(loopVariableName, currentTemp, debugInfo));
    return instructions;
  }

  /**
   * Generate increment: current++ or current--.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateIncrement(
      final String currentTemp,
      final ISymbol rangeType,
      final String incrementOperator,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var newValueTemp = stackContext.generateTempName();

    // Create scope for increment temps and push onto stack
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Call current++ or current-- (returns rangeType object) with memory management (using helper)
    final var incCallContext = CallContext.forUnaryOperation(
        rangeType,
        incrementOperator,
        currentTemp,
        rangeType,
        stackContext.currentScopeId()
    );
    final var incCallResult = generators.callDetailsBuilder.apply(incCallContext);
    final var incCallInstructions = new ArrayList<IRInstr>();
    incCallInstructions.addAll(incCallResult.allInstructions());
    incCallInstructions.add(CallInstr.operator(newValueTemp, debugInfo, incCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> incCallInstructions,
        new VariableDetails(newValueTemp, debugInfo)
    ));

    // Update current variable (STORE only, current is already owned by loop_scope)
    instructions.add(MemoryInstr.store(currentTemp, newValueTemp, debugInfo));

    // Exit scope and pop from stack
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate increment by: current += by.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateIncrementBy(
      final String currentTemp,
      final String byTemp,
      final ISymbol rangeType,
      final ISymbol byType,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();
    final var newValueTemp = stackContext.generateTempName();

    // Create scope for increment temps and push onto stack
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Call current += by (returns rangeType object) with memory management (using helper)
    final var addCallContext = CallContext.forBinaryOperation(
        rangeType,
        byType,
        rangeType,
        operatorMap.getForward("+="),
        currentTemp,
        byTemp,
        stackContext.currentScopeId()
    );
    final var addCallResult = generators.callDetailsBuilder.apply(addCallContext);
    final var addCallInstructions = new ArrayList<IRInstr>();
    addCallInstructions.addAll(addCallResult.allInstructions());
    addCallInstructions.add(CallInstr.operator(newValueTemp, debugInfo, addCallResult.callDetails()));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> addCallInstructions,
        new VariableDetails(newValueTemp, debugInfo)
    ));

    // Update current variable (STORE only, current is already owned by loop_scope)
    instructions.add(MemoryInstr.store(currentTemp, newValueTemp, debugInfo));

    // Exit scope and pop from stack
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Extract the primitive variable name from an IR sequence.
   * After adding SCOPE_ENTER/EXIT, the sequence is:
   * SCOPE_ENTER, ..., CALL (_true() returning primitive boolean), SCOPE_EXIT
   * We need to find the CallInstr before the SCOPE_EXIT.
   */
  private String extractPrimitiveVariable(final List<IRInstr> instructions) {
    if (instructions.size() < 2) {
      throw new CompilerException("Cannot extract primitive variable from instruction list with < 2 instructions");
    }

    // Find the CallInstr before the SCOPE_EXIT (should be second-to-last)
    final var secondToLastInstr = instructions.get(instructions.size() - 2);
    if (secondToLastInstr instanceof CallInstr callInstr) {
      return callInstr.getResult();
    }

    throw new CompilerException("Expected CallInstr as second-to-last instruction for primitive extraction");
  }

  /**
   * Generate loop metadata containing variable names and types.
   */
  private ForRangePolymorphicInstr.LoopMetadata generateLoopMetadata(
      final InitializationData initData,
      final String loopVariableName) {

    return new ForRangePolymorphicInstr.LoopMetadata(
        initData.directionTemp,
        initData.currentTemp,
        loopVariableName,
        initData.endTemp,
        initData.rangeType.getFullyQualifiedName(),
        initData.byTemp,
        initData.byType != null ? initData.byType.getFullyQualifiedName() : null
    );
  }

  /**
   * Helper record to pass initialization data between methods.
   */
  private record InitializationData(
      List<IRInstr> instructions,
      String startTemp,
      String endTemp,
      String byTemp,           // nullable
      String directionTemp,
      String currentTemp,
      ISymbol rangeType,
      ISymbol byType           // nullable
  ) {}
}
