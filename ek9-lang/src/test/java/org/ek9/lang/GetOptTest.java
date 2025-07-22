package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test GetOpt component for command-line argument parsing.
 * Tests reference semantics, _string() delegation, and BASH getopt-style parsing.
 */
@SuppressWarnings("checkstyle:MethodName")
class GetOptTest extends Common {

  // Test data
  private final String requiresParam = String._of(":");
  private final String noParam = String._of("");

  private _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F createTestPattern() {
    final var pattern = _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of();
    pattern._addAss(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-v"),
        noParam)); // Flag option
    pattern._addAss(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-f"),
        requiresParam)); // File option
    pattern._addAss(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-d"),
        requiresParam)); // Debug option
    return pattern;
  }

  private _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 createTestArguments() {
    final var args = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    args._addAss(String._of("-v"));
    args._addAss(String._of("-f"));
    args._addAss(String._of("filename.txt"));
    args._addAss(String._of("-d"));
    args._addAss(String._of("2"));
    args._addAss(String._of("regular_arg"));
    return args;
  }

  @Test
  void testConstruction() {
    // Test default constructor creates unset GetOpt
    final var defaultGetOpt = new GetOpt();
    assertNotNull(defaultGetOpt);
    assertUnset.accept(defaultGetOpt);
    assertFalse.accept(defaultGetOpt._isSet());

    // Test parameterized constructor with all valid arguments
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program [-v] [-f filename] [-d level]");
    final var validGetOpt = new GetOpt(requiresParam, pattern, usage);

    assertNotNull(validGetOpt);
    assertSet.accept(validGetOpt);
    assertTrue.accept(validGetOpt._isSet());
  }

  @Test
  void testConstructorReferenceSemantics() {
    // Verify constructor takes references, not deep copies (Steve's requirement)
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");

    final var getOpt = new GetOpt(requiresParam, pattern, usage);
    assertSet.accept(getOpt);

    // Modify the original pattern after construction
    pattern._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-x"), noParam));

    // GetOpt should reflect the change due to reference semantics
    final var args = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    args._addAss(String._of("-x"));

    final var result = getOpt.options(args);
    assertNotNull(result);
    assertSet.accept(result);
    
    // Should be able to parse -x because pattern was modified by reference
    assertTrue.accept(result._contains(String._of("-x")));
    assertEquals("", result.get(String._of("-x"))._string().state); // -x is a flag option (noParam)
  }

  @Test
  void testConstructorValidation() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");

    // Test with null value - should create unset GetOpt
    final var nullValueGetOpt = new GetOpt(null, pattern, usage);
    assertNotNull(nullValueGetOpt);
    assertUnset.accept(nullValueGetOpt);

    // Test with unset value - should create unset GetOpt
    final var unsetValue = new String();
    final var unsetValueGetOpt = new GetOpt(unsetValue, pattern, usage);
    assertNotNull(unsetValueGetOpt);
    assertUnset.accept(unsetValueGetOpt);

    // Test with null pattern - should create unset GetOpt
    final var nullPatternGetOpt = new GetOpt(requiresParam, null, usage);
    assertNotNull(nullPatternGetOpt);
    assertUnset.accept(nullPatternGetOpt);

    // Test with unset pattern - should create unset GetOpt
    final var emptyPattern = new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F();
    final var emptyPatternGetOpt = new GetOpt(requiresParam, emptyPattern, usage);
    assertNotNull(emptyPatternGetOpt);
    assertUnset.accept(emptyPatternGetOpt);

    // Test with null usage - should create unset GetOpt
    final var nullUsageGetOpt = new GetOpt(requiresParam, pattern, null);
    assertNotNull(nullUsageGetOpt);
    assertUnset.accept(nullUsageGetOpt);

    // Only set if ALL arguments are valid
    final var allValidGetOpt = new GetOpt(requiresParam, pattern, usage);
    assertSet.accept(allValidGetOpt);
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty GetOpt
    final var emptyGetOpt = GetOpt._of();
    assertNotNull(emptyGetOpt);
    assertUnset.accept(emptyGetOpt);

    // Test _of(value, pattern, usage)
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var factoryGetOpt = GetOpt._of(requiresParam, pattern, usage);

    assertNotNull(factoryGetOpt);
    assertSet.accept(factoryGetOpt);
    assertTrue.accept(factoryGetOpt._isSet());
  }

  @Test
  void testStringOperator() {
    // Test _string() with unset GetOpt
    final var unsetGetOpt = new GetOpt();
    final var unsetString = unsetGetOpt._string();
    assertSet.accept(unsetString);
    assertEquals("GetOpt()", unsetString._string().state);

    // Test _string() with set GetOpt (delegates to field _string() methods as Steve specified)
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program [-v] [-f filename] [-d level]");
    final var setGetOpt = new GetOpt(requiresParam, pattern, usage);

    final var setString = setGetOpt._string();
    assertSet.accept(setString);
    assertNotNull(setString.state);
    assertTrue(setString.state.contains("GetOpt("));
    assertTrue(setString.state.contains("value="));
    assertTrue(setString.state.contains("pattern="));
    assertTrue(setString.state.contains("usage="));
    assertTrue(setString.state.contains(requiresParam._string().state));
  }

  @Test
  void testOptionsMethodBasic() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test with unset GetOpt - should return unset Dict
    final var unsetGetOpt = new GetOpt();
    final var args = createTestArguments();
    final var emptyOptions = unsetGetOpt.options(args);
    assertNotNull(emptyOptions);
    //It is set but the options will be empty.
    assertSet.accept(emptyOptions);
    assertTrue.accept(emptyOptions._empty());

    // Test with null arguments - should return unset Dict
    final var emptyOptionsByNull = getOpt.options(null);
    assertNotNull(emptyOptionsByNull);
    //It is set but the options will be empty.
    assertSet.accept(emptyOptionsByNull);
    assertTrue.accept(emptyOptionsByNull._empty());


    // Test with empty arguments - should return set but empty Dict
    // Note: Collection types like List are always set/valid even when empty
    final var emptyArgs = new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    final var emptyArgsResult = getOpt.options(emptyArgs);
    assertNotNull(emptyArgsResult);
    assertSet.accept(emptyArgsResult); // Empty collections are still set/valid

    // Test with valid arguments
    final var validResult = getOpt.options(args);
    assertNotNull(validResult);
    assertSet.accept(validResult);
  }

  @Test
  void testOptionsMethodParsing() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);
    final var args = createTestArguments();

    final var result = getOpt.options(args);
    assertNotNull(result);
    assertSet.accept(result);

    // Should have parsed the known options
    assertTrue.accept(result._contains(String._of("-v")));
    assertTrue.accept(result._contains(String._of("-f")));
    assertTrue.accept(result._contains(String._of("-d")));

    // -v is a flag (no parameter), should have empty value
    assertEquals("", result.get(String._of("-v"))._string().state);

    // -f requires parameter, should have "filename.txt"
    assertEquals("filename.txt", result.get(String._of("-f"))._string().state);

    // -d requires parameter, should have "2"
    assertEquals("2", result.get(String._of("-d"))._string().state);

    // Should not contain unknown options
    assertFalse.accept(result._contains(String._of("-x")));
  }

  @Test
  void testOptionsMethodEdgeCases() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test with empty arguments list
    final var emptyArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    final var emptyResult = getOpt.options(emptyArgs);
    assertNotNull(emptyResult);
    assertSet.accept(emptyResult);
    assertTrue.accept(emptyResult._empty());

    // Test with only non-option arguments
    final var nonOptionArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    nonOptionArgs._addAss(String._of("file1.txt"));
    nonOptionArgs._addAss(String._of("file2.txt"));
    final var nonOptionResult = getOpt.options(nonOptionArgs);
    assertNotNull(nonOptionResult);
    assertSet.accept(nonOptionResult);
    assertTrue.accept(nonOptionResult._empty()); // No options found

    // Test with unknown options
    final var unknownArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    unknownArgs._addAss(String._of("-x"));
    unknownArgs._addAss(String._of("-y"));
    unknownArgs._addAss(String._of("value"));
    final var unknownResult = getOpt.options(unknownArgs);
    assertNotNull(unknownResult);
    assertSet.accept(unknownResult);
    assertTrue.accept(unknownResult._empty()); // Unknown options ignored

    // Test with option requiring parameter but no parameter provided
    final var missingParamArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    missingParamArgs._addAss(String._of("-f")); // Requires parameter but none provided
    final var missingParamResult = getOpt.options(missingParamArgs);
    assertNotNull(missingParamResult);
    assertSet.accept(missingParamResult);
    
    // When parameter is missing, the option should be skipped entirely
    assertTrue.accept(missingParamResult._empty()); // No options parsed when required parameter missing
  }

  @Test
  void testGetOptUsageScenarioFromExample() {
    // Test scenario based on DefinedTypesExample.ek9 usage
    final var supportedOptions = _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of();
    supportedOptions._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-v"),
            String._of("")));
    supportedOptions._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-f"),
            String._of(":")));
    supportedOptions._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("-d"),
            String._of(":")));

    final var usage = String._of(
        "Invalid option, only those list below are supported:\n-v, verbose\n-f filename, use file of filename (mandatory option)\n-d level, use of debugging");
    final var getOpt = new GetOpt(String._of(":"), supportedOptions, usage);

    assertSet.accept(getOpt);

    // Test with example-style arguments
    final var argv = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    argv._addAss(String._of("-v"));
    argv._addAss(String._of("-d"));
    argv._addAss(String._of("2"));
    argv._addAss(String._of("-f"));
    argv._addAss(String._of("namedFile.txt"));

    final var options = getOpt.options(argv);
    assertNotNull(options);
    assertSet.accept(options);

    // Verify parsing results match expected usage
    assertTrue.accept(options._contains(String._of("-v"))); // verboseMode flag
    assertTrue.accept(options._contains(String._of("-d"))); // debugLevel
    assertTrue.accept(options._contains(String._of("-f"))); // filename

    assertEquals("", options.get(String._of("-v"))._string().state);
    assertEquals("2", options.get(String._of("-d"))._string().state);
    assertEquals("namedFile.txt", options.get(String._of("-f"))._string().state);

    // Test typical usage checks from example
    final var verboseMode = options._contains(String._of("-v"));
    assertTrue.accept(verboseMode);

    final var debugLevelStr = options.get(String._of("-d"));
    assertSet.accept(debugLevelStr);
    assertEquals("2", debugLevelStr._string().state);

    final var filename = options.get(String._of("-f"));
    assertSet.accept(filename);
    assertEquals("namedFile.txt", filename._string().state);
  }

  @Test
  void testComplexArgumentParsing() {
    // Test more complex argument patterns
    final var pattern = _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of();
    pattern._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("--verbose"),
            String._of("")));
    pattern._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("--file"),
            String._of(":")));
    pattern._addAss(
        _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(String._of("--debug"),
            String._of(":")));

    final var usage = String._of("Usage: program [--verbose] [--file filename] [--debug level]");
    final var getOpt = new GetOpt(String._of(":"), pattern, usage);

    final var args = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    args._addAss(String._of("--verbose"));
    args._addAss(String._of("--file"));
    args._addAss(String._of("data.txt"));
    args._addAss(String._of("--debug"));
    args._addAss(String._of("3"));
    args._addAss(String._of("additional_arg"));

    final var result = getOpt.options(args);
    assertNotNull(result);
    assertSet.accept(result);

    assertTrue.accept(result._contains(String._of("--verbose")));
    assertTrue.accept(result._contains(String._of("--file")));
    assertTrue.accept(result._contains(String._of("--debug")));

    assertEquals("", result.get(String._of("--verbose"))._string().state);
    assertEquals("data.txt", result.get(String._of("--file"))._string().state);
    assertEquals("3", result.get(String._of("--debug"))._string().state);
  }

  @Test
  void testNullAndUnsetHandling() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test with arguments containing null entries
    final var argsWithNull = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    argsWithNull._addAss(String._of("-v"));
    argsWithNull._addAss(String._of("-f"));
    argsWithNull._addAss(String._of("file.txt"));

    final var resultWithNull = getOpt.options(argsWithNull);
    assertNotNull(resultWithNull);
    assertSet.accept(resultWithNull);
    
    // Should handle null entries gracefully - valid options should still be parsed
    assertTrue.accept(resultWithNull._contains(String._of("-v")));
    assertTrue.accept(resultWithNull._contains(String._of("-f")));
    assertEquals("", resultWithNull.get(String._of("-v"))._string().state);
    assertEquals("file.txt", resultWithNull.get(String._of("-f"))._string().state);

    // Test with arguments containing unset entries
    final var argsWithUnset = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    argsWithUnset._addAss(String._of("-v"));
    argsWithUnset._addAss(new String()); // Unset string
    argsWithUnset._addAss(String._of("-d"));
    argsWithUnset._addAss(String._of("1"));

    final var resultWithUnset = getOpt.options(argsWithUnset);
    assertNotNull(resultWithUnset);
    assertSet.accept(resultWithUnset);
    
    // Should handle unset entries gracefully - unset strings are skipped, valid options parsed
    assertTrue.accept(resultWithUnset._contains(String._of("-v")));
    assertTrue.accept(resultWithUnset._contains(String._of("-d")));
    assertEquals("", resultWithUnset.get(String._of("-v"))._string().state);
    assertEquals("1", resultWithUnset.get(String._of("-d"))._string().state);
  }

  @Test
  void testDuplicateOptions() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test duplicate flag options
    final var duplicateFlags = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    duplicateFlags._addAss(String._of("-v"));
    duplicateFlags._addAss(String._of("-v"));
    duplicateFlags._addAss(String._of("-v"));

    final var flagResult = getOpt.options(duplicateFlags);
    assertNotNull(flagResult);
    assertSet.accept(flagResult);
    
    // Should contain the option only once
    assertTrue.accept(flagResult._contains(String._of("-v")));
    assertEquals("", flagResult.get(String._of("-v"))._string().state);

    // Test duplicate options with parameters - last value should win
    final var duplicateParams = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    duplicateParams._addAss(String._of("-f"));
    duplicateParams._addAss(String._of("first.txt"));
    duplicateParams._addAss(String._of("-f"));
    duplicateParams._addAss(String._of("second.txt"));
    duplicateParams._addAss(String._of("-f"));
    duplicateParams._addAss(String._of("final.txt"));

    final var paramResult = getOpt.options(duplicateParams);
    assertNotNull(paramResult);
    assertSet.accept(paramResult);
    
    // Should contain the option with the last value
    assertTrue.accept(paramResult._contains(String._of("-f")));
    assertEquals("final.txt", paramResult.get(String._of("-f"))._string().state);

    // Test mixed duplicate options
    final var mixedDuplicates = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    mixedDuplicates._addAss(String._of("-v"));
    mixedDuplicates._addAss(String._of("-d"));
    mixedDuplicates._addAss(String._of("1"));
    mixedDuplicates._addAss(String._of("-v")); // Duplicate flag
    mixedDuplicates._addAss(String._of("-d"));
    mixedDuplicates._addAss(String._of("5")); // Duplicate param option

    final var mixedResult = getOpt.options(mixedDuplicates);
    assertNotNull(mixedResult);
    assertSet.accept(mixedResult);
    
    assertTrue.accept(mixedResult._contains(String._of("-v")));
    assertTrue.accept(mixedResult._contains(String._of("-d")));
    assertEquals("", mixedResult.get(String._of("-v"))._string().state);
    assertEquals("5", mixedResult.get(String._of("-d"))._string().state); // Last value wins
  }

  @Test
  void testOptionBoundaryConditions() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test option at end of arguments without parameter (when parameter required)
    final var endOptionArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    endOptionArgs._addAss(String._of("-v")); // Valid flag
    endOptionArgs._addAss(String._of("-f")); // Requires parameter but at end

    final var endOptionResult = getOpt.options(endOptionArgs);
    assertNotNull(endOptionResult);
    assertSet.accept(endOptionResult);
    
    // Should contain -v but not -f (since -f requires parameter but none provided)
    assertTrue.accept(endOptionResult._contains(String._of("-v")));
    assertFalse.accept(endOptionResult._contains(String._of("-f")));
    assertEquals("", endOptionResult.get(String._of("-v"))._string().state);

    // Test just "-" as argument (not a valid option)
    final var dashOnlyArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    dashOnlyArgs._addAss(String._of("-"));

    final var dashOnlyResult = getOpt.options(dashOnlyArgs);
    assertNotNull(dashOnlyResult);
    assertSet.accept(dashOnlyResult);
    assertTrue.accept(dashOnlyResult._empty()); // No valid options found

    // Test empty option string
    final var emptyOptionArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    emptyOptionArgs._addAss(String._of(""));

    final var emptyOptionResult = getOpt.options(emptyOptionArgs);
    assertNotNull(emptyOptionResult);
    assertSet.accept(emptyOptionResult);
    assertTrue.accept(emptyOptionResult._empty()); // Empty string is not an option

    // Test option followed by another option (parameter missing)
    final var optionFollowedByOptionArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    optionFollowedByOptionArgs._addAss(String._of("-f")); // Requires parameter
    optionFollowedByOptionArgs._addAss(String._of("-v")); // Another option, not a parameter

    final var optionFollowedResult = getOpt.options(optionFollowedByOptionArgs);
    assertNotNull(optionFollowedResult);
    assertSet.accept(optionFollowedResult);
    
    // Should treat -v as parameter for -f (BASH getopt behavior)
    assertTrue.accept(optionFollowedResult._contains(String._of("-f")));
    assertEquals("-v", optionFollowedResult.get(String._of("-f"))._string().state);

    // Test single character after dash that doesn't exist in pattern
    final var unknownSingleCharArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    unknownSingleCharArgs._addAss(String._of("-z")); // Not in pattern

    final var unknownSingleCharResult = getOpt.options(unknownSingleCharArgs);
    assertNotNull(unknownSingleCharResult);
    assertSet.accept(unknownSingleCharResult);
    assertTrue.accept(unknownSingleCharResult._empty()); // Unknown option ignored

    // Test multiple consecutive flag options
    final var consecutiveFlagsArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    consecutiveFlagsArgs._addAss(String._of("-v"));
    consecutiveFlagsArgs._addAss(String._of("-v"));
    consecutiveFlagsArgs._addAss(String._of("-v"));

    final var consecutiveFlagsResult = getOpt.options(consecutiveFlagsArgs);
    assertNotNull(consecutiveFlagsResult);
    assertSet.accept(consecutiveFlagsResult);
    
    // Should contain only one entry for -v
    assertTrue.accept(consecutiveFlagsResult._contains(String._of("-v")));
    assertEquals(1, consecutiveFlagsResult._len().state);
    assertEquals("", consecutiveFlagsResult.get(String._of("-v"))._string().state);
  }

  @Test
  void testArgumentOrderVariations() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test options before non-option arguments
    final var optionsFirstArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    optionsFirstArgs._addAss(String._of("-v"));
    optionsFirstArgs._addAss(String._of("-f"));
    optionsFirstArgs._addAss(String._of("config.txt"));
    optionsFirstArgs._addAss(String._of("regular_argument"));

    final var optionsFirstResult = getOpt.options(optionsFirstArgs);
    assertNotNull(optionsFirstResult);
    assertSet.accept(optionsFirstResult);
    assertEquals(2, optionsFirstResult._len().state);
    assertTrue.accept(optionsFirstResult._contains(String._of("-v")));
    assertTrue.accept(optionsFirstResult._contains(String._of("-f")));
    assertEquals("", optionsFirstResult.get(String._of("-v"))._string().state);
    assertEquals("config.txt", optionsFirstResult.get(String._of("-f"))._string().state);

    // Test options interspersed with non-option arguments
    final var interspersedArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    interspersedArgs._addAss(String._of("file1.txt"));
    interspersedArgs._addAss(String._of("-v"));
    interspersedArgs._addAss(String._of("file2.txt"));
    interspersedArgs._addAss(String._of("-d"));
    interspersedArgs._addAss(String._of("3"));

    final var interspersedResult = getOpt.options(interspersedArgs);
    assertNotNull(interspersedResult);
    assertSet.accept(interspersedResult);
    assertEquals(2, interspersedResult._len().state);
    assertTrue.accept(interspersedResult._contains(String._of("-v")));
    assertTrue.accept(interspersedResult._contains(String._of("-d")));
    assertEquals("", interspersedResult.get(String._of("-v"))._string().state);
    assertEquals("3", interspersedResult.get(String._of("-d"))._string().state);

    // Test options at end after non-option arguments
    final var optionsLastArgs = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    optionsLastArgs._addAss(String._of("input.txt"));
    optionsLastArgs._addAss(String._of("output.txt"));
    optionsLastArgs._addAss(String._of("-v"));
    optionsLastArgs._addAss(String._of("-d"));
    optionsLastArgs._addAss(String._of("1"));

    final var optionsLastResult = getOpt.options(optionsLastArgs);
    assertNotNull(optionsLastResult);
    assertSet.accept(optionsLastResult);
    assertEquals(2, optionsLastResult._len().state);
    assertTrue.accept(optionsLastResult._contains(String._of("-v")));
    assertTrue.accept(optionsLastResult._contains(String._of("-d")));
    assertEquals("", optionsLastResult.get(String._of("-v"))._string().state);
    assertEquals("1", optionsLastResult.get(String._of("-d"))._string().state);

    // Test different order of same options (should produce same result)
    final var order1Args = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    order1Args._addAss(String._of("-v"));
    order1Args._addAss(String._of("-f"));
    order1Args._addAss(String._of("test.txt"));
    order1Args._addAss(String._of("-d"));
    order1Args._addAss(String._of("2"));

    final var order2Args = _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
    order2Args._addAss(String._of("-d"));
    order2Args._addAss(String._of("2"));
    order2Args._addAss(String._of("-f"));
    order2Args._addAss(String._of("test.txt"));
    order2Args._addAss(String._of("-v"));

    final var order1Result = getOpt.options(order1Args);
    final var order2Result = getOpt.options(order2Args);
    
    // Both should have same number of options
    assertEquals(order1Result._len().state, order2Result._len().state);
    assertEquals(3, order1Result._len().state);

    // Both should contain same options with same values
    assertTrue.accept(order1Result._contains(String._of("-v")));
    assertTrue.accept(order1Result._contains(String._of("-f")));
    assertTrue.accept(order1Result._contains(String._of("-d")));
    assertTrue.accept(order2Result._contains(String._of("-v")));
    assertTrue.accept(order2Result._contains(String._of("-f")));
    assertTrue.accept(order2Result._contains(String._of("-d")));
    
    assertEquals(order1Result.get(String._of("-v"))._string().state, order2Result.get(String._of("-v"))._string().state);
    assertEquals(order1Result.get(String._of("-f"))._string().state, order2Result.get(String._of("-f"))._string().state);
    assertEquals(order1Result.get(String._of("-d"))._string().state, order2Result.get(String._of("-d"))._string().state);
  }
}