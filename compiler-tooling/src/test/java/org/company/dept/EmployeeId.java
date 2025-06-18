package org.company.dept;

import org.ek9tooling.Ek9ConstrainedType;

/**
 * Example of an EK9 constrained type.
 */
@Ek9ConstrainedType("""
    EmployeeId as Integer constrain
      > 0""")
public class EmployeeId {
  //TODO add in appropriate methods.
}
