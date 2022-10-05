package org.ek9lang.core.utils;

/**
 * Designed to enable a developer to wrap lots of different type of
 * processing and expose any exceptions.
 */
@FunctionalInterface
@SuppressWarnings("java:S112")
public interface Processor<T> {
  T process() throws Exception;
}
