package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * Represents the abstract concept of a File, i.e. like TextFile.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MethodName"})
@Ek9Trait("""
    File as open""")
public interface File extends Any {

  @Ek9Method("""
      isWritable() as pure
        <- rtn as Boolean?""")
  default Boolean isWritable() {
    return new Boolean();
  }

  @Ek9Method("""
      isReadable() as pure
        <- rtn as Boolean?""")
  default Boolean isReadable() {
    return new Boolean();
  }

  @Ek9Method("""
      isExecutable() as pure
        <- rtn as Boolean?""")
  default Boolean isExecutable() {
    return new Boolean();
  }

  @Ek9Method("""
      lastModified() as pure
        <- rtn as DateTime?""")
  default DateTime lastModified() {
    return new DateTime();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  default String _string() {
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  default Integer _hashcode() {
    return new Integer();
  }
}
