package org.ek9lang.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Can be a normal version number like 6.8.2-9 i.e 9 is the build number
 * Or can be for a feature like 6.1.6-specialFeature12-19 i.e. build number 19 of specialFeature12
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {
  private static final String MAJOR_R = "(?<major>\\d+)";
  private static final String MINOR_R = "(?<minor>\\d+)";
  private static final String PATCH_R = "(?<patch>\\d+)";
  private static final String FEATURE_R = "(?<feature>[a-zA-Z]+[a-zA-Z0-9]*)";

  private static final String MAIN_REGEX =
      "^" + MAJOR_R + "(\\.)" + MINOR_R + "(\\.)" + PATCH_R + "((-)" + FEATURE_R + ")?";

  private static final String BUILD_NO_REGEX = "(-)(?<buildNumber>\\d+)";
  private int major = 0;
  private int minor = 0;
  private int patch = 0;
  private String feature = null;
  private int buildNumber = 0;
  private boolean valid = false;

  private SemanticVersion() {
  }

  public SemanticVersion(String value) {
    parseWithBuildNumber(value);
  }

  /**
   * Parse a version string to produce a Semanic Version object.
   * If unparsable then an invalid semantic version will result.
   */
  public static SemanticVersion of(String value) {
    SemanticVersion rtn = new SemanticVersion();

    if (!rtn.parseWithBuildNumber(value)) {
      return new SemanticVersion();
    }

    return rtn;
  }

  /**
   * Parse a version string that also have a build number to produce a Semanic Version object.
   * If unparsable then an invalid semantic version will result.
   */
  public static SemanticVersion withNoBuildNumber(String value) {
    SemanticVersion rtn = new SemanticVersion();

    if (!rtn.parseWithoutBuildNumber(value)) {
      return new SemanticVersion();
    }

    return rtn;
  }

  public int major() {
    return major;
  }

  /**
   * Just add one to the major part of the version.
   * But resets minor, patch and build to zero.
   */
  public void incrementMajor() {
    major++;
    minor = 0;
    patch = 0;
    buildNumber = 0;
  }

  public int minor() {
    return minor;
  }

  /**
   * Just add one to the minor part of the version.
   * But resets patch and build to zero.
   */
  public void incrementMinor() {
    minor++;
    patch = 0;
    buildNumber = 0;
  }

  public int patch() {
    return patch;
  }

  /**
   * Just add one to the patch part of the version.
   * But resets build to zero.
   */
  public void incrementPatch() {
    patch++;
    buildNumber = 0;
  }

  public String feature() {
    return feature;
  }

  public int buildNumber() {
    return buildNumber;
  }

  /**
   * Just add one to the build number part of the version.
   */
  public void incrementBuildNumber() {
    buildNumber++;
  }

  private boolean parseWithBuildNumber(String value) {
    Pattern p = Pattern.compile(MAIN_REGEX + BUILD_NO_REGEX + "$");
    Matcher m = p.matcher(value);

    if (!extractMatches(m)) {
      return false;
    }

    this.buildNumber = java.lang.Integer.parseInt(m.group("buildNumber"));
    this.valid = true;
    return true;
  }

  private boolean parseWithoutBuildNumber(String value) {
    Pattern p = Pattern.compile(MAIN_REGEX + "$");
    Matcher m = p.matcher(value);
    if (!extractMatches(m)) {
      return false;
    }

    this.buildNumber = 0;
    this.valid = true;
    return true;
  }

  private boolean extractMatches(final Matcher m) {
    if (!m.find()) {
      return false;
    }

    this.major = java.lang.Integer.parseInt(m.group("major"));
    this.minor = java.lang.Integer.parseInt(m.group("minor"));
    this.patch = java.lang.Integer.parseInt(m.group("patch"));
    //might not be present
    this.feature = m.group("feature");
    return true;
  }

  @Override
  public int compareTo(SemanticVersion ver) {
    if (this.major == ver.major) {
      return compareMinor(ver);
    }
    return java.lang.Integer.compare(this.major, ver.major);
  }

  private int compareMinor(SemanticVersion ver) {
    if (this.minor == ver.minor) {
      return comparePatch(ver);
    }
    return java.lang.Integer.compare(this.minor, ver.minor);
  }

  private int comparePatch(SemanticVersion ver) {
    if (this.patch == ver.patch) {
      return compareFeatureAndBuildNumber(ver);
    }
    return java.lang.Integer.compare(this.patch, ver.patch);
  }

  private int compareFeatureAndBuildNumber(SemanticVersion ver) {
    if (feature != null && ver.feature != null) {
      return compareFeature(ver);
    } else if (feature != null) {
      //because it has a feature it is not as important as those without.
      return -1;
    } else if (ver.feature != null) {
      return 1;
    }
    return compareBuildNumber(ver);
  }

  private int compareFeature(SemanticVersion ver) {
    int featureCompare = feature.compareTo(ver.feature);
    if (featureCompare == 0) {
      return compareBuildNumber(ver);
    }
    return featureCompare;
  }

  private int compareBuildNumber(SemanticVersion ver) {
    return java.lang.Integer.compare(this.buildNumber, ver.buildNumber);
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public String toString() {
    //Format
    StringBuilder buffer = new StringBuilder();
    buffer.append(major).append(".").append(minor).append(".").append(patch);
    if (feature != null) {
      buffer.append("-").append(feature);
    }
    buffer.append("-").append(buildNumber);
    return buffer.toString();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SemanticVersion) {
      return toString().equals(obj.toString());
    }
    return false;
  }
}