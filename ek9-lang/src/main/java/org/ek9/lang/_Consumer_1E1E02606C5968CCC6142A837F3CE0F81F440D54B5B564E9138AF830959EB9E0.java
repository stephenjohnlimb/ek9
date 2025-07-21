package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Function type.
 * In this case the EK9 Consumer generic function type has been parameterised with String.
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
    Consumer of String""")
public class _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 extends BuiltinType {

  private final Consumer delegate;

  public _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0() {
    this(new Consumer());
  }

  //Internal constructor.
  private _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0(Consumer delegate) {
    this.delegate = delegate;
  }

  /**
   * The primary function call method - accepts String parameter instead of Any.
   * This provides type safety for the parameterized Consumer of String.
   * Consumer functions are pure, so this maintains that semantic.
   */
  public void _call(String t) {
    delegate._call(t); // Delegate to base Consumer, String is compatible with Any
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
    if (arg instanceof _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 asConsumer) {
      return _eq(asConsumer);
    }
    return new Boolean();
  }

  public Boolean _eq(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public static _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 _of() {
    return new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0();
  }

  public static _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 _of(Consumer consumer) {
    if (consumer != null) {
      return new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0(consumer);
    }
    return new _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0();
  }

}