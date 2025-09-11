package org.ek9lang.compiler.phase7;

import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.generator.VariableDeclInstrGenerator;
import org.ek9lang.compiler.phase7.generator.VariableOnlyDeclInstrGenerator;
import org.ek9lang.compiler.phase7.support.FieldCreator;
import org.ek9lang.compiler.phase7.support.FieldsFromCapture;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.NotImplicitSuper;
import org.ek9lang.compiler.phase7.support.OperationDetailContextOrError;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Common code of Aggregate Construct types.
 */
abstract class AggregateDfnGenerator extends AbstractDfnGenerator {

  private final NotImplicitSuper notImplicitSuper = new NotImplicitSuper();

  private final OperationDetailContextOrError operationDetailContextOrError;

  private final SymbolGenus expectedGenus;

  AggregateDfnGenerator(final IRGenerationContext stackContext, final SymbolGenus forGenus) {
    super(stackContext);
    this.operationDetailContextOrError = new OperationDetailContextOrError(stackContext.getParsedModule());
    this.expectedGenus = forGenus;
  }

  protected IRConstruct processAggregate(final AggregateSymbol aggregateSymbol,
                                         final EK9Parser.AggregatePartsContext ctx) {

    AssertValue.checkTrue("Only configured for " + expectedGenus,
        expectedGenus == aggregateSymbol.getGenus());

    final var construct = new IRConstruct(aggregateSymbol);

    // Create field declarations from symbol table
    createFieldDeclarations(construct, aggregateSymbol);

    // Create three-phase initialization operations
    createInitializationOperations(construct, aggregateSymbol, ctx);

    // Process aggregateParts if present (methods, operators)
    if (ctx != null) {
      createOperationsForAggregateParts(construct, aggregateSymbol, ctx);
    }

    return construct;
  }

  /**
   * Create field declarations using AggregateSymbol.getProperties().
   * This provides structural metadata about the class's data members,
   * separate from the behavioral operations (methods).
   */
  protected void createFieldDeclarations(final IRConstruct construct, final AggregateSymbol aggregateSymbol) {

    final var debugInfoCreator = createDebugInfoCreator();
    final var fieldCreator = new FieldCreator(construct, debugInfoCreator);
    final var fieldsFromCapture = new FieldsFromCapture(fieldCreator);

    aggregateSymbol.getProperties().forEach(fieldCreator);
    fieldsFromCapture.accept(aggregateSymbol);
  }

  /**
   * Create the three-phase initialization operations for the class:
   * 1. c_init - Class/static initialization
   * 2. i_init - Instance initialization (property declarations and immediate initialization)
   * 3. Constructor methods will be processed separately in createOperationsForAggregateParts
   */
  protected void createInitializationOperations(final IRConstruct construct,
                                                final AggregateSymbol aggregateSymbol,
                                                final EK9Parser.AggregatePartsContext ctx) {

    // Create c_init operation for class/static initialization
    createInitOperation(construct, aggregateSymbol);

    //Now the i_init operation for the instance when created.
    createInstanceInitOperation(construct, aggregateSymbol, ctx);

  }

