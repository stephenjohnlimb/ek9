package org.ek9lang.compiler.support;

import java.io.IOException;
import org.objectweb.asm.util.ASMifier;

/**
 * Just wraps the standard ASMifier into EK9 support package.
 * Very useful for creating asm source by reverse engineering a java class.
 * So write a simple java class of what you want, compile it then use this
 * to show what the asm code would look like.
 */
public class ASMSupport {

  /**
   * Prints the ASM source code to generate the given class to the standard output.
   * Usage: ASMSupport [-nodebug] <binary class name or class file name>
   * Params:
   * args – the command line arguments.
   * Throws:
   * IOException – if the class cannot be found, or if an IOException occurs.
   */
  public static void main(String[] args) throws IOException {
    ASMifier.main(args);
  }
}
