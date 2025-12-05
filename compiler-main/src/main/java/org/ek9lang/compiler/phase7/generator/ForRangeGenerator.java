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
import org.ek9lang.compiler.phase7.support.BinaryOperatorParams;
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ComparisonParams;
import org.ek9lang.compiler.phase7.support.ConditionEvaluationResult;
import org.ek9lang.compiler.phase7.support.DirectionCheckParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IncrementParams;
import org.ek9lang.compiler.phase7.support.LiteralParams;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for for-range loops and expressions using FOR_RANGE_POLYMORPHIC instruction.
 * <p>
 * Supports both statement and expression forms:
 * </p>
 * <ul>
 *   <li>Statement form: {@code for i in 1 ... 10} - no return value</li>
 *   <li>Expression form: {@code result <- for i in 1 ... 10 <- rtn <- 0} - accumulator pattern</li>
 * </ul>
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
public final class ForRangeGenerator extends AbstractGenerator
    implements Function<EK9Parser.ForStatementExpressionContext, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final GeneratorSet generators;
  private final LoopGuardHelper loopGuardHelper;
  private final ReturningParamProcessor returningParamProcessor;

  ForRangeGenerator(final IRGenerationContext stackContext,
                    final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.loopGuardHelper = new LoopGuardHelper(stackContext, generators);
    this.returningParamProcessor = new ReturningParamProcessor(stackContext, generators);
  }

  /**
   * Main orchestration method for for-range loop IR generation.
   * Generates FOR_RANGE_POLYMORPHIC instruction with initialization and body.
   * <p>
   * Supports both statement form (no returningParam) and expression form
   * (with returningParam for accumulator pattern).
   * </p>
   * <p>
   * Scope structure (follows CONTROL_FLOW_CHAIN pattern):
   * </p>
   * <pre>
   *   Outer Scope: Loop wrapper for guards + return variable (expression form)
   *   Loop Scope: Initialization temps (start, end, by, direction, current)
   *   Body Scope: Body iteration temps, enters/exits each iteration
   * </pre>
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.ForStatementExpressionContext ctx) {

    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var forRangeCtx = ctx.forRange();

    // SCOPE 1: Enter guard/outer scope
    // Matches while pattern for architectural consistency
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(
        forRangeCtx.preFlowStatement(), outerScopeId);

    // Process return variable for expression form (declared in outer scope)
    final var returnDetails = returningParamProcessor.process(ctx.returningParam(), outerScopeId);

    // Generate the core for-range loop
    final var loopInstructions = generateForRangeLoopCore(ctx, forRangeCtx, debugInfo, outerScopeId);

    // If guard has entry check, wrap loop in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      // For expression form with guards, use wrapExpressionFormWithGuardEntryCheck
      // This emits return variable setup OUTSIDE IF wrapper so variable exists on all code paths
      instructions = loopGuardHelper.wrapExpressionFormWithGuardEntryCheck(
          guardDetails,
          returnDetails.returnVariableSetup(),  // Return variable init goes OUTSIDE IF
          loopInstructions,                     // Body goes INSIDE IF
          outerScopeId,
          debugInfo);
    } else {
      // No guard - add scope enter/exit around the loop
      instructions = new ArrayList<>();
      instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));
      // Emit return variable setup in outer scope (expression form)
      instructions.addAll(returnDetails.returnVariableSetup());
      instructions.addAll(loopInstructions);
      instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    }

    // Exit outer scope from stack context
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate the core for-range loop instructions (without guard wrapping).
   * <p>
   * For expression form, the return variable is already declared in outer scope.
   * The body statements include assignments to the return variable (accumulator pattern).
   * </p>
   */
  private List<IRInstr> generateForRangeLoopCore(
      final EK9Parser.ForStatementExpressionContext ctx,
      final EK9Parser.ForRangeContext forRangeCtx,
      final DebugInfo debugInfo,
      final String outerScopeId) {

    final var instructions = new ArrayList<IRInstr>();

    // Get loop variable name
    final var loopVariableName = forRangeCtx.identifier(0).getText();

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
    final var rangeTypeName = typeNameOrException.apply(initializationData.rangeType);
    instructions.add(MemoryInstr.reference(loopVariableName, rangeTypeName, debugInfo));

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
        final var byTypeName = typeNameOrException.apply(byType);

        byDebugInfo = stackContext.createDebugInfo(literalCtx);
        final var byLiteralInstructions = new ArrayList<IRInstr>();
        byLiteralInstructions.add(LiteralInstr.literal(
            byTemp,
            literalCtx.getText(),
            byTypeName,
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
        final var byTypeName = typeNameOrException.apply(byType);

        byDebugInfo = stackContext.createDebugInfo(identCtx);
        final var byRefInstructions = new ArrayList<IRInstr>();
        byRefInstructions.add(MemoryInstr.reference(identCtx.getText(), byTypeName, byDebugInfo));
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
    final var extractionParams = new BooleanExtractionParams(
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
    final var equalCase = generateEqualCase(
        loopVariableName, initData.currentTemp, initData.directionTemp, debugInfo);

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
    // PLUS: when BY clause present, validate that 'by' moves us TOWARD the end
    // This is type-agnostic: works with Integer, Duration, Date, or any user type
    // Check: (start + by) <=> start must have same sign as required direction
    final var directionCheckResult = generateDirectionCheckWithByValidation(
        config.directionOperator(),
        initData.directionTemp,
        initData.startTemp,
        initData.byTemp,
        initData.rangeType,
        debugInfo);

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
   * <p>
   * Includes explicit direction check to prevent fall-through execution when
   * BY validation fails for ascending/descending cases.
   * </p>
   */
  private ForRangePolymorphicInstr.EqualCase generateEqualCase(
      final String loopVariableName,
      final String currentTemp,
      final String directionTemp,
      final DebugInfo debugInfo) {

    // Direction check: direction == 0 (meaning start == end)
    final var directionCheck = generateDirectionEqualZero(directionTemp, debugInfo);

    // Body setup: loopVariable = current
    final var loopBodySetup = generateBodySetup(loopVariableName, currentTemp, debugInfo);

    return new ForRangePolymorphicInstr.EqualCase(
        directionCheck.instructions(),
        directionCheck.primitiveVariableName(),
        loopBodySetup,
        true  // Single iteration flag
    );
  }

  /**
   * Generate direction check: direction == 0 (for equal case).
   * Returns structured result with IR sequence and primitive boolean variable.
   */
  private ConditionEvaluationResult generateDirectionEqualZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var params = new DirectionCheckParams(
        directionTemp,
        operatorMap.getForward("=="),
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
   * Generate direction check: direction &lt; 0.
   * Returns structured result with IR sequence and primitive boolean variable.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private ConditionEvaluationResult generateDirectionLessThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var params = new DirectionCheckParams(
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
  private ConditionEvaluationResult generateDirectionGreaterThanZero(
      final String directionTemp,
      final DebugInfo debugInfo) {

    final var params = new DirectionCheckParams(
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
   * Generate direction check with BY value validation.
   * <p>
   * When a BY clause is present, we must verify that applying '+= by' moves us
   * TOWARD the end, not away from it. This check is type-agnostic and works with
   * Integer, Duration, Date, or any user-defined type.
   * </p>
   * <p>
   * The validation computes: (start + by) &lt;=&gt; start
   * <ul>
   *   <li>For ascending (direction &lt; 0): result must be &gt; 0 (by moves us UP)</li>
   *   <li>For descending (direction &gt; 0): result must be &lt; 0 (by moves us DOWN)</li>
   * </ul>
   * </p>
   * <p>
   * Without this check, mismatched direction/by combinations cause infinite loops:
   * <ul>
   *   <li>{@code for i in 1 ... 10 by -1} would loop: 1, 0, -1, -2, ... forever</li>
   *   <li>{@code for i in 10 ... 1 by 2} would loop: 10, 12, 14, ... forever</li>
   * </ul>
   * </p>
   */
  private ConditionEvaluationResult generateDirectionCheckWithByValidation(
      final String directionOperator,
      final String directionTemp,
      final String startTemp,
      final String byTemp,  // nullable - no BY clause
      final ISymbol rangeType,
      final DebugInfo debugInfo) {

    // Generate basic direction check (produces EK9 Boolean object and primitive)
    final var basicCheck = directionOperator.equals("<")
        ? generateDirectionLessThanZero(directionTemp, debugInfo)
        : generateDirectionGreaterThanZero(directionTemp, debugInfo);

    // If no BY clause, just return the basic direction check
    if (byTemp == null) {
      return basicCheck;
    }

    // BY clause present: validate that 'by' moves us toward the end
    // We combine both checks using Boolean._and() method at the EK9 object level
    // This avoids JVM issues with primitive boolean storage across control flow paths
    return generateCombinedDirectionAndByCheck(
        directionOperator,
        directionTemp,
        startTemp,
        byTemp,
        rangeType,
        debugInfo
    );
  }

  /**
   * Generate combined direction and BY validation check.
   * <p>
   * Produces both Boolean objects and combines them with _and() before extracting primitive.
   * This approach avoids JVM bytecode verification issues with primitive boolean storage.
   * </p>
   */
  private ConditionEvaluationResult generateCombinedDirectionAndByCheck(
      final String directionOperator,
      final String directionTemp,
      final String startTemp,
      final String byTemp,
      final ISymbol rangeType,
      final DebugInfo debugInfo) {

    // Temps for direction check
    final var zeroTemp1 = stackContext.generateTempName();
    final var directionBooleanTemp = stackContext.generateTempName();

    // Temps for BY validation
    final var testValueTemp = stackContext.generateTempName();
    final var byDirectionTemp = stackContext.generateTempName();
    final var zeroTemp2 = stackContext.generateTempName();
    final var byBooleanTemp = stackContext.generateTempName();

    // Temps for combining
    final var combinedBooleanTemp = stackContext.generateTempName();
    final var combinedPrimitiveTemp = stackContext.generateTempName();

    final var booleanType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
    final var integerType = stackContext.getParsedModule().getEk9Types().ek9Integer();
    final var requiredSign = directionOperator.equals("<") ? ">" : "<";

    final var instructions = generators.scopedInstructionExecutor.execute(() -> {

      // === Part 1: Direction check (direction < 0 or direction > 0) ===
      // Load literal 0 for direction comparison
      final var literal1Params = new LiteralParams(zeroTemp1, "0", integerType, debugInfo);
      final var scopedInstructions = new ArrayList<>(generators.managedLiteralLoader.apply(literal1Params));

      // Compare direction with 0 (produces Boolean object, no primitive extraction yet)
      final var directionCmpParams = new BinaryOperatorParams(
          directionTemp,
          zeroTemp1,
          operatorMap.getForward(directionOperator),
          integerType,
          integerType,
          booleanType,
          directionBooleanTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.binaryOperatorInvoker.apply(directionCmpParams));

      // === Part 2: BY validation ((start + by) <=> start) compared to 0 ===
      // testValue = start + by
      final var addParams = new BinaryOperatorParams(
          startTemp,
          byTemp,
          operatorMap.getForward("+"),
          rangeType,
          rangeType,
          rangeType,
          testValueTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.binaryOperatorInvoker.apply(addParams));

      // byDirection = testValue <=> start
      final var cmpParams = new BinaryOperatorParams(
          testValueTemp,
          startTemp,
          operatorMap.getForward("<=>"),
          rangeType,
          rangeType,
          integerType,
          byDirectionTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.binaryOperatorInvoker.apply(cmpParams));

      // Load literal 0 for BY comparison
      final var literal2Params = new LiteralParams(zeroTemp2, "0", integerType, debugInfo);
      scopedInstructions.addAll(generators.managedLiteralLoader.apply(literal2Params));

      // byOk = byDirection > 0 (ascending) or byDirection < 0 (descending)
      final var byCmpParams = new BinaryOperatorParams(
          byDirectionTemp,
          zeroTemp2,
          operatorMap.getForward(requiredSign),
          integerType,
          integerType,
          booleanType,
          byBooleanTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.binaryOperatorInvoker.apply(byCmpParams));

      // === Part 3: Combine with Boolean._and() ===
      // combined = directionOk._and(byOk)
      final var andParams = new BinaryOperatorParams(
          directionBooleanTemp,
          byBooleanTemp,
          operatorMap.getForward("and"),
          booleanType,
          booleanType,
          booleanType,
          combinedBooleanTemp,
          stackContext.currentScopeId(),
          debugInfo
      );
      scopedInstructions.addAll(generators.binaryOperatorInvoker.apply(andParams));

      // === Part 4: Extract primitive from combined Boolean ===
      final var extractionParams = new BooleanExtractionParams(
          combinedBooleanTemp,
          combinedPrimitiveTemp,
          debugInfo
      );
      scopedInstructions.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));

      return scopedInstructions;
    }, debugInfo);

    return new ConditionEvaluationResult(instructions, combinedPrimitiveTemp);
  }

  /**
   * Generate loop condition: current (&lt;=|&gt;=) end.
   * Returns structured result with IR sequence and primitive boolean variable.
   * Wraps all temps in SCOPE_ENTER/EXIT for proper memory management.
   */
  private ConditionEvaluationResult generateLoopCondition(
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
      final var cmpParams = new BinaryOperatorParams(
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
      final var literalParams = new LiteralParams(
          zeroTemp,
          "0",
          stackContext.getParsedModule().getEk9Types().ek9Integer(),
          debugInfo
      );
      scopedInstructions.addAll(generators.managedLiteralLoader.apply(literalParams));

      // Step 3: Compare cmpResult (<=|>=) 0 and extract primitive boolean
      final var comparisonParams = new ComparisonParams(
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

    return new ConditionEvaluationResult(instructions, primitiveBooleanTemp);
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
      final var params = new IncrementParams(
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
   * <p>
   * Note: += is a mutating operator that modifies 'current' in place and returns void.
   * No result storage is needed - the left operand is updated directly.
   * </p>
   */
  private List<IRInstr> generateIncrementBy(
      final String currentTemp,
      final String byTemp,
      final ISymbol rangeType,
      final ISymbol byType,
      final DebugInfo debugInfo) {

    return generators.scopedInstructionExecutor.execute(() -> {

      // Call current += by - this mutates current in place (returns void)
      // Pass null for result type and result temp since += returns void
      final var params = new BinaryOperatorParams(
          currentTemp,
          byTemp,
          operatorMap.getForward("+="),
          rangeType,
          byType,
          null,  // Mutating operator returns void - no return type
          null,  // No result temp needed - operator mutates in place
          stackContext.currentScopeId(),
          debugInfo
      );

      // No store needed after call - += mutates currentTemp in place
      return new ArrayList<>(generators.binaryOperatorInvoker.apply(params));
    }, debugInfo);
  }

  /**
   * Generate loop metadata containing variable names and types.
   */
  private ForRangePolymorphicInstr.LoopMetadata generateLoopMetadata(
      final InitializationData initData,
      final String loopVariableName) {

    final var rangeTypeName = typeNameOrException.apply(initData.rangeType);
    final var byTypeName = initData.byType != null ? typeNameOrException.apply(initData.byType) : null;

    return new ForRangePolymorphicInstr.LoopMetadata(
        initData.directionTemp,
        initData.currentTemp,
        loopVariableName,
        initData.endTemp,
        rangeTypeName,
        initData.byTemp,
        byTypeName
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
}
