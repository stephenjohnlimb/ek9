package org.ek9lang.compiler.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.RuleContext;
import org.ek9lang.antlr.EK9BaseVisitor;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.ModuleDeclarationContext;
import org.ek9lang.antlr.EK9Parser.ProgramBlockContext;
import org.ek9lang.compiler.common.ErrorListener.SemanticClassification;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.Digest;

/**
 * A cut down visitor that just deals with packages at a basic level.
 * Now refactored the results of the visit out to an immutable record,
 * so the private properties in this visitor are now transient.
 */
public class Ek9SourceVisitor extends EK9BaseVisitor<Void> {
  private String moduleName;

  //All parts of the package if one is defined
  private boolean packagePresent;
  private boolean foundVersion;
  private boolean foundDescription;
  //Only version is actually mandatory
  private boolean publicAccess;
  private String version;
  private int versionNumberOnLine;
  private String description;
  private String license;
  private List<String> tags;
  private Map<String, String> deps;
  private Map<String, String> devDeps;
  private Map<String, String> excludeDeps;
  private boolean applyStandardIncludes;
  private List<String> includeFiles;
  private boolean applyStandardExcludes;
  private List<String> excludeFiles;
  //Now any programs that might be in the file.
  private List<String> programs;

  private ErrorListener errorListener;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<PackageDetails> packageDetails;

  /**
   * This is the key to lexing and parsing the source file mainly just for package information.
   */
  public void visit(final EK9Parser.CompilationUnitContext context, final ErrorListener errorListener) {

    //The setup
    initialiseTransientProperties();
    setErrorListener(errorListener);

    //Trigger the processing
    visitCompilationUnit(context);

    //Gather the results
    packageDetails = Optional.of(new PackageDetails(moduleName,
        packagePresent,
        publicAccess,
        version,
        versionNumberOnLine,
        description,
        tags,
        license,
        applyStandardIncludes,
        includeFiles,
        applyStandardExcludes,
        deps,
        excludeDeps,
        devDeps,
        excludeFiles,
        programs,
        getDependencyFingerPrint()));

  }

  private void initialiseTransientProperties() {

    packagePresent = false;
    foundVersion = false;
    foundDescription = false;
    publicAccess = false;
    version = "";
    versionNumberOnLine = 0;
    description = "";
    license = "";
    tags = new ArrayList<>();
    deps = new HashMap<>();
    devDeps = new HashMap<>();
    excludeDeps = new HashMap<>();
    applyStandardIncludes = false; //We expect do just do single source.
    includeFiles = new ArrayList<>();
    applyStandardExcludes = true;
    excludeFiles = new ArrayList<>();
    //Now any programs that might be in the file.
    programs = new ArrayList<>();

    //Reset the results.
    packageDetails = Optional.empty();

  }

  public Optional<PackageDetails> getPackageDetails() {

    return packageDetails;

  }

  @Override
  public Void visitModuleDeclaration(final ModuleDeclarationContext ctx) {

    moduleName = ctx.dottedName().getText();
    return super.visitModuleDeclaration(ctx);

  }

  @Override
  public Void visitProgramBlock(final ProgramBlockContext ctx) {

    ctx.methodDeclaration().forEach(
        methodDeclaration -> programs.add(methodDeclaration.identifier().getText().trim()));
    return super.visitProgramBlock(ctx);

  }

  @Override
  public Void visitPackageBlock(final EK9Parser.PackageBlockContext ctx) {

    //Initialise the state variables.
    packagePresent = true;
    foundVersion = false;
    foundDescription = false;

    //These are the processors that deal with gathering the assignments.
    final var assignments = getAssignmentProcessors();

    //Need to manually traverse down, so we only get version in this context
    ctx.variableDeclaration().forEach(
        vd -> assignments.getOrDefault(vd.identifier().getText(), this::processUnknownProperty)
            .accept(vd.assignmentExpression()));

    //Now we must assert that the description is present as that is mandatory.
    if (!foundDescription) {
      errorListener.semanticError(new Ek9Token(ctx.start),
          "Package: property 'description' is mandatory if a package is declared.",
          SemanticClassification.NOT_RESOLVED);
    }

    return super.visitPackageBlock(ctx);

  }

  /**
   * We need to know if the developer alters any of the dependency
   * configuration, deps, devDeps and excludeDeps.
   * So hash with all the contents; and then it can be stored in the properties file.
   * If it changes then we need to trigger a full recompile.
   *
   * @return The fingerprint of the dependencies.
   */
  private String getDependencyFingerPrint() {

    final var all = deps.toString() + devDeps.toString() + excludeDeps.toString();

    return Digest.digest(all).toString();

  }

  private void setErrorListener(final ErrorListener errorListener) {

    this.errorListener = errorListener;

  }

  private Map<String, Consumer<EK9Parser.AssignmentExpressionContext>> getAssignmentProcessors() {

    //Need to do this in two phases because there is no method signature in Map.of for all values.
    final HashMap<String, Consumer<EK9Parser.AssignmentExpressionContext>> rtn = new HashMap<>();

    rtn.putAll(Map.of(
        "publicAccess", this::processPublicAccess,
        "version", this::processVersion,
        "description", this::processDescription,
        "license", this::processLicense,
        "tags", this::processTags,
        "deps", this::processDeps,
        "devDeps", this::processDevDeps,
        "excludeDeps", this::processExcludeDeps,
        "applyStandardIncludes", this::processApplyStandardIncludes
    ));

    rtn.putAll(Map.of(
        "includeFiles", this::processIncludeFiles,
        "applyStandardExcludes", this::processApplyStandardExcludes,
        "excludeFiles", this::processExcludeFiles
    ));

    return rtn;

  }

