package org.company.dept;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Text;

@Ek9Text(value = "WelcomePageText", lang = "en_GB")
public class WelcomePageText_en_GB extends WelcomePageText {

  /*
   * Note that while there is no explicit return for text methods, they all
   * must return a String.
   * So, EK9 omits the return because they are always Strings and you cannot do
   * any other 'major' function calls other than interpolated calls.
   * Note that it is important to provide some form of empty value in the declaration
   * i.e. ``.
   */
  @Ek9Method("""
      namedWelcome()
        -> name String
        ``""")
  public String namedWelcome(final String name) {
    return String.format("Welcome %s", name);
  }
}
