package org.ek9lang.core.utils;

/**
 * Designed to enable a developer to wrap lots of different type of
 * processing and expose any exceptions.
 */
@FunctionalInterface
public interface Processor {
  boolean process() throws Exception;
}
