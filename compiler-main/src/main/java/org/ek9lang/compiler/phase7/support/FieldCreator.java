package org.ek9lang.compiler.phase7.support;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.Field;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Creates the appropriate Field object for the symbol and adds it to the construct.
 */
public class FieldCreator implements Consumer<ISymbol> {

  private final IRConstruct construct;
  private final DebugInfoCreator debugInfoCreator;
  final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public FieldCreator(final IRConstruct construct,
                            final DebugInfoCreator debugInfoCreator) {

    this.construct = construct;
    this.debugInfoCreator = debugInfoCreator;
  }

  @Override
  public void accept(final ISymbol symbol) {
    if (symbol instanceof VariableSymbol variableSymbol && variableSymbol.isPropertyField()) {
      final var fieldName = variableSymbol.getName();
      final var typeName = typeNameOrException.apply(variableSymbol);
      final var debugInfo = debugInfoCreator.apply(variableSymbol);

      final var field = new Field(variableSymbol, fieldName, typeName, debugInfo);
      construct.addField(field);
    }
  }
}