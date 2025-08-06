package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a component declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class ComponentCreator implements Function<EK9Parser.ComponentDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  public ComponentCreator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public IRConstruct apply(final EK9Parser.ComponentDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.COMPONENT) {
      final var construct = new IRConstruct(symbol);
      
      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }
      
      return construct;
    }
    throw new CompilerException("Cannot create Component - expect AggregateSymbol of COMPONENT Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each method in the component (effectively static methods)
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var methodSymbol = parsedModule.getRecordedSymbol(methodCtx);
      if (methodSymbol instanceof MethodSymbol method) {
        final var operation = new Operation(method);
        
        // Process method executable content
        if (methodCtx.operationDetails() != null) {
          // Create IRGenerationContext for this method's executable code
          final var context = new IRGenerationContext(parsedModule, compilerFlags);
          final var basicBlockCreator = new BasicBlockCreator(context);
          
          // Generate BasicBlock IR for the method
          if (methodCtx.operationDetails().instructionBlock() != null) {
            final var basicBlock = basicBlockCreator.apply(methodCtx.operationDetails().instructionBlock());
            operation.setBody(basicBlock);
          }
        }
        
        construct.add(operation);
      }
    }
    
    // Create Operation nodes for each operator in the component
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var operatorSymbol = parsedModule.getRecordedSymbol(operatorCtx);
      if (operatorSymbol instanceof MethodSymbol method) {
        final var operation = new Operation(method);
        
        // Process operator executable content
        if (operatorCtx.operationDetails() != null) {
          // Create IRGenerationContext for this operator's executable code
          final var context = new IRGenerationContext(parsedModule, compilerFlags);
          final var basicBlockCreator = new BasicBlockCreator(context);
          
          // Generate BasicBlock IR for the operator
          if (operatorCtx.operationDetails().instructionBlock() != null) {
            final var basicBlock = basicBlockCreator.apply(operatorCtx.operationDetails().instructionBlock());
            operation.setBody(basicBlock);
          }
        }
        
        construct.add(operation);
      }
    }
    
    // Note: Component properties are like static fields - handled differently than operations
    // They would be processed for initialization expressions but don't create Operation nodes
  }
}