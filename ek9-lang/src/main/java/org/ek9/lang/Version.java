package org.ek9.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents and Ek9 Version 'vector'. For example.<br/>
 * 1.3.6-6<br/>
 * 2.5.9-bugfix_8888-9<br/>
 * Major.Minor.Patch{-Feature}-BuildNumber
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Version""")
public class Version extends BuiltinType {

  private int major = 0;
  private int minor = 0;
  private int patch = 0;
  private java.lang.String feature = null;
  private int buildNumber = 0;

  private static final Pattern fullPattern;
  private static final Pattern noBuilderNumberPattern;

  static {
    final var majorMinorPatchPart = "(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)";
    final var featurePart = "((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9_]*))?";
    final var buildNumberPart = "(-)(?<buildNumber>\\d+)";

    final var fullPatternStr = java.lang.String.format("^%s%s%s$", majorMinorPatchPart, featurePart, buildNumberPart);
    fullPattern = Pattern.compile(fullPatternStr);

    final var noBuilderNumberPatternStr = java.lang.String.format("^%s%s$", majorMinorPatchPart, featurePart);
    noBuilderNumberPattern = Pattern.compile(noBuilderNumberPatternStr);

  }

  @Ek9Constructor("""
      Version() as pure""")
  public Version() {
    super.unSet();
  }

  @Ek9Constructor("""
      Version() as pure
        -> arg0 as String""")
  public Version(String arg0) {
    unSet();
    if (isValid(arg0)) {
      parse(arg0.state);
    }
  }

  @Ek9Constructor("""
      Version() as pure
        -> arg0 as Version""")
  public Version(Version arg0) {
    if (isValid(arg0)) {
      assign(arg0._string().state);
    }
  }

  @Ek9Method("""
      major() as pure
        <- rtn as Integer?""")
  public Integer major() {
    if (this.isSet) {
      return Integer._of(major);
    }
    return new Integer();
  }

  @Ek9Method("""
      incrementMajor()
      """)
  public void incrementMajor() {
    major++;
    minor = 0;
    patch = 0;
    buildNumber = 0;
  }

  @Ek9Method("""
      minor() as pure
        <- rtn as Integer?""")
  public Integer minor() {
    if (this.isSet) {
      return Integer._of(minor);
    }
    return new Integer();
  }

  @Ek9Method("""
      incrementMinor()
      """)
  public void incrementMinor() {
    minor++;
    patch = 0;
    buildNumber = 0;
  }

  @Ek9Method("""
      patch() as pure
        <- rtn as Integer?""")
  public Integer patch() {
    if (this.isSet) {
      return Integer._of(patch);
    }
    return new Integer();
  }

  @Ek9Method("""
      incrementMinor()
      """)
  public void incrementPatch() {
    patch++;
    buildNumber = 0;
  }

  @Ek9Method("""
      feature() as pure
        <- rtn as String?""")
  public String feature() {
    if (this.isSet && feature != null) {
      return String._of(feature);
    }
    return new String();
  }

  @Ek9Method("""
      buildNumber() as pure
        <- rtn as Integer?""")
  public Integer buildNumber() {
    if (this.isSet) {
      return Integer._of(buildNumber);
    }
    return new Integer();
  }

  @Ek9Method("""
      incrementBuildNumber()
      """)
  public void incrementBuildNumber() {
    buildNumber++;
  }


  @Ek9Operator("""
      operator :=:
        -> arg as Version""")
  public void _copy(Version arg) {
    assign(arg);
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    return _string();
  }


  @Ek9Operator("""
      operator <=> as pure
        -> arg as Version
        <- rtn as Integer?""")
  public Integer _cmp(Version arg) {
    if (canProcess(arg)) {
      return Integer._of(compare(arg));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Version asVersion) {
      return _cmp(asVersion);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _lt(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _lteq(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _gt(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _gteq(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _eq(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Version
        <- rtn as Boolean?""")
  public Boolean _neq(Version arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      //Format
      StringBuilder buffer = new StringBuilder();
      buffer.append(major).append(".").append(minor).append(".").append(patch);
      if (feature != null) {
        buffer.append("-").append(feature);
      }
      buffer.append("-").append(buildNumber);
      return String._of(buffer.toString());
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

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  //Start of utility methods

  protected Version _new() {
    return new Version();
  }

  private void assign(Version value) {
    assign(value._string().state);
  }

  public void assign(java.lang.String theState) {
    parse(theState);
  }

  private java.lang.Integer compare(Version value) {

    if (this.major == value.major) {
      if (this.minor == value.minor) {
        if (this.patch == value.patch) {
          if (feature != null && value.feature != null) {
            int featureCompare = feature.compareTo(value.feature);
            if (featureCompare == 0) {
              return java.lang.Integer.compare(this.buildNumber, value.buildNumber);
            }
            return featureCompare;
          } else if (feature != null) {
            //because it has a feature it is not as important as those without.
            return -1;
          } else if (value.feature != null) {
            return 1;
          }
          return java.lang.Integer.compare(this.buildNumber, value.buildNumber);
        }
        return java.lang.Integer.compare(this.patch, value.patch);
      }
      return java.lang.Integer.compare(this.minor, value.minor);
    }
    return java.lang.Integer.compare(this.major, value.major);
  }

  protected void parse(java.lang.String value) {
    Matcher m = fullPattern.matcher(value);

    if (!m.find()) {
      throw new RuntimeException("Unable to use " + value + " as a VersionNumber");
    }
    this.major = java.lang.Integer.parseInt(m.group("major"));
    this.minor = java.lang.Integer.parseInt(m.group("minor"));
    this.patch = java.lang.Integer.parseInt(m.group("patch"));
    //might not be present
    this.feature = m.group("feature");
    this.buildNumber = java.lang.Integer.parseInt(m.group("buildNumber"));
    this.isSet = true;
  }

  @Override
  public int hashCode() {
    return _string().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Version version) {
      return _string().equals(version._string());
    }
    return false;
  }

  @Override
  public java.lang.String toString() {
    return _string().toString();
  }

  public static Version _of(java.lang.String value) {
    Version rtn = new Version();

    rtn.parse(value);

    return rtn;
  }

  public static Version _withNoBuildNumber(java.lang.String value) {
    Version rtn = new Version();

    Matcher m = noBuilderNumberPattern.matcher(value);

    if (!m.find()) {
      throw new RuntimeException("Unable to use " + value + " as a VersionNumber");
    }
    rtn.major = java.lang.Integer.parseInt(m.group("major"));
    rtn.minor = java.lang.Integer.parseInt(m.group("minor"));
    rtn.patch = java.lang.Integer.parseInt(m.group("patch"));
    //might not be present
    rtn.feature = m.group("feature");
    rtn.buildNumber = 0;
    rtn.isSet = true;
    return rtn;
  }

}
