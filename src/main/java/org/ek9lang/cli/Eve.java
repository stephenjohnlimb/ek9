package org.ek9lang.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.utils.ExceptionConverter;
import org.ek9lang.core.utils.Processor;

/**
 * Base for the versioning commands.
 */
public abstract class Eve extends E {
  protected Eve(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  public boolean preConditionCheck() {
    if (commandLine.noPackageIsPresent()) {
      report("File " + super.commandLine.getSourceFileName() + " does not define a package");
      return false;
    }
    return super.preConditionCheck();
  }

  /**
   * Just sets the new version into the source file.
   */
  public boolean setVersionNewNumber(Version newVersion) {

    Processor processor = () -> {
      List<String> output = loadAndUpdateVersionFromSourceFile(newVersion);
      if (!output.isEmpty()) {
        saveUpdatedSourceFile(output);
        return true;
      }
      return false;
    };
    return new ExceptionConverter().apply(processor);
  }

  private void saveUpdatedSourceFile(List<String> output) {
    //Now write it back out

    Processor processor = () -> {
      try (PrintWriter writer = new PrintWriter(commandLine.getFullPathToSourceFileName(),
          StandardCharsets.UTF_8)) {
        output.forEach(writer::println);
        return true;
      }
    };
    new ExceptionConverter().apply(processor);
  }

  private List<String> loadAndUpdateVersionFromSourceFile(Version newVersion) {
    //Now for processing of existing to get the line number.
    Integer versionLineNumber = commandLine.processEk9FileProperties(true);
    return versionLineNumber != null ? updateVersionOnLineNumber(newVersion, versionLineNumber) :
        List.of();
  }

  private List<String> updateVersionOnLineNumber(final Version newVersion,
                                                 final Integer versionLineNumber) {
    List<String> output = new ArrayList<>();

    Processor processor = () -> {
      try (BufferedReader br = new BufferedReader(
          new FileReader(commandLine.getFullPathToSourceFileName()))) {
        int lineCount = 0;
        String line;
        while ((line = br.readLine()) != null) {
          lineCount++;
          if (lineCount == versionLineNumber) {
            line = updateVersionOnLine(newVersion, line);
          }
          output.add(line);
        }
      }
      return true;
    };
    new ExceptionConverter().apply(processor);
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
   * Holder for the version.
   * Takes a version text for features or plain versions.
   * Both with or without a build number.
   * This then allows the developer to manipulate major, minor, patch and build numbers.
   */
  public static class Version {
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
    public static boolean isInvalidVersionAddressPart(String versionParam) {
      return (!versionParam.equals("major")
          && !versionParam.equals("minor")
          && !versionParam.equals("patch")
          && !versionParam.equals("build"));
    }

    /**
     * Parse the incoming - but expect no build number.
     */
    public static Version withNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + FEATURE_REGEX + "?$", value);
      return parse(false, true, m, value);
    }

    public static Version withNoFeatureNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + "$", value);
      return parse(false, false, m, value);
    }

    public static Version withFeatureNoBuildNumber(String value) {
      Matcher m = matcher("^" + MAJOR_MINOR_PATCH_REGEX + FEATURE_REGEX + "$", value);
      return parse(false, true, m, value);
    }

    /**
     * Parse the incoming - but expect a build number.
     */
    public static Version withBuildNumber(String value) {
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

    public Integer major() {
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

    public Integer minor() {
      return minor;
    }

    /**
     * Increments the minor part of the version number.
     */

    public void incrementMinor() {
      minor++;
      patch = 0;
      buildNumber = 0;
    }

    public Integer patch() {
      return patch;
    }

    /**
     * Increments the patch part of the version number.
     */

    public void incrementPatch() {
      patch++;
      buildNumber = 0;
    }

    public String feature() {
      return feature;
    }

    public Integer buildNumber() {
      return buildNumber;
    }

    /**
     * Increments the build part of the version number.
     */
    public void incrementBuildNumber() {
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