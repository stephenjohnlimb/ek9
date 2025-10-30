package org.ek9lang.compiler.phase7.generator;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Deals with the generation of the IR for OperationDetails.
 * This is used quite widely by many of the constructs defined in the grammar.
 * methods, operators, etc. In various construct types.
 * <p>
 * But the context for this is only for methods/operators that do actually have processing defined.
 * Note, that in Ek9 it is possible to use the 'default' reserved word to get EK9 to generate the
 * appropriate functionality. For example '?' _isSet can be auto generated if the properties on the aggregate
 * have the '?' _isSet Operator.
 * </p>
 * <p>
 * This processing does NOT deal with that.
 * </p>
 */
public final class OperationDfnGenerator implements BiConsumer<OperationInstr, EK9Parser.OperationDetailsContext> {

  private final IRGenerationContext stackContext;
  private final GeneratorSet generators;
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  public OperationDfnGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    AssertValue.checkNotNull("Stack context cannot be null", stackContext);
    AssertValue.checkNotNull("Generators cannot be null", generators);
    this.stackContext = stackContext;
    this.generators = generators;
  }

  @Override
  public void accept(final OperationInstr operation, final EK9Parser.OperationDetailsContext ctx) {

    // Create instruction builder - uses current method scope from stack
    var instructionBuilder = new IRInstructionBuilder(stackContext);

    // Add constructor initialization if needed
    if (operation.getSymbol() instanceof MethodSymbol method && method.isConstructor()) {
      // Check if developer provided explicit this() or super() call
      final var hasExplicitConstruction = method.getSquirrelledData(CommonValues.HAS_EXPLICIT_CONSTRUCTION);
      if (!"TRUE".equals(hasExplicitConstruction)) {
        // Only generate synthetic super/i_init if no explicit call
        instructionBuilder.addInstructions(generateConstructorInitialization(method));
      }
    }

    // REFACTOR: Create function body scope BEFORE processing parameters/returns/body
    // This ensures all function-local state (parameters, return variables, temporaries)
    // lives inside a proper scope, fixing the "_call" phantom scope bug.
    // See EK9_ARC_OWNERSHIP_TRANSFER_PATTERN.md for architectural rationale.
    final var hasInstructionBlock = ctx.instructionBlock() != null;
    final var hasReturnParameters = ctx.returningParam() != null;
    final var needsFunctionBodyScope = hasInstructionBlock || hasReturnParameters;

    String bodyScopeId = null;
    DebugInfo bodyScopeDebugInfo = null;

    if (needsFunctionBodyScope) {
      // Create function body scope
      bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
      // Use instruction block start if available, otherwise use operation start
      // Note: Both SCOPE_ENTER and SCOPE_EXIT use the same DebugInfo for symmetry
      bodyScopeDebugInfo = hasInstructionBlock
          ? stackContext.createDebugInfo(ctx.instructionBlock().start)
          : stackContext.createDebugInfo(ctx.start);

      // Enter scope on stack for child generators to access
      stackContext.enterScope(bodyScopeId, bodyScopeDebugInfo, IRFrameType.BLOCK);

      // Add SCOPE_ENTER instruction
      instructionBuilder.addInstruction(ScopeInstr.enter(bodyScopeId, bodyScopeDebugInfo));
    }

    // Process in correct order: parameters -> returns -> body (all inside function body scope)

    // 1. Process incoming parameters first (-> arg0 as String)
    if (ctx.argumentParam() != null) {
      instructionBuilder.addInstructions(processArgumentParam(ctx.argumentParam()));
    }

    // 2. Process return parameters second (<- rtn as String: String())
    if (ctx.returningParam() != null) {
      final var returnResult = processReturningParamWithScope(ctx.returningParam());
      instructionBuilder.addInstructions(returnResult.instructions());
    }

    // 3. Process instruction block statements (no new scope - already inside function body scope)
    if (hasInstructionBlock) {
      instructionBuilder.addInstructions(processInstructionBlockStatements(ctx.instructionBlock()));
    }

    // 4. Exit function body scope BEFORE return (scope cleanup must happen before function exit)
    if (needsFunctionBodyScope) {
      // Add SCOPE_EXIT instruction (uses same DebugInfo as SCOPE_ENTER for symmetry)
      instructionBuilder.addInstruction(ScopeInstr.exit(bodyScopeId, bodyScopeDebugInfo));

      // Pop scope from stack
      stackContext.exitScope();
    }

    // 5. Add return statement based on function signature (happens after scope cleanup)
    instructionBuilder.addInstructions(generateReturnStatement(operation));

    // Set body using fluent pattern
    operation.setBody(new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL))
        .addInstructions(instructionBuilder));
  }

  /**
   * Process argument parameters (incoming parameters like -> arg0 as String).
   * These are variable-only declarations that get allocated for the caller to populate.
   * Parameters are declared in the current method scope, not a separate parameter scope.
   * REFACTORED: Now reuses generator from shared tree instead of creating new instance.
   */
  private List<IRInstr> processArgumentParam(final EK9Parser.ArgumentParamContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    for (final var varOnlyCtx : ctx.variableOnlyDeclaration()) {
      // REFACTORED: Access generator directly from struct
      instructions.addAll(generators.variableOnlyDeclGenerator.apply(varOnlyCtx));
    }

    return instructions;
  }

  /**
   * Result record for return parameter processing.
   * STACK-BASED: Scope tracking is now handled via stack context.
   */
  private record ReturnParamResult(List<IRInstr> instructions) {
  }

  /**
   * Process returning parameters with scope tracking (<- rtn as String: String()).
   * These can be variable declarations (with initialization) or variable-only declarations.
   * REFACTORED: Now reuses generators from shared tree instead of creating new instances.
   */
  private ReturnParamResult processReturningParamWithScope(final EK9Parser.ReturningParamContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    // Process return variable declarations
    if (ctx.variableDeclaration() != null) {
      // Return variable with initialization: <- rtn as String: String()
      // REFACTORED: Access generator directly from struct
      instructions.addAll(generators.variableDeclGenerator.apply(ctx.variableDeclaration()));
    } else if (ctx.variableOnlyDeclaration() != null) {
      // Return variable without initialization: <- rtn as String
      // REFACTORED: Access generator directly from struct
      instructions.addAll(generators.variableOnlyDeclGenerator.apply(ctx.variableOnlyDeclaration()));
    }

    // Return variables are now managed in method scope - no separate return scope to track
    return new ReturnParamResult(instructions);
  }

  /**
   * Process instruction block statements WITHOUT creating a new scope.
   * Used when the function body scope has already been created (e.g., for functions with return parameters).
   * This ensures all function-local state lives in a single scope, avoiding the "_call" phantom scope bug.
   * REFACTORED: Access generator directly from shared tree instead of creating new instance.
   */
  private List<IRInstr> processInstructionBlockStatements(final EK9Parser.InstructionBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    // Process all block statements - scope already entered by caller
    for (final var blockStmtCtx : ctx.blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStmtCtx));
    }

    return instructions;
  }

  /**
   * Generate return statement based on the operation's return type.
   * If the operation returns a value, return the return variable.
   * If the operation returns void, return void.
   */
  private List<IRInstr> generateReturnStatement(final OperationInstr operation) {
    final var instructions = new ArrayList<IRInstr>();

    // Check if the operation has a return type
    final var operationSymbol = operation.getSymbol();
    final var operationDebugInfo = stackContext.createDebugInfo(operationSymbol.getSourceToken());

    if (operationSymbol instanceof IMayReturnSymbol mayReturnSymbol && mayReturnSymbol.isReturningSymbolPresent()) {
      final var returnSymbol = mayReturnSymbol.getReturningSymbol();
      final var returnType = symbolTypeOrException.apply(returnSymbol);

      if (!stackContext.getParsedModule().getEk9Types().ek9Void().isExactSameType(returnType)) {
        // Function returns a value - return the return variable
        final var debugInfo = stackContext.createDebugInfo(returnSymbol.getSourceToken());
        instructions.add(BranchInstr.returnValue(returnSymbol.getName(), debugInfo));
      } else {
        // Function returns void or has no explicit return type
        instructions.add(BranchInstr.returnVoid(operationDebugInfo));
      }
    } else {
      // No return symbol - treat as void return
      instructions.add(BranchInstr.returnVoid(operationDebugInfo));
    }

    return instructions;
  }

  /**
   * Generate constructor initialization sequence:
   * 1. Call super constructor (if applicable)
   * 2. Call own class's i_init method
   * This ensures consistent initialization for both explicit and synthetic constructors.
   */
  private List<IRInstr> generateConstructorInitialization(final MethodSymbol constructorSymbol) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(constructorSymbol.getSourceToken());
    final var aggregateSymbol = (AggregateSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)  
      if (isNotImplicitSuperClass(superSymbol)) {
        // Try to find constructor symbol in superclass for metadata
        final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
        final var constructorSymbolOpt =
            superSymbol.resolve(new SymbolSearch(superSymbol.getName()));
        final var metaData = constructorSymbolOpt.isPresent() ? metaDataExtractor.apply(constructorSymbolOpt.get()) :
            CallMetaDataDetails.defaultMetaData();

        final var callDetails = new CallDetails(
            IRConstants.SUPER, // Target super object
            superSymbol.getFullyQualifiedName(),
            superSymbol.getName(), // Constructor name matches class name
            java.util.List.of(), // No parameters for default constructor
            superSymbol.getFullyQualifiedName(), // Return type is the super class
            java.util.List.of(), // No arguments
            metaData,
            false
        );
        instructions.add(CallInstr.call(IRConstants.TEMP_SUPER_INIT, debugInfo, callDetails));
      }
    }

    // 2. Call own class's i_init method to initialize this class's fields
    // Try to find i_init method symbol for metadata
    final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    final var iInitMethodOpt =
        aggregateSymbol.resolve(new SymbolSearch(IRConstants.I_INIT_METHOD));
    final var iInitMetaData = iInitMethodOpt.isPresent() ? metaDataExtractor.apply(iInitMethodOpt.get()) :
        CallMetaDataDetails.defaultMetaData();

    final var iInitCallDetails = new CallDetails(
        IRConstants.THIS, // Target this object
        aggregateSymbol.getFullyQualifiedName(),
        IRConstants.I_INIT_METHOD,
        java.util.List.of(), // No parameters
        EK9_VOID, // Return type
        java.util.List.of(), // No arguments
        iInitMetaData,
        false
    );
    // i_init returns void, so don't assign to a temp variable
    instructions.add(CallInstr.call(null, debugInfo, iInitCallDetails));

    return instructions;
  }

  /**
   * Check if the super class is not an implicit base class.
   * This logic should match ClassDfnGenerator.isNotImplicitSuperClass()
   */
  private boolean isNotImplicitSuperClass(final IAggregateSymbol superSymbol) {
    return !stackContext.getParsedModule().getEk9Types().ek9Any().isExactSameType(superSymbol);
  }
}
