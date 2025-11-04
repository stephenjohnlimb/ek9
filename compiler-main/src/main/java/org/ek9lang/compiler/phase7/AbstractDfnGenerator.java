package org.ek9lang.compiler.phase7;

import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRContext;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.generator.GeneratorSet;
import org.ek9lang.compiler.phase7.generator.IRConstructGenerators;
import org.ek9lang.compiler.phase7.generator.OperationDfnGenerator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.NotImplicitSuper;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Acts as a base for the main construct definitions.
 * As there are lots of common processing aspects across constructs and, they are
 * only really very focussed on IR generation (and not reusable elsewhere) hence defined in this base.
 */
abstract class AbstractDfnGenerator {

  protected final NotImplicitSuper notImplicitSuper = new NotImplicitSuper();
  protected final OperationDfnGenerator operationDfnGenerator;
  protected final IRGenerationContext stackContext;
  protected final String voidStr;
  // Generator tree created ONCE per construct, reused for all processing
  protected final GeneratorSet generators;

  /**
   * Primary constructor using stack context - the single source of truth.
   * Creates the complete generator tree once for efficient reuse via static factory.
   */
  AbstractDfnGenerator(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
    this.generators = IRConstructGenerators.create(stackContext);
    this.operationDfnGenerator = new OperationDfnGenerator(stackContext, generators);
    this.voidStr = stackContext.getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();
  }

  /**
   * Get the parsed module from stack context - the single source of truth.
   */
  protected ParsedModule getParsedModule() {
    return stackContext.getParsedModule();
  }

