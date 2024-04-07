package org.ek9lang.compiler.support;

import java.util.function.Predicate;

/**
 * Given a parent and a dependent generic type, check that all parameters have been provided with suitable
 * arguments, so that the Parameterized type can be used.
 * Note that the parent is provided so that any conceptual arguments can be checked against those in the parent.
 */
public class CheckParameterizationComplete implements Predicate<GenericInGenericData> {
  @Override
  public boolean test(final GenericInGenericData data) {

    final var parentParameterizationArguments = data.parent().getTypeParameterOrArguments();

    for (var arg : data.dependent().getTypeParameterOrArguments()) {
      if (arg.isConceptualTypeParameter() && !parentParameterizationArguments.contains(arg)) {
        return false;
      }
    }

    return true;
  }
}
