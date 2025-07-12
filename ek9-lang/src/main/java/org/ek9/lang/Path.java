package org.ek9.lang;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * Used to represent some sort of 'Path' into structured data.
 * <pre>
 *   path1 <- $?.some.path.inc[0].array
 *   path2 <- $?.another[2][1].multi-Pathal.array.access_value
 * </pre>
 * <p>
 * This is not like a file path with is just '/some/dir/someFile.txt'.
 * It is more for something like a JSON path. i.e. addressing something in a structure.
 * </p>
 * <p>
 * It will typically be used with something like JSON, but could be used with Lists, Maps
 * and EK9 records.
 * </p>
 * <pre>
 *   PathLiteral
 *     : PATH PathPart+
 *     ;
 *   PathPart
 *     : (DOT [a-zA-Z0-9_-]+)  | (LBRACK Digit+ RBRACK)
 *     ;
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Path as open""")
public class Path extends BuiltinType {

  //Just holds the actual path, the $? is not held internally.
  java.lang.String state = "";

  @Ek9Constructor("""
      Path() as pure""")
  public Path() {
    unSet();
    //default constructor
  }

  @Ek9Constructor("""
      Path() as pure
        -> arg0 as Path""")
  public Path(Path arg0) {
    //Copy Constructor
    unSet();
    if (isValid(arg0)) {
      assign(arg0);
    }
  }

  @Ek9Constructor("""
      Path() as pure
        -> arg0 as String""")
  public Path(String arg0) {
    //From a String
    unSet();
    if (isValid(arg0)) {
      assign("$?" + arg0.state);
    }
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Path
        <- rtn as Integer?""")
  public Integer _cmp(Path arg) {
    if (canProcess(arg)) {
      return Integer._of(this.compare(arg));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Path asPath) {
      return _cmp(asPath);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Path
        <- rtn as Integer?""")
  public Integer _fuzzy(Path arg) {
    //Just delegate to the String fuzzy match
    if (canProcess(arg)) {
      return String._of(this.state)._fuzzy(String._of(arg.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _lt(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _lteq(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _gt(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _gteq(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _eq(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _neq(Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator + as pure
        -> arg as Path
        <- rtn as Path?""")
  public Path _add(Path arg) {
    Path rtn = _new();
    if (canProcess(arg)) {
      rtn.assign("$?" + state + arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as String
        <- rtn as Path?""")
  public Path _add(String arg) {
    Path rtn = _new();
    if (canProcess(arg)) {
      rtn.assign("$?" + state + arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Character
        <- rtn as Path?""")
  public Path _add(Character arg) {
    Path rtn = _new();
    if (canProcess(arg)) {
      rtn.assign("$?" + state + arg.state);
    }
    return rtn;
  }


  @Ek9Operator("""
      operator +=
        -> arg as String""")
  public void _addAss(String value) {
    if (canProcess(value)) {
      assign("$?" + state + value.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Path""")
  public void _addAss(Path value) {
    if (canProcess(value)) {
      assign("$?" + state + value.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Character""")
  public void _addAss(Character value) {
    if (canProcess(value)) {
      assign("$?" + state + value.state);
    } else {
      unSet();
    }
  }


  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (isSet) {
      return String._of(this.toString());
    }
    return new String();
  }

  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(this.hashCode());
    }
    return rtn;
  }


  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(this.state == null || this.state.isBlank());
    }

    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    return _string()._len();
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _contains(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(this.state.contains(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _contains(Path arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(this.state.contains(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Path""")
  public void _merge(Path arg) {
    if (isValid(arg)) {
      if (isSet) {
        _addAss(arg);
      } else {
        assign(arg);
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Path""")
  public void _replace(Path arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Path""")
  public void _copy(Path value) {
    if (isValid(value)) {
      assign(value);
    } else {
      super.unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Path""")
  public void _pipe(Path arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator |
        -> arg as String""")
  public void _pipe(String arg) {
    if (isValid(arg)) {
      if (isSet) {
        _addAss(arg);
      } else {
        assign(arg.state);
      }
    }
  }

  //Start of Utility methods


  @Override
  public final boolean equals(final Object obj) {

    if (super.equals(obj) && obj instanceof Path path) {
      if (isSet) {
        return Objects.equals(state, path.state);
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(state);
    return result;
  }

  @Override
  public java.lang.String toString() {
    if (isSet) {
      return "$?" + state;
    }
    return "";
  }

  private java.lang.Integer compare(Path arg) {

    return java.lang.Integer.compare(this.state.compareTo(arg.state), 0);

  }

  private void assign(Path arg) {
    if (isValid(arg)) {
      assign("$?" + arg.state);
    } else {
      unSet();
    }
  }

  private void assign(java.lang.String value) {

    java.lang.String stateBefore = this.state;
    boolean beforeIsValid = isSet;
    parse(value);
    set();
    if (!validateConstraints().isSet) {
      java.lang.String stringTo = this.toString();
      state = stateBefore;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + this + " to " + stringTo);
    }

  }

  protected void parse(java.lang.String value) {

    Pattern p = Pattern.compile("^\\$\\?(?:\\.[a-zA-Z_][\\w-]*|\\[\\d+])+$");

    Matcher m = p.matcher(value);
    if (m.matches()) {
      //Remove the $? we don't store that.
      this.state = value.substring(2);
      //Then we can just use the String
    } //else remains Unset.
  }

  /**
   * Static factory method for creating a Path from a String.
   */
  public static Path _of(java.lang.String arg) {
    Path rtn = new Path();
    if (arg != null) {
      rtn.parse(arg);
      if (rtn.state != null && !rtn.state.isEmpty()) {
        rtn.set();
      }
    }
    return rtn;
  }

  @Override
  protected Path _new() {
    return new Path();
  }
}
