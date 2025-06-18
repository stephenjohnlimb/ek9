package org.company.dept;


import org.ek9tooling.Ek9Function;

/**
 * Check how an EK9 Function can be declared as a Java class.
 * This one extends an abstract base function.
 */
@Ek9Function("""
    Multiply() is MathOperation as pure
      -> value as Float
      <- result as Float?""")
public class Multiply extends MathOperation {

  //For Functions (i.e. none dynamic) there is an _instance of the function.
  //This is to enable functions to be passed around.
  public static final Multiply _instance = new Multiply();

  public Multiply() {
    //Default constructor - not exposed in any way to EK9
  }

  //Now implement the abstract function.
  public Float _call(final Float value) {
    return value * 5.6f;
  }

}

