package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

public class AssertSet implements Consumer<BuiltinType> {
  @Override
  public void accept(final BuiltinType builtinType) {
    assertNotNull(builtinType);
    assertTrue(builtinType.isSet);
  }
}
