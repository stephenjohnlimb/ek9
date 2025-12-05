package org.ek9lang.compiler.phase7.generator;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.data.ReturnVariableDetails;
import org.ek9lang.compiler.ir.data.TryBlockDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for try/catch/finally constructs using CONTROL_FLOW_CHAIN.
 * <p>
 * Supports both statement and expression forms:
 * </p>
 * <ul>
 *   <li>Statement form: {@code try { body }} - no return value</li>
 *   <li>Expression form: {@code result <- try <- rtn <- defaultValue { body }} - accumulator pattern</li>
 * </ul>
 * <p>
 * Scope structure:
 * </p>
 * <pre>
 *   Outer Scope (_scope_1): Try/catch wrapper for guards + return variable (expression form)
 *   Control Flow Scope (_scope_2): Try/catch control structure
 *   Try Scope (_scope_3): Try block execution (isolated)
 *   Catch Scope (_scope_4): Catch handler execution (isolated, if present)
 *   Finally Scope (_scope_5): Finally block execution (if present)
 * </pre>
 * <p>
 * For expression form, the return variable is declared in the outer scope and can be
 * assigned in both try and catch blocks. The finally block executes for cleanup
 * but does not affect the return value.
 * </p>
 * <p>
 * EK9 supports SINGLE catch block only (not multiple).
 * Both 'catch' and 'handle' keywords are supported.
 * </p>
 */
