package org.ek9lang.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Processor;

/**
 * Base for the versioning commands.
 */
abstract class Eve extends E {
  Eve(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  boolean preConditionCheck() {
    if (compilationContext.commandLine().noPackageIsPresent()) {
      report("File " + compilationContext.commandLine().getSourceFileName()
          + " does not define a package");
      return false;
    }
    return super.preConditionCheck();
  }

  /**
   * Just sets the new version into the source file.
   */
  boolean setVersionNewNumber(Version newVersion) {

    Processor<Boolean> processor = () -> {
      List<String> output = loadAndUpdateVersionFromSourceFile(newVersion);
      var rtn = !output.isEmpty();
      if (rtn) {
        saveUpdatedSourceFile(output);
      }
      return rtn;
    };
    return new ExceptionConverter<Boolean>().apply(processor);
  }

  private void saveUpdatedSourceFile(List<String> output) {
    //Now write it back out

    Processor<Boolean> processor = () -> {
      try (PrintWriter writer = new PrintWriter(
          compilationContext.commandLine().getFullPathToSourceFileName(),
          StandardCharsets.UTF_8)) {
        output.forEach(writer::println);
        return true;
      }
    };
    new ExceptionConverter<Boolean>().apply(processor);
  }

  private List<String> loadAndUpdateVersionFromSourceFile(Version newVersion) {
    //Now for processing of existing to get the line number.
    Integer versionLineNumber = compilationContext.commandLine().processEk9FileProperties(true);
    return updateVersionOnLineNumber(newVersion, versionLineNumber);
  }

  private List<String> updateVersionOnLineNumber(final Version newVersion,
                                                 final Integer versionLineNumber) {
    List<String> output = new ArrayList<>();

    Processor<Boolean> processor = () -> {
      try (BufferedReader br = new BufferedReader(
          new FileReader(compilationContext.commandLine().getFullPathToSourceFileName()))) {
        int lineCount = 0;
        String line;
        while ((line = br.readLine()) != null) {
          lineCount++;
          if (Integer.valueOf(lineCount).equals(versionLineNumber)) {
            line = updateVersionOnLine(newVersion, line);
          }
          output.add(line);
        }
      }
      return true;
    };
    new ExceptionConverter<Boolean>().apply(processor);
    return output;
  }

  private String updateVersionOnLine(final Version newVersion, String line) {
    Matcher m = Pattern.compile("^(?<ver>[ a-z]+)(.*)$").matcher(line);
    if (m.find()) {
      String prefix = m.group("ver");
      if (prefix.contains(" as ")) {
        line = prefix + " Version := " + newVersion;
      } else {
        line = prefix + "<- " + newVersion;
      }
    }
    return line;
  }

  /**
   * The version.
   * Takes a version text for features or plain versions.
   * Both with or without a build number.
   * This then allows the developer to manipulate major, minor, patch and build numbers.
   */
  static class Version {
    private static final String MAJOR_MINOR_PATCH_REGEX =
        "(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)";
    private static final String FEATURE_REGEX = "((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))";
    private static final String BUILD_NO_REGEX = "(-)(?<buildNumber>\\d+)";
    private int major = 0;
    private int minor = 0;
    private int patch = 0;
    private String feature = null;
    private int buildNumber = 0;

    /**
     * True if version segment is not major, minor, patch or build.
     */
    static boolean isInvalidVersionAddressPart(String versionParam) {
      return (!versionParam.equals("major")
          && !versionParam.equals("minor")
          && !versionParam.equals("patch")
          && !versionParam.equals("build"));
    }

    /**
     * Parse the incoming - but expect no build number.
     */
    static Version withNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + FEATURE_REGEX + "?$", value);
      return parse(false, true, m, value);
    }

    static Version withNoFeatureNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + "$", value);
      return parse(false, false, m, value);
    }

    static Version withFeatureNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + FEATURE_REGEX + "$", value);
      return parse(false, true, m, value);
    }

    /**
     * Parse the incoming - but expect a build number.
     */
    static Version withBuildNumber(String value) {
      Matcher m =
          matcher("^" + MAJOR_MINOR_PATCH_REGEX + FEATURE_REGEX + "?" + BUILD_NO_REGEX + "$",
              value);
      return parse(true, true, m, value);
    }

    /**
     * Does the parsing of a version number either with or without a build number.
     */
    private static Version parse(boolean withBuildNumber, boolean withFeature, Matcher m,
                                 String value) {
      Version rtn = new Version();

      if (!m.find()) {
        throw new CompilerException("Unable to use " + value + " as a VersionNumber");
      }
      rtn.major = Integer.parseInt(m.group("major"));
      rtn.minor = Integer.parseInt(m.group("minor"));
      rtn.patch = Integer.parseInt(m.group("patch"));
      //might not be present
      if (withFeature) {
        rtn.feature = m.group("feature");
      }
      if (withBuildNumber) {
        rtn.buildNumber = Integer.parseInt(m.group("buildNumber"));
      }
      return rtn;
    }

    private static Matcher matcher(String pattern, String value) {
      return Pattern.compile(pattern).matcher(value);
    }

    Integer major() {
      return major;
    }

    /**
     * Increments the major part of the version number.
     */
    public void incrementMajor() {
      major++;
      minor = 0;
      patch = 0;
      buildNumber = 0;
    }

    Integer minor() {
      return minor;
    }

    /**
     * Increments the minor part of the version number.
     */

    void incrementMinor() {
      minor++;
      patch = 0;
      buildNumber = 0;
    }

    Integer patch() {
      return patch;
    }

    /**
     * Increments the patch part of the version number.
     */

    void incrementPatch() {
      patch++;
      buildNumber = 0;
    }

    String feature() {
      return feature;
    }

    Integer buildNumber() {
      return buildNumber;
    }

    /**
     * Increments the build part of the version number.
     */
    void incrementBuildNumber() {
      buildNumber++;
    }

    @Override
    public String toString() {
      //Format
      StringBuilder buffer = new StringBuilder();
      buffer.append(major()).append(".").append(minor()).append(".").append(patch());
      if (feature != null) {
        buffer.append("-").append(feature());
      }
      buffer.append("-").append(buildNumber());
      return buffer.toString();
    }
  }
}