package org.company.dept;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Note how it is declared. Also, it must have the right methods almost like it 'overrides'
 * the generic type that it has parameterised.
 * <p>
 * For example Object next(), becomes StringExample next().
 * </p>
 * <p>
 * All the other methods and operators must also be present on this class as they were in the generic class
 * being parameterised.
 * </p>
 * <p>
 *   In effect, you are manually providing the implementation of the generic type but with your selected type
 *   arguments. Clearly you should do the minimal amount of this, the EK9 compiler will in time generate this
 *   code (byte-code) given a generic type and the parameters.
 * </p>
 * <p>
 *   But for API development it is sometimes necessary to do these by hand. Because you actually do need
 *   a '_IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5' class to be present
 *   to get your code to compile.
 * </p>
 * <p>
 *   When the EK9 compiler encounters multiple implementations of
 *   '_IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5'
 *   (as it may if an application references multiple third party EK9 modules), it will resolve
 *   those to a single implementation (not quite sure how yet, but it will).
 * </p>
 */
@Ek9ParameterisedType("""
    IteratorExample of StringExample""")
public class _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5 {
  private final IteratorExample implementation;

  /**
   * Just to be used internally.
   *
   * @param arg0 to use as the implementation.
   */
  public _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5(IteratorExample arg0) {
    this.implementation = arg0;
  }

  public _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5() {
    this.implementation = new IteratorExample();
  }

  public _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5(StringExample arg0) {
    this.implementation = new IteratorExample(arg0);
  }

  public Boolean hasNext() {
    return implementation.hasNext();
  }

  public StringExample next() {
    //Now we must cast to the correct type.
    return (StringExample) implementation.next();
  }

  public Boolean _isSet() {
    return hasNext();
  }
}
