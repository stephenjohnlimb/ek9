package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable only declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 */
final class VariableOnlyDeclInstrGenerator extends AbstractVariableDeclGenerator
    implements BiFunction<EK9Parser.VariableOnlyDeclarationContext, String, List<IRInstr>> {


  VariableOnlyDeclInstrGenerator(final IRContext context) {
    super(context);
  }

  /**
   * Generate IR instructions for variable only declaration.
   * Example: someVar as String?
   * Generates: REFERENCE only (no assignment)
   */
  public List<IRInstr> apply(final EK9Parser.VariableOnlyDeclarationContext ctx,
                             final String scopeId) {
    AssertValue.checkNotNull("VariableOnlyDeclarationContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    return getDeclInstructions(ctx, scopeId);
  }

}