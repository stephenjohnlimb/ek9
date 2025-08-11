package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.core.AssertValue;

/**
 * Pulls out the common code for variable and variable-only declarations. DRY.
 */
abstract class AbstractVariableDeclGenerator extends AbstractGenerator {

  private final ShouldRegisterVariableInScope shouldRegisterVariableInScope = new ShouldRegisterVariableInScope();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  AbstractVariableDeclGenerator(final IRContext context) {
    super(context);
  }

  public List<IRInstr> getDeclInstrs(final ParseTree ctx,
                                     final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the resolved variable symbol
    final var varSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", varSymbol);

    final var varName = varSymbol.getName();
    final var typeName = typeNameOrException.apply(varSymbol);
    final var debugInfo = debugInfoCreator.apply(varSymbol);

    instructions.add(MemoryInstr.reference(varName, typeName, debugInfo));

    if (shouldRegisterVariableInScope.test(scopeId)) {
      instructions.add(ScopeInstr.register(varName, scopeId, debugInfo));
    }

    return instructions;

  }
}
