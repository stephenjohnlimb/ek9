package org.company.dept;


import org.ek9tooling.Ek9Function;

/**
* Check how an EK9 Function can be declared as a Java class.
 */
@Ek9Function("""
    MathOperation() as pure abstract
      -> value as Float
      <- result as Float?""")
public abstract class MathOperation {

  public MathOperation() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a MathOperation will implement this.
  public abstract Float _call(final Float value) ;

  //The EK9 compiler will expect even a function to have '?' i.e. _isSet()
  //This is so that EK9 can pass around functions.
  //Note it will always be true! But the EK9 generated code will also check for null.
  public Boolean _isSet() {
    return true;
  }
}

