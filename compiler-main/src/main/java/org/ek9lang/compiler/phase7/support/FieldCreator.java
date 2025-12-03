package org.ek9lang.compiler.phase7.support;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.instructions.Field;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Creates the appropriate Field object for the symbol and adds it to the construct.
 */
public class FieldCreator implements Consumer<ISymbol> {

  private final IRConstruct construct;
  private final IRGenerationContext context;
  private final DebugInfoCreator debugInfoCreator;
  final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public FieldCreator(final IRConstruct construct,
                      final IRGenerationContext context,
                      final DebugInfoCreator debugInfoCreator) {
    this.construct = construct;
    this.context = context;
    this.debugInfoCreator = debugInfoCreator;
  }

  @Override
  public void accept(final ISymbol symbol) {
    if (symbol instanceof VariableSymbol variableSymbol && variableSymbol.isPropertyField()) {
      final var fieldName = variableSymbol.getName();
      final var typeName = typeNameOrException.apply(variableSymbol);
      final var debugInfo = debugInfoCreator.apply(variableSymbol.getSourceToken());

      final var field = new Field(variableSymbol, fieldName, typeName, debugInfo);
      final var ek9Types = context.getParsedModule().getEk9Types();
      construct.addField(field, ek9Types);
    }
  }
}