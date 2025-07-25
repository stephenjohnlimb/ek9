package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionTest extends Common {

  @Test
  void testConstruction() {
    final var unset = new Version();
    assertFalse(unset.isSet);

    final var v1 = new Version(unset);
    assertUnset.accept(v1);

    final var defaultConstructor = new Version();
    assertUnset.accept(defaultConstructor);

    final var v2 = new Version(String._of("1.2.3-9"));
    final var v3 = new Version(v2);
    assertEquals(v2, v3);

    assertSet.accept(v2);
    assertSet.accept(v2.major());
    assertSet.accept(v2.minor());
    assertSet.accept(v2.patch());
    assertSet.accept(v2.buildNumber());

    assertUnset.accept(v2.feature());

    assertEquals(1, v2.major().state);
    assertEquals(2, v2.minor().state);
    assertEquals(3, v2.patch().state);
    assertEquals(9, v2.buildNumber().state);

    final var v4 = new Version(String._of("10.21.3-feature16-19"));
    assertEquals(10, v4.major().state);
    assertEquals(21, v4.minor().state);
    assertEquals(3, v4.patch().state);
    assertEquals("feature16", v4.feature().state);
    assertEquals(19, v4.buildNumber().state);

    assertUnset.accept(unset.major());
    assertUnset.accept(unset.minor());
    assertUnset.accept(unset.patch());
    assertUnset.accept(unset.feature());
    assertUnset.accept(unset.buildNumber());

  }

  @Test
  void testIsSet() {
    final var unset = new Version();
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var v1 = Version._withNoBuildNumber("1.2.3");
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());

  }

  @Test
  void testWithNoBuildNumber() {
    final var v1 = Version._withNoBuildNumber("1.2.3");
    assertEquals(1, v1.major().state);
    assertEquals(2, v1.minor().state);
    assertEquals(3, v1.patch().state);
    assertUnset.accept(v1.feature());
    assertEquals(0, v1.buildNumber().state);

    final var v2 = Version._withNoBuildNumber("10.20.30-special_feature1");
    assertEquals(10, v2.major().state);
    assertEquals(20, v2.minor().state);
    assertEquals(30, v2.patch().state);
    assertEquals("special_feature1", v2.feature().state);
    assertEquals(0, v2.buildNumber().state);
  }

  @Test
  void testInvalidVersionNumber() {
    assertThrows(RuntimeException.class, () -> new Version(String._of("1_000.2.3-9")));
    assertThrows(RuntimeException.class, () -> new Version(String._of("1.2_000.3-9")));
    assertThrows(RuntimeException.class, () -> new Version(String._of("1.2.3_000-9")));
    assertThrows(RuntimeException.class, () -> new Version(String._of("1.2.3-9_000")));

    assertThrows(RuntimeException.class, () -> Version._withNoBuildNumber("1_000.2.3"));
    assertThrows(RuntimeException.class, () -> Version._withNoBuildNumber("1.2_000.3"));
    assertThrows(RuntimeException.class, () -> Version._withNoBuildNumber("1.2.3_000"));

  }

  @Test
  void testEquality() {

    final var unset = new Version();
    assertFalse(unset.isSet);

    final var v2 = Version._of("1.2.3-9");
    final var v3 = Version._of("1.2.3-9");

    //Eq
    assertTrue.accept(v2._eq(v3));
    assertTrue.accept(v3._eq(v2));

    assertUnset.accept(unset._eq(v3));
    assertUnset.accept(v3._eq(unset));

    assertUnset.accept(unset._eq(unset));

    //Neq
    assertFalse.accept(v2._neq(v3));
    assertFalse.accept(v3._neq(v2));

    assertUnset.accept(unset._neq(v3));
    assertUnset.accept(v3._neq(unset));

    assertUnset.accept(unset._neq(unset));

    //Lt
    assertFalse.accept(v2._lt(v3));
    assertFalse.accept(v3._lt(v2));

    assertUnset.accept(v3._lt(unset));
    assertUnset.accept(unset._lt(v3));

    assertUnset.accept(unset._lt(unset));

    //Gt
    assertFalse.accept(v2._gt(v3));
    assertFalse.accept(v3._gt(v2));

    assertUnset.accept(v3._gt(unset));
    assertUnset.accept(unset._gt(v3));

    assertUnset.accept(unset._gt(unset));

    //Lteq
    assertTrue.accept(v2._lteq(v3));
    assertTrue.accept(v3._lteq(v2));

    assertUnset.accept(v3._lteq(unset));
    assertUnset.accept(unset._lteq(v3));

    assertUnset.accept(unset._lteq(unset));

    //Gteq
    assertTrue.accept(v2._gteq(v3));
    assertTrue.accept(v3._gteq(v2));

    assertUnset.accept(v3._gteq(unset));
    assertUnset.accept(unset._gteq(v3));

    assertUnset.accept(unset._gteq(unset));

  }

  @Test
  void testCompare() {
    final var unset = new Version();
    //So the value itself is not set
    assertFalse(unset.isSet);

    //The isSet() method returns a Boolean indicating it 'is' set and with a value of false.
    assertTrue(unset._isSet().isSet);
    assertFalse(unset._isSet().state);

    final var v2 = new Version(String._of("1.2.3-9"));
    final var v3 = new Version(String._of("1.2.3-9"));

    assertEquals(0, v2._cmp(v3).state);

    //Mutate the build
    v3.incrementBuildNumber();
    assertEquals(-1, v2._cmp(v3).state);

    v2.incrementPatch();
    assertEquals(1, v2._cmp(v3).state);

    assertUnset.accept(unset._cmp(v2));
    assertUnset.accept(v2._cmp(unset));
    assertUnset.accept(v2._cmp(new Any(){}));
  }

  @Test
  void testDetailedComparisonNoFeature() {

    final var v1 = Version._of("1.0.0-0");
    final var v2 = Version._of("1.0.0-0");

    assertEquals(Integer._of(0), v1._cmp(v2));

    v2.incrementBuildNumber();
    assertEquals(Integer._of(-1), v1._cmp(v2));

    //Now just move up through patch, minor, major
    v1.incrementPatch();
    assertEquals(Integer._of(1), v1._cmp(v2));

    v1.incrementMinor();
    assertEquals(Integer._of(1), v1._cmp(v2));

    v1.incrementMajor();
    assertEquals(Integer._of(1), v1._cmp(v2));
  }

  @Test
  void testDetailedComparisonWithFeature() {

    final var v1 = Version._of("1.0.0-special_one-0");
    final var v2 = Version._of("1.0.0-special_one-0");

    assertEquals(Integer._of(0), v1._cmp(v2));

    v2._copy(Version._of("1.0.0-special_two-0"));
    //Now compare with a slightly different feature.
    assertEquals(Integer._of(-5), v1._cmp(v2));

    //Should still be minus 5 because feature is different.
    v2.incrementBuildNumber();
    assertEquals(Integer._of(-5), v1._cmp(v2));

    //Now just move up through patch, minor, major
    //So the patch, minor and major all more important than the feature name.
    v1.incrementPatch();
    assertEquals(Integer._of(1), v1._cmp(v2));

    v1.incrementMinor();
    assertEquals(Integer._of(1), v1._cmp(v2));

    v1.incrementMajor();
    assertEquals(Integer._of(1), v1._cmp(v2));

  }

  @Test
  void testComparisonMixOfFeatures() {
    //Now mix of feature parts in a version.
    final var v1 = Version._of("1.0.0-special_one-0");
    final var v2 = Version._of("1.0.0-0");

    assertEquals(Integer._of(-1), v1._cmp(v2));
    assertEquals(Integer._of(1), v2._cmp(v1));

  }


  @Test
  void testVersionMutation() {
    final var v2 = new Version(String._of("1.2.3-9"));
    assertEquals("1.2.3-9", v2._string().state);


    v2.incrementBuildNumber();
    assertEquals("1.2.3-10", v2._string().state);

    v2.incrementPatch();
    assertEquals("1.2.4-0", v2._string().state);

    v2.incrementBuildNumber();
    assertEquals("1.2.4-1", v2._string().state);

    v2.incrementMinor();
    assertEquals("1.3.0-0", v2._string().state);

    v2.incrementMajor();
    assertEquals("2.0.0-0", v2._string().state);
  }

  @Test
  void testAsStringNoFeature() {
    final var versionStr = String._of("1.2.3-9");
    final var v1 = new Version(versionStr);
    assertEquals("1.2.3-9", v1._string().state);

    final var asString = v1._promote();
    assertEquals(versionStr, asString);

    final var unset = new Version();
    assertUnset.accept(unset._string());
    assertUnset.accept(unset._promote());

    assertNotNull(v1.toString());
    assertNotNull(unset.toString());

  }

  @Test
  void testAsStringWithFeature() {
    final var versionStr = String._of("1.2.3-special_feature_1-9");
    final var v1 = new Version(versionStr);
    assertEquals("1.2.3-special_feature_1-9", v1._string().state);

    final var asString = v1._promote();
    assertEquals(versionStr, asString);

    final var unset = new Version();
    assertUnset.accept(unset._string());
    assertUnset.accept(unset._promote());

  }

  @Test
  void testCopy() {
    final var v1 = new Version(String._of("1.2.3-9"));

    final var v2 = new Version();
    v2._copy(v1);
    assertEquals(v1, v2);
  }

  @Test
  void testHashCode() {
    final var v1 = Version._of("6.8.9-0");
    //Just make a new empty Version
    final var v2 = v1._new();
    v2._copy(v1);

    assertEquals(v1._hashcode(), v2._hashcode());

    //Now mutate and check it no longer matches.
    v2.incrementMinor();
    assertNotEquals(v1._hashcode(), v2._hashcode());
  }

  @Test
  void testJsonOperator() {
    // Test unset Version returns unset JSON
    final var unset = new Version();
    assertUnset.accept(unset._json());

    // Test Version without feature
    final var v1 = Version._of("1.2.3-9");
    final var json1 = v1._json();
    assertSet.accept(json1);
    
    final var expectedJson1 = """
        {
          "major" : 1,
          "minor" : 2,
          "patch" : 3,
          "buildNumber" : 9
        }""";
    assertEquals(expectedJson1, json1.prettyPrint().state);

    // Test Version with feature
    final var v2 = Version._of("10.21.3-feature16-19");
    final var json2 = v2._json();
    assertSet.accept(json2);
    
    final var expectedJson2 = """
        {
          "major" : 10,
          "minor" : 21,
          "patch" : 3,
          "feature" : "feature16",
          "buildNumber" : 19
        }""";
    assertEquals(expectedJson2, json2.prettyPrint().state);

    // Test Version with no build number (using _withNoBuildNumber)
    final var v3 = Version._withNoBuildNumber("5.4.3-alpha");
    final var json3 = v3._json();
    assertSet.accept(json3);
    
    final var expectedJson3 = """
        {
          "major" : 5,
          "minor" : 4,
          "patch" : 3,
          "feature" : "alpha",
          "buildNumber" : 0
        }""";
    assertEquals(expectedJson3, json3.prettyPrint().state);

    // Test Version with no feature and no build number
    final var v4 = Version._withNoBuildNumber("2.1.0");
    final var json4 = v4._json();
    assertSet.accept(json4);
    
    final var expectedJson4 = """
        {
          "major" : 2,
          "minor" : 1,
          "patch" : 0,
          "buildNumber" : 0
        }""";
    assertEquals(expectedJson4, json4.prettyPrint().state);

    // Test that feature is conditionally excluded (verify no "feature" property exists)
    assertTrue(json4.objectNature().state); // Verify it's an object
    assertUnset.accept(json4.get(String._of("feature"))); // Verify no feature property
  }

}