  /**
   * Create c_init operation for class/static initialization.
   * This runs once per class loading.
   */
  protected void createInitOperation(final IRConstruct construct, final AggregateSymbol aggregateSymbol) {
    // Create a synthetic method symbol for c_init is when the class/construct definition is actually loaded.
    final var cInitOperation = newSyntheticInitOperation(aggregateSymbol, IRConstants.C_INIT_METHOD);

    // Use stack context for method-level coordination with fresh IRContext
    var debugInfo = stackContext.createDebugInfo(aggregateSymbol.getSourceToken());
    stackContext.enterMethodScope("c_init", debugInfo, IRFrameType.METHOD);

    // Generate c_init body

    final var allInstructions = new java.util.ArrayList<IRInstr>();

    // Call super class c_init if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)
      // Check if this is an explicit inheritance (not implicit base class)
      if (notImplicitSuper.test(superSymbol)) {
        // Try to find c_init method symbol in superclass for metadata
        final var metaDataExtractor = createCallMetaDataExtractor();
        final var cInitMethodOpt =
            superSymbol.resolve(new org.ek9lang.compiler.search.SymbolSearch(IRConstants.C_INIT_METHOD));
        final var metaData = cInitMethodOpt.isPresent()
            ? metaDataExtractor.apply(cInitMethodOpt.get()) :
            CallMetaDataDetails.defaultMetaData();

        final var callDetails = new CallDetails(
            null, // No target object for static call
            superSymbol.getFullyQualifiedName(),
            IRConstants.C_INIT_METHOD,
            java.util.List.of(), // No parameters
            voidStr, // Return type
            java.util.List.of(), // No arguments
            metaData
        );
        allInstructions.add(CallInstr.callStatic(IRConstants.TEMP_C_INIT, null, callDetails));
      }
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

  /**
   * Create i_init operation for instance initialization.
   * This handles property REFERENCE declarations and immediate initializations.
   */
  protected void createInstanceInitOperation(final IRConstruct construct,
                                             final AggregateSymbol aggregateSymbol,
                                             final EK9Parser.AggregatePartsContext ctx) {
    final var iInitOperation = newSyntheticInitOperation(aggregateSymbol, IRConstants.I_INIT_METHOD);

    // Use stack context for method-level coordination with fresh IRContext
    var debugInfo = stackContext.createDebugInfo(aggregateSymbol.getSourceToken());
    stackContext.enterMethodScope("i_init", debugInfo, IRFrameType.METHOD);

    // Generate i_init body
    final var allInstructions = new java.util.ArrayList<IRInstr>();

    //Now it is possible that there are captured variables, TODO

    if (ctx != null) {
      allInstructions.addAll(processPropertiesForInstanceInit(ctx));
    }

    allInstructions.add(BranchInstr.returnVoid());
    // Create BasicBlock with all instructions - use stack context for consistent labeling
    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    iInitOperation.setBody(basicBlock);

    construct.add(iInitOperation);
    stackContext.exitScope();
  }

  /**
   * Process all properties for instance initialization.
   * Generates REFERENCE declarations and immediate initializations.
   */
  protected List<IRInstr> processPropertiesForInstanceInit(
      final EK9Parser.AggregatePartsContext ctx) {

    final var instructions = new java.util.ArrayList<IRInstr>();
    final var scopeId = stackContext.generateScopeId(IRConstants.I_INIT_METHOD);

    // Process each property in the aggregate
    for (final var propertyCtx : ctx.aggregateProperty()) {
      if (propertyCtx.variableDeclaration() != null) {
        final var variableDeclInstrGenerator = new VariableDeclInstrGenerator(stackContext);
        instructions.addAll(variableDeclInstrGenerator.apply(propertyCtx.variableDeclaration(), scopeId));
      } else if (propertyCtx.variableOnlyDeclaration() != null) {
        var instructionBuilder = new IRInstructionBuilder(stackContext);
        final var variableOnlyDeclInstrGenerator = new VariableOnlyDeclInstrGenerator(instructionBuilder);
        instructions.addAll(variableOnlyDeclInstrGenerator.apply(propertyCtx.variableOnlyDeclaration(), scopeId));
      }
    }

    return instructions;
  }


  protected void createOperationsForAggregateParts(final IRConstruct construct,
                                                   final AggregateSymbol aggregateSymbol,
                                                   final EK9Parser.AggregatePartsContext ctx) {

    // Properties are now handled in i_init operation - no processing needed here

    // Process ALL methods from symbol table (explicit + synthetic)
    final var allMethods = aggregateSymbol.getAllMethodInThisScopeOnly();

    if (allMethods.isEmpty()) {
      throw new CompilerException("No methods found for class "
          + aggregateSymbol.getFullyQualifiedName() + " - earlier phases may have failed");
    }

    for (final var method : allMethods) {
      if (method.isSynthetic()) {
        processSyntheticMethod(construct, method);
      } else {
        processExplicitMethod(construct, method, ctx);
      }
    }
  }

  /**
   * Process a synthetic method (generated by earlier compiler phases).
   * These include synthetic constructors, operators from 'default operator', and other generated methods.
   */
  private void processSyntheticMethod(final IRConstruct construct, final MethodSymbol method) {
    if (method.isConstructor()) {
      // Synthetic default constructor
      processSyntheticConstructor(construct, method);
    } else if (method.isOperator()) {
      // Synthetic operators from default operator
      processSyntheticOperator(construct, method);
    } else {
      // Synthetic regular methods (e.g., _isSet, _hash)
      processSyntheticRegularMethod(construct, method);
    }
  }

  /**
   * Process an explicit method (defined in source code with parse context).
   */
  private void processExplicitMethod(final IRConstruct construct, final MethodSymbol method,
                                     final EK9Parser.AggregatePartsContext ctx) {

    // Find the corresponding parse context for this method
    final var operationCtx = operationDetailContextOrError.apply(method, ctx);
    processAsMethodOrOperator(construct, method, operationCtx);

  }

  /**
   * Process a synthetic constructor (default constructor created by earlier phases).
   * Implements proper constructor inheritance chain:
   * 1. Call super constructor (if not implicit base class)
   * 2. Call own class's i_init method
   * 3. Return this
   */
  private void processSyntheticConstructor(final IRConstruct construct, final MethodSymbol constructorSymbol) {
    final var debugInfo = stackContext.createDebugInfo(constructorSymbol.getSourceToken());
    final var operation = new OperationInstr(constructorSymbol, debugInfo);

    final var instructions = new java.util.ArrayList<IRInstr>();
    final var aggregateSymbol = (AggregateSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)
      if (notImplicitSuper.test(superSymbol)) {
        // Try to find constructor symbol in superclass for metadata
        final var metaDataExtractor = createCallMetaDataExtractor();
        final var constructorSymbolOpt =
            superSymbol.resolve(new org.ek9lang.compiler.search.SymbolSearch(superSymbol.getName()));
        final var metaData = constructorSymbolOpt.isPresent()
            ? metaDataExtractor.apply(constructorSymbolOpt.get()) :
            CallMetaDataDetails.defaultMetaData();

        final var callDetails = new CallDetails(
            IRConstants.SUPER, // Target super object
            superSymbol.getFullyQualifiedName(),
            superSymbol.getName(), // Constructor name matches class name
            java.util.List.of(), // No parameters for default constructor
            superSymbol.getFullyQualifiedName(), // Return type is the super class
            java.util.List.of(), // No arguments
            metaData
        );
        instructions.add(CallInstr.call(IRConstants.TEMP_SUPER_INIT, debugInfo, callDetails));
      }
    }

    // 2. Call own class's i_init method to initialize this class's fields
    // Try to find i_init method symbol for metadata
    final var metaDataExtractor = createCallMetaDataExtractor();
    final var iInitMethodOpt =
        aggregateSymbol.resolve(new org.ek9lang.compiler.search.SymbolSearch(IRConstants.I_INIT_METHOD));
    final var iInitMetaData = iInitMethodOpt.isPresent()
        ? metaDataExtractor.apply(iInitMethodOpt.get()) :
        CallMetaDataDetails.defaultMetaData();

    final var iInitCallDetails = new CallDetails(
        IRConstants.THIS, // Target this object
        aggregateSymbol.getFullyQualifiedName(),
        IRConstants.I_INIT_METHOD,
        java.util.List.of(), // No parameters
        voidStr, // Return type
        java.util.List.of(), // No arguments
        iInitMetaData
    );
    instructions.add(CallInstr.call(IRConstants.TEMP_I_INIT, debugInfo, iInitCallDetails));

    // 3. Return this
    instructions.add(BranchInstr.returnValue(IRConstants.THIS, debugInfo));

    final var label = stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL);
    final var basicBlock = new BasicBlockInstr(label);
    basicBlock.addInstructions(instructions);
    operation.setBody(basicBlock);

