package org.ek9lang.core.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.ek9lang.core.exception.CompilerException;
import org.junit.jupiter.api.Test;

class SharedThreadContextTest {

  /**
   * The idea is to create this object then pass it over to the context for safe keeping
   * From here it is only ever accessible via the context - don't hang on to it else where!
   */
  @Test
  void testSharedAccess() {
    final var underTest = new SharedThreadContext<>(new SomeDataAggregate());

    underTest.own(stc -> {
      underTest.get().setSomeOtherValue(45.77f);
      underTest.get().setSomeValue(23);
    });

    //Now check the values has stuck
    underTest.own(stc -> {
      assertEquals(45.77f, underTest.get().getSomeOtherValue());
      assertEquals(23, underTest.get().getSomeValue());
    });
  }

  @Test
  @SuppressWarnings("java:S5578")
  void testNotOwningThread() {
    final var underTest = new SharedThreadContext<>(new SomeDataAggregate());
    assertThrows(CompilerException.class, () -> {
      //So here we expect a failure! because we did not 'own' the data
      assertNull(underTest.get().getSomeValue());
      fail("Should have received exception");
    });
  }

  /**
   * This is just used for testing the shared context concept.
   */
  static class SomeDataAggregate {
    private Integer someValue;
    private Float someOtherValue;

    public SomeDataAggregate() {
    }

    public void setSomeValue(int newValue) {
      this.someValue = newValue;
    }

    public Integer getSomeValue() {
      return this.someValue;
    }

    public void setSomeOtherValue(float newValue) {
      this.someOtherValue = newValue;
    }

    public Float getSomeOtherValue() {
      return this.someOtherValue;
    }
  }
}