  private void processUnknownProperty(final EK9Parser.AssignmentExpressionContext ive) {

    final var identifier = getTextFromRuleContext(ive.parent);
    errorListener.semanticError(ive.expression().start,
        "Package: unusable '" + identifier + "' property.",
        SemanticClassification.PARAMETER_MISMATCH);

  }

  private void processPublicAccess(final EK9Parser.AssignmentExpressionContext ive) {

    if (isValidPrimaryAssignmentContext(ive)) {
      publicAccess = Boolean.parseBoolean(getTextFromRuleContext(ive.expression()));
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'publicAccess' property must be a Boolean.",
          SemanticClassification.PARAMETER_MISMATCH);
    }
  }

  private void processVersion(final EK9Parser.AssignmentExpressionContext ive) {

    if (foundVersion) {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'version'.", SemanticClassification.DUPLICATE_VARIABLE);
    }
    if (isValidPrimaryAssignmentContext(ive)) {
      version = getTextFromRuleContext(ive.expression());
      versionNumberOnLine = ive.expression().getStart().getLine();
      foundVersion = true;
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'version' property must be a VersionNumber type.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processDescription(final EK9Parser.AssignmentExpressionContext ive) {

    if (isValidPrimaryAssignmentContext(ive)) {
      description = getTextFromRuleContext(ive.expression()).replace("\"", "");
      foundDescription = true;
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'description' property must be a String type.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processLicense(final EK9Parser.AssignmentExpressionContext ive) {

    if (isValidPrimaryAssignmentContext(ive)) {
      license = getTextFromRuleContext(ive.expression()).replace("\"", "");
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'license' property must be a String type.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processExcludeFiles(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidListAssignmentContext(ive)) {
      excludeFiles = extractListFromArray(ive.expression().list());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'excludeFiles' property must be List of String.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processApplyStandardExcludes(final EK9Parser.AssignmentExpressionContext ive) {

    if (isValidPrimaryAssignmentContext(ive)) {
      applyStandardExcludes = Boolean.parseBoolean(getTextFromRuleContext(ive.expression()));
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'applyStandardExcludes' property must be a Boolean.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processIncludeFiles(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidListAssignmentContext(ive)) {
      includeFiles = extractListFromArray(ive.expression().list());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'includeFiles' property must be List of String.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processApplyStandardIncludes(final EK9Parser.AssignmentExpressionContext ive) {

    if (isValidPrimaryAssignmentContext(ive)) {
      applyStandardIncludes = Boolean.parseBoolean(getTextFromRuleContext(ive.expression()));
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'applyStandardIncludes' property must be a Boolean.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processExcludeDeps(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidDictAssignmentContext(ive)) {
      excludeDeps = extractMapFromDict(ive.expression().dict());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'excludeDeps' property must be Dict of (String, String).",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processDevDeps(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidDictAssignmentContext(ive)) {
      devDeps = extractMapFromDict(ive.expression().dict());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In " + moduleName + " package: 'devDeps' property must be Dict of (String, String).",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processDeps(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidDictAssignmentContext(ive)) {
      deps = extractMapFromDict(ive.expression().dict());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'deps' property must be Dict of (String, String).",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private void processTags(final EK9Parser.AssignmentExpressionContext ive) {

    //We cannot assume this because user might have not used correct type
    if (isValidListAssignmentContext(ive)) {
      tags = extractListFromArray(ive.expression().list());
    } else {
      errorListener.semanticError(ive.expression().start,
          "In '" + moduleName + "' package: 'tags' property must be a List of String type.",
          SemanticClassification.PARAMETER_MISMATCH);
    }

  }

  private List<String> extractListFromArray(final EK9Parser.ListContext ctx) {

    return ctx
        .expression()
        .stream()
        .map(this::getTextFromRuleContext)
        .map(value -> value.replace("\"", ""))
        .toList();
  }

  private Map<String, String> extractMapFromDict(final EK9Parser.DictContext ctx) {

    //A bit of a contortion to use a stream here with Collectors.toMap
    return ctx
        .initValuePair()
        .stream()
        .map(EK9Parser.InitValuePairContext::expression)
        .collect(Collectors.toMap(expr -> getUnQuotedTextFromRuleContext(expr.get(0)),
            expr -> getUnQuotedTextFromRuleContext(expr.get(1)),
            (o1, o2) -> o1,
            HashMap::new
        ));
  }

  private boolean isValidPrimaryAssignmentContext(final EK9Parser.AssignmentExpressionContext ive) {

    return ive.expression() != null && ive.expression().primary() != null;

  }

  private boolean isValidListAssignmentContext(final EK9Parser.AssignmentExpressionContext ive) {

    return ive.expression() != null && ive.expression().list() != null;

  }

  private boolean isValidDictAssignmentContext(final EK9Parser.AssignmentExpressionContext ive) {

    return ive.expression() != null && ive.expression().dict() != null;

  }

  private String getUnQuotedTextFromRuleContext(final RuleContext ctx) {

    return ctx.getText().trim().replace("\"", "");
  }

  private String getTextFromRuleContext(final RuleContext ctx) {

    return ctx.getText().trim();
  }

}
