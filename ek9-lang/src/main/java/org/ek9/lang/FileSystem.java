package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Gives EK9 access to the file system for getting standard directories.
 * This component is always in a valid state (isSet is always true).
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    FileSystem as open""")
public class FileSystem extends BuiltinType {

  @Ek9Constructor("""
      FileSystem() as pure""")
  public FileSystem() {
    set(); // Always set - FileSystem is always valid
  }

  @Ek9Method("""
      cwd() as pure
        <- rtn as FileSystemPath?""")
  public FileSystemPath cwd() {
    return new FileSystemPath().withCurrentWorkingDirectory();
  }

  @Ek9Method("""
      tmp() as pure
        <- rtn as FileSystemPath?""")
  public FileSystemPath tmp() {
    return new FileSystemPath().withTemporaryDirectory();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true); // FileSystem is always set
  }

}