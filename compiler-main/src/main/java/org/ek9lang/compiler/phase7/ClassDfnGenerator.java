package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a class declaration.
 * <p>
 * Deals with the following ANTLR grammar.
 * </p>
 * <pre>
 *   classDeclaration
 *     : Identifier extendDeclaration? (traitPreamble traitsList)? (AS? (ABSTRACT | OPEN))? aggregateParts?
 *     | Identifier parameterisedParams (AS? (ABSTRACT | OPEN))? aggregateParts
 *     ;
 * </pre>
 * <p>
 * The key part here is that you can use the 'ctx' to look up the class aggregate.
 * For generic type use with parameterisation this will still be an aggregate (but with the appropriate
 * configuration).
 * </p>
 * //TODO pull most of the methods down into AbstractDfnGenerator and rename them.
 * //TODO most of the init and properties stuff applies to records, components, dynamic classes
 * //TODO and even Functions as these become classes in implementation. Dynamic functions with
 * //TODO capture variables are just properties on the class again in implementation.
 */
final class ClassDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.ClassDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  ClassDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    super(parsedModule, compilerFlags);
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public IRConstruct apply(final EK9Parser.ClassDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.CLASS) {
      final var construct = new IRConstruct(symbol);

      // Create three-phase initialization operations
      createClassInitializationOperations(construct, aggregateSymbol, ctx);

      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }

      return construct;
    }
    throw new CompilerException("Cannot create Class - expect AggregateSymbol of CLASS Genus");
  }

  /**
   * Create the three-phase initialization operations for the class:
   * 1. c_init - Class/static initialization
   * 2. i_init - Instance initialization (property declarations and immediate initialization)
   * 3. Constructor methods will be processed separately in createOperationsForAggregateParts
   */
  private void createClassInitializationOperations(final IRConstruct construct,
                                                   final AggregateSymbol aggregateSymbol,
                                                   final EK9Parser.ClassDeclarationContext ctx) {

    // Create c_init operation for class/static initialization
    createClassInitOperation(construct, aggregateSymbol);

    // Create i_init operation for instance initialization
    if (ctx.aggregateParts() != null) {
      createInstanceInitOperation(construct, aggregateSymbol, ctx.aggregateParts());
    }
  }

  /**
   * Create c_init operation for class/static initialization.
   * This runs once per class loading.
   */
  private void createClassInitOperation(final IRConstruct construct, final AggregateSymbol aggregateSymbol) {
    // Create a synthetic method symbol for c_init
    final var cInitSymbol = createSyntheticInitMethodSymbol(aggregateSymbol, "c_init",
        parsedModule.getEk9Types().ek9Void());

    final var cInitOperation = new Operation(cInitSymbol);

    // Generate c_init body
    final var context = new IRContext(parsedModule, compilerFlags);
    final var allInstructions = new java.util.ArrayList<IRInstr>();

    // Call super class c_init if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)
      // Check if this is an explicit inheritance (not implicit base class)
      if (!isImplicitSuperClass(superSymbol)) {
        final var callDetails = new CallDetails(
            null, // No target object for static call
            superSymbol.getFullyQualifiedName(),
            "c_init",
            java.util.List.of(), // No parameters
            "org.ek9.lang::Void", // Return type
            java.util.List.of() // No arguments
        );
        allInstructions.add(CallInstr.call("_temp_c_init", null, callDetails));
      }
    }

    // TODO: Add static field initialization when static fields are supported

    // Return void
    allInstructions.add(BranchInstr.returnVoid());

    // Create BasicBlock with all instructions
    final var basicBlock = new org.ek9lang.compiler.ir.BasicBlockInstr(context.generateBlockLabel("entry"));
    basicBlock.addInstructions(allInstructions);
    cInitOperation.setBody(basicBlock);

    construct.add(cInitOperation);
  }

  /**
   * Create i_init operation for instance initialization.
   * This handles property REFERENCE declarations and immediate initializations.
   */
  private void createInstanceInitOperation(final IRConstruct construct,
                                           final AggregateSymbol aggregateSymbol,
                                           final EK9Parser.AggregatePartsContext ctx) {

    // Create a synthetic method symbol for i_init
    final var iInitSymbol = createSyntheticInitMethodSymbol(aggregateSymbol, "i_init",
        parsedModule.getEk9Types().ek9Void());

    final var iInitOperation = new Operation(iInitSymbol);

    // Generate i_init body
    final var context = new IRContext(parsedModule, compilerFlags);
    final var allInstructions = new java.util.ArrayList<IRInstr>();

    // Call super class i_init if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)
      if (!isImplicitSuperClass(superSymbol)) {
        final var callDetails = new CallDetails(
            "this", // Target this object for instance call
            superSymbol.getFullyQualifiedName(),
            "i_init",
            java.util.List.of(), // No parameters
            "org.ek9.lang::Void", // Return type
            java.util.List.of() // No arguments
        );
        allInstructions.add(CallInstr.call("_temp_i_init", null, callDetails));
      }
    }

    // Process properties for this class (REFERENCE declarations and immediate initialization)
    allInstructions.addAll(processPropertiesForInstanceInit(ctx, context));

    // Return void
    allInstructions.add(BranchInstr.returnVoid());

    // Create BasicBlock with all instructions
    final var basicBlock = new BasicBlockInstr(context.generateBlockLabel("entry"));
    basicBlock.addInstructions(allInstructions);
    iInitOperation.setBody(basicBlock);

    construct.add(iInitOperation);
  }

  /**
   * Process all properties for instance initialization.
   * Generates REFERENCE declarations and immediate initializations.
   */
  private java.util.List<IRInstr> processPropertiesForInstanceInit(
      final EK9Parser.AggregatePartsContext ctx, final IRContext context) {

    final var instructions = new java.util.ArrayList<IRInstr>();
    final var debugInfoCreator = new DebugInfoCreator(context);
    final var typeNameOrException = new TypeNameOrException();

    // Process each property in the aggregate
    for (final var propertyCtx : ctx.aggregateProperty()) {
      if (propertyCtx.variableDeclaration() != null) {
        // Property with immediate initialization (bField := String(), cField := "Steve", etc.)
        final var propertySymbol = parsedModule.getRecordedSymbol(propertyCtx.variableDeclaration());
        if (propertySymbol instanceof VariableSymbol variableSymbol && variableSymbol.isPropertyField()) {

          final var propertyName = "this." + variableSymbol.getName();
          final var typeName = typeNameOrException.apply(variableSymbol);
          final var debugInfo = debugInfoCreator.apply(variableSymbol);

          // Add REFERENCE declaration
          instructions.add(MemoryInstr.reference(propertyName, typeName, debugInfo));

          // Process immediate initialization
          final var assignmentCtx = propertyCtx.variableDeclaration().assignmentExpression();
          if (assignmentCtx != null) {
            final var tempResult = context.generateTempName();
            final var assignmentExprInstrGenerator = new AssignmentExprInstrGenerator(context, "_i_init");
            instructions.addAll(assignmentExprInstrGenerator.apply(assignmentCtx, tempResult));

            // Property assignment: RETAIN + STORE + RETAIN (no SCOPE_REGISTER for properties)
            instructions.add(MemoryInstr.retain(tempResult, debugInfo));
            instructions.add(MemoryInstr.store(propertyName, tempResult, debugInfo));
            instructions.add(MemoryInstr.retain(propertyName, debugInfo));
          }
        }
      } else if (propertyCtx.variableOnlyDeclaration() != null) {
        // Property without immediate initialization (aField as String?)
        final var propertySymbol = parsedModule.getRecordedSymbol(propertyCtx.variableOnlyDeclaration());
        if (propertySymbol instanceof VariableSymbol variableSymbol && variableSymbol.isPropertyField()) {

          final var propertyName = "this." + variableSymbol.getName();
          final var typeName = typeNameOrException.apply(variableSymbol);
          final var debugInfo = debugInfoCreator.apply(variableSymbol);

          // Add REFERENCE declaration only (no immediate initialization)
          instructions.add(MemoryInstr.reference(propertyName, typeName, debugInfo));
        }
      }
    }

    return instructions;
  }

  /**
   * Check if the given super class is an implicit base class that should be ignored for initialization calls.
   * In EK9, classes might implicitly extend base types that don't need explicit super initialization.
   */
  private boolean isImplicitSuperClass(final IAggregateSymbol superSymbol) {
    // Check if this is an implicit base class like "Any" or similar
    final var superName = superSymbol.getFullyQualifiedName();

    // EK9's implicit base classes that don't need explicit super initialization
    return "org.ek9.lang::Any".equals(superName)
        || superSymbol.getName().isEmpty(); // Handle cases where name might be empty
  }

  /**
   * Create a synthetic method symbol for initialization methods (c_init, i_init).
   */
  private MethodSymbol createSyntheticInitMethodSymbol(
      final AggregateSymbol aggregateSymbol, final String methodName,
      final ISymbol returnType) {

    final var methodSymbol = new MethodSymbol(methodName, aggregateSymbol);
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol("_rtn", returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {

    // Properties are now handled in i_init operation - no processing needed here

    // Create Operation nodes for each method in the class (including constructors)
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }

    // Create Operation nodes for each operator in the class
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }

  }

}
