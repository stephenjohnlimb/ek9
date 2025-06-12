package org.company.dept;


import org.ek9tooling.Ek9Construct;
import org.ek9tooling.Ek9Constructor;


@Ek9Construct("program")
public class Program1 {

  /**
   * This is the Java entry point - because it has to be for Java.
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    //Here the developer would need to handle the incoming arguments and
    //convert to the appropriate EK9 versions and then call _main

    //i.e.
    final var program1 = new Program1();
    program1._main(/* Passing in any EK9 instances once created. */);
  }

  //So you could do this in the constructor - "Program1() with application of DemoApp"
  //In the implementation you'd need to create the 'DemoApp' and hold it as a property
  //of Program1. Then you can use the properties of 'DemoApp' as injected values.
  //See https://www.ek9.io/components.html for details.
  @Ek9Constructor("Program1()")
  public Program1() {
    //Public constructor of the program.
    //Typically, this can use applications and the like as well.
  }

  public void _main() {
    //This would actually be the main entry point in terms of EK9
  }

}

