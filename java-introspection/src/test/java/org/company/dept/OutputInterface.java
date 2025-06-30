package org.company.dept;

import org.ek9tooling.Ek9Component;
import org.ek9tooling.Ek9Method;

@Ek9Component("OutputInterface as abstract")
public abstract class OutputInterface {

  @Ek9Method("""
      output() as pure
        -> value as String""")
  public void output(String value) {
    System.out.println(value);
  }
}
