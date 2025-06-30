package org.company.dept;


import org.ek9tooling.Ek9Function;

/**
 * Check how an EK9 Function can be declared as a Java class.
 */
@Ek9Function("""
    Multiplier() as pure
      <- value as Float?""")
public class Multiplier {

  //For Functions (i.e. none dynamic) there is an _instance of the function.
  //This is to enable functions to be passed around.
  public static final Multiplier _instance = new Multiplier();

  public Multiplier() {
    //Default constructor - not exposed in any way to EK9
  }

  //Note that the EK9 compiler will call this method.
  public Float _call() {
    return 5.6f;
  }

}

