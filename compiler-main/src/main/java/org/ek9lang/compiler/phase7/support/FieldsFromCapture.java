package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.symbols.ICanCaptureVariables;
import org.ek9lang.compiler.symbols.SymbolTable;

/**
 * Accepts a ICanCaptureVariables (either a function or an aggregate).
 * It then processes the captured symbols and converts them to Fields and
 * adds them to the construct via the fieldCreator.
 */
public class FieldsFromCapture implements Consumer<ICanCaptureVariables> {

  private final FieldCreator fieldCreator;

  public FieldsFromCapture(final FieldCreator fieldCreator) {

    this.fieldCreator = fieldCreator;
  }

  @Override
  public void accept(final ICanCaptureVariables canCaptureVariables) {
    canCaptureVariables
        .getCapturedVariables()
        .stream()
        .map(SymbolTable::getSymbolsForThisScope)
        .flatMap(List::stream)
        .forEach(fieldCreator);
  }
}
