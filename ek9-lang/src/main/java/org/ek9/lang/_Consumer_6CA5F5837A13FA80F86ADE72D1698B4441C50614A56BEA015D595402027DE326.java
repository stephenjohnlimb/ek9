package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Function type.
 * In this case the EK9 Consumer generic function type has been parameterised with Boolean.
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
    Consumer of Boolean""")
public class _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 extends BuiltinType {

  private final Consumer delegate;

  public _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326() {
    this(new Consumer());
  }

  //Internal constructor.
  private _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326(Consumer delegate) {
    this.delegate = delegate;
  }

  /**
   * The primary function call method - accepts Boolean parameter instead of Any.
   * This provides type safety for the parameterized Consumer of Boolean.
   * Consumer functions are pure, so this maintains that semantic.
   */
  public void _call(Boolean t) {
    delegate._call(t); // Delegate to base Consumer, Boolean is compatible with Any
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
    if (arg instanceof _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 asConsumer) {
      return _eq(asConsumer);
    }
    return new Boolean();
  }

  public Boolean _eq(_Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public static _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 _of() {
    return new _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326();
  }

  public static _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 _of(Consumer consumer) {
    if (consumer != null) {
      return new _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326(consumer);
    }
    return new _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326();
  }

}