package org.ek9introspection;

import java.util.function.Predicate;

/**
 * Just used to check if a Ek9InterfaceOrError has a valid generated EK9 interface or an error.
 */
public class ValidEk9Interface implements Predicate<Ek9InterfaceOrError> {

  @Override
  public boolean test(final Ek9InterfaceOrError ek9InterfaceOrError) {
    return ek9InterfaceOrError.ek9Interface() != null
        && !ek9InterfaceOrError.ek9Interface().isEmpty()
        && (ek9InterfaceOrError.errorMessage() == null || !ek9InterfaceOrError.errorMessage().isEmpty());
  }
}
