package org.ek9.lang;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 RegEx type - provides regular expression functionality with EK9 semantics.
 * Uses Java Pattern/Matcher for underlying regex operations.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:CatchParameterName"})
@Ek9Class("""
    RegEx as open""")
public class RegEx extends BuiltinType implements Any {
  private Pattern state;

  @Ek9Constructor("""
      RegEx() as pure""")
  public RegEx() {
    super.unSet();
  }

  @Ek9Constructor("""
      RegEx() as pure
        -> arg0 as RegEx""")
  public RegEx(final RegEx arg0) {
    unSet();
    assign(arg0);
  }

  @Ek9Constructor("""
      RegEx() as pure
        -> arg0 as String""")
  public RegEx(final String arg0) {
    unSet();
    if (isValid(arg0)) {
      try {
        this.state = Pattern.compile(arg0.state);
        set();
      } catch (final PatternSyntaxException _) {
        // Invalid regex pattern results in unset state
        unSet();
      }
    }
  }

  @Ek9Method("""
      split() as pure
        -> toSplit as String
        <- rtn as List of String?""")
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 split(final String toSplit) {
    if (canProcess(toSplit)) {
      final var parts = state.split(toSplit.state);
      final java.util.List<Any> ekStrings = Arrays.stream(parts)
          .map(String::_of)
          .collect(Collectors.toList());
      return _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of(ekStrings);
    }
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as RegEx
        <- rtn as Boolean?""")
  public Boolean _eq(final RegEx arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.pattern().equals(arg.state.pattern()) 
          && state.flags() == arg.state.flags());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as RegEx
        <- rtn as Boolean?""")
  public Boolean _neq(final RegEx arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as RegEx
        <- rtn as Integer?""")
  public Integer _cmp(final RegEx arg) {
    if (canProcess(arg)) {
      return Integer._of(state.pattern().compareTo(arg.state.pattern()));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as RegEx
        <- rtn as Integer?""")
  public Integer _fuzzyCompare(final RegEx arg) {
    // For regex, fuzzy compare is same as regular compare
    return _cmp(arg);
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as RegEx
        <- rtn as RegEx?""")
  public RegEx _add(final RegEx arg) {
    if (canProcess(arg)) {
      // Concatenate regex patterns
      return new RegEx(String._of(state.pattern() + arg.state.pattern()));
    }
    return new RegEx();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as String
        <- rtn as RegEx?""")
  public RegEx _add(final String arg) {
    if (canProcess(arg)) {
      // Concatenate regex pattern with string (escaped)
      return new RegEx(String._of(state.pattern() + Pattern.quote(arg.state)));
    }
    return new RegEx();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Character
        <- rtn as RegEx?""")
  public RegEx _add(final Character arg) {
    if (canProcess(arg)) {
      // Concatenate regex pattern with character (escaped)
      return new RegEx(String._of(state.pattern() + Pattern.quote(arg._string().state)));
    }
    return new RegEx();
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    return _string();
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      return new JSON(this);
    }
    return new JSON();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(state.pattern());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      return Integer._of(state.hashCode());
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(state.pattern().isEmpty());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _length() {
    if (isSet) {
      return Integer._of(state.pattern().length());
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator matches as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _matches(final String arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.matcher(arg.state).matches());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator matches as pure
        -> arg as Path
        <- rtn as Boolean?""")
  public Boolean _matches(final Path arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.matcher(arg._string().state).matches());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator matches as pure
        -> arg as FileSystemPath
        <- rtn as Boolean?""")
  public Boolean _matches(final FileSystemPath arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.matcher(arg._string().state).matches());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator matches as pure
        -> arg as Locale
        <- rtn as Boolean?""")
  public Boolean _matches(final Locale arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.matcher(arg._string().state).matches());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as RegEx""")
  public void _merge(final RegEx arg) {
    if (isValid(arg)) {
      // Merge creates union pattern (pattern1|pattern2)
      if (isSet) {
        try {
          this.state = Pattern.compile("(" + state.pattern() + ")|(" + arg.state.pattern() + ")");
        } catch (final PatternSyntaxException _) {
          unSet();
        }
      } else {
        assign(arg);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as RegEx""")
  public void _replace(final RegEx arg) {
    assign(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as RegEx""")
  public void _copy(final RegEx arg) {
    assign(arg);
  }

  @Ek9Operator("""
      operator |
        -> arg as RegEx""")
  public void _or(final RegEx arg) {
    if (isValid(arg)) {
      // Logical OR creates alternation pattern
      if (isSet) {
        try {
          this.state = Pattern.compile(state.pattern() + "|" + arg.state.pattern());
        } catch (final PatternSyntaxException _) {
          unSet();
        }
      } else {
        assign(arg);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as RegEx""")
  public void _addAss(final RegEx arg) {
    final var result = _add(arg);
    if (result._isSet().state) {
      assign(result);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as String""")
  public void _addAss(final String arg) {
    final var result = _add(arg);
    if (result._isSet().state) {
      assign(result);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Character""")
  public void _addAss(final Character arg) {
    final var result = _add(arg);
    if (result._isSet().state) {
      assign(result);
    } else {
      unSet();
    }
  }

  /**
   * Factory method to create set RegEx from String.
   */
  public static RegEx _of(final java.lang.String pattern) {
    return new RegEx(String._of(pattern));
  }

  @Override
  protected RegEx _new() {
    return new RegEx();
  }

  public void assign(final RegEx value) {
    if (isValid(value)) {
      try {
        this.state = Pattern.compile(value.state.pattern(), value.state.flags());
        set();
      } catch (final PatternSyntaxException _) {
        unSet();
      }
    } else {
      unSet();
    }
  }
}