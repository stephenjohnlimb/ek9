package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template type.
 * In this case the EK9 Optional generic type has been parameterised with String.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic type
 * (which is actually just a normal type in Java where the 'T' is just 'Any').
 * When parameterising with a type the T (Any) is just replaced with the parameterising type in the
 * appropriate places. All the calls are just applied to the 'delegate.
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
    Optional of String""")
public class _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC extends BuiltinType {

  private final Optional delegate;

  public _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC() {
    this(new Optional());
  }

  public _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(String arg0) {
    this(new Optional(arg0));
  }

  //Internal constructor.
  private _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(Optional delegate) {
    this.delegate = delegate;
  }

  public _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC asEmpty() {
    return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
  }

  public String get() {
    return (String) delegate.get();
  }

  public String getOrDefault(String arg) {
    return (String) delegate.getOrDefault(arg);
  }

  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator() {
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(delegate.iterator());
  }

  public void whenPresent(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptor) {
    if (acceptor != null && delegate._isSet().state) {
      acceptor._call(get()); // Call parameterized Acceptor with String
    }
  }

  public void whenPresent(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumer) {
    if (consumer != null && delegate._isSet().state) {
      consumer._call(get()); // Call parameterized Consumer with String
    }
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
    if (arg instanceof _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC asOptional) {
      return _eq(asOptional);
    }
    return new Boolean();
  }

  public Boolean _eq(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public Boolean _empty() {
    return delegate._empty();
  }

  public Boolean _contains(String arg) {
    return delegate._contains(arg);
  }

  public void _merge(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC arg) {
    if (arg != null && arg._isSet().state) {
      delegate._merge(arg.get());
    }
  }

  public void _replace(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC arg) {
    if (arg != null && arg._isSet().state) {
      delegate._replace(arg.get());
    } else {
      delegate._replace(null);
    }
  }

  public void _copy(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC arg) {
    if (arg != null && arg._isSet().state) {
      delegate._copy(arg.get());
    } else {
      delegate._copy(null);
    }
  }

  public void _pipe(String arg) {
    delegate._pipe(arg);
  }

  public static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC _of() {
    return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
  }

  public static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC _of(String string) {
    if (string != null) {
      return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(string);
    }
    return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
  }

  public static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC _of(Optional optional) {
    if (optional != null) {
      return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(optional);
    }
    return new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
  }

}