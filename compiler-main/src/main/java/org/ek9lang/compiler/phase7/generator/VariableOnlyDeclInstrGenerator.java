package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable only declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 * <p>
 * STACK-BASED: Now uses IRGenerationContext for scope management instead of parameter threading.
 * </p>
 */
public final class VariableOnlyDeclInstrGenerator extends AbstractVariableDeclGenerator
    implements Function<EK9Parser.VariableOnlyDeclarationContext, List<IRInstr>> {

  public VariableOnlyDeclInstrGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
  }

  /**
   * Generate IR instructions for variable only declaration.
   * Example: someVar as String?
   * Generates: REFERENCE only (no assignment)
   * STACK-BASED: Gets scope ID from stack context instead of parameter threading.
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.VariableOnlyDeclarationContext ctx) {
    AssertValue.checkNotNull("VariableOnlyDeclarationContext cannot be null", ctx);

    return getDeclInstrs(ctx);
  }
}