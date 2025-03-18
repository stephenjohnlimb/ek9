package org.ek9lang.compiler.backend;

import org.ek9lang.core.TargetArchitecture;

/**
 * Target for a Java virtual machine.
 * i.e. this Target would produce ByteCode that would execute on a
 * Java Virtual Machine (JVM).
 */
public class JvmTarget implements Target {
  @Override
  public TargetArchitecture getArchitecture() {
    return TargetArchitecture.JVM;
  }

  @Override
  public boolean isSupported() {
    return true;
  }
}
