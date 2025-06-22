package org.company.dept;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * This is an example of a parameterised generic type.
 * Use org.ek9lang.compiler.support.DecoratedName to generate the name.
 * i.e. With arguments like this.
 * <pre>
 *  ListExample org.company.dept::ListExample org.company.dept::StringExample
 * </pre>
 * <p>
 * So, here creating a new parameterised type from the generic type 'ListExample'
 * and the parameter is type 'StringExample'. Note the use of the fully qualified names.
 * </p>
 * <p>
 * But note how it is declared. Also, it must have the right methods almost like it 'overrides'
 * the generic type that it has parameterised.
 * </p>
 */
@Ek9ParameterisedType("""
    ListExample of StringExample""")
public class _ListExample_6D0CFE7BAD96ADBE4DEC4233902464439625334748EAE781FC7A68C5F42BCDF9 {
  private final ListExample implementation;

  public _ListExample_6D0CFE7BAD96ADBE4DEC4233902464439625334748EAE781FC7A68C5F42BCDF9() {
    this.implementation = new ListExample();
  }

  public _ListExample_6D0CFE7BAD96ADBE4DEC4233902464439625334748EAE781FC7A68C5F42BCDF9(StringExample arg0) {
    this.implementation = new ListExample(arg0);
  }

  public _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5 iterator() {
    return new _IteratorExample_F3DA346530B783C083C5EEB9223B775DAFC330BE01B97078E66445EA304C85D5(
        this.implementation.iterator());
  }
}
