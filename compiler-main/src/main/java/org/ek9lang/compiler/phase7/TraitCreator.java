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
 * Creates the appropriate IR Construct for a trait declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class TraitCreator implements Function<EK9Parser.TraitDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  public TraitCreator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public IRConstruct apply(final EK9Parser.TraitDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.CLASS_TRAIT) {
      final var construct = new IRConstruct(symbol);
      
      // Process aggregateParts if present (default method implementations, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }
      
      return construct;
    }
    throw new CompilerException("Cannot create Trait - expect AggregateSymbol of CLASS_TRAIT Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each default method implementation in the trait
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var methodSymbol = parsedModule.getRecordedSymbol(methodCtx);
      if (methodSymbol instanceof MethodSymbol method) {
        final var operation = new Operation(method);
        
        // Process method executable content (default implementations)
        if (methodCtx.operationDetails() != null) {
          // Create IRGenerationContext for this method's executable code
          final var context = new IRGenerationContext(parsedModule, compilerFlags);
          final var basicBlockCreator = new BasicBlockCreator(context);
          
          // Generate BasicBlock IR for the default method implementation
          if (methodCtx.operationDetails().instructionBlock() != null) {
            final var basicBlock = basicBlockCreator.apply(methodCtx.operationDetails().instructionBlock());
            operation.setBody(basicBlock);
          }
        }
        
        construct.add(operation);
      }
    }
    
    // Create Operation nodes for each default operator implementation in the trait
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var operatorSymbol = parsedModule.getRecordedSymbol(operatorCtx);
      if (operatorSymbol instanceof MethodSymbol method) {
        final var operation = new Operation(method);
        
        // Process operator executable content (default implementations)
        if (operatorCtx.operationDetails() != null) {
          // Create IRGenerationContext for this operator's executable code
          final var context = new IRGenerationContext(parsedModule, compilerFlags);
          final var basicBlockCreator = new BasicBlockCreator(context);
          
          // Generate BasicBlock IR for the default operator implementation
          if (operatorCtx.operationDetails().instructionBlock() != null) {
            final var basicBlock = basicBlockCreator.apply(operatorCtx.operationDetails().instructionBlock());
            operation.setBody(basicBlock);
          }
        }
        
        construct.add(operation);
      }
    }
    
    // Note: Trait properties are handled differently - they're typically abstract declarations
    // Only concrete default implementations create Operation nodes
  }
}