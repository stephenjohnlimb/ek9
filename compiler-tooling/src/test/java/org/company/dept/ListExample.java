package org.company.dept;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;

/**
 * Another generic type example.
 * <pre>
 *   ListExample of type T as open
 *     ListExample() as pure
 *
 *     ListExample() as pure
 *       -&gt; arg0 as T
 * </pre>
 */
@Ek9Class("""
    ListExample of type T as open""")
public class ListExample {
  private final java.util.List<Object> implementation;

  @Ek9Constructor("""
      ListExample() as pure""")
  public ListExample() {
    //Default Constructor
    implementation = new java.util.ArrayList<>();
  }

  @Ek9Constructor("""
      ListExample() as pure
        -> arg0 as T""")
  public ListExample(final Object arg0) {
    implementation = new java.util.ArrayList<>();
    implementation.add(arg0);
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as IteratorExample of T?""")
  public IteratorExample iterator() {
    return IteratorExample._of(this.implementation);
  }


}
