package org.ek9lang.compiler.main.phases;

/**
 * Identifies the ordered set of compilation phases used in EK9.
 * This enables compilation to run upto specific phases.
 * So for example in a language-server - you may never want to get to
 * the 'generate' phase.
 * Or during development - we may wish to focus on work upto a specific phase.
 */
public enum CompilationPhase {

  PREPARE_PARSE("Preparation for Parsing"),
  PARSING("Source Code Parsing"),
  SYMBOL_DEFINITION("Defining Symbols"),
  DUPLICATION_CHECK("Duplicate Type Checking"),
  REFERENCE_CHECKS("Reference Checking"),
  DUPLICATE_CHECKS("Duplicate Symbol Checking"),
  EXPLICIT_TYPE_SYMBOL_DEFINITION("Second Pass to Define and Resolve Non-Inferred Template Types"),
  TEMPLATE_DEFINITION_RESOLUTION("Definition and Resolution of Template Parts (S, T, etc)"),
  FURTHER_SYMBOL_DEFINITION("Definition of more Symbols based on Template Types"),
  TEMPLATE_EXPANSION("Expansion of Templates"),
  FULL_RESOLUTION("Resolving all Symbols/Types"),
  PLUGIN_RESOLUTION("Resolving Plugin Points"),
  SIMPLE_IR_GENERATION("Generating IR for Simple Constructs"),
  PROGRAM_IR_CONFIGURATION("Add IR to compilable program"),
  TEMPLATE_IR_GENERATION("Generating IR for Templated Types"),
  IR_ANALYSIS("Analysing Intermediate Representation"),
  IR_OPTIMISATION("Optimising the Intermediate Representation"),
  CODE_GENERATION_PREPARATION("Code Generation Preparation"),
  CODE_GENERATION_AGGREGATES("Generating Code for Aggregates"),
  CODE_GENERATION_CONSTANTS("Generating Code for Constants"),
  CODE_GENERATION_FUNCTIONS("Generating Code for Functions"),
  CODE_OPTIMISATION("Optimising Generated Code"),
  PLUGIN_LINKAGE("Linking Plugins"),
  APPLICATION_PACKAGING("Application Packaging"),
  PACKAGING_POST_PROCESSING("Completing Post Processing");

  private final String description;

  CompilationPhase(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
