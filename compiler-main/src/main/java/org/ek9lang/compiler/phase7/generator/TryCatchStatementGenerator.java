package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
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
 * Currently handles simple try/catch/finally (no guards, statement form only).
 * <p>
 * Scope structure:
 * </p>
 * <pre>
 *   Outer Scope (_scope_1): Try/catch wrapper for future guards
 *   Control Flow Scope (_scope_2): Try/catch control structure
 *   Try Scope (_scope_3): Try block execution (isolated)
 *   Catch Scope (_scope_4): Catch handler execution (isolated, if present)
 *   Finally Scope (_scope_5): Finally block execution (if present)
 * </pre>
 * <p>
 * EK9 supports SINGLE catch block only (not multiple).
 * Both 'catch' and 'handle' keywords are supported.
 * </p>
 */
public final class TryCatchStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.TryStatementExpressionContext, List<IRInstr>> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final GeneratorSet generators;

  public TryCatchStatementGenerator(final IRGenerationContext stackContext,
                                    final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.TryStatementExpressionContext ctx) {
    AssertValue.checkNotNull("TryStatementExpressionContext cannot be null", ctx);

    // Check for expression form (returningParam)
    if (ctx.returningParam() != null) {
      throw new CompilerException("Try expression form not yet implemented");
    }

    // Check for guards (preFlowStatement)
    if (ctx.preFlowStatement() != null) {
      throw new CompilerException("Try with guards not yet implemented");
    }

    // Check for resource management (declareArgumentParam)
    if (ctx.declareArgumentParam() != null) {
      return generateTryWithResources(ctx);
    }

    // Simple try/catch/finally (statement form)
    return generateSimpleTryCatchFinally(ctx);
  }

  /**
   * Generate IR for simple try/catch/finally statement.
   * Follows the same two-scope pattern as while loops for architectural consistency.
   */
  private List<IRInstr> generateSimpleTryCatchFinally(
      final EK9Parser.TryStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 1: Enter outer scope (for future guards)
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

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
        null,                           // No result (statement form)
        GuardVariableDetails.none(),    // No guards yet
        ReturnVariableDetails.none(),   // No return variable (statement form)
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

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate IR for try-with-resources statement.
   * <p>
   * Scope structure:
   * </p>
   * <pre>
   *   Outer Scope (_scope_1): Try/catch wrapper
   *   Resource Scope (_scope_2): Resource initialization and management
   *   Control Flow Scope (_scope_3): Try/catch control structure
   *   Try Scope (_scope_4): Try block execution
   *   Catch Scope (_scope_5): Catch handler execution (if present)
   *   Finally Scope (_scope_6): Finally block execution (synthetic or explicit)
   * </pre>
   * <p>
   * Resources are initialized in _scope_2 with RETAIN + SCOPE_REGISTER.
   * Resources are closed automatically in EVALUATION (finally) in REVERSE order.
   * close() executes BEFORE user's finally code (matches Java semantics).
   * </p>
   */
  private List<IRInstr> generateTryWithResources(
      final EK9Parser.TryStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 1: Enter outer scope
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

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
        null,                           // No result (statement form)
        GuardVariableDetails.none(),    // No guards
        ReturnVariableDetails.none(),   // No return variable (statement form)
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

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

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

      // RETAIN tempResult (producer pattern - creates ownership)
      instructions.add(MemoryInstr.retain(tempResult, resourceDebugInfo));

      // SCOPE_REGISTER tempResult, resourceScopeId (consumer pattern - registers for cleanup)
      instructions.add(ScopeInstr.register(tempResult, resourceScopeId, resourceDebugInfo));

      // STORE resource, tempResult
      instructions.add(MemoryInstr.store(resourceName, tempResult, resourceDebugInfo));

      // Track resource for automatic close() generation
      resources.add(new ResourceVariable(resourceSymbol, resourceDebugInfo));
    }

    return new ResourceVariables(instructions, resources);
  }

  /**
   * Process finally block with automatic resource cleanup.
   * <p>
   * Creates synthetic finally block if user didn't provide one (when resources present).
   * close() calls execute BEFORE user's finally code (matches Java semantics).
   * Resources closed in REVERSE order: last declared, first closed.
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

    // AUTO-GENERATED: close() calls FIRST, REVERSE order (last declared, first closed)
    for (int i = resources.size() - 1; i >= 0; i--) {
      final var resource = resources.get(i);
      final var resourceSymbol = resource.symbol();
      final var resourceDebugInfo = resource.debugInfo();

      // Generate call to resource.close() operator
      // close() is an operator, so method name is "_close"
      final var resourceName = resourceSymbol.getName();
      final var resourceTypeName = typeNameOrException.apply(resourceSymbol);

      // Create CallDetails for close() operator
      final var closeCallDetails = new CallDetails(
          resourceName,                                    // target object
          resourceTypeName,                                // target type
          "_close",                                        // method name (operator close)
          List.of(),                                       // no parameter types
          IRConstants.VOID,                                // returns void
          List.of(),                                       // no arguments
          new CallMetaDataDetails(true, 0),                // pure=true, complexity=0
          false                                            // not trait call
      );

      // CALL resource._close() - null result (void return)
      finallyEvaluation.add(CallInstr.call(null, resourceDebugInfo, closeCallDetails));
    }

    // User's finally code SECOND (after close() calls, if user provided finally)
    if (finallyCtx != null) {
      finallyEvaluation.addAll(processBlockStatements(finallyCtx.block()));
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
