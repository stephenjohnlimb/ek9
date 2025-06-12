package org.company.dept;


import org.ek9tooling.Ek9Construct;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;

/**
 * Just used to test the annotations can be located on the class.
 * Used to create an EK9 signature specification to be imported into
 * the compiler when a 'jar' containing something like this is employed.
 * This means that third party jars can be downloaded and if annotated correctly
 * can be exposed to the EK9 compiler and then used for development.
 * This is like a C header and the jar is like the "*.so"/"*.a", but the header is built
 * into the jar/library and can be extracted.
 */
@Ek9Construct(construct = "class", value = "StringExample")
public class StringExample {

  private java.lang.String value;

  @Ek9Constructor("StringExample() as pure")
  public StringExample() {

  }

  @Ek9Method("""
      upperCase() as pure
        <- rtn as StringExample?""")
  public StringExample upperCase() {
    if (value != null) {
      return new StringExample(value.toUpperCase());
    }
    return new StringExample();
  }


  private StringExample(final java.lang.String value) {
    this.value = value;
  }

  public static StringExample of(final java.lang.String value) {
    return new StringExample(value);
  }

  public static java.lang.String from(final StringExample string) {
    return string.value;
  }
}

