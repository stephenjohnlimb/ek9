package org.ek9lang.cli.support;

import org.ek9lang.antlr.EK9BaseVisitor;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.ModuleDeclarationContext;
import org.ek9lang.antlr.EK9Parser.ProgramBlockContext;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.errors.ErrorListener.SemanticClassification;
import org.ek9lang.core.utils.Digest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A cut down visitor that just deals with packages at a basic level.
 *
 * May consider pulling a record out of the details here as this is a visitor
 * but also holds the values from the source file it has parsed.
 * Might be good to separate these.
 */
public class EK9SourceVisitor extends EK9BaseVisitor<Void>
{
	private String moduleName;

	//All parts of the package if one is defined
	private boolean packagePresent = false;
	private boolean foundVersion = false;
	private boolean foundDescription = false;
	//Only version is actually mandatory
	private boolean publicAccess = false;
	private String version = new String();
	boolean semanticVersioning = true;
	private int versionNumberOnLine = 0;
	private String description = new String();
	private String license = new String();
	private List<String> tags = new ArrayList<>();
	private Map<String, String> deps = new HashMap<>();
	private Map<String, String> devDeps = new HashMap<>();
	private Map<String, String> excludeDeps = new HashMap<>();
	private boolean applyStandardIncludes = false; //We expect do just do single source.
	private List<String> includeFiles = new ArrayList<>();
	private boolean applyStandardExcludes = true;
	private List<String> excludeFiles = new ArrayList<>();

	//Now any programs that might be in the file.
	private final List<String> programs = new ArrayList<>();

	private ErrorListener errorListener;

	public EK9SourceVisitor setErrorListener(ErrorListener errorListener)
	{
		this.errorListener = errorListener;
		return this;
	}

	@Override
	public Void visitModuleDeclaration(ModuleDeclarationContext ctx)
	{
		moduleName = ctx.dottedName().getText();
		return super.visitModuleDeclaration(ctx);
	}

	@Override
	public Void visitProgramBlock(ProgramBlockContext ctx)
	{
		ctx.methodDeclaration().forEach(methodDeclaration -> programs.add(methodDeclaration.identifier().getText().trim()));
		return super.visitProgramBlock(ctx);
	}

	@Override
	public Void visitPackageBlock(EK9Parser.PackageBlockContext ctx)
	{
		packagePresent = true;
		foundVersion = false;
		foundDescription = false;

		var assignments = getAssignmentProcessors();
		//need to manually traverse down so we only get version in this context
		for(EK9Parser.VariableDeclarationContext vd : ctx.variableDeclaration())
			assignments.getOrDefault(vd.identifier().getText(), this::processUnknownProperty).accept(vd.assignmentExpression());

		if(!foundDescription)
			errorListener.semanticError(ctx.start, "Package: property 'description' is mandatory if a package is declared.", SemanticClassification.NOT_RESOLVED);
		return super.visitPackageBlock(ctx);
	}

	private Map<String, Consumer<EK9Parser.AssignmentExpressionContext>> getAssignmentProcessors()
	{
		HashMap<String, Consumer<EK9Parser.AssignmentExpressionContext>> rtn = new HashMap<>();
		rtn.putAll(Map.of(
				"publicAccess", ive -> processPublicAccess(ive),
				"version", ive -> processVersion(ive),
				"description", ive -> processDescription(ive),
				"license", ive -> processLicense(ive),
				"tags", ive -> processTags(ive),
				"deps", ive -> processDeps(ive),
				"devDeps", ive -> processDevDeps(ive),
				"excludeDeps", ive -> processExcludeDeps(ive),
				"applyStandardIncludes", ive -> processApplyStandardIncludes(ive)
		));
		rtn.putAll(Map.of(
				"includeFiles", ive -> processIncludeFiles(ive),
				"applyStandardExcludes", ive -> processApplyStandardExcludes(ive),
				"excludeFiles", ive -> processExcludeFiles(ive)
		));
		return rtn;
	}

