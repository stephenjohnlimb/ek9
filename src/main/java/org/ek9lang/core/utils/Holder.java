package org.ek9lang.core.utils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Used mainly in getting values out of a lambda consumer into a parent scope.
 * So these can then be returned.
 * Effectively it is a mutable holder of T.
 */
public class Holder<T> implements Consumer<Optional<T>>, Supplier<Optional<T>> {
  private Optional<T> value = Optional.empty();

  /**
   * A new empty holder.
   */
  public Holder() {
  }

  /**
   * A new Holder with a set value.
   *
   * @param value The value to use initially.
   */
  public Holder(T value) {
    accept(Optional.ofNullable(value));
  }

  /**
   * Provides access to an optional of the value.
   *
   * @return The Optional of T (can be empty)
   */
  public Optional<T> get() {
    return value;
  }


  /**
   * Set the T value directly can be null.
   *
   * @param value the value or null.
   */
  public void accept(Optional<T> value) {
    this.value = value;
  }

  /**
   * Checks if a valid value is held.
   *
   * @return true if the T is present.
   */
  public boolean isPresent() {
    return value.isPresent();
  }

  /**
   * Check if no value is held.
   */
  public boolean isEmpty() {
    return !isPresent();
  }
}