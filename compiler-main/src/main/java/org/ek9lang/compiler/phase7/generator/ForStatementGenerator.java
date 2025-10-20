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

    // Phase 3: Create dispatch metadata
    final var dispatchMetadata = createDispatchMetadata(
        initializationData,
        loopVariableName
    );

    // Phase 4: Create scope metadata
    final var scopeMetadata = new ForRangePolymorphicInstr.ScopeMetadata(
        outerScopeId,
        loopScopeId,
        bodyScopeId
    );

    // Phase 5: Create the polymorphic loop instruction
    instructions.add(ForRangePolymorphicInstr.forRangePolymorphic(
        initializationData.instructions,
        dispatchMetadata,
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
    instructions.addAll(generators.exprGenerator.apply(
        new ExprProcessingDetails(forRangeCtx.range().expression(0), startResult)
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
    instructions.addAll(generators.exprGenerator.apply(
        new ExprProcessingDetails(forRangeCtx.range().expression(1), endResult)
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
        byRefInstructions.add(MemoryInstr.load(loadTemp, identCtx.getText(), debugInfo));
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
    instructions.addAll(isSetCallResult.allInstructions());
    instructions.add(CallInstr.operator(isSetTemp, debugInfo, isSetCallResult.callDetails()));

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
   * Create dispatch metadata describing polymorphic operator selection.
   */
  private ForRangePolymorphicInstr.DispatchMetadata createDispatchMetadata(
      final InitializationData initData,
      final String loopVariableName) {

    // Determine operators based on BY clause presence
    final String ascendingIncrement;
    final String descendingIncrement;

    if (initData.byTemp != null) {
      // With BY: always use += (sign of BY determines direction)
      ascendingIncrement = operatorMap.getForward("+=");  // "_addAss"
      descendingIncrement = operatorMap.getForward("+="); // "_addAss"
    } else {
      // Without BY: use ++ for ascending, -- for descending
      ascendingIncrement = operatorMap.getForward("++");  // "_inc"
      descendingIncrement = operatorMap.getForward("--"); // "_dec"
    }

    final var ascending = new ForRangePolymorphicInstr.DispatchCase(
        operatorMap.getForward("<="),  // "_lteq"
        ascendingIncrement
    );

    final var descending = new ForRangePolymorphicInstr.DispatchCase(
        operatorMap.getForward(">="),  // "_gteq"
        descendingIncrement
    );

    return new ForRangePolymorphicInstr.DispatchMetadata(
        initData.directionTemp,
        initData.currentTemp,
        loopVariableName,
        initData.endTemp,
        initData.rangeType.getFullyQualifiedName(),
        initData.byTemp,
        initData.byType != null ? initData.byType.getFullyQualifiedName() : null,
        ascending,
        descending,
        true  // Equal case does single iteration
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
