package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SemanticVersionTest {

  @Test
  void testInvalidSemanticVersionNumber() {
    assertFalse(SemanticVersion.withNoBuildNumber("1.2.A").isValid());

    assertFalse(SemanticVersion.of("1.2.A").isValid());
  }

  @Test
  void testSemanticVersionWithoutBuildNumber() {
    SemanticVersion v2 = SemanticVersion.withNoBuildNumber("1.2.3");
    assertEquals(1, v2.major());
    assertEquals(2, v2.minor());
    assertEquals(3, v2.patch());
    assertEquals(0, v2.buildNumber());
    assertNull(v2.feature());

    SemanticVersion v3 = SemanticVersion.withNoBuildNumber("10.8.13-feature29");
    assertEquals(10, v3.major());
    assertEquals(8, v3.minor());
    assertEquals(13, v3.patch());
    assertEquals(0, v3.buildNumber());
    assertEquals("feature29", v3.feature());
  }

  @Test
  void testSemanticVersionNumber() {

    SemanticVersion v1 = new SemanticVersion("1.2.3-9");

    SemanticVersion v2 = SemanticVersion.of("1.2.3-9");
    assertEquals(1, v2.major());
    assertEquals(2, v2.minor());
    assertEquals(3, v2.patch());
    assertEquals(9, v2.buildNumber());
    assertNull(v2.feature());

    assertEquals(v2, v1);
  }

  @Test
  void testFeatureSemanticVersionNumber() {
    SemanticVersion v3 = SemanticVersion.of("10.8.13-feature29-95");
    assertEquals(10, v3.major());
    assertEquals(8, v3.minor());
    assertEquals(13, v3.patch());
    assertEquals(95, v3.buildNumber());
    assertEquals("feature29", v3.feature());

    SemanticVersion v4 = new SemanticVersion("10.8.13-feature29-95");
    assertEquals(v4, v3);

    SemanticVersion v5 = new SemanticVersion("10.8.13-95");
    assertNotEquals(v4, v5);

    v3.incrementBuildNumber();
    assertEquals("10.8.13-feature29-96", v3.toString());

    v3.incrementPatch();
    assertEquals("10.8.14-feature29-0", v3.toString());

    v3.incrementBuildNumber();
    assertEquals("10.8.14-feature29-1", v3.toString());

    v3.incrementMinor();
    assertEquals("10.9.0-feature29-0", v3.toString());

    v3.incrementBuildNumber();
    assertEquals("10.9.0-feature29-1", v3.toString());

    v3.incrementMajor();
    assertEquals("11.0.0-feature29-0", v3.toString());

    assertNotEquals(null, SemanticVersion.of("1.0.0-0"));
  }

  @Test
  void testSemanticVersionNumberComparisons() {
    assertEquals(0, SemanticVersion.of("1.0.0-0").compareTo(SemanticVersion.of("1.0.0-0")));

    assertEquals(SemanticVersion.of("1.0.0-0").hashCode(),
        SemanticVersion.of("1.0.0-0").hashCode());

    assertTrue(SemanticVersion.of("1.0.0-0").compareTo(SemanticVersion.of("1.0.0-10")) < 0);

    assertTrue(SemanticVersion.of("1.0.1-2").compareTo(SemanticVersion.of("1.0.0-10")) > 0);

    assertTrue(SemanticVersion.of("1.0.21-0").compareTo(SemanticVersion.of("1.1.0-10")) < 0);

    assertTrue(SemanticVersion.of("1.0.2-2").compareTo(SemanticVersion.of("1.0.1-10")) > 0);

    assertTrue(SemanticVersion.of("4.0.21-0").compareTo(SemanticVersion.of("6.1.9-10")) < 0);

    assertTrue(SemanticVersion.of("8.5.2-2").compareTo(SemanticVersion.of("7.99.1-10")) > 0);


    //Features
    assertTrue(
        SemanticVersion.of("1.0.0-alpha1-0").compareTo(SemanticVersion.of("1.0.0-alpha3-10")) < 0);

    assertTrue(
        SemanticVersion.of("1.0.0-alpha-2").compareTo(SemanticVersion.of("1.0.0-beta-1")) < 0);

    //Because it is a feature.
    assertTrue(SemanticVersion.of("1.0.0-alpha-2").compareTo(SemanticVersion.of("1.0.0-1")) < 0);
  }

}