public final class TryCatchStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.TryStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final OperatorMap operatorMap = new OperatorMap();
  private final LoopGuardHelper loopGuardHelper;
  private final ReturningParamProcessor returningParamProcessor;

  public TryCatchStatementGenerator(final IRGenerationContext stackContext,
                                    final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.loopGuardHelper = new LoopGuardHelper(stackContext, generators);
    this.returningParamProcessor = new ReturningParamProcessor(stackContext, generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.TryStatementExpressionContext ctx) {
    AssertValue.checkNotNull("TryStatementExpressionContext cannot be null", ctx);

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 1: Enter outer scope (guard scope + return variable for expression form)
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(ctx.preFlowStatement(), outerScopeId);

    // Process return variable for expression form (declared in outer scope)
    final var returnDetails = returningParamProcessor.process(ctx.returningParam(), outerScopeId);

    // Generate the core try/catch/finally (with or without resources)
    final List<IRInstr> tryInstructions;
    if (ctx.declareArgumentParam() != null) {
      tryInstructions = generateTryWithResourcesCore(ctx, returnDetails, debugInfo);
    } else {
      tryInstructions = generateSimpleTryCatchFinallyCore(ctx, returnDetails, debugInfo);
    }

    // If guard has entry check, wrap try in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      // For expression form with guards, use wrapExpressionFormWithGuardEntryCheck
      // This emits return variable setup OUTSIDE IF wrapper so variable exists on all code paths
      instructions = loopGuardHelper.wrapExpressionFormWithGuardEntryCheck(
          guardDetails,
          returnDetails.returnVariableSetup(),  // Return variable init goes OUTSIDE IF
          tryInstructions,                      // Body goes INSIDE IF
          outerScopeId,
          debugInfo);
    } else {
      // No guard - just add scope enter/exit around the try
      instructions = new ArrayList<>();
      instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));
      // Emit return variable setup in outer scope (expression form)
      instructions.addAll(returnDetails.returnVariableSetup());
      instructions.addAll(tryInstructions);
      instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    }

    // Exit outer scope from stack context
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate IR for simple try/catch/finally statement (without guard wrapping).
   * The outer scope is managed by the caller (apply method).
   * <p>
   * For expression form, the return variable is already declared in outer scope.
   * The try and catch blocks include assignments to the return variable.
   * </p>
   */
  private List<IRInstr> generateSimpleTryCatchFinallyCore(
      final EK9Parser.TryStatementExpressionContext ctx,
      final ReturnVariableDetails returnDetails,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // SCOPE 2: Enter control flow scope (try/catch control structure)
    final var controlFlowScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(controlFlowScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(controlFlowScopeId, debugInfo));

    // Process try block
    final var tryBlockDetails = processTryBlock(ctx);

    // Process catch handler (if present)
    final var catchHandler = processCatchHandler(ctx);

    // Process finally block (if present)
    final var finallyEvaluation = processFinallyBlock(ctx);

    // Create CONTROL_FLOW_CHAIN
    final var tryCatchDetails = ControlFlowChainDetails.createTryCatchFinally(
        null,                           // No result (expression result via return variable)
        GuardVariableDetails.none(),    // No guards yet
        returnDetails,                  // Return variable for expression form
        tryBlockDetails,                // Try block details
        catchHandler.map(List::of).orElse(List.of()), // Single catch handler or empty
        finallyEvaluation,              // Finally block instructions
        debugInfo,
        controlFlowScopeId
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(tryCatchDetails));

    // Exit control flow scope
    instructions.add(ScopeInstr.exit(controlFlowScopeId, debugInfo));
    stackContext.exitScope();

    // Note: outer scope exit is handled by the caller (apply method)

    return instructions;
  }

  /**
   * Generate IR for try-with-resources statement (without guard wrapping).
   * The outer scope is managed by the caller (apply method).
   * <p>
   * Scope structure:
   * </p>
   * <pre>
   *   Resource Scope (_scope_2): Resource initialization and management
   *   Control Flow Scope (_scope_3): Try/catch control structure
   *   Try Scope (_scope_4): Try block execution
   *   Catch Scope (_scope_5): Catch handler execution (if present)
   *   Finally Scope (_scope_6): Finally block execution (synthetic or explicit)
   * </pre>
   * <p>
   * Resources are initialized in resource scope with RETAIN + SCOPE_REGISTER.
   * Resources are closed automatically in EVALUATION (finally) in REVERSE order.
   * close() executes BEFORE user's finally code (matches Java semantics).
   * </p>
   * <p>
   * For expression form, the return variable is already declared in outer scope.
   * The try and catch blocks include assignments to the return variable.
   * </p>
   */
  private List<IRInstr> generateTryWithResourcesCore(
      final EK9Parser.TryStatementExpressionContext ctx,
      final ReturnVariableDetails returnDetails,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // SCOPE 2: Enter resource scope for resource initialization
    final var resourceScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(resourceScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(resourceScopeId, debugInfo));

    // Initialize resources with ARC (RETAIN + SCOPE_REGISTER)
    final var resourceVariables = processResourceDeclarations(ctx, resourceScopeId);
    instructions.addAll(resourceVariables.instructions());

    // SCOPE 3: Enter control flow scope (try/catch control structure)
    final var controlFlowScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(controlFlowScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(controlFlowScopeId, debugInfo));

    // Process try block
    final var tryBlockDetails = processTryBlock(ctx);

    // Process catch handler (if present)
    final var catchHandler = processCatchHandler(ctx);

    // Process finally block with auto-generated resource cleanup
    final var finallyEvaluation = processFinallyBlockWithResourceCleanup(
        ctx, resourceVariables.resources());

    // Create CONTROL_FLOW_CHAIN
    final var tryCatchDetails = ControlFlowChainDetails.createTryCatchFinally(
        null,                           // No result (expression result via return variable)
        GuardVariableDetails.none(),    // No guards
        returnDetails,                  // Return variable for expression form
        tryBlockDetails,                // Try block details
        catchHandler.map(List::of).orElse(List.of()), // Single catch handler or empty
        finallyEvaluation,              // Finally block with resource cleanup
        debugInfo,
        controlFlowScopeId
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(tryCatchDetails));

    // Exit control flow scope
    instructions.add(ScopeInstr.exit(controlFlowScopeId, debugInfo));
    stackContext.exitScope();

    // Exit resource scope (triggers RELEASE for all registered resources)
    instructions.add(ScopeInstr.exit(resourceScopeId, debugInfo));
    stackContext.exitScope();

    // Note: outer scope exit is handled by the caller (apply method)

    return instructions;
  }

  /**
   * Process resource declarations from declareArgumentParam.
   * Each resource is initialized with ARC pattern: RETAIN + SCOPE_REGISTER.
   * Resources are tracked for later automatic cleanup in reverse order.
   */
  private ResourceVariables processResourceDeclarations(
      final EK9Parser.TryStatementExpressionContext ctx,
      final String resourceScopeId) {

    final var instructions = new ArrayList<IRInstr>();
    final var resources = new ArrayList<ResourceVariable>();

    // Get variable declarations from declareArgumentParam
    final var declareParam = ctx.declareArgumentParam();
    AssertValue.checkNotNull("declareArgumentParam cannot be null", declareParam);

    final var variableDeclarations = declareParam.variableDeclaration();
    AssertValue.checkTrue("Must have at least one resource", !variableDeclarations.isEmpty());

    // Process each resource declaration
    for (var varDecl : variableDeclarations) {
      final var resourceDebugInfo = stackContext.createDebugInfo(varDecl);

      // Get the symbol for the resource variable
      final var resourceSymbol = getRecordedSymbolOrException(varDecl);
      final var resourceName = resourceSymbol.getName();
      final var resourceType = typeNameOrException.apply(resourceSymbol);

      // REFERENCE resource, Type
      instructions.add(MemoryInstr.reference(resourceName, resourceType, resourceDebugInfo));

      // Get the assignment expression (initialization)
      final var assignmentExpr = varDecl.assignmentExpression();
      if (assignmentExpr == null) {
        throw new CompilerException("Resource must be initialized: " + resourceName);
      }

      // Generate temp for initialization result
      final var tempResult = stackContext.generateTempName();

      // Generate IR for resource initialization expression (using ExprProcessingDetails)
      // assignmentExpression contains an expression() subnode
      final var exprDetails = new org.ek9lang.compiler.phase7.support.ExprProcessingDetails(
          assignmentExpr.expression(),
          new org.ek9lang.compiler.phase7.support.VariableDetails(tempResult, resourceDebugInfo)
      );
      final var initInstructions = generators.exprGenerator.apply(exprDetails);
      instructions.addAll(initInstructions);

      // STORE resource, tempResult (create actual variable first)
      instructions.add(MemoryInstr.store(resourceName, tempResult, resourceDebugInfo));

      // SCOPE_REGISTER resource, resourceScopeId (register ACTUAL variable to outermost scope)
      instructions.add(ScopeInstr.register(resourceName, resourceScopeId, resourceDebugInfo));

      // RETAIN resource (ARC manage actual variable)
      instructions.add(MemoryInstr.retain(resourceName, resourceDebugInfo));

      // Track resource for automatic close() generation
      resources.add(new ResourceVariable(resourceSymbol, resourceDebugInfo));
    }

    return new ResourceVariables(instructions, resources);
  }

  /**
   * Process finally block with automatic resource cleanup.
   * <p>
   * Creates synthetic finally block if user didn't provide one (when resources present).
   * User's finally code executes FIRST (so user can still use resources).
   * close() calls execute AFTER user's finally code (compiler-added cleanup).
   * Resources closed in REVERSE order: last declared, first closed.
   * </p>
   * <p>
   * This differs from Java where close() runs before explicit finally.
   * EK9's approach ensures user's cleanup code has access to valid resources
   * and is guaranteed to run even if close() throws an exception.
   * </p>
   */
  private List<IRInstr> processFinallyBlockWithResourceCleanup(
      final EK9Parser.TryStatementExpressionContext ctx,
      final List<ResourceVariable> resources) {

    // If no finally AND no resources, return empty (no EVALUATION block)
    if (ctx.finallyStatementExpression() == null && resources.isEmpty()) {
      return List.of();
    }

    final var finallyCtx = ctx.finallyStatementExpression();
    final var debugInfo = finallyCtx != null
        ? stackContext.createDebugInfo(finallyCtx)
        : stackContext.createDebugInfo(ctx);  // Use try debug info for synthetic finally

    // Create finally scope (explicit OR synthetic)
    final var finallyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(finallyScopeId, debugInfo, IRFrameType.BLOCK);

    final var finallyEvaluation = new ArrayList<IRInstr>();
    finallyEvaluation.add(ScopeInstr.enter(finallyScopeId, debugInfo));

    // User's finally code FIRST (if user provided explicit finally block)
    // This ensures user's cleanup code can still use resources before they're closed
    if (finallyCtx != null) {
      finallyEvaluation.addAll(processBlockStatements(finallyCtx.block()));
    }

    // AUTO-GENERATED: close() calls SECOND (after user's finally code)
    // Resources closed in REVERSE order (last declared, first closed)
    // This is "invisible" to the EK9 developer - compiler adds close() after their code
    for (int i = resources.size() - 1; i >= 0; i--) {
      final var resource = resources.get(i);
      final var resourceSymbol = resource.symbol();
      final var resourceDebugInfo = resource.debugInfo();

      // Generate call to resource.close() operator
      // Use OperatorMap to get correct method name for "close" operator
      final var resourceName = resourceSymbol.getName();
      final var resourceTypeName = typeNameOrException.apply(resourceSymbol);
      final var closeMethodName = operatorMap.getForward("close");

      // Create CallDetails for close() operator
      final var closeCallDetails = new CallDetails(
          resourceName,                                    // target object
          resourceTypeName,                                // target type
          closeMethodName,                                 // method name from OperatorMap
          List.of(),                                       // no parameter types
          EK9_VOID,                                        // returns org.ek9.lang::Void
          List.of(),                                       // no arguments
          new CallMetaDataDetails(true, 0),                // pure=true, complexity=0
          false                                            // not trait call
      );

      // CALL resource.close() - null result (void return)
      finallyEvaluation.add(CallInstr.call(null, resourceDebugInfo, closeCallDetails));
    }

    finallyEvaluation.add(ScopeInstr.exit(finallyScopeId, debugInfo));

    // Exit finally scope from context
    stackContext.exitScope();

    return finallyEvaluation;
  }

  /**
   * Process try block with isolated scope.
   */
  private TryBlockDetails processTryBlock(final EK9Parser.TryStatementExpressionContext ctx) {
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 3: Try block scope
    final var tryScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(tryScopeId, debugInfo, IRFrameType.BLOCK);

    // Process try block body
    final var tryBodyEvaluation = new ArrayList<IRInstr>();
    tryBodyEvaluation.add(ScopeInstr.enter(tryScopeId, debugInfo));

    // Process instruction block if present
    if (ctx.instructionBlock() != null) {
      tryBodyEvaluation.addAll(processBlockStatements(ctx.instructionBlock()));
    }

    tryBodyEvaluation.add(ScopeInstr.exit(tryScopeId, debugInfo));

    // Exit try scope from context
    stackContext.exitScope();

    return new TryBlockDetails(
        tryScopeId,
        tryBodyEvaluation,
        null  // No result (statement form)
    );
  }

  /**
   * Process catch handler (if present).
   * EK9 supports single catch block only.
   */
  private Optional<ConditionCaseDetails> processCatchHandler(
      final EK9Parser.TryStatementExpressionContext ctx) {

    final var catchCtx = ctx.catchStatementExpression();
    if (catchCtx == null) {
      return Optional.empty();
    }

    final var debugInfo = stackContext.createDebugInfo(catchCtx);

    // Extract exception type and variable name from argumentParam
    // Grammar: (CATCH|HANDLE) NL+ INDENT NL* argumentParam instructionBlock DEDENT
    // argumentParam: '->' variableOnlyDeclaration
    // variableOnlyDeclaration: identifier AS typeDef
    final var argumentParam = catchCtx.argumentParam();
    if (argumentParam == null) {
      throw new CompilerException("Catch handler must have exception parameter (-> ex as Exception)");
    }

    // Get the variableOnlyDeclaration from argumentParam
    final var varDecls = argumentParam.variableOnlyDeclaration();
    if (varDecls.isEmpty()) {
      throw new CompilerException("Catch handler argumentParam must have variableOnlyDeclaration");
    }
    final var varDecl = varDecls.getFirst();  // Get first (and only) declaration

    final var exceptionVariable = varDecl.identifier().getText();
    final var typeDef = varDecl.typeDef();

    // typeDef contains identifierReference
    final var identifierRef = typeDef.identifierReference();
    if (identifierRef == null) {
      throw new CompilerException("Catch handler must specify exception type");
    }

    // Resolve exception type to fully qualified name
    final var exceptionSymbol = getRecordedSymbolOrException(identifierRef);
    final var exceptionType = exceptionSymbol.getFullyQualifiedName();

    // SCOPE 4: Catch block scope
    final var catchScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(catchScopeId, debugInfo, IRFrameType.BLOCK);

    // Process catch block body
    final var catchBodyEvaluation = new ArrayList<IRInstr>();
    catchBodyEvaluation.add(ScopeInstr.enter(catchScopeId, debugInfo));

    // ARC ownership transfer pattern: Exception variable
    // Backend creates exception with refcount=1 and stores to variable slot
    // Catch handler receives ownership: REFERENCE + SCOPE_REGISTER (no RETAIN)
    catchBodyEvaluation.add(MemoryInstr.reference(exceptionVariable, exceptionType, debugInfo));
    catchBodyEvaluation.add(ScopeInstr.register(exceptionVariable, catchScopeId, debugInfo));

    // Process instruction block
    catchBodyEvaluation.addAll(processBlockStatements(catchCtx.instructionBlock()));

    catchBodyEvaluation.add(ScopeInstr.exit(catchScopeId, debugInfo));

    // Exit catch scope from context
    stackContext.exitScope();

    // Create exception handler ConditionCaseDetails
    final var catchHandler = ConditionCaseDetails.createExceptionHandler(
        catchScopeId,           // Catch scope ID
        exceptionType,          // Fully qualified exception type
        exceptionVariable,      // Exception variable name
        null,                   // No separate body scope (body uses catch scope)
        catchBodyEvaluation,    // Catch body instructions
        null                    // No result (statement form)
    );

    return Optional.of(catchHandler);
  }

  /**
   * Process finally block (if present).
   */
  private List<IRInstr> processFinallyBlock(final EK9Parser.TryStatementExpressionContext ctx) {
    final var finallyCtx = ctx.finallyStatementExpression();
    if (finallyCtx == null) {
      return List.of();
    }

    final var debugInfo = stackContext.createDebugInfo(finallyCtx);

    // SCOPE 5: Finally block scope
    final var finallyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(finallyScopeId, debugInfo, IRFrameType.BLOCK);

    // Process finally block body
    final var finallyEvaluation = new ArrayList<IRInstr>();
    finallyEvaluation.add(ScopeInstr.enter(finallyScopeId, debugInfo));

    // Process block statements
    finallyEvaluation.addAll(processBlockStatements(finallyCtx.block()));

    finallyEvaluation.add(ScopeInstr.exit(finallyScopeId, debugInfo));

    // Exit finally scope from context
    stackContext.exitScope();

    return finallyEvaluation;
  }

  /**
   * Process all block statements in an instruction block.
   */
  private List<IRInstr> processBlockStatements(final EK9Parser.InstructionBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();
    for (var blockStatement : ctx.blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }
    return instructions;
  }

  /**
   * Process all statements in a block (for finally block).
   */
  private List<IRInstr> processBlockStatements(final EK9Parser.BlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();
    final var instructionBlock = ctx.instructionBlock();
    if (instructionBlock != null) {
      for (var blockStatement : instructionBlock.blockStatement()) {
        instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
      }
    }
    return instructions;
  }

  /**
   * Holds resource variables and their initialization instructions.
   */
  private record ResourceVariables(
      List<IRInstr> instructions,
      List<ResourceVariable> resources
  ) {
  }

  /**
   * Tracks a single resource variable for automatic cleanup.
   */
  private record ResourceVariable(
      ISymbol symbol,
      DebugInfo debugInfo
  ) {
  }
}
