package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Function type.
 * In this case the EK9 Consumer generic function type has been parameterised with JSON.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic function type
 * (which is actually just a normal type in Java where the 'T' is just 'Any').
 * When parameterising with a type the T (Any) is just replaced with the parameterising type in the
 * appropriate places. All the calls are just applied to the 'delegate'.
 * </p>
 * <p>
 * But note it is necessary to 'cast' in specific situations.
 * </p>
 * <p>
 * This whole approach depends upon ensuring only objects of the parameterised type (or subtypes) are
 * allowed into the implementation, thereby ensuring the returning casts can be made without error.
 * </p>
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    Consumer of JSON""")
public class _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 extends BuiltinType {

  private final Consumer delegate;

  public _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588() {
    this(new Consumer());
  }

  //Internal constructor.
  private _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588(Consumer delegate) {
    this.delegate = delegate;
  }

  /**
   * The primary function call method - accepts JSON parameter instead of Any.
   * This provides type safety for the parameterized Consumer of JSON.
   * Consumer functions are pure, so this maintains that semantic.
   */
  public void _call(JSON t) {
    delegate._call(t); // Delegate to base Consumer, JSON is compatible with Any
  }

  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  @Override
  public String _string() {
    return delegate._string();
  }

  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 asConsumer) {
      return _eq(asConsumer);
    }
    return new Boolean();
  }

  public Boolean _eq(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public static _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 _of() {
    return new _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588();
  }

  public static _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 _of(Consumer consumer) {
    if (consumer != null) {
      return new _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588(consumer);
    }
    return new _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588();
  }

}