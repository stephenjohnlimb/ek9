package org.ek9lang.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SharedThreadContextTest {

  /**
   * The idea is to create this object then pass it over to the context for safe keeping
   * From here it is only ever accessible via the context - don't hang on to it else where!
   */
  @Test
  void testSharedAccess() {
    final var underTest = new SharedThreadContext<>(new SomeDataAggregate());

    underTest.accept(data -> {
      data.setSomeOtherValue(45.77f);
      data.setSomeValue(23);
    });

    //Now check the values has stuck
    underTest.accept(data -> {
      assertEquals(45.77f, data.getSomeOtherValue());
      assertEquals(23, data.getSomeValue());
    });
  }

  @Test
  void testJustReadAccess() {
    final var underTest = new SharedThreadContext<>(new SomeDataAggregate(21));
    underTest.accept(myAggregate -> assertEquals(21, myAggregate.getSomeValue()));
  }

  @Test
  void testReentrantNature() {
    final Consumer<Consumer<SomeDataAggregate>> underTest = new SharedThreadContext<>(new SomeDataAggregate());

    underTest.accept(data -> {
      data.setSomeOtherValue(45.77f);
      data.setSomeValue(23);
      //Test we can get the lock when we already have it.
      underTest.accept(data2 -> data2.setSomeValue(24));
    });

    underTest.accept(data -> {
      assertEquals(45.77f, data.getSomeOtherValue());
      assertEquals(24, data.getSomeValue());
    });
  }

  @Test
  void testExceptionOnInvalidData() {
    assertThrows(IllegalArgumentException.class, () -> new SharedThreadContext<>(null));
  }

  /**
   * This is just used for testing the shared context concept.
   */
  static class SomeDataAggregate {
    private Integer someValue;
    private Float someOtherValue;

    public SomeDataAggregate() {
    }

    public SomeDataAggregate(int someValue) {
      this.someValue = someValue;
      someOtherValue = 0.0f;
    }

    public Integer getSomeValue() {
      return this.someValue;
    }

    public void setSomeValue(int newValue) {
      this.someValue = newValue;
    }

    public Float getSomeOtherValue() {
      return this.someOtherValue;
    }

    public void setSomeOtherValue(float newValue) {
      this.someOtherValue = newValue;
    }
  }
}
