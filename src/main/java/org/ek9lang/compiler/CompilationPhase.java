package org.ek9lang.compiler;

/**
 * Identifies the ordered set of compilation phases used in EK9.
 * This enables compilation to run upto specific phases.
 * So for example in a language-server - you may never want to get to
 * the 'generate' phase.
 * Or during development - we may wish to focus on work upto a specific phase.
 */
public enum CompilationPhase {

  PARSING("Source Code Parsing"),
  SYMBOL_DEFINITION("Defining Symbols"),
  REFERENCE_CHECKS("Reference Checking"),
  DUPLICATE_CHECKS("Duplicate Symbol Checking"),
  SIMPLE_RESOLUTION("Resolving Simple Types"),
  TEMPLATE_EXPANSION("Expansion of Templates"),
  FULL_RESOLUTION("Resolving all Types"),
  PLUGIN_RESOLUTION("Resolving Plugin Points"),
  SIMPLE_IR_GENERATION("Generating IR for Simple Constructs"),
  TEMPLATE_IR_GENERATION("Generating IR for Templated Types"),
  IR_ANALYSIS("Analysing Intermediate Representation"),
  FUNCTION_CODE_GENERATION("Generating Code for Functions"),
  AGGREGATE_CODE_GENERATION("Generating Code for Aggregates"),
  CONSTANT_CODE_GENERATION("Generating Code for Constants"),
  APPLICATION_PACKAGING("Application Packaging");

  private final String description;

  CompilationPhase(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