	private void processUnknownProperty(EK9Parser.AssignmentExpressionContext ive)
	{
		String identifier = ive.parent.getText().trim();
		errorListener.semanticError(ive.expression().start, "Package: unusable '" + identifier + "' property.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processPublicAccess(EK9Parser.AssignmentExpressionContext ive)
	{
		if(ive.expression() != null && ive.expression().primary() != null)
			publicAccess = Boolean.parseBoolean(ive.expression().getText().trim());
		else
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'publicAccess' property must be a Boolean.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processVersion(EK9Parser.AssignmentExpressionContext ive)
	{
		if(foundVersion)
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'version'.", SemanticClassification.DUPLICATE_VARIABLE);
		if(ive.expression() != null && ive.expression().primary() != null)
		{
			this.version = ive.expression().getText().trim();
			versionNumberOnLine = ive.expression().getStart().getLine();
			foundVersion = true;
		}
		else
		{
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'version' property must be a VersionNumber type.", SemanticClassification.PARAMETER_MISMATCH);
		}
	}

	private void processDescription(EK9Parser.AssignmentExpressionContext ive)
	{
		if(ive.expression() != null && ive.expression().primary() != null)
		{
			description = ive.expression().getText().trim().replaceAll("\"", "");
			foundDescription = true;
		}
		else
		{
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'description' property must be a String type.", SemanticClassification.PARAMETER_MISMATCH);
		}
	}

	private void processLicense(EK9Parser.AssignmentExpressionContext ive)
	{
		if(ive.expression() != null && ive.expression().primary() != null)
			license = ive.expression().getText().trim().replaceAll("\"", "");
		else
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'license' property must be a String type.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processExcludeFiles(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().array() != null)
			excludeFiles = extractListFromArray(ive.expression().array());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'excludeFiles' property must be List of String.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processApplyStandardExcludes(EK9Parser.AssignmentExpressionContext ive)
	{
		if(ive.expression() != null && ive.expression().primary() != null)
			applyStandardExcludes = Boolean.parseBoolean(ive.expression().getText().trim());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'applyStandardExcludes' property must be a Boolean.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processIncludeFiles(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().array() != null)
			includeFiles = extractListFromArray(ive.expression().array());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'includeFiles' property must be List of String.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processApplyStandardIncludes(EK9Parser.AssignmentExpressionContext ive)
	{
		if(ive.expression() != null && ive.expression().primary() != null)
			applyStandardIncludes = Boolean.parseBoolean(ive.expression().getText().trim());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'applyStandardIncludes' property must be a Boolean.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processExcludeDeps(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().dict() != null)
			excludeDeps = extractMapFromDict(ive.expression().dict());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'excludeDeps' property must be Dict of (String, String).", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processDevDeps(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().dict() != null)
			devDeps = extractMapFromDict(ive.expression().dict());
		else
			errorListener.semanticError(ive.expression().start, "In " + moduleName + " package: 'devDeps' property must be Dict of (String, String).", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processDeps(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().dict() != null)
			deps = extractMapFromDict(ive.expression().dict());
		else
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'deps' property must be Dict of (String, String).", SemanticClassification.PARAMETER_MISMATCH);
	}

	private void processTags(EK9Parser.AssignmentExpressionContext ive)
	{
		//We cannot assume this because user might have not used correct type
		if(ive.expression() != null && ive.expression().array() != null)
			tags = extractListFromArray(ive.expression().array());
		else
			errorListener.semanticError(ive.expression().start, "In '" + moduleName + "' package: 'tags' property must be a List of String type.", SemanticClassification.PARAMETER_MISMATCH);
	}

	private List<String> extractListFromArray(EK9Parser.ArrayContext ctx)
	{
		return ctx
				.expression()
				.stream()
				.map(value -> value.getText().replaceAll("\"", ""))
				.collect(Collectors.toList());
	}

	private Map<String, String> extractMapFromDict(EK9Parser.DictContext ctx)
	{
		HashMap<String, String> rtn = new HashMap<>();

		ctx.initValuePair().forEach(entry -> {
			String key = entry.expression().get(0).getText().replaceAll("\"", "");
			String value = entry.expression().get(1).getText().replaceAll("\"", "");
			rtn.put(key, value);
		});
		return rtn;
	}

	public boolean isPackagePresent()
	{
		return packagePresent;
	}

	public String getModuleName()
	{
		return moduleName;
	}

	public List<String> getPrograms()
	{
		return programs;
	}

	public String getVersion()
	{
		return version;
	}

	public boolean isPublicAccess()
	{
		return publicAccess;
	}

	public boolean isSemanticVersioning()
	{
		return semanticVersioning;
	}

	public int getVersionNumberOnLine()
	{
		return versionNumberOnLine;
	}

	public String getDescription()
	{
		return description;
	}

	public String getLicense()
	{
		return license;
	}

	public List<String> getTags()
	{
		return tags;
	}

	/**
	 * We need to know if the developer alters any of the dependency
	 * configuration, deps, devDeps and excludeDeps.
	 * So hash with all the contents and then it can be stored in the properties file.
	 * If it changes then we need to trigger a full recompile.
	 *
	 * @return The fingerprint of the dependencies.
	 */
	public String getDependencyFingerPrint()
	{
		String all = deps.toString() + devDeps.toString() + excludeDeps.toString();
		return Digest.digest(all).toString();
	}

	public Map<String, String> getDeps()
	{
		return deps;
	}

	public Map<String, String> getDevDeps()
	{
		return devDeps;
	}

	public Map<String, String> getExcludeDeps()
	{
		return excludeDeps;
	}

	public List<String> getIncludeFiles()
	{
		return includeFiles;
	}

	public List<String> getExcludeFiles()
	{
		return excludeFiles;
	}

	public boolean isApplyStandardIncludes()
	{
		return applyStandardIncludes;
	}

	public boolean isApplyStandardExcludes()
	{
		return applyStandardExcludes;
	}
}
