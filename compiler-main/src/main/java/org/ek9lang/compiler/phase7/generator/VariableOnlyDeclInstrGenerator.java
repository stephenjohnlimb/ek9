package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable only declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 * <p>
 * STACK-BASED: Now uses IRGenerationContext for scope management instead of parameter threading.
 * </p>
 */
public final class VariableOnlyDeclInstrGenerator extends AbstractGenerator
    implements Function<EK9Parser.VariableOnlyDeclarationContext, List<IRInstr>> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

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

  private List<IRInstr> getDeclInstrs(final ParseTree ctx) {
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    // Get symbol from the parsed module
    final var variableSymbol = getRecordedSymbolOrException(ctx);

    final var variableName = variableNameForIR.apply(variableSymbol);
    final var variableTypeName = typeNameOrException.apply(variableSymbol);

    // STACK-BASED: Create debug info using stack context
    final var debugInfo = stackContext.createDebugInfo(variableSymbol.getSourceToken());

    // Create memory reference instruction with proper debug info
    return List.of(MemoryInstr.reference(variableName, variableTypeName, debugInfo));
  }
}