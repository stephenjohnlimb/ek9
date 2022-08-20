package org.ek9lang.compiler.files;

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
 */
public class CompilerFlags {
  /**
   * Does the developer want suggestions for compiler errors or not.
   */
  private boolean suggestionRequired = true;

  /**
   * How many suggestions.
   */
  private int numberOfSuggestions = 5;

  public boolean isSuggestionRequired() {
    return suggestionRequired;
  }

  public void setSuggestionRequired(boolean suggestionRequired) {
    this.suggestionRequired = suggestionRequired;
  }

  public int getNumberOfSuggestions() {
    return numberOfSuggestions;
  }

  public void setNumberOfSuggestions(int numberOfSuggestions) {
    this.numberOfSuggestions = numberOfSuggestions;
  }
}
