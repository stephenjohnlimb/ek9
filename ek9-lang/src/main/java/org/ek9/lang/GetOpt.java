package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 GetOpt type for command-line argument parsing, similar to BASH getopt.
 * Provides parsing of command-line arguments based on pattern specifications.
 * Uses reference semantics - constructor takes references to arguments, no deep copying.
 * Only set if ALL constructor arguments are set.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("GetOpt")
public class GetOpt extends BuiltinType {

  // Fields stored as references (no deep copying)
  private String value;           // The value string parameter (e.g., ":" for required args)
  private _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F pattern; // Pattern dict  
  private String usage;           // Usage string for help

  @Ek9Constructor("GetOpt() as pure")
  public GetOpt() {
    // Default constructor creates unset GetOpt
    // All fields remain null, isSet remains false
  }

  @Ek9Constructor("""
      GetOpt() as pure
        ->
          value as String
          pattern as Dict of (String, String)
          usage as String""")
  public GetOpt(String value, _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F pattern,
                String usage) {
    if (isValid(value) && isValid(pattern) && isValid(usage)) {
      this.value = value;      // Take reference, no deep copy
      this.pattern = pattern;  // Take reference, no deep copy
      this.usage = usage;      // Take reference, no deep copy
      if (!pattern._empty().state) {
        set();                   // Only set if all args valid and there is a set of supported options.
      }
    }
    // If any argument invalid, object remains unset (isSet = false)
  }

  @Ek9Method("""
      options() as pure
        -> arguments as List of String
        <- rtn as Dict of (String, String)?""")
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F options(
      _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arguments) {

    if (!isSet || !isValid(arguments)) {
      final var unsetResult = new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F();
      unsetResult.unSet(); // Explicitly make it unset
      return unsetResult;
    }

    // Create result dictionary
    final var result = new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F();

    // Simple BASH getopt-style parsing
    final var argIterator = arguments.iterator();
    while (argIterator.hasNext().state) {
      final var arg = argIterator.next();
      if (!isValid(arg)) {
        continue;
      }

      final var argStr = arg._string().state;
      if (argStr.startsWith("-") && pattern._contains(String._of(argStr)).state) {
        final var optionSpec = pattern.get(String._of(argStr));

        if (isValid(optionSpec) && optionSpec._string().state.equals(value._string().state)) {
          // This option requires a parameter
          if (argIterator.hasNext().state) {
            final var paramValue = argIterator.next();
            if (isValid(paramValue)) {
              result._addAss(
                  _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of(argStr),
                      paramValue));
            }
          }
        } else {
          // Option without parameter
          result._addAss(
              _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of(argStr),
                  String._of()));
        }
      }

    }

    return result;
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (isSet) {
      // Delegate to field _string() methods as Steve specified
      final var valueStr = value._string();
      final var patternStr = pattern._string();
      final var usageStr = usage._string();
      return String._of("GetOpt(value=" + valueStr._string().state
          + ", pattern=" + patternStr._string().state
          + ", usage=" + usageStr._string().state + ")");
    }
    return String._of("GetOpt()");
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      final var jsonObject = new JSON().object(); // Create JSON object
      
      // Add "value" property
      final var valueJson = this.value._json();
      if (valueJson._isSet().state) {
        final var valueProperty = new JSON(String._of("value"), valueJson);
        jsonObject._merge(valueProperty);
      }
      
      // Add "pattern" property - handle unset case
      final var patternJson = this.pattern._json();
      if (patternJson._isSet().state) {
        final var patternProperty = new JSON(String._of("pattern"), patternJson);
        jsonObject._merge(patternProperty);
      } else {
        // If pattern JSON is unset, create empty JSON object as fallback
        final var emptyPatternJson = new JSON().object();
        final var patternProperty = new JSON(String._of("pattern"), emptyPatternJson);
        jsonObject._merge(patternProperty);
      }
      
      // Add "usage" property
      final var usageJson = this.usage._json();
      if (usageJson._isSet().state) {
        final var usageProperty = new JSON(String._of("usage"), usageJson);
        jsonObject._merge(usageProperty);
      }
      
      return jsonObject;
    }
    return new JSON();
  }

  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  @Override
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  // Factory methods for convenience
  public static GetOpt _of() {
    return new GetOpt();
  }

  public static GetOpt _of(String value, _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F pattern,
                           String usage) {
    return new GetOpt(value, pattern, usage);
  }
}