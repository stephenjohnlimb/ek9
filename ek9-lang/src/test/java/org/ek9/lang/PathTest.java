package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class PathTest extends Common {

  final Path unset = new Path();
  final Path simplePath = Path._of("$?.prop");
  final Path arrayPath = Path._of("$?.arr[0]");
  final Path nestedPath = Path._of("$?.deep.nested[1].value");

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Path();
    assertUnset.accept(defaultConstructor);

    // Static factory method - valid paths
    final var pathFromString1 = Path._of("$?.simple");
    assertSet.accept(pathFromString1);
    assertEquals("$?.simple", pathFromString1.toString());

    final var pathFromString2 = Path._of("$?.arr[0]");
    assertSet.accept(pathFromString2);
    assertEquals("$?.arr[0]", pathFromString2.toString());

    final var pathFromString3 = Path._of("$?.deep.nested[1].value");
    assertSet.accept(pathFromString3);
    assertEquals("$?.deep.nested[1].value", pathFromString3.toString());

    // Static factory method - invalid paths
    final var unset1 = Path._of("invalid-path");
    assertUnset.accept(unset1);
    final var unset2 = Path._of("?.missing-dollar");
    assertUnset.accept(unset2);
    final var unset4 = Path._of("$?.");
    assertUnset.accept(unset4);
    final var unset5 = Path._of(null);
    assertUnset.accept(unset5);

    // Static factory method - now valid with enhanced regex
    final var rootPath = Path._of("$?"); // Root access is now valid
    assertSet.accept(rootPath);
    assertEquals("$?", rootPath.toString());

    // String constructor - valid paths (constructor adds $? prefix automatically)
    final var pathFromStringConstructor1 = new Path(String._of(".simple"));
    assertSet.accept(pathFromStringConstructor1);
    assertEquals("$?.simple", pathFromStringConstructor1.toString());

    final var pathFromStringConstructor2 = new Path(new String());
    assertUnset.accept(pathFromStringConstructor2);

    // Copy constructor
    final var copyValid = new Path(simplePath);
    assertSet.accept(copyValid);
    assertEquals("$?.prop", copyValid.toString());

    final var copyUnset = new Path(unset);
    assertUnset.accept(copyUnset);
  }

  @Test
  void testPathFormatValidation() {
    // Test valid path formats
    final var validPaths = List.of(
        "$?.prop",
        "$?.arr[0]",
        "$?.deep.nested[1].value",
        "$?._private",
        "$?.kebab-case",
        "$?.data[999]",
        "$?.a1.b2[3].c4",
        "$?.mixed_under-score",
        "$?.number123"
    );

    for (java.lang.String validPath : validPaths) {
      final var path = Path._of(validPath);
      assertSet.accept(path);
      assertEquals(validPath, path.toString());
    }

    // Test invalid path formats
    final var invalidPaths = List.of(
        "invalid",
        "?.missing-dollar",
        "$?.",
        "$?.prop.", // trailing dot
        "$?.prop[abc]", // non-numeric array index
        "$?.prop[0", // missing closing bracket
        "$?.prop 0]", // space in array index
        "$?.pro p", // space in property name
        "$?.prop[0.5]" // decimal array index
    );

    // Test now-valid path formats (Jayway JsonPath validation)
    final var nowValidPaths = List.of(
        "$?", // root access
        "$?.[0]", // root array access - valid JsonPath as $[0]
        "$?.123invalid", // JsonPath allows properties starting with numbers
        "$?.prop[-1]", // negative array index
        "$?..double.dot", // recursive descent then property access
        "$?.prop..double", // property access then recursive descent
        "$?.prop[", // unclosed bracket - Jayway considers this valid incomplete expression
        "$?.prop]" // no opening bracket - Jayway considers this valid  
    );

    for (java.lang.String validPath : nowValidPaths) {
      final var path = Path._of(validPath);
      assertSet.accept(path);
      assertEquals(validPath, path.toString());
    }

    for (java.lang.String invalidPath : invalidPaths) {
      final var path = Path._of(invalidPath);
      // Steve look here - one of these paths is now valid with Jayway validation
      // Test failure indicates a path in invalidPaths is actually valid JsonPath syntax
      if (path._isSet().state) {
        System.out.println("STEVE DEBUG: This 'invalid' path is now VALID: '" + invalidPath + "'");
      }
      assertUnset.accept(path);
    }
  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var setPath = Path._of("$?.test");
    assertNotNull(setPath);
    assertTrue.accept(setPath._isSet());

    // Test that constructed unset path returns false
    final var constructedUnset = new Path();
    assertFalse.accept(constructedUnset._isSet());

    // Test copy constructor preserves set/unset state
    final var copyOfSet = new Path(setPath);
    assertTrue.accept(copyOfSet._isSet());

    final var copyOfUnset = new Path(unset);
    assertFalse.accept(copyOfUnset._isSet());

    // Test that invalid paths become unset
    final var invalidPath = Path._of("invalid-path");
    assertFalse.accept(invalidPath._isSet());

    // Test String constructor
    final var stringConstructedSet = new Path(String._of(".valid"));
    assertTrue.accept(stringConstructedSet._isSet());

    final var stringConstructedUnset = new Path(new String());
    assertFalse.accept(stringConstructedUnset._isSet());
  }

  @Test
  void testEquality() {
    final var path1 = Path._of("$?.prop");
    final var path2 = Path._of("$?.prop");
    final var path3 = Path._of("$?.different");

    // Equality
    assertTrue.accept(path1._eq(path2));
    assertFalse.accept(path1._eq(path3));
    assertUnset.accept(unset._eq(path1));
    assertUnset.accept(path1._eq(unset));
    assertUnset.accept(unset._eq(unset));

    // Inequality
    assertFalse.accept(path1._neq(path2));
    assertTrue.accept(path1._neq(path3));
    assertUnset.accept(unset._neq(path1));
    assertUnset.accept(path1._neq(unset));
    assertUnset.accept(unset._neq(unset));

    // Java equals and hashCode
    assertEquals(path1, path2);
    assertNotEquals(path1, path3);
    assertEquals(path1.hashCode(), path2.hashCode());
    assertNotEquals(path1.hashCode(), path3.hashCode());
  }

  @Test
  void testComparison() {
    final var pathA = Path._of("$?.a");
    final var pathB = Path._of("$?.b");
    final var pathA2 = Path._of("$?.a");

    // Less than
    assertTrue.accept(pathA._lt(pathB));
    assertFalse.accept(pathB._lt(pathA));
    assertFalse.accept(pathA._lt(pathA2));
    assertUnset.accept(unset._lt(pathA));
    assertUnset.accept(pathA._lt(unset));

    // Less than or equal
    assertTrue.accept(pathA._lteq(pathB));
    assertTrue.accept(pathA._lteq(pathA2));
    assertFalse.accept(pathB._lteq(pathA));
    assertUnset.accept(unset._lteq(pathA));
    assertUnset.accept(pathA._lteq(unset));

    // Greater than
    assertTrue.accept(pathB._gt(pathA));
    assertFalse.accept(pathA._gt(pathB));
    assertFalse.accept(pathA._gt(pathA2));
    assertUnset.accept(unset._gt(pathA));
    assertUnset.accept(pathA._gt(unset));

    // Greater than or equal
    assertTrue.accept(pathB._gteq(pathA));
    assertTrue.accept(pathA._gteq(pathA2));
    assertFalse.accept(pathA._gteq(pathB));
    assertUnset.accept(unset._gteq(pathA));
    assertUnset.accept(pathA._gteq(unset));

    // Compare
    assertEquals(-1, (int) pathA._cmp(pathB).state);
    assertEquals(0, (int) pathA._cmp(pathA2).state);
    assertEquals(1, (int) pathB._cmp(pathA).state);
    assertUnset.accept(unset._cmp(pathA));
    assertUnset.accept(pathA._cmp(unset));
    assertUnset.accept(pathA._cmp(new Any(){}));

    // Fuzzy comparison
    final var fuzzyResult = pathA._fuzzy(pathA2);
    assertEquals(0, fuzzyResult.state); // identical strings
    assertSet.accept(fuzzyResult);
    assertUnset.accept(unset._fuzzy(pathA));
    assertUnset.accept(pathA._fuzzy(unset));
  }

  @Test
  void testConcatenation() {
    final var basePath = Path._of("$?.base");
    final var strSuffix = String._of(".append");
    final var charSuffix = Character._of('X');

    // Addition with String (this should work since we're adding to the internal state)
    final var addStringResult = basePath._add(strSuffix);
    assertSet.accept(addStringResult);
    assertEquals("$?.base.append", addStringResult.toString());
    assertUnset.accept(unset._add(strSuffix));
    assertUnset.accept(basePath._add(new String()));

    // Addition with Character
    final var addCharResult = basePath._add(charSuffix);
    assertSet.accept(addCharResult);
    assertEquals("$?.baseX", addCharResult.toString());
    assertUnset.accept(unset._add(charSuffix));
    assertUnset.accept(basePath._add(new Character()));

    // Addition with Path - we need another valid path to add
    final var anotherPath = Path._of("$?.other");
    final var addPathResult = basePath._add(anotherPath);
    assertSet.accept(addPathResult);
    assertEquals("$?.base.other", addPathResult.toString()); // This concatenates the internal states with dots
    assertUnset.accept(unset._add(anotherPath));
    assertUnset.accept(basePath._add(unset));

    // Addition assignment with Path - using another valid path
    final var path1 = Path._of("$?.start");
    final var endPath = Path._of("$?.end");
    path1._addAss(endPath);
    assertEquals("$?.start.end", path1.toString()); // Concatenates internal states with dots

    // Addition assignment with String
    final var path2 = Path._of("$?.start");
    path2._addAss(String._of("[0]"));
    assertEquals("$?.start[0]", path2.toString());

    // Addition assignment with Character
    final var path3 = Path._of("$?.start");
    path3._addAss(Character._of('_'));
    assertEquals("$?.start_", path3.toString());

    // Test unset propagation
    final var pathUnset = new Path();
    pathUnset._addAss(endPath);
    assertUnset.accept(pathUnset);
  }

  @Test
  void testUtilityMethods() {
    final var testPath = Path._of("$?.data.items[5].name");

    // Contains with String
    assertTrue.accept(testPath._contains(String._of("data")));
    assertTrue.accept(testPath._contains(String._of("items")));
    assertTrue.accept(testPath._contains(String._of("[5]")));
    assertFalse.accept(testPath._contains(String._of("notfound")));
    assertUnset.accept(unset._contains(String._of("test")));
    assertUnset.accept(testPath._contains(new String()));

    // Contains with Path
    assertTrue.accept(testPath._contains(Path._of("$?.data")));
    assertFalse.accept(testPath._contains(Path._of("$?.other")));
    assertUnset.accept(unset._contains(Path._of("$?.test")));
    assertUnset.accept(testPath._contains(unset));

    // Empty check
    assertFalse.accept(testPath._empty());
    assertUnset.accept(new Path()._empty());
    assertUnset.accept(unset._empty());

    // Length
    assertEquals(21, testPath._len().state); // "$?.data.items[5].name" = 21 characters
    assertEquals(0, unset._len().state);

    // String conversion
    assertEquals("$?.data.items[5].name", testPath._string().state);
    assertUnset.accept(unset._string());
    assertEquals("$?.data.items[5].name", testPath.toString());
    assertEquals("", unset.toString());

    // Hash code
    assertSet.accept(testPath._hashcode());
    assertUnset.accept(unset._hashcode());
    final var testPath2 = Path._of("$?.data.items[5].name");
    assertEquals(testPath._hashcode().state, testPath2._hashcode().state);
  }

  @Test
  void testCopyAndAssignmentOperators() {
    final var path1 = Path._of("$?.first");
    final var path2 = Path._of("$?.second");

    // Copy operation
    path1._copy(path2);
    assertEquals("$?.second", path1.toString());

    // Copy unset
    final var pathSet = Path._of("$?.test");
    pathSet._copy(unset);
    assertUnset.accept(pathSet);

    // Replace operation
    final var path3 = Path._of("$?.original");
    final var path4 = Path._of("$?.replacement");
    path3._replace(path4);
    assertEquals("$?.replacement", path3.toString());

    // Merge operation - target is set
    final var path5 = Path._of("$?.base");
    final var path6 = Path._of("$?.addition");
    path5._merge(path6);
    assertEquals("$?.base.addition", path5.toString()); // Concatenates internal states with dots

    // Merge operation - target is unset
    final var pathUnset = new Path();
    final var path7 = Path._of("$?.newvalue");
    pathUnset._merge(path7);
    assertEquals("$?.newvalue", pathUnset.toString());

    // Pipe operation with Path
    final var path8 = Path._of("$?.start");
    final var path9 = Path._of("$?.pipe");
    path8._pipe(path9);
    assertEquals("$?.start.pipe", path8.toString()); // Concatenates internal states with dots

    // Pipe operation with String - target is set
    final var path10 = Path._of("$?.base");
    path10._pipe(String._of(".string"));
    assertEquals("$?.base.string", path10.toString());

    // Pipe operation with String - target is unset
    final var pathUnset2 = new Path();
    pathUnset2._pipe(String._of("$?.fromstring"));
    assertEquals("$?.fromstring", pathUnset2.toString());
  }

  @Test
  void testEdgeCases() {
    // Null string handling
    final var nullPath = new Path((String) null);
    assertUnset.accept(nullPath);

    // Very long path
    final var longPathString =
        "$?.very.long.path.with.many.segments[0].and[1].arrays[2].deeply[3].nested[4].structures";
    final var longPath = Path._of(longPathString);
    assertSet.accept(longPath);
    assertEquals(longPathString, longPath.toString());

    // Large array indices
    final var largeIndexPath = Path._of("$?.arr[999999]");
    assertSet.accept(largeIndexPath);
    assertEquals("$?.arr[999999]", largeIndexPath.toString());

    // Minimum valid path
    final var minPath = Path._of("$?.a");
    assertSet.accept(minPath);
    assertEquals("$?.a", minPath.toString());

    // Path with all valid characters
    final var allCharsPath = Path._of("$?.aB_c1-d2[123].eF_g4-h5");
    assertSet.accept(allCharsPath);
    assertEquals("$?.aB_c1-d2[123].eF_g4-h5", allCharsPath.toString());

    // Edge case: property name starting with underscore
    final var underscoreStartPath = Path._of("$?._validName");
    assertSet.accept(underscoreStartPath);
    assertEquals("$?._validName", underscoreStartPath.toString());

    // Test with unset arguments in operations
    final var validPath = Path._of("$?.test");

    // Concatenation with unset should return unset
    assertUnset.accept(validPath._add(unset));
    assertUnset.accept(validPath._add(new String()));
    assertUnset.accept(validPath._add(new Character()));

    // Contains with unset should return unset
    assertUnset.accept(validPath._contains(unset));
    assertUnset.accept(validPath._contains(new String()));
  }

  @Test
  void testStringRepresentation() {
    // Test that toString includes the $? prefix
    final var testPath = Path._of("$?.test.path");
    assertEquals("$?.test.path", testPath.toString());

    // Test that internal state doesn't include $?
    // We can't directly access the state, but we can test through concatenation
    final var basePath = Path._of("$?.base");
    final var result = basePath._add(String._of(".suffix"));
    assertEquals("$?.base.suffix", result.toString());

    // Test unset string representation
    assertEquals("", unset.toString());

    // Test _string() method
    assertEquals("$?.test.path", testPath._string().state);
    assertUnset.accept(unset._string());

    // Test that different paths have different string representations
    final var path1 = Path._of("$?.path1");
    final var path2 = Path._of("$?.path2");
    assertNotEquals(path1.toString(), path2.toString());
    assertNotEquals(path1._string().state, path2._string().state);
  }

  @Test
  void testPipeLogic() {
    var mutatedValue = new Path();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(simplePath);
    assertEquals(simplePath, mutatedValue);

    // Keep adding - using a valid path
    mutatedValue._pipe(Path._of("$?.additional"));
    assertEquals(Path._of("$?.prop.additional"), mutatedValue); // Concatenates internal states with dots

    // Even if we pipe in something unset, for pipes this is ignored
    mutatedValue._pipe(unset);
    assertEquals(Path._of("$?.prop.additional"), mutatedValue);

    // Test pipe with String
    mutatedValue._pipe(String._of("[0]"));
    assertEquals(Path._of("$?.prop.additional[0]"), mutatedValue);
  }

  @Test
  void testMutationOperatorsWithUnsetPath() {
    final var mutatedValue = Path._of("$?.test");
    assertNotNull(mutatedValue);
    mutatedValue._addAss(unset);
    assertUnset.accept(mutatedValue);
  }

  @Test
  void testMutationOperatorsWithUnsetString() {
    final var mutatedValue = Path._of("$?.test");
    assertNotNull(mutatedValue);
    mutatedValue._addAss(new String());
    assertUnset.accept(mutatedValue);
  }

  @Test
  void testMutationOperatorsWithUnsetCharacter() {
    final var mutatedValue = Path._of("$?.test");
    assertNotNull(mutatedValue);
    mutatedValue._addAss(new Character());
    assertUnset.accept(mutatedValue);
  }

  @Test
  void testMutationOperators() {
    final var mutatedValue = Path._of("$?.base");

    mutatedValue._addAss(String._of(".property"));
    assertEquals(Path._of("$?.base.property"), mutatedValue);

    mutatedValue._addAss(Path._of("$?.suffix"));
    assertEquals(Path._of("$?.base.property.suffix"), mutatedValue); // Concatenates internal states with dots

    mutatedValue._addAss(Character._of('A'));
    assertEquals(Path._of("$?.base.property.suffixA"), mutatedValue);
  }

  @Test
  void testReplaceAndCopyLogic() {
    var mutatedValue = Path._of("$?.original");
    assertEquals(Path._of("$?.original"), mutatedValue);

    mutatedValue._replace(simplePath);
    assertEquals(simplePath, mutatedValue);

    mutatedValue._replace(arrayPath);
    assertEquals(arrayPath, mutatedValue);

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    // Now just check that it can take a value after being unset
    mutatedValue._replace(nestedPath);
    assertEquals(nestedPath, mutatedValue);
  }

  // ===== COMPREHENSIVE JSONPATH FEATURE TESTS =====
  // These tests will expose defects in the current regex pattern

  @Test
  void testRootPathAccess() {
    // Test root path access - should be valid but current regex requires '+'
    final var rootPath = Path._of("$?");
    // This should be valid for JSON root access but current regex will fail
    assertSet.accept(rootPath); // This will likely fail due to regex limitation
    assertEquals("$?", rootPath.toString());
  }

  @Test
  void testArrayWildcardAndSlicing() {
    // Test array wildcard patterns - not supported by current regex
    final var wildcardPath = Path._of("$?.array[*]");
    assertSet.accept(wildcardPath); // Should work but current regex doesn't support [*]
    assertEquals("$?.array[*]", wildcardPath.toString());

    // Test array slicing - not supported by current regex  
    final var slicePath1 = Path._of("$?.array[0:5]");
    assertSet.accept(slicePath1); // Should work but current regex doesn't support [0:5]
    assertEquals("$?.array[0:5]", slicePath1.toString());

    final var slicePath2 = Path._of("$?.array[1:]");
    assertSet.accept(slicePath2); // Should work but current regex doesn't support [1:]
    assertEquals("$?.array[1:]", slicePath2.toString());

    final var slicePath3 = Path._of("$?.array[:3]");
    assertSet.accept(slicePath3); // Should work but current regex doesn't support [:3]
    assertEquals("$?.array[:3]", slicePath3.toString());
  }

  @Test
  void testNegativeArrayIndexing() {
    // Test negative array indexing - current regex only supports \\d+ (positive digits)
    final var negativeIndexPath = Path._of("$?.array[-1]");
    assertSet.accept(negativeIndexPath); // Should work but current regex doesn't support negative indices
    assertEquals("$?.array[-1]", negativeIndexPath.toString());

    final var negativeIndexPath2 = Path._of("$?.items[-5]");
    assertSet.accept(negativeIndexPath2);
    assertEquals("$?.items[-5]", negativeIndexPath2.toString());
  }

  @Test
  void testRecursiveDescentQueries() {
    // Test recursive descent (..) - not supported by current regex
    final var recursivePath1 = Path._of("$?..name");
    assertSet.accept(recursivePath1); // Should work but current regex doesn't support '..'
    assertEquals("$?..name", recursivePath1.toString());

    final var recursivePath2 = Path._of("$?.store..price");
    assertSet.accept(recursivePath2);
    assertEquals("$?.store..price", recursivePath2.toString());

    final var recursivePath3 = Path._of("$?..items[0]");
    assertSet.accept(recursivePath3);
    assertEquals("$?..items[0]", recursivePath3.toString());
  }

  @Test
  void testWildcardPropertyAccess() {
    // Test wildcard property access - not supported by current regex
    final var wildcardProp = Path._of("$?.*");
    assertSet.accept(wildcardProp); // Should work but current regex doesn't support '*' properties
    assertEquals("$?.*", wildcardProp.toString());

    final var nestedWildcard = Path._of("$?.data.*.value");
    assertSet.accept(nestedWildcard);
    assertEquals("$?.data.*.value", nestedWildcard.toString());

    final var arrayWildcard = Path._of("$?.users[*].name");
    assertSet.accept(arrayWildcard);
    assertEquals("$?.users[*].name", arrayWildcard.toString());
  }

  @Test
  void testFilterExpressions() {
    // Test JSONPath filter expressions - not supported by current regex
    final var filterPath1 = Path._of("$?.items[?(@.price > 10)]");
    assertSet.accept(filterPath1); // Should work but current regex doesn't support filter expressions
    assertEquals("$?.items[?(@.price > 10)]", filterPath1.toString());

    final var filterPath2 = Path._of("$?.books[?(@.author == 'Jane')]");
    assertSet.accept(filterPath2);
    assertEquals("$?.books[?(@.author == 'Jane')]", filterPath2.toString());

    final var filterPath3 = Path._of("$?.products[?(@.category in ['electronics', 'books'])]");
    assertSet.accept(filterPath3);
    assertEquals("$?.products[?(@.category in ['electronics', 'books'])]", filterPath3.toString());
  }

  @Test
  void testComplexJsonPathQueries() {
    // Test complex real-world JSONPath queries that should be valid
    final var complexPath1 = Path._of("$?.store.book[*].author");
    assertSet.accept(complexPath1);
    assertEquals("$?.store.book[*].author", complexPath1.toString());

    final var complexPath2 = Path._of("$?..book[2].title");
    assertSet.accept(complexPath2);
    assertEquals("$?..book[2].title", complexPath2.toString());

    final var complexPath3 = Path._of("$?.data[0:3].items[*].name");
    assertSet.accept(complexPath3);
    assertEquals("$?.data[0:3].items[*].name", complexPath3.toString());

    final var complexPath4 = Path._of("$?.users[?(@.age > 18)].profile.email");
    assertSet.accept(complexPath4);
    assertEquals("$?.users[?(@.age > 18)].profile.email", complexPath4.toString());
  }

  @Test
  void testSpecialCharactersInPaths() {
    // Test paths with special characters that JSONPath supports
    final var quotedProperty = Path._of("$?.['special-key']");
    assertSet.accept(quotedProperty);
    assertEquals("$?.['special-key']", quotedProperty.toString());

    final var spacedProperty = Path._of("$?.['key with spaces']");
    assertSet.accept(spacedProperty);
    assertEquals("$?.['key with spaces']", spacedProperty.toString());

    final var numberStartProperty = Path._of("$?.['2023-data']");
    assertSet.accept(numberStartProperty);
    assertEquals("$?.['2023-data']", numberStartProperty.toString());
  }

  @Test
  void testPathBoundaryConditions() {
    // Test various boundary conditions for JSONPath validity
    
    // Empty bracket should be invalid
    final var emptyBracket = Path._of("$?.array[]");
    assertUnset.accept(emptyBracket);

    // Multiple consecutive dots should be for recursive descent only
    final var multipleDots = Path._of("$?.a...b");
    assertUnset.accept(multipleDots); // Should be invalid

    // Mixed valid complex patterns
    final var mixedPattern1 = Path._of("$?.store..book[0:5].author");
    assertSet.accept(mixedPattern1);
    assertEquals("$?.store..book[0:5].author", mixedPattern1.toString());

    final var mixedPattern2 = Path._of("$?.data[*].items[?(@.active == true)].name");
    assertSet.accept(mixedPattern2);
    assertEquals("$?.data[*].items[?(@.active == true)].name", mixedPattern2.toString());
  }
}