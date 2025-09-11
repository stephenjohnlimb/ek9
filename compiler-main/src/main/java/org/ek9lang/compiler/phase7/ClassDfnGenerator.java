package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
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
final class ClassDfnGenerator extends AggregateDfnGenerator
    implements Function<EK9Parser.ClassDeclarationContext, IRConstruct> {

  /**
   * Constructor using stack context - the single source of state.
   */
  ClassDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext, SymbolGenus.CLASS);
  }

  @Override
  public IRConstruct apply(final EK9Parser.ClassDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol) {
      return processAggregate(aggregateSymbol, ctx.aggregateParts());
    }

    throw new CompilerException("Cannot create Class - expect AggregateSymbol of CLASS Genus");
  }

}
