package org.ek9lang.compiler.main;

import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * Used to drive how the compiler operates.
 * As error message details, future will be to add debug output files for 'edb' the Ek9 Debugger.
 * We may even have things like target or flags to drive different type out output generation.
 * Maybe one day - but right now it's just how much help do you want on errors.
 * I'm expecting that when we run 'ek9' or whatever we call the compiler there will be flags
 * like '-Cg/-cg' for debug and the like.
 * These options below will also have flags like -Xhelp:key(,key)*
 * -Xhelp:+method,-type,+variable - would be a good way to enable methodSuggestionRequired,
 * disable typeFunctionConstructorSuggestionRequired and enable variableSuggestionRequired.
 * Also -Xlint:key(,key)* for different levels of warning - which we can drive from here
 * when we want to control warnings.
 * Some of these flags exist in CommandLineDetails, I may move some or all of that configuration
 * into this class. But those options are more about 'what' to do with the compiler.
 * These flags are more like - how you want the compiler to behave when issuing errors or
 * suggestions.
 */
public class CompilerFlags {

  //Normally populated via the CLI.
  private boolean verbose = false;

  private boolean debuggingInstrumentation = false;

  private boolean devBuild = false;

  private boolean checkCompilationOnly = false;

  /**
   * Does the developer want suggestions for compiler errors or not.
   */
  private boolean suggestionRequired = true;

  /**
   * How many suggestions.
   */
  private int numberOfSuggestions = 5;

  /**
   * Enable the developer to limit how far the compilation should run to.
   * Very useful when developing the compiler, so you can stop it early.
   * Also, useful for the language server. You may only want to go up to IR_ANALYSIS.
   */
  private CompilationPhase compileToPhase;

  public CompilerFlags() {
    this(CompilationPhase.APPLICATION_PACKAGING);
  }

  public CompilerFlags(CompilationPhase compileToPhase) {
    this.compileToPhase = compileToPhase;
  }

  public CompilerFlags(CompilationPhase compileToPhase, boolean verbose) {
    this.compileToPhase = compileToPhase;
    this.verbose = verbose;
  }

  public boolean isSuggestionRequired() {
    return suggestionRequired;
  }

  public void setSuggestionRequired(boolean suggestionRequired) {
    this.suggestionRequired = suggestionRequired;
  }

  public int getNumberOfSuggestions() {
    return numberOfSuggestions;
  }

  /**
   * Configure the number of suggestions for interactive help on errors.
   */
  public void setNumberOfSuggestions(int numberOfSuggestions) {
    if (numberOfSuggestions < 1) {
      setSuggestionRequired(false);
      this.numberOfSuggestions = 0;
    } else {
      this.numberOfSuggestions = numberOfSuggestions;
    }
  }

  public CompilationPhase getCompileToPhase() {
    return compileToPhase;
  }

  /**
   * Only compile to a specific phase of the overall compilation process.
   */
  public void setCompileToPhase(CompilationPhase compileToPhase) {
    this.compileToPhase = compileToPhase;
    if (compileToPhase != CompilationPhase.APPLICATION_PACKAGING) {
      this.checkCompilationOnly = true;
    }
  }

  public boolean isDebuggingInstrumentation() {
    return debuggingInstrumentation;
  }

  public void setDebuggingInstrumentation(boolean debuggingInstrumentation) {
    this.debuggingInstrumentation = debuggingInstrumentation;
  }

  public boolean isDevBuild() {
    return devBuild;
  }

  public void setDevBuild(boolean devBuild) {
    this.devBuild = devBuild;
  }

  public boolean isCheckCompilationOnly() {
    return checkCompilationOnly;
  }

  /**
   * Only run a check compilation.
   * This means run upto IR Analysis phase only.
   */
  public void setCheckCompilationOnly(boolean checkCompilationOnly) {
    this.checkCompilationOnly = checkCompilationOnly;
    if (checkCompilationOnly) {
      compileToPhase = CompilationPhase.IR_ANALYSIS;
    }
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public boolean isVerbose() {
    return verbose;
  }
}
