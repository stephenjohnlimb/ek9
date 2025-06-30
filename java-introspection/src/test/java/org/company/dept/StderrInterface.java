package org.company.dept;

import org.ek9tooling.Ek9Component;
import org.ek9tooling.Ek9Method;

@Ek9Component("StderrInterface is OutputInterface")
public class StderrInterface extends OutputInterface {

  @Override
  @Ek9Method("""
      override output() as pure
        -> value as String""")
  public void output(String value) {
    System.err.println(value);
  }

}
