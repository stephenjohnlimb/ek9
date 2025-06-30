package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.function.Consumer;

public class AssertFalse implements Consumer<Boolean> {
  protected final AssertSet assertSet = new AssertSet();

  @Override
  public void accept(final Boolean aBoolean) {
    assertSet.accept(aBoolean);
    assertFalse(aBoolean.state);
  }
}
