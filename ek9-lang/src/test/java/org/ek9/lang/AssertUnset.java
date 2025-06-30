package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Consumer;

public class AssertUnset implements Consumer<BuiltinType> {
  @Override
  public void accept(final BuiltinType builtinType) {
    assertNotNull(builtinType);
    assertFalse(builtinType.isSet);
  }
}
