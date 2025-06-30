package org.company.dept;


import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Program;
import org.ek9tooling.Ek9References;


/**
 * Example of another program, this uses references as a demonstration.
 * But I need to ensure that, there is no duplication in the references when converted to
 * an extern ek9 module. So adding in some of the same referenced values to check for de-duplication.
 */
@Ek9Program
@Ek9References("""
    net.customer.geometry::PI
    net.customer.geometry::Zeta
    """)
public class Program2 {

  /**
   * This is the Java entry point - because it has to be for Java.
   *
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    //Here the developer would need to handle the incoming arguments and
    //convert to the appropriate EK9 versions and then call _main
    //When the EK9 compiler creates this it will use the arguments on the Program
    //and expect those from 'args' and convert the incoming Strings to the
    //appropriate type and then call '._main'.

    if (args.length != 1) {
      System.err.println("Expecting one argument");
      System.exit(2);
    }
    final var arg1 = args[0];

    //i.e. construct the program - we'd add in any 'Application' here.
    final var program = new Program2();

    //Now we can call the EK9 version of main - with the right type of argument.
    program._main(arg1);
  }

  //Example of how arguments can be added
  //But note they are applied on _main()
  @Ek9Constructor("""
      Program2()
        -> arg1 as String""")
  public Program2() {
    //Public constructor of the program.
    //Typically, this can use applications and the like as well.
  }

  /**
   * For a program, this will be the actual entry point from and EK9 point of view.
   */
  public void _main(String arg1) {
    assert arg1 != null;
    //This would actually be the main entry point in terms of EK9
  }

}

