package org.ek9lang.cli;

import java.util.List;
import java.util.Map;

/**
 * An immutable snapshot of EK9 main package directives.
 * Normally created by parsing an EK9 file.
 */
public record PackageDetails(
    String moduleName,
    boolean packagePresent,
    boolean publicAccess,
    String version,
    int versionNumberOnLine,
    String description,
    List<String> tags,
    String license,
    boolean applyStandardIncludes,
    List<String> includeFiles,
    boolean applyStandardExcludes,
    Map<String, String> deps,
    Map<String, String> excludeDeps,
    Map<String, String> devDeps,
    List<String> excludeFiles,
    List<String> programs,
    String dependencyFingerPrint) {
}
