package org.ek9lang.compiler.phase7;

import static org.ek9lang.compiler.support.AggregateManipulator.PUBLIC;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.FieldCreator;
import org.ek9lang.compiler.phase7.support.FieldsFromCapture;
import org.ek9lang.compiler.phase7.support.OperationDetailContextOrError;
import org.ek9lang.compiler.phase7.synthesis.SyntheticOperatorGenerator;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Common code of Aggregate Construct types.
 */
abstract class AggregateDfnGenerator extends AbstractDfnGenerator {

  private final OperationDetailContextOrError operationDetailContextOrError;
  private final SyntheticOperatorGenerator syntheticOperatorGenerator;
  private final SymbolGenus expectedGenus;

  AggregateDfnGenerator(final IRGenerationContext stackContext, final SymbolGenus forGenus) {
    super(stackContext);
    this.operationDetailContextOrError = new OperationDetailContextOrError(stackContext.getParsedModule());
    this.syntheticOperatorGenerator = new SyntheticOperatorGenerator(stackContext);
    this.expectedGenus = forGenus;
  }

  protected IRConstruct processAggregate(final AggregateSymbol aggregateSymbol,
                                         final EK9Parser.AggregatePartsContext ctx) {

    AssertValue.checkTrue("Only configured for " + expectedGenus,
        expectedGenus == aggregateSymbol.getGenus());

    final var sourceFileName = getParsedModule().getSource().getRelativeFileName();
    final var construct = new IRConstruct(aggregateSymbol, sourceFileName);

    // Populate implemented traits if the aggregate symbol supports them
    if (aggregateSymbol instanceof AggregateWithTraitsSymbol aggregateWithTraitsSymbol) {
      aggregateWithTraitsSymbol.getAllTraits().forEach(construct::addImplementedTrait);
    }

    // Create field declarations from symbol table
    createFieldDeclarations(construct, aggregateSymbol);

    // Create three-phase initialization operations
    createInitializationOperations(construct, aggregateSymbol, ctx);

    // Add _fieldSetStatus synthetic method if needed (just-in-time for IR generation)
    // This catches ALL aggregates including monomorphized ones
    addFieldSetStatusMethodIfRequired(aggregateSymbol);

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
    final var fieldCreator = new FieldCreator(construct, stackContext, debugInfoCreator);
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
    final ISymbol superType = aggregateSymbol.getSuperAggregate().orElse(null);
    createInitOperation(construct, aggregateSymbol, superType);

    //Now the i_init operation for the instance when created.
    createInstanceInitOperation(construct, aggregateSymbol, ctx);

  }

  protected void createOperationsForAggregateParts(final IRConstruct construct,
                                                   final AggregateSymbol aggregateSymbol,
                                                   final EK9Parser.AggregatePartsContext ctx) {

    final var allMethods = aggregateSymbol.getAllMethodInThisScopeOnly();
    if (allMethods.isEmpty()) {
      throw new CompilerException("No methods found for class "
          + aggregateSymbol.getFullyQualifiedName() + " - earlier phases may have failed");
    }

    // Pass 1: Create all OperationInstr objects so they can be cross-referenced.
    final Map<MethodSymbol, OperationInstr> operationsMap = new HashMap<>();
    for (final var method : allMethods) {
      final var debugInfo = createDebugInfoCreator().apply(method.getSourceToken());
      operationsMap.put(method, new OperationInstr(method, debugInfo));
    }

    // Pass 2: Populate dispatcher implementations and process bodies.
    for (final var method : allMethods) {
      final var operation = operationsMap.get(method);

      if (method.isMarkedAsDispatcher()) {
        operation.setDispatcher(true);
        // Now find all the implementations and add them to the dispatcher operation.
        allMethods.stream()
            .filter(impl -> impl.getName().equals(method.getName()) && !impl.isMarkedAsDispatcher())
            .forEach(impl -> {
              final var implOperation = operationsMap.get(impl);
              operation.addDispatchImplementation(implOperation);
            });
      }

      // Now process the body of the operation.
      if (method.isSynthetic()) {
        processSyntheticMethod(operation);
      } else {
        processExplicitMethod(operation, ctx);
      }
    }
    
    //Finally now add all the operations to the construct.
    operationsMap.values().forEach(construct::add);
  }

