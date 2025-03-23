package org.ek9lang.asm;

/**
 * Only design to be an experiment for Asmifier.
 * Use with
 * <pre>
 * java -cp $HOME/.m2/repository/org/ow2/asm/asm-util/9.7.1/asm-util-9.7.1.jar:$HOME/.m2/repository/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar:. org.objectweb.asm.util.ASMifier org.ek9lang.asm.SimpleExampleForAsm
 * </pre>
 * <p>
 *   For pure bytes code rather than the ASM coded needed create it use:
 * </p>
 * <p>
 *   javap -c -p org.ek9lang.asm.SimpleExampleForAsm
 * </p>
 */
public class SimpleExampleForAsm {

  public int checkMethod(String[] args) {
    int a;

    a = 165;

    int b;

    b = a;

    int c;

    c = a + b;

    return c;
  }
}
