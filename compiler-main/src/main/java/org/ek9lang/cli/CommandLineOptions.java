package org.ek9lang.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.ek9lang.core.Logger;

/**
 * Holds the sets of command line options that have been configured for use.
 * Provides helper methods to make the notional value of the options each to handle.
 */
public class CommandLineOptions {
  private final List<String> ek9AppParameters = new ArrayList<>();

  private final List<String> ek9ProgramParameters = new ArrayList<>();

  public List<String> getEk9AppParameters() {
    return ek9AppParameters;
  }

  public List<String> getEk9ProgramParameters() {
    return ek9ProgramParameters;
  }

  public boolean isModifierParam(String param) {

    return Set.of("-V", "-h", "-v", "-dv", "-ve", "-ls", "-lsh").contains(param);
  }

  public boolean isMainParam(final String param) {

    return Set.of("-c", "-ch", "-cg", "-cd", "-cdh", "-Cp", "-Cdp", "-C", "-Ch", "-Cg", "-Cd",
        "-Cdh", "-Cl", "-Dp", "-t", "-d", "-P", "-I", "-Gk", "-D", "-IV", "-SV", "-SF", "-PV",
        "-Up", "-O0", "-O2", "-O3").contains(param);
  }

  public boolean isParameterUnacceptable(final String param) {

    if (isModifierParam(param)) {
      return false;
    }

    final var builder = new StringBuilder("Option '").append(param);

    if (!isMainParam(param)) {
      Logger.error(builder.append("' not understood"));
      return true;
    }
    //only if we are not one of these already.
    if (isJustBuildTypeOption()) {
      Logger.error(builder.append("' not compatible with existing build option"));
      return true;
    }
    if (isDeveloperManagementOption()) {
      Logger.error(builder.append("' not compatible with existing management option"));
      return true;
    }
    if (isReleaseVectorOption()) {
      Logger.error(builder.append("' not compatible with existing release option"));
      return true;
    }
    if (isRunOption()) {
      Logger.error(builder.append("' not compatible with existing run option"));
      return true;
    }
    if (isUnitTestExecution()) {
      Logger.error(builder.append("' not compatible with existing unit test option"));
      return true;
    }

    return false;
  }

  public void appendRunOptionIfNecessary() {

    //Add in run mode if no options supplied as default.
    if (!isJustBuildTypeOption() && !isReleaseVectorOption() && !isDeveloperManagementOption()
        && !isRunDebugMode() && !isRunEk9AsLanguageServer() && !isUnitTestExecution()) {
      ek9AppParameters.add("-r");
    }

  }

  public boolean isDebuggingInstrumentation() {

    return isOptionPresentInAppParameters(
        Set.of("-cg", "-cd", "-cdh", "-Cg", "-Cd", "-Cdh", "-Cdp"));
  }

  public boolean isDevBuild() {

    return isOptionPresentInAppParameters(Set.of("-cd", "-cdh", "-Cd", "-Cdh", "-Cdp"));
  }

  public boolean isJustBuildTypeOption() {

    return isCleanAll() || isResolveDependencies() || isIncrementalCompile() || isFullCompile()
        || isPackaging() || isInstall() || isDeployment();
  }

  public boolean isReleaseVectorOption() {

    return isPrintReleaseVector() || isIncrementReleaseVector() || isSetReleaseVector()
        || isSetFeatureVector();
  }

  public boolean isDeveloperManagementOption() {

    return isGenerateSigningKeys() || isUpdateUpgrade();
  }

  public boolean isVerbose() {

    return isOptionPresentInAppParameters(Set.of("-v"));
  }

  public boolean isDebugVerbose() {

    return isOptionPresentInAppParameters(Set.of("-dv"));
  }

  public boolean isErrorVerbose() {

    return isOptionPresentInAppParameters(Set.of("-ve"));
  }

  public boolean isGenerateSigningKeys() {

    return isOptionPresentInAppParameters(Set.of("-Gk"));
  }

  public boolean isUpdateUpgrade() {

    return isOptionPresentInAppParameters(Set.of("-Up"));
  }

  public boolean isCleanAll() {

    return isOptionPresentInAppParameters(Set.of("-Cl"));
  }

  public boolean isResolveDependencies() {

    return isOptionPresentInAppParameters(Set.of("-Dp"));
  }

  public boolean isIncrementalCompile() {

    return isOptionPresentInAppParameters(Set.of("-c", "-ch", "-cg", "-cd", "-cdh"));
  }

  public boolean isFullCompile() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp", "-C", "-Ch", "-Cg", "-Cd", "-Cdh"));
  }

  public boolean isCheckCompileOnly() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp", "-ch", "-cdh", "-Ch", "-Cdh"));
  }

  public boolean isPhasedCompileOnly() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp"));
  }

  public boolean isInstall() {

    return isOptionPresentInAppParameters(Set.of("-I"));
  }

  public boolean isPackaging() {

    return isOptionPresentInAppParameters(Set.of("-P"));
  }

  public boolean isDeployment() {

    return isOptionPresentInAppParameters(Set.of("-D"));
  }

  public boolean isPrintReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-PV"));
  }

  public boolean isIncrementReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-IV"));
  }

  public boolean isSetReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-SV"));
  }

  public boolean isSetFeatureVector() {

    return isOptionPresentInAppParameters(Set.of("-SF"));
  }

  public boolean isHelp() {

    return isOptionPresentInAppParameters(Set.of("-h"));
  }

  public boolean isVersionOfEk9Option() {

    return isOptionPresentInAppParameters(Set.of("-V"));
  }

  public boolean isRunEk9AsLanguageServer() {

    return isOptionPresentInAppParameters(Set.of("-ls")) || isEk9LanguageServerHelpEnabled();
  }

  public boolean isEk9LanguageServerHelpEnabled() {

    return isOptionPresentInAppParameters(Set.of("-lsh"));
  }

  public boolean isRunOption() {

    return isRunDebugMode() || isRunNormalMode();
  }

  public boolean isUnitTestExecution() {

    return isOptionPresentInAppParameters(Set.of("-t"));
  }

  public boolean isRunDebugMode() {

    return isOptionPresentInAppParameters(Set.of("-d"));
  }

  public boolean isRunNormalMode() {

    return isOptionPresentInAppParameters(Set.of("-r"));
  }

  private boolean isOptionPresentInAppParameters(final Set<String> options) {

    return options.stream().anyMatch(ek9AppParameters::contains);
  }

  /**
   * Access a parameter option from the command line.
   */
  public String getOptionParameter(final String option) {

    String rtn = null;
    int optionIndex = ek9AppParameters.indexOf(option);
    optionIndex++;
    if (optionIndex < ek9AppParameters.size()) {
      rtn = ek9AppParameters.get(optionIndex);
    }

    return rtn;
  }

  public boolean isOptimizationLevelSpecified() {

    return isOptionPresentInAppParameters(Set.of("-O0", "-O2", "-O3"));
  }

}