  /**
   * Process a synthetic method (generated by earlier compiler phases).
   * These include synthetic constructors, operators from 'default operator', and other generated methods.
   */
  private void processSyntheticMethod(final OperationInstr operation) {
    final var method = (MethodSymbol) operation.getSymbol();

    if (method.isConstructor()) {
      // Synthetic default constructor
      final var aggregateSymbol = (AggregateSymbol) method.getParentScope();
      final IScopedSymbol superType = aggregateSymbol.getSuperAggregate().orElse(null);
      processSyntheticConstructor(operation, superType);
    } else if (method.isOperator()) {
      // Synthetic operators from default operator
      processSyntheticOperator(operation);
    } else {
      // Synthetic regular methods (e.g., _isSet, _hash)
      processSyntheticRegularMethod(operation);
    }
  }

  /**
   * Process an explicit method (defined in source code with parse context).
   */
  private void processExplicitMethod(final OperationInstr operation,
                                     final EK9Parser.AggregatePartsContext ctx) {

    // Find the corresponding parse context for this method
    final var operationCtx = operationDetailContextOrError.apply((MethodSymbol) operation.getSymbol(), ctx);
    super.processAsMethodOrOperator(operation, operationCtx);

  }

  /**
   * Process a synthetic operator (generated from 'default operator' declarations).
   *
   * <p>Delegates to {@link SyntheticOperatorGenerator} which produces complete IR
   * instruction sequences for each operator type. This ensures:</p>
   * <ul>
   *   <li>Single implementation serves both JVM and LLVM backends</li>
   *   <li>Testable via @IR directives</li>
   *   <li>IR optimization benefits both backends</li>
   * </ul>
   *
   */
  private void processSyntheticOperator(final OperationInstr operationToPopulate) {
    final var operatorSymbol = (MethodSymbol) operationToPopulate.getSymbol();
    final var aggregateSymbol = (AggregateSymbol) operatorSymbol.getParentScope();
    final var generatedOperation = syntheticOperatorGenerator.generateOperator(operatorSymbol, aggregateSymbol);
    
    //Copy the body from the generated one to the one from our map.
    operationToPopulate.setBody(generatedOperation.getBody());
  }

  /**
   * Process a synthetic regular method (e.g., _isSet generated from properties).
   *
   * <p>Delegates to {@link SyntheticOperatorGenerator} which produces complete IR
   * instruction sequences for synthetic methods.</p>
   *
   * @param operationToPopulate The operation to populate.
   */
  private void processSyntheticRegularMethod(final OperationInstr operationToPopulate) {
    final var methodSymbol = (MethodSymbol) operationToPopulate.getSymbol();
    final var aggregateSymbol = (AggregateSymbol) methodSymbol.getParentScope();
    final var generatedOperation = syntheticOperatorGenerator.generateMethod(methodSymbol, aggregateSymbol);

    //Copy the body from the generated one to the one from our map.
    operationToPopulate.setBody(generatedOperation.getBody());
  }

  /**
   * Adds the _fieldSetStatus synthetic method to an aggregate if required.
   *
   * <p>This method is called just-in-time during IR generation to ensure ALL aggregates
   * (including monomorphized generics) get the _fieldSetStatus method. This is infrastructure
   * for comparison operators and is not callable by EK9 developers.</p>
   *
   * <p>The method returns a Bits value where each bit represents a field's set status.
   * Using Bits instead of Integer removes the 32-field limit.</p>
   *
   * @param aggregateSymbol The aggregate to potentially add _fieldSetStatus to.
   */
  private void addFieldSetStatusMethodIfRequired(final AggregateSymbol aggregateSymbol) {

    // Skip generic templates - only process concrete aggregates
    if (aggregateSymbol.getCategory().equals(SymbolCategory.TEMPLATE_TYPE)) {
      return;
    }

    // Only add if aggregate has properties
    if (aggregateSymbol.getProperties().isEmpty()) {
      return;
    }

    // Check if _fieldSetStatus already exists
    final var search = new MethodSymbolSearch("_fieldSetStatus");
    if (aggregateSymbol.resolveInThisScopeOnly(search).isPresent()) {
      return;
    }

    // Create and add the _fieldSetStatus method
    final var bitsType = resolveBits(aggregateSymbol);
    final var method = new MethodSymbol("_fieldSetStatus", aggregateSymbol);

    method.setReturningSymbol(new VariableSymbol("_rtn", bitsType.orElse(null)));
    method.setParsedModule(aggregateSymbol.getParsedModule());
    method.setAccessModifier(PUBLIC);
    method.setMarkedPure(true);
    method.setOperator(false);
    method.setSynthetic(true);
    method.setSourceToken(aggregateSymbol.getSourceToken());

    aggregateSymbol.define(method);
  }

  /**
   * Resolves the Bits type from the parsed module's EK9 types.
   */
  private Optional<ISymbol> resolveBits(final AggregateSymbol aggregateSymbol) {

    return Optional.of(getParsedModule().getEk9Types().ek9Bits());
  }

}
