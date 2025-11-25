package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 */
public final class VariableDeclInstrGenerator extends AbstractVariableDeclGenerator
    implements Function<EK9Parser.VariableDeclarationContext, List<IRInstr>> {

  private final GeneratorSet generators;

  /**
   * Constructor accepting GeneratorSet for unified access to all generators.
   */
  public VariableDeclInstrGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  /**
   * Generate IR instructions for variable declaration with assignment using stack-based scope management.
   * Example: stdout &lt;- Stdout()
   * Generates: REFERENCE + assignment processing
   * STACK-BASED: Gets scope ID from stack context instead of parameter threading.
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.VariableDeclarationContext ctx) {
    AssertValue.checkNotNull("VariableDeclarationContext cannot be null", ctx);

    final var instructions = getDeclInstrs(ctx);

    // Process the assignment expression (right-hand side)
    if (ctx.assignmentExpression() != null) {

      final var lhsSymbol = getRecordedSymbolOrException(ctx);

      // Use ExprInstrGenerator from generators struct
      final var generator = new AssignmentExprInstrGenerator(stackContext, generators, ctx.assignmentExpression());
      //Now because we are declaring and initialising a new variable - we do not need to 'release' any memory
      //it could be pointing to - because it is a new variable and so could not be pointing to anything.
      final var assignExpressionToSymbol = new AssignExpressionToSymbol(stackContext, generators.variableMemoryManagement, false, generator);
      instructions.addAll(assignExpressionToSymbol.apply(lhsSymbol, ctx.assignmentExpression()));
    }

    return instructions;
  }
}