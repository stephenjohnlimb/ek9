package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRContext;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generator.OperationDfnGenerator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.NotImplicitSuper;
import org.ek9lang.compiler.symbols.IScope;
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

  /**
   * Primary constructor using stack context - the single source of truth.
   */
  AbstractDfnGenerator(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
    this.operationDfnGenerator = new OperationDfnGenerator(stackContext);
    this.voidStr = stackContext.getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();
  }

  /**
   * Get the parsed module from stack context - the single source of truth.
   */
  protected ParsedModule getParsedModule() {
    return stackContext.getParsedModule();
  }

  /**
   * Create CallMetaDataExtractor using stack context - the single source of truth.
   */
  protected CallMetaDataExtractor createCallMetaDataExtractor() {
    return new CallMetaDataExtractor(getParsedModule().getEk9Types());
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

    stackContext.enterMethodScope("c_init", cInitOperation.getDebugInfo(), IRFrameType.METHOD);

    // Generate c_init body

    final var allInstructions = new java.util.ArrayList<IRInstr>();

    // Call super class c_init if this class explicitly extends another class

    if (superType != null && notImplicitSuper.test(superType)) {

      final var metaData = CallMetaDataDetails.defaultMetaData();

      final var callDetails = new CallDetails(
          null, // No target object for static call
          superType.getFullyQualifiedName(),
          IRConstants.C_INIT_METHOD,
          java.util.List.of(), // No parameters
          voidStr, // Return type
          java.util.List.of(), // No arguments
          metaData
      );
      allInstructions.add(CallInstr.callStatic(IRConstants.TEMP_C_INIT, null, callDetails));
    }


    // TODO: Add static field initialization when static fields are supported

    // Return void
    allInstructions.add(BranchInstr.returnVoid());

    // Create BasicBlock with all instructions - use stack context for consistent labeling
    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    cInitOperation.setBody(basicBlock);

    construct.add(cInitOperation);
    stackContext.exitScope();
  }
}
