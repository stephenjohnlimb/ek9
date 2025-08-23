package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.IRInstr;

/**
 * Converts the single IRInstr supplied by the supplier into a List with one element in.
 */
public class IRInstrToList implements Function<Supplier<IRInstr>, List<IRInstr>> {

  @Override
  public List<IRInstr> apply(final Supplier<IRInstr> supplier) {
    final var instructions = new ArrayList<IRInstr>();
    instructions.add(supplier.get());
    return instructions;
  }
}
