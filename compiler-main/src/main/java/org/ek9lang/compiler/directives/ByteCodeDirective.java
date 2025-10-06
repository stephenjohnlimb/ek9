package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code to assert that generated JVM bytecode matches expectations.
 * <pre>
 * {@code @BYTECODE}: CODE_GENERATION_AGGREGATES: TYPE: "Example": `
 *   public class Example {
 *     public Example();
 *       Code:
 *         0: aload_0
 *         1: invokespecial #CP  // Method java/lang/Object."&lt;init&gt;":()V
 *         4: return
 *   }
 * `
 * </pre>
 */
public class ByteCodeDirective extends ResolutionDirective {

  /**
   * A new bytecode validation directive.
   * The symbol name identifies the construct to validate bytecode for.
   * The additionalName contains the expected normalized bytecode output from javap.
   */
  public ByteCodeDirective(final DirectiveSpec spec) {

    super(spec);

  }

  @Override
  public DirectiveType type() {

    return DirectiveType.BYTECODE;
  }

}