    construct.add(operation);
  }

  /**
   * Process a synthetic operator (generated from 'default operator' declarations).
   * For now, creates placeholder - full implementation will be added later.
   */
  private void processSyntheticOperator(final IRConstruct construct, final MethodSymbol operatorSymbol) {
    // Create placeholder synthetic operator operation
    final var context = newPerConstructContext();
    final var debugInfo = new DebugInfoCreator(context).apply(operatorSymbol.getSourceToken());
    final var operation = new OperationInstr(operatorSymbol, debugInfo);

    // TODO: Implement based on operator type and base operators
    // This will be complex and handled in later implementation phases
    final var instructions = new java.util.ArrayList<IRInstr>();

    instructions.add(BranchInstr.returnVoid()); // Placeholder

    final var label = stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL);
    final var basicBlock = new BasicBlockInstr(label);
    basicBlock.addInstructions(instructions);
    operation.setBody(basicBlock);

    construct.add(operation);
  }

  /**
   * Process a synthetic regular method (e.g., _isSet, _hash generated from properties).
   * For now, creates placeholder - full implementation will be added later.
   */
  private void processSyntheticRegularMethod(final IRConstruct construct, final MethodSymbol methodSymbol) {
    // Create placeholder synthetic regular method operation
    final var context = newPerConstructContext();
    final var debugInfo = new DebugInfoCreator(context).apply(methodSymbol.getSourceToken());
    final var operation = new OperationInstr(methodSymbol, debugInfo);

    // TODO: Implement based on method semantics (e.g., _isSet, _hash)
    final var instructions = new java.util.ArrayList<IRInstr>();

    instructions.add(BranchInstr.returnVoid()); // Placeholder

    final var label = stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL);
    final var basicBlock = new BasicBlockInstr(label);
    basicBlock.addInstructions(instructions);
    operation.setBody(basicBlock);

    construct.add(operation);
  }

}
