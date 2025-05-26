/**
 * <p>
 * This module and package is designed at enabling normal Java code to
 * be annotated with a set of annotations, that then enable the 'EK9 interface extractor'
 * to pull out the ek9 based interface.
 * </p>
 * <p>
 * The basic scenario is that a developer (i.e. me) will define a set of Java
 * classes that are annotated and then packaged up in a jar.
 * </p>
 * <p>
 *   The jar is that 'scanned' via introspection to pull out the EK9 interface.
 *   In short this is like a 'C' header file. But is not in a separate file but
 *   bound into the class files in the jar.
 * </p>
 * <p>
 *   So, for example, the org.ek9.lang.jar would contain a set of classes as follows:
 * </p>
 * <p>File org/ek9/lang/package-info.java</p>
 * <pre>&#64;Ek9Module("defines extern module org.ek9.lang")
 package org.ek9.lang;
 import org.ek9tooling.Ek9Module;
 </pre>
 <p>Then an actual class can use the following annotations</p>
 <pre>package org.ek9.lang;

 import org.ek9tooling.Ek9Construct;
 import org.ek9tooling.Ek9Constructor;
 import org.ek9tooling.Ek9Method;

 &#64;Ek9Construct(construct = "class", value = "Stdout with trait of StringOutput, PipedOutput")
 public class Stdout {

   &#64;Ek9Constructor("Stdout() as pure")
   public Stdout() {
   //Just a default constructor, but we need the annotation.
   }

   //Note defining in different order, to ensure introspection reorders for EK9 compiler.
   &#64;Ek9Method("""override operator ? as pure
   &lt;- rtn as Boolean?""")
   public Boolean _isSet() {
     return Boolean.of("true");
   }

 &#64;Ek9Method("""println() as pure
 -&gt; arg0 as String""")
 public void println(final org.ek9.lang.String arg0) {
   System.out.println(org.ek9.lang.String.from(arg0));
   }
 }
 </pre>
 <p>The 'EK9 interface extractor' will find these annotations and pull out the
 EK9 text and use that as the 'bridge' between EK9 and the Java class.
 </p>
 */

package org.ek9tooling;