package org.company.dept;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9References;

/**
 * Just another example, obviously isset would not return a java Boolean but an EK9 Boolean.
 */
@Ek9Class
@Ek9References("""
    net.customer.geometry::Zeta""")
public class General {

  @Ek9Constructor("General() as pure")
  public General() {
    //default constructor
  }

  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return true;
  }


}
