package org.ek9lang.compiler;

/**
 * Defines the concept of an EK9 source file.
 * This can be development or library or just a main line source file.
 * i.e. not dev and not lib - means it is main project source.
 */
public interface Source {
  String getFileName();

  default boolean isDev() {
    return false;
  }

  default boolean isLib() {
    return false;
  }
}
