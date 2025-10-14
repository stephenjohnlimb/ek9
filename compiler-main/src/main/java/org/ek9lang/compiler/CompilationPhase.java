package org.ek9lang.compiler;

/**
 * Identifies the ordered set of compilation phases used in EK9.
 * This enables compilation to run up to specific phases.
 * So for example in a language-server - you may never want to get to
 * the 'generate' phase.
 * Or during development - we may wish to focus on work up to a specific phase.
 */
public enum CompilationPhase {

  READING("Source Code Reading"),
  PARSING("Source Code Parsing"),
  SYMBOL_DEFINITION("Defining Symbols"),
  DUPLICATION_CHECK("Duplicate Type Checking"),
  REFERENCE_CHECKS("Reference Checking"),
  EXPLICIT_TYPE_SYMBOL_DEFINITION("Second Pass to Define and Resolve Non-Inferred Template Types"),
  TYPE_HIERARCHY_CHECKS("Type Hierarchy Checking"),
  FULL_RESOLUTION("Third Pass to Define and Resolve Inferred Types and Template Types"),
  POST_RESOLUTION_CHECKS("Forth Pass to Check Symbols and Template Types"),
  PRE_IR_CHECKS("Fifth Pass to Check Code Flow"),
  PLUGIN_RESOLUTION("Resolving Plugin Points"),
  IR_GENERATION("Generating IR for Constructs"),
  IR_ANALYSIS("Analysing Intermediate Representation"),
  IR_OPTIMISATION("Optimising the Intermediate Representation"),
  CODE_GENERATION_PREPARATION("Code Generation Preparation"),
  CODE_GENERATION_AGGREGATES("Generating Code for Aggregates"),
  CODE_GENERATION_CONSTANTS("Generating Code for Constants"),
  CODE_OPTIMISATION("Optimising Generated Code"),
  PLUGIN_LINKAGE("Linking Plugins"),
  APPLICATION_PACKAGING("Application Packaging"),
  PACKAGING_POST_PROCESSING("Completing Post Processing");

  private final String description;

  CompilationPhase(final String description) {

    this.description = description;

  }

  public String getDescription() {

    return description;
  }
}
