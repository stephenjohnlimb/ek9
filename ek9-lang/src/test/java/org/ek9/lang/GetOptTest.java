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
  private final String noParam = String._of();

  // Factory methods for cleaner object creation
  private static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F dictStringString() {
    return _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F._of();
  }

  private static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 listString() {
    return _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
  }

  private static _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 dictEntryStringString(String key, String value) {
    return _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(key, value);
  }

  // Helper methods for test efficiency
  private _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 createArgsList(java.lang.String... args) {
    final var list = listString();
    for (java.lang.String arg : args) {
      list._addAss(String._of(arg));
    }
    return list;
  }

  private void assertBasicParsing(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F result, 
                                  java.lang.String option, java.lang.String expectedValue) {
    assertNotNull(result);
    assertSet.accept(result);
    assertTrue.accept(result._contains(String._of(option)));
    assertEquals(expectedValue, result.get(String._of(option))._string().state);
  }

  private void assertOptionNotPresent(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F result, 
                                      java.lang.String option) {
    assertNotNull(result);
    assertSet.accept(result);
    assertFalse.accept(result._contains(String._of(option)));
  }

  private _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F createTestPattern() {
    final var pattern = dictStringString();
    pattern._addAss(dictEntryStringString(String._of("-v"), noParam)); // Flag option
    pattern._addAss(dictEntryStringString(String._of("-f"), requiresParam)); // File option
    pattern._addAss(dictEntryStringString(String._of("-d"), requiresParam)); // Debug option
    return pattern;
  }

  private _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 createTestArguments() {
    final var args = listString();
    args._addAss(String._of("-v"));
    args._addAss(String._of("-f"));
    args._addAss(String._of("filename.txt"));
    args._addAss(String._of("-d"));
    args._addAss(String._of("2"));
    args._addAss(String._of("regular_arg"));
    return args;
  }

  @Test
  void testConstructorBehavior() {
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

    // Test constructor validation - null/unset parameters should create unset GetOpt
    assertUnset.accept(new GetOpt(null, pattern, usage));
    assertUnset.accept(new GetOpt(new String(), pattern, usage));
    assertUnset.accept(new GetOpt(requiresParam, null, usage));
    assertUnset.accept(new GetOpt(requiresParam, dictStringString(), usage));
    assertUnset.accept(new GetOpt(requiresParam, pattern, null));

    // Test factory methods
    assertUnset.accept(GetOpt._of());
    assertSet.accept(GetOpt._of(requiresParam, pattern, usage));
  }

  @Test
  void testConstructorReferenceSemantics() {
    // Verify constructor takes references, not deep copies (Steve's requirement)
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");

    final var getOpt = new GetOpt(requiresParam, pattern, usage);
    assertSet.accept(getOpt);

    // Modify the original pattern after construction
    pattern._addAss(dictEntryStringString(String._of("-x"), noParam));

    // GetOpt should reflect the change due to reference semantics
    final var args = listString();
    args._addAss(String._of("-x"));

    final var result = getOpt.options(args);
    assertNotNull(result);
    assertSet.accept(result);
    
    // Should be able to parse -x because pattern was modified by reference
    assertTrue.accept(result._contains(String._of("-x")));
    //There is an option but it does not require a parameter.
    assertUnset.accept(result.get(String._of("-x")));

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
  void testOptionsParsing() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test with unset GetOpt - should return empty but set Dict
    final var unsetGetOpt = new GetOpt();
    final var args = createTestArguments();
    final var emptyOptions = unsetGetOpt.options(args);
    assertNotNull(emptyOptions);
    assertSet.accept(emptyOptions);
    assertTrue.accept(emptyOptions._empty());

    // Test with null/empty arguments
    assertSet.accept(getOpt.options(null));
    assertTrue.accept(getOpt.options(null)._empty());
    
    final var emptyArgsResult = getOpt.options(createArgsList());
    assertSet.accept(emptyArgsResult);
    assertTrue.accept(emptyArgsResult._empty());

    // Test successful parsing with mixed options
    final var result = getOpt.options(args);
    assertBasicParsing(result, "-v", "");           // Flag option
    assertBasicParsing(result, "-f", "filename.txt"); // Parameter option
    assertBasicParsing(result, "-d", "2");            // Parameter option
    assertOptionNotPresent(result, "-x");            // Unknown option

    // Test edge cases
    assertSet.accept(getOpt.options(createArgsList("file1.txt", "file2.txt")));
    assertTrue.accept(getOpt.options(createArgsList("file1.txt", "file2.txt"))._empty());

    assertSet.accept(getOpt.options(createArgsList("-x", "-y", "value")));
    assertTrue.accept(getOpt.options(createArgsList("-x", "-y", "value"))._empty());

    // Missing parameter case
    final var missingParamResult = getOpt.options(createArgsList("-f"));
    assertSet.accept(missingParamResult);
    assertTrue.accept(missingParamResult._empty());
  }

  @Test
  void testGetOptUsageScenarioFromExample() {
    // Test scenario based on DefinedTypesExample.ek9 usage
    final var supportedOptions = dictStringString();
    supportedOptions._addAss(dictEntryStringString(String._of("-v"), String._of("")));
    supportedOptions._addAss(dictEntryStringString(String._of("-f"), String._of(":")));
    supportedOptions._addAss(dictEntryStringString(String._of("-d"), String._of(":")));

    final var usage = String._of(
        "Invalid option, only those list below are supported:\n-v, verbose\n-f filename, use file of filename (mandatory option)\n-d level, use of debugging");
    final var getOpt = new GetOpt(String._of(":"), supportedOptions, usage);

    assertSet.accept(getOpt);

    // Test with example-style arguments
    final var argv = listString();
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
    final var pattern = dictStringString();
    pattern._addAss(dictEntryStringString(String._of("--verbose"), String._of("")));
    pattern._addAss(dictEntryStringString(String._of("--file"), String._of(":")));
    pattern._addAss(dictEntryStringString(String._of("--debug"), String._of(":")));

    final var usage = String._of("Usage: program [--verbose] [--file filename] [--debug level]");
    final var getOpt = new GetOpt(String._of(":"), pattern, usage);

    final var args = listString();
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
  void testDuplicateOptions() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test duplicate flags (should appear only once)
    assertBasicParsing(getOpt.options(createArgsList("-v", "-v", "-v")), "-v", "");
    assertEquals(1, getOpt.options(createArgsList("-v", "-v", "-v"))._len().state);

    // Test duplicate parameter options (last value wins)
    assertBasicParsing(getOpt.options(createArgsList("-f", "first.txt", "-f", "final.txt")), "-f", "final.txt");

    // Test mixed duplicates
    final var mixedResult = getOpt.options(createArgsList("-v", "-d", "1", "-v", "-d", "5"));
    assertBasicParsing(mixedResult, "-v", "");
    assertBasicParsing(mixedResult, "-d", "5"); // Last value wins
  }

  @Test
  void testBoundaryConditions() {
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test option at end without required parameter
    final var endOptionResult = getOpt.options(createArgsList("-v", "-f"));
    assertBasicParsing(endOptionResult, "-v", "");
    assertOptionNotPresent(endOptionResult, "-f");

    // Test invalid option formats
    assertTrue.accept(getOpt.options(createArgsList("-"))._empty());
    assertTrue.accept(getOpt.options(createArgsList(""))._empty());
    assertTrue.accept(getOpt.options(createArgsList("-z"))._empty());

    // Test option followed by option (BASH getopt behavior)
    final var optionFollowedResult = getOpt.options(createArgsList("-f", "-v"));
    assertBasicParsing(optionFollowedResult, "-f", "-v");

    // Test multiple consecutive flags
    final var consecutiveResult = getOpt.options(createArgsList("-v", "-v", "-v"));
    assertBasicParsing(consecutiveResult, "-v", "");
    assertEquals(1, consecutiveResult._len().state);

    // Test null/unset argument handling
    final var argsWithUnset = createArgsList("-v", "-d", "1");
    argsWithUnset._addAss(new String()); // Add unset string
    final var unsetResult = getOpt.options(argsWithUnset);
    assertBasicParsing(unsetResult, "-v", "");
    assertBasicParsing(unsetResult, "-d", "1");
  }

  @Test
  void testArgumentOrderVariations() {
    final var pattern = createTestPattern();
    assertNotNull(pattern);
    final var usage = String._of("Usage: program");
    final var getOpt = new GetOpt(requiresParam, pattern, usage);

    // Test various argument orderings produce consistent results
    java.lang.String[][] testCases = {
        {"-v", "-f", "config.txt", "regular_argument"},          // Options first
        {"file1.txt", "-v", "file2.txt", "-d", "3"},            // Interspersed
        {"input.txt", "output.txt", "-v", "-d", "1"},           // Options last
        {"-v", "-f", "test.txt", "-d", "2"},                    // Order 1
        {"-d", "2", "-f", "test.txt", "-v"}                     // Order 2 (reordered)
    };

    for (java.lang.String[] testCase : testCases) {
      final var result = getOpt.options(createArgsList(testCase));
      assertSet.accept(result);
      // Each test case should parse at least some options
      assertFalse.accept(result._empty());
    }
  }

  @Test
  void testAsJson() {
    // Test unset GetOpt - should return unset JSON
    final var unsetGetOpt = new GetOpt();
    final var unsetJson = unsetGetOpt._json();
    assertUnset.accept(unsetJson);

    // Test set GetOpt with structured JSON output
    final var pattern = createTestPattern();
    final var usage = String._of("Usage: program [-v] [-f filename] [-d level]");
    final var validGetOpt = new GetOpt(requiresParam, pattern, usage);
    final var validJson = validGetOpt._json();
    
    assertSet.accept(validJson);
    assertTrue.accept(validJson.objectNature());
    assertFalse.accept(validJson.arrayNature());
    assertFalse.accept(validJson.valueNature());
    
    // Verify structured properties exist
    final var valueProperty = validJson.get(String._of("value"));
    final var patternProperty = validJson.get(String._of("pattern"));
    final var usageProperty = validJson.get(String._of("usage"));
    
    assertSet.accept(valueProperty);
    assertSet.accept(patternProperty);
    assertSet.accept(usageProperty);
    
    // Test expected JSON structure as string - pattern now shows actual flag options with tri-state semantics
    final var actualJsonString = validJson._string().state;
    
    final var expectedJsonStructure = """
        {"value":":","pattern":{"-v":null,"-f":":","-d":":"},"usage":"Usage: program [-v] [-f filename] [-d level]"}""";
    
    assertEquals(expectedJsonStructure.trim(), actualJsonString);
    
    // Test with different values - flag-only pattern
    final var flagPattern = dictStringString();
    flagPattern._addAss(dictEntryStringString(String._of("-h"), String._of("")));
    
    final var helpUsage = String._of("Usage: program [-h]");
    final var helpGetOpt = new GetOpt(String._of(""), flagPattern, helpUsage);
    final var helpJson = helpGetOpt._json();
    
    final var expectedHelpJson = """
        {"value":"","pattern":{"-h":""},"usage":"Usage: program [-h]"}""";
    
    assertEquals(expectedHelpJson.trim(), helpJson._string().state);
    
    // Test with more complex pattern to show structure clearly
    final var complexPattern = dictStringString();
    complexPattern._addAss(dictEntryStringString(String._of("-i"), requiresParam)); // input file
    complexPattern._addAss(dictEntryStringString(String._of("-o"), requiresParam)); // output file  
    complexPattern._addAss(dictEntryStringString(String._of("-v"), noParam));       // verbose flag
    complexPattern._addAss(dictEntryStringString(String._of("-q"), noParam));       // quiet flag
    
    final var complexUsage = String._of("Usage: tool [-v] [-q] [-i input] [-o output]");
    final var complexGetOpt = new GetOpt(requiresParam, complexPattern, complexUsage);
    final var complexJson = complexGetOpt._json();
    
    // This shows the full structured JSON clearly - pattern shows tri-state semantics
    final var expectedComplexJson = """
        {"value":":","pattern":{"-i":":","-o":":","-v":null,"-q":null},"usage":"Usage: tool [-v] [-q] [-i input] [-o output]"}""";
    
    assertEquals(expectedComplexJson.trim(), complexJson._string().state);
    
    // Verify individual property access works correctly
    assertTrue.accept(complexJson.get(String._of("value"))._eq(String._of(":")._json()));
    assertTrue.accept(complexJson.get(String._of("usage"))._eq(complexUsage._json()));
    
    // Verify pattern property is a proper JSON object
    final var patternJsonObj = complexJson.get(String._of("pattern"));
    assertTrue.accept(patternJsonObj.objectNature());
    assertFalse.accept(patternJsonObj.arrayNature());
    assertFalse.accept(patternJsonObj.valueNature());
  }
}