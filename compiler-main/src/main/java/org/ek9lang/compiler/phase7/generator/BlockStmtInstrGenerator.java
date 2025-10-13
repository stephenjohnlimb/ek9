package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates IR instructions for block statements.
 * Handles variable declarations, variable only declarations, and statements.
 * <p>
 * Deals with the following ANTLR grammar.
 * </p>
 * <pre>
 *   blockStatement
 *     : variableDeclaration
 *     | variableOnlyDeclaration
 *     | statement
 *     ;
 * </pre>
 */
public final class BlockStmtInstrGenerator extends AbstractGenerator
    implements Function<EK9Parser.BlockStatementContext, List<IRInstr>> {

  private final GeneratorSet generators;

  /**
   * Constructor accepting GeneratorSet for unified access to all generators.
   */
  BlockStmtInstrGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
  }

  /**
   * Generate IR instructions for a block statement using stack-based scope management.
   * STACK-BASED: Gets scope ID from stack context instead of parameter threading.
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.BlockStatementContext ctx) {
    AssertValue.checkNotNull("BlockStatementContext cannot be null", ctx);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.variableDeclaration() != null) {
      // STACK-BASED: VariableDeclInstrGenerator uses stack context directly
      instructions.addAll(generators.variableDeclGenerator.apply(ctx.variableDeclaration()));
    } else if (ctx.variableOnlyDeclaration() != null) {
      // STACK-BASED: VariableOnlyDeclInstrGenerator now uses stack context directly
      instructions.addAll(generators.variableOnlyDeclGenerator.apply(ctx.variableOnlyDeclaration()));
    } else if (ctx.statement() != null) {
      // STACK-BASED: StmtInstrGenerator uses stack context directly
      instructions.addAll(generators.stmtGenerator.apply(ctx.statement()));
    } else {
      throw new CompilerException("Not expecting any other type of block statement");
    }

    return instructions;
  }
}