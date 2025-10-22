package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
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
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
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
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
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
      return generateForInLoop(ctx);
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

    // Declare loop variable in loop scope for proper lifetime management
    // Loop variable type matches range type (Integer, Float, Duration, etc.)
    instructions.add(
        MemoryInstr.reference(loopVariableName, initializationData.rangeType.getFullyQualifiedName(), debugInfo));

    // Register loop variable to loop scope once (establishes ownership)
    // Variable doesn't have value yet, but SCOPE_EXIT will release it at loop end
    instructions.add(ScopeInstr.register(loopVariableName, loopScopeId, debugInfo));

    // SCOPE 3: Generate body scope ID (will be entered per iteration)
    // Body temps register to this scope and are released each iteration
    final var bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyScopeId, debugInfo, IRFrameType.BLOCK);

    // Phase 2: Generate body instructions with scope wrapping (uses bodyScopeId from currentScopeId())
    final var bodyInstructions = generateBodyInstructions(ctx, debugInfo, bodyScopeId);

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

    // Get range type from resolved symbols
    final var rangeSymbol = getRecordedSymbolOrException(forRangeCtx.range());
    final var rangeType = rangeSymbol.getType().orElseThrow(
        () -> new CompilerException("Range expression must have a type")
    );

    // Create expression-specific debug info for accurate source location tracking
    final var startDebugInfo = stackContext.createDebugInfo(forRangeCtx.range().expression(0));
    final var endDebugInfo = stackContext.createDebugInfo(forRangeCtx.range().expression(1));

    // Evaluate start expression (first expression in range)
    final var startTemp = stackContext.generateTempName();
    final var startResult = createTempVariable(startDebugInfo);
    // Add memory management for exprGenerator result (loop scope is on stack)
    final var instructions = new ArrayList<>(generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(
            new ExprProcessingDetails(forRangeCtx.range().expression(0), startResult)
        ),
        startResult
    ));
    final var loadStartInstructions = new ArrayList<IRInstr>();
    loadStartInstructions.add(MemoryInstr.load(startTemp, startResult.resultVariable(), startDebugInfo));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> loadStartInstructions,
        new VariableDetails(startTemp, startDebugInfo)
    ));

    // Evaluate end expression (second expression in range)
    final var endTemp = stackContext.generateTempName();
    final var endResult = createTempVariable(endDebugInfo);
    // Add memory management for exprGenerator result (loop scope is on stack)
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(
            new ExprProcessingDetails(forRangeCtx.range().expression(1), endResult)
        ),
        endResult
    ));
    final var loadEndInstructions = new ArrayList<IRInstr>();
    loadEndInstructions.add(MemoryInstr.load(endTemp, endResult.resultVariable(), endDebugInfo));
    instructions.addAll(generators.variableMemoryManagement.apply(
        () -> loadEndInstructions,
        new VariableDetails(endTemp, endDebugInfo)
    ));

    // Evaluate BY expression if present
    String byTemp = null;
    ISymbol byType = null;
    DebugInfo byDebugInfo = null;
    if (forRangeCtx.BY() != null) {
      byTemp = stackContext.generateTempName();

      if (forRangeCtx.literal() != null) {
        // BY with literal
        final var literalCtx = forRangeCtx.literal();
        final var bySymbol = getRecordedSymbolOrException(literalCtx);
        byType = bySymbol.getType().orElseThrow();

        byDebugInfo = stackContext.createDebugInfo(literalCtx);
        final var byLiteralInstructions = new ArrayList<IRInstr>();
        byLiteralInstructions.add(LiteralInstr.literal(
            byTemp,
            literalCtx.getText(),
            byType.getFullyQualifiedName(),
            byDebugInfo
        ));
        instructions.addAll(generators.variableMemoryManagement.apply(
            () -> byLiteralInstructions,
            new VariableDetails(byTemp, byDebugInfo)
        ));
      } else {
        // BY with identifier
        final var identCtx = forRangeCtx.identifier(1);
        final var bySymbol = getRecordedSymbolOrException(identCtx);
        byType = bySymbol.getType().orElseThrow();

        byDebugInfo = stackContext.createDebugInfo(identCtx);
        final var byRefInstructions = new ArrayList<IRInstr>();
        byRefInstructions.add(MemoryInstr.reference(identCtx.getText(), byType.getFullyQualifiedName(), byDebugInfo));
        final var loadTemp = stackContext.generateTempName();
        // Load from identifier with memory management
        final var loadInstructions = new ArrayList<IRInstr>();
        loadInstructions.add(MemoryInstr.load(loadTemp, identCtx.getText(), byDebugInfo));
        byRefInstructions.addAll(generators.variableMemoryManagement.apply(
            () -> loadInstructions,
            new VariableDetails(loadTemp, byDebugInfo)
        ));
        byRefInstructions.add(MemoryInstr.store(byTemp, loadTemp, byDebugInfo));
        instructions.addAll(generators.variableMemoryManagement.apply(
            () -> byRefInstructions,
            new VariableDetails(byTemp, byDebugInfo)
        ));
      }
    }

    // Assert start is set
    instructions.addAll(generateIsSetAssertion(startTemp, rangeType, startDebugInfo,
        "For-range 'start' value must be set"));

    // Assert end is set
    instructions.addAll(generateIsSetAssertion(endTemp, rangeType, endDebugInfo,
        "For-range 'end' value must be set"));

    // Assert by is set (if present)
    if (byTemp != null) {
      instructions.addAll(generateIsSetAssertion(byTemp, byType, byDebugInfo,
          "For-range 'by' value must be set"));
    }

    // Calculate direction: direction = start <=> end
    final var directionTemp = stackContext.generateTempName();


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
    final var directionInstructions = new ArrayList<>(cmpCallResult.allInstructions());
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
      final DebugInfo debugInfo,
      final String assertMessage) {

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
    final var isSetInstructions = new ArrayList<>(isSetCallResult.allInstructions());
    isSetInstructions.add(CallInstr.operator(isSetTemp, debugInfo, isSetCallResult.callDetails()));
    final var instructions = new ArrayList<>(generators.variableMemoryManagement.apply(
        () -> isSetInstructions,
        new VariableDetails(isSetTemp, debugInfo)
    ));

    // Convert to primitive boolean and assert
    final var primitiveCondition = stackContext.generateTempName();
    final var extractionParams = new org.ek9lang.compiler.phase7.support.BooleanExtractionParams(
        isSetTemp, primitiveCondition, debugInfo);
    instructions.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));
    instructions.add(org.ek9lang.compiler.ir.instructions.BranchInstr.assertValue(
        primitiveCondition,
        assertMessage,
        debugInfo
    ));

    return instructions;
  }

  /**
   * Generate body instructions (user code) with scope management.
   * Body is wrapped with SCOPE_ENTER/EXIT for per-iteration memory management.
   * These are generated ONCE and stored in the FOR_RANGE_POLYMORPHIC instruction.
   * Backends will emit them multiple times (once per direction case).
   * <p>
   * NOTE: Loop variable assignment (loopVariable = current) is NOT generated here.
   * It's generated separately by generateBodySetup() and stored in each dispatch
   * case's loopBodySetup field, enabling single body storage with per-case setup.
   * </p>
   *
   * @param ctx         ForStatementExpressionContext
   * @param debugInfo   Debug information
   * @param bodyScopeId Body iteration scope ID for SCOPE_ENTER/EXIT
   */
  private List<IRInstr> generateBodyInstructions(
      final EK9Parser.ForStatementExpressionContext ctx,
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
   * Unified directional case builder for ascending/descending for-range loops.
   * <p>
   * ELIMINATES SYMMETRIC DUPLICATION: The only differences between ascending
   * and descending cases are the 3 operators configured in DirectionConfig:
   * </p>
   * <ul>
   *   <li>Direction check operator: "&lt;" (ascending) vs "&gt;" (descending)</li>
   *   <li>Loop condition operator: "&lt;=" (ascending) vs "&gt;=" (descending)</li>
   *   <li>Increment operator: "++" (ascending) vs "--" (descending)</li>
   * </ul>
   * <p>
   * This method makes the symmetric relationship explicit via DirectionConfig
   * instead of duplicating 50+ lines of identical code.
   * </p>
   *
   * @param config           Direction configuration (ascending or descending)
   * @param initData         Initialization data from generateInitialization
   * @param loopVariableName Name of loop variable
   * @param debugInfo        Debug information
   * @param caseConstructor  Constructor reference (AscendingCase::new or DescendingCase::new)
   * @param <T>              Case type (AscendingCase or DescendingCase)
   * @return Constructed case object with all IR sequences
   */
  private <T> T generateDirectionalCase(
      final org.ek9lang.compiler.phase7.support.DirectionConfig config,
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo,
      final java.util.function.Function<CaseConstructorParams, T> caseConstructor) {

    // Direction check: direction < 0 (ascending) or direction > 0 (descending)
    final var directionCheckResult = config.directionOperator().equals("<")
        ? generateDirectionLessThanZero(initData.directionTemp, debugInfo)
        : generateDirectionGreaterThanZero(initData.directionTemp, debugInfo);

    // Loop condition: current <= end (ascending) or current >= end (descending)
    final var loopConditionResult = generateLoopCondition(
        initData.currentTemp,
        initData.endTemp,
        initData.rangeType,
        operatorMap.getForward(config.conditionOperator()),
        debugInfo
    );

    // Body setup: loopVariable = current (same for both)
    final var loopBodySetup = generateBodySetup(loopVariableName, initData.currentTemp, debugInfo);

    // Loop increment: current++ (ascending) or current-- (descending) OR current += by (both)
    final var loopIncrement = generateIncrementOperation(
        initData.currentTemp,
        initData.byTemp,
        initData.rangeType,
        initData.byType,
        operatorMap.getForward(config.incrementOperator()),
        debugInfo
    );

    // Construct case using provided constructor
    final var params = new CaseConstructorParams(
        directionCheckResult.instructions(),
        directionCheckResult.primitiveVariableName(),
        loopConditionResult.instructions(),
        loopConditionResult.primitiveVariableName(),
        loopBodySetup,
        loopIncrement
    );
    return caseConstructor.apply(params);
  }

  /**
   * Parameters for case constructor (AscendingCase or DescendingCase).
   * Both constructors have identical signatures.
   */
  private record CaseConstructorParams(
      List<IRInstr> directionCheck,
      String directionPrimitive,
      List<IRInstr> loopConditionTemplate,
      String loopConditionPrimitive,
      List<IRInstr> loopBodySetup,
      List<IRInstr> loopIncrement
  ) {
    ForRangePolymorphicInstr.AscendingCase toAscendingCase() {
      return new ForRangePolymorphicInstr.AscendingCase(
          directionCheck, directionPrimitive,
          loopConditionTemplate, loopConditionPrimitive,
          loopBodySetup, loopIncrement
      );
    }

    ForRangePolymorphicInstr.DescendingCase toDescendingCase() {
      return new ForRangePolymorphicInstr.DescendingCase(
          directionCheck, directionPrimitive,
          loopConditionTemplate, loopConditionPrimitive,
          loopBodySetup, loopIncrement
      );
    }
  }

  /**
   * Generate ascending dispatch case (direction &lt; 0).
   */
  private ForRangePolymorphicInstr.AscendingCase generateAscendingCase(
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo) {

    return generateDirectionalCase(
        org.ek9lang.compiler.phase7.support.DirectionConfig.ascending(),
        initData,
        loopVariableName,
        debugInfo,
        CaseConstructorParams::toAscendingCase
    );
  }

  /**
   * Generate descending dispatch case (direction &gt; 0).
   */
  private ForRangePolymorphicInstr.DescendingCase generateDescendingCase(
      final InitializationData initData,
      final String loopVariableName,
      final DebugInfo debugInfo) {

    return generateDirectionalCase(
        org.ek9lang.compiler.phase7.support.DirectionConfig.descending(),
        initData,
        loopVariableName,
        debugInfo,
        CaseConstructorParams::toDescendingCase
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
   * Generate direction check: direction &lt; 0.
   * Returns structured result with IR sequence and primitive boolean variable.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private org.ek9lang.compiler.phase7.support.ConditionEvaluationResult generateDirectionLessThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var params = new org.ek9lang.compiler.phase7.support.DirectionCheckParams(
        directionTemp,
        operatorMap.getForward("<"),
        stackContext.generateTempName(),  // zeroTemp
        stackContext.generateTempName(),  // booleanObjectTemp
        stackContext.generateTempName(),  // primitiveBooleanTemp
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        stackContext.currentScopeId(),
        debugInfo
    );

    return generators.directionCheckBuilder.apply(params);
  }

  /**
   * Generate direction check: direction &gt; 0.
   * Returns structured result with IR sequence and primitive boolean variable.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private org.ek9lang.compiler.phase7.support.ConditionEvaluationResult generateDirectionGreaterThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var params = new org.ek9lang.compiler.phase7.support.DirectionCheckParams(
        directionTemp,
        operatorMap.getForward(">"),
        stackContext.generateTempName(),  // zeroTemp
        stackContext.generateTempName(),  // booleanObjectTemp
        stackContext.generateTempName(),  // primitiveBooleanTemp
        stackContext.getParsedModule().getEk9Types().ek9Integer(),
        stackContext.getParsedModule().getEk9Types().ek9Boolean(),
        stackContext.currentScopeId(),
        debugInfo
    );

    return generators.directionCheckBuilder.apply(params);
  }

  /**
   * Generate loop condition: current (&lt;=|&gt;=) end.
   * Returns structured result with IR sequence and primitive boolean variable.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private org.ek9lang.compiler.phase7.support.ConditionEvaluationResult generateLoopCondition(
      final String currentTemp,
      final String endTemp,
      final ISymbol rangeType,
      final String comparisonOperator,
      final DebugInfo debugInfo) {

    // Must generate temp names in the correct order to match original implementation
    final var cmpResultTemp = stackContext.generateTempName();
    final var zeroTemp = stackContext.generateTempName();
    final var booleanObjectTemp = stackContext.generateTempName();
    final var primitiveBooleanTemp = stackContext.generateTempName();

    final var instructions = generators.scopedInstructionExecutor.execute(() -> {

      // Step 1: Call current <=> end (returns Integer)
      final var cmpParams = new org.ek9lang.compiler.phase7.support.BinaryOperatorParams(
          currentTemp,
          endTemp,
          operatorMap.getForward("<=>"),
          rangeType,
          rangeType,
          stackContext.getParsedModule().getEk9Types().ek9Integer(),
          cmpResultTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      final var scopedInstructions = new ArrayList<>(generators.binaryOperatorInvoker.apply(cmpParams));

      // Step 2: Load literal 0
      final var literalParams = new org.ek9lang.compiler.phase7.support.LiteralParams(
          zeroTemp,
          "0",
          stackContext.getParsedModule().getEk9Types().ek9Integer(),
          debugInfo
      );
      scopedInstructions.addAll(generators.managedLiteralLoader.apply(literalParams));

      // Step 3: Compare cmpResult (<=|>=) 0 and extract primitive boolean
      final var comparisonParams = new org.ek9lang.compiler.phase7.support.ComparisonParams(
          cmpResultTemp,
          zeroTemp,
          comparisonOperator,
          stackContext.getParsedModule().getEk9Types().ek9Integer(),
          stackContext.getParsedModule().getEk9Types().ek9Integer(),
          stackContext.getParsedModule().getEk9Types().ek9Boolean(),
          booleanObjectTemp,
          primitiveBooleanTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.comparisonEvaluator.apply(comparisonParams));

      return scopedInstructions;
    }, debugInfo);

    return new org.ek9lang.compiler.phase7.support.ConditionEvaluationResult(instructions, primitiveBooleanTemp);
  }

  /**
   * Generate body setup: loopVariable = current.
   * Uses reassignment pattern: RELEASE + STORE + RETAIN.
   * Variable already registered to loop scope at declaration.
   */
  private List<IRInstr> generateBodySetup(
      final String loopVariableName,
      final String currentTemp,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Reassignment pattern for loop variable (matches for-in pattern)
    // RELEASE is NULL-safe (first iteration: variable is NULL/uninitialized)
    // Variable already registered to loop scope at declaration
    instructions.add(MemoryInstr.release(loopVariableName, debugInfo));
    instructions.add(MemoryInstr.store(loopVariableName, currentTemp, debugInfo));
    instructions.add(MemoryInstr.retain(loopVariableName, debugInfo));

    return instructions;
  }

  /**
   * Generate increment operation: current++ / current-- OR current += by.
   * <p>
   * Unified helper that handles both increment forms:
   * - Without BY clause: uses unary operator (++ or --)
   * - With BY clause: uses binary operator (+=)
   * </p>
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private List<IRInstr> generateIncrementOperation(
      final String currentTemp,
      final String byTemp,  // nullable
      final ISymbol rangeType,
      final ISymbol byType,  // nullable when byTemp is null
      final String incrementOperator,  // ++ or --
      final DebugInfo debugInfo) {

    if (byTemp != null) {
      // BY clause present: current += by
      return generateIncrementBy(currentTemp, byTemp, rangeType, byType, debugInfo);
    } else {
      // No BY clause: current++ or current--
      return generateIncrement(currentTemp, rangeType, incrementOperator, debugInfo);
    }
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

    return generators.scopedInstructionExecutor.execute(() -> {
      final var params = new org.ek9lang.compiler.phase7.support.IncrementParams(
          currentTemp,
          incrementOperator,
          rangeType,
          stackContext.generateTempName(),  // incrementResultTemp
          stackContext.currentScopeId(),
          debugInfo
      );
      return generators.incrementEvaluator.apply(params);
    }, debugInfo);
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

    return generators.scopedInstructionExecutor.execute(() -> {

      // Call current += by with memory management
      final var newValueTemp = stackContext.generateTempName();
      final var params = new org.ek9lang.compiler.phase7.support.BinaryOperatorParams(
          currentTemp,
          byTemp,
          operatorMap.getForward("+="),
          rangeType,
          byType,
          rangeType,
          newValueTemp,
          stackContext.currentScopeId(),
          debugInfo
      );

      final var scopedInstructions = new ArrayList<>(generators.binaryOperatorInvoker.apply(params));

      // Update current variable (STORE only, current is already owned by loop_scope)
      scopedInstructions.add(MemoryInstr.store(currentTemp, newValueTemp, debugInfo));

      return scopedInstructions;
    }, debugInfo);
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
  ) {
  }

  // ========== FOR-IN LOOP IMPLEMENTATION ==========

  /**
   * Generate IR for for-in: for item in collection
   * Reuses while loop infrastructure with iterator setup.
   * <p>
   * Follows WhileStatementGenerator pattern with 4 scopes:
   * - Outer scope (guards + iterator)
   * - Whole loop scope
   * - Condition iteration scope (hasNext)
   * - Body iteration scope (next + user body)
   * </p>
   */
  private List<IRInstr> generateForInLoop(final EK9Parser.ForStatementExpressionContext ctx) {

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