  /**
   * Create DebugInfoCreator using stack context - the single source of truth.
   */
  protected DebugInfoCreator createDebugInfoCreator() {
    return new DebugInfoCreator(stackContext.getCurrentIRContext());
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    AssertValue.checkNotNull("IRConstruct cannot be null", construct);
    AssertValue.checkNotNull("ISymbol cannot be null", symbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    if (symbol instanceof MethodSymbol method) {
      final var debugInfo = createDebugInfoCreator().apply(method.getSourceToken());
      final var operation = new OperationInstr(method, debugInfo);

      // Use stack context for method-level coordination with fresh IRContext
      var methodDebugInfo = stackContext.createDebugInfo(method.getSourceToken());
      stackContext.enterMethodScope(method.getName(), methodDebugInfo, IRFrameType.METHOD);

      operationDfnGenerator.accept(operation, ctx);

      stackContext.exitScope();
      construct.add(operation);
    } else {
      throw new CompilerException("Expecting a Method Symbol");
    }
  }

  /**
   * Create a new IRContext for per-construct isolation.
   * This ensures each construct gets fresh block labeling (_entry_1, _temp_1, etc.)
   * while sharing the core parsedModule and compilerFlags.
   */
  protected IRContext newPerConstructContext() {
    return new IRContext(stackContext.getCurrentIRContext());
  }

  protected OperationInstr newSyntheticInitOperation(final IScope scope,
                                                     final String methodName) {

    final var method = newSyntheticInitMethodSymbol(scope, methodName);
    final var debugInfo = createDebugInfoCreator().apply(method.getSourceToken());
    return new OperationInstr(method, debugInfo);

  }

  private MethodSymbol newSyntheticInitMethodSymbol(final IScope scope,
                                                    final String methodName) {
    final var methodSymbol = new MethodSymbol(methodName, scope);
    final ISymbol returnType = stackContext.getParsedModule().getEk9Types().ek9Void();
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol(IRConstants.RETURN_VARIABLE, returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }

  /**
   * Create c_init operation for class/static initialization.
   * This runs once per class loading.
   */
  protected void createInitOperation(final IRConstruct construct,
                                     final IScope aggregateSymbol, ISymbol superType) {
    // Create a synthetic method symbol for c_init is when the class/construct definition is actually loaded.
    final var cInitOperation = newSyntheticInitOperation(aggregateSymbol, IRConstants.C_INIT_METHOD);

    // Use stack context for method-level coordination with fresh IRContext

    stackContext.enterMethodScope(IRConstants.C_INIT_METHOD, cInitOperation.getDebugInfo(), IRFrameType.METHOD);

    // Generate c_init body using stack-based instruction builder
    var instructionBuilder = new IRInstructionBuilder(stackContext);

    // Call super class c_init if this class explicitly extends another class
    if (superType != null && notImplicitSuper.test(superType)) {
      instructionBuilder.callSuperStaticMethod(superType, IRConstants.C_INIT_METHOD);
    }

    // TODO: Add static field initialization when static fields are supported

    // Return void
    instructionBuilder.returnVoid();

    // Create BasicBlock with all instructions - use stack context for consistent labeling
    cInitOperation.setBody(instructionBuilder.createBasicBlock(IRConstants.ENTRY_LABEL));

    construct.add(cInitOperation);
    stackContext.exitScope();
  }

  /**
   * Create i_init operation for instance initialization.
   * This handles property REFERENCE declarations and immediate initializations.
   */
  protected void createInstanceInitOperation(final IRConstruct construct,
                                             final IScope symbol,
                                             final EK9Parser.AggregatePartsContext ctx) {
    final var iInitOperation = newSyntheticInitOperation(symbol, IRConstants.I_INIT_METHOD);

    // Use stack context for method-level coordination with fresh IRContext
    stackContext.enterMethodScope(IRConstants.I_INIT_METHOD, iInitOperation.getDebugInfo(), IRFrameType.METHOD);

    // Generate i_init body using stack-based instruction builder
    var instructionBuilder = new IRInstructionBuilder(stackContext);

    //Now it is possible that there are captured variables, TODO

    if (ctx != null) {
      instructionBuilder.addInstructions(processPropertiesForInstanceInit(ctx));
    }

    instructionBuilder.returnVoid();
    // Create BasicBlock with all instructions - use stack context for consistent labeling
    iInitOperation.setBody(instructionBuilder.createBasicBlock(IRConstants.ENTRY_LABEL));

    construct.add(iInitOperation);
    stackContext.exitScope();
  }

  /**
   * Process all properties for instance initialization.
   * Generates REFERENCE declarations and immediate initializations.
   * REFACTORED: Now reuses generators from shared generator tree instead of creating new instances.
   */
  protected List<IRInstr> processPropertiesForInstanceInit(
      final EK9Parser.AggregatePartsContext ctx) {

    final var instructions = new java.util.ArrayList<IRInstr>();

    // Process each property in the aggregate
    for (final var propertyCtx : ctx.aggregateProperty()) {
      if (propertyCtx.variableDeclaration() != null) {
        // REFACTORED: Access generator directly from struct
        instructions.addAll(generators.variableDeclGenerator.apply(propertyCtx.variableDeclaration()));
      } else if (propertyCtx.variableOnlyDeclaration() != null) {
        // REFACTORED: Access generator directly from struct
        instructions.addAll(generators.variableOnlyDeclGenerator.apply(propertyCtx.variableOnlyDeclaration()));
      }
    }

    return instructions;
  }

  /**
   * Process a synthetic constructor (default constructor created by earlier phases).
   * Implements proper constructor inheritance chain:
   * 1. Call super constructor (if not implicit base class)
   * 2. Call own class's i_init method
   * 3. Return this
   */
  protected void processSyntheticConstructor(final IRConstruct construct,
                                             final MethodSymbol constructorSymbol,
                                             final IScopedSymbol superType) {
    final var debugInfo = stackContext.createDebugInfo(constructorSymbol.getSourceToken());
    final var operation = new OperationInstr(constructorSymbol, debugInfo);

    // Generate constructor body using stack-based instruction builder
    var instructionBuilder = new IRInstructionBuilder(stackContext);
    final var scopedSymbol = (IScopedSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    if (superType != null && notImplicitSuper.test(superType)) {
      instructionBuilder.callSuperMethod(superType, IRConstants.INIT_METHOD);
    }

    // 2. Call own class's i_init method (i_init returns void, so don't assign to temp variable)
    instructionBuilder.callThisMethod(scopedSymbol.getFullyQualifiedName(), IRConstants.I_INIT_METHOD,
        null, debugInfo);


    // 3. Return this
    instructionBuilder.returnValue(IRConstants.THIS, debugInfo);

    //Now package up the instructions in a block and add to the operation.
    operation.setBody(instructionBuilder.createBasicBlock(IRConstants.ENTRY_LABEL));

    construct.add(operation);
  }

}
