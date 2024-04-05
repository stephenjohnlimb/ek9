package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStack;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.AnySymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Antlr listener for the references phase.
 * i.e. check each reference and ensure it resolves, but also check
 * that no named constructs conflict with any named reference.
 * This is mainly rules and conflicts in different scenarios.
 */
final class ReferencesPhase1Listener extends EK9BaseListener {

  private final CompilableProgram compilableProgram;
  private final ParsedModule parsedModule;

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final CheckForInvalidUseOfReference checkForInvalidUseOfReference;
  private final ReferenceDoesNotResolve referenceDoesNotResolve;
  private final ConstructAndReferenceConflict duplicateSymbolByReference;
  private final ConstructAndReferenceConflict constantAndReferenceConflict;
  private final ConstructAndReferenceConflict functionAndReferenceConflict;
  private final ConstructAndReferenceConflict recordAndReferenceConflict;
  private final ConstructAndReferenceConflict traitAndReferenceConflict;
  private final ConstructAndReferenceConflict classAndReferenceConflict;
  private final ConstructAndReferenceConflict methodAndReferenceConflict;
  private final ConstructAndReferenceConflict dynamicClassAndReferenceConflict;
  private final ConstructAndReferenceConflict componentAndReferenceConflict;
  private final ConstructAndReferenceConflict serviceAndReferenceConflict;
  private final ConstructAndReferenceConflict serviceOperationAndReferenceConflict;
  private final ConstructAndReferenceConflict serviceMethodAndReferenceConflict;
  private final ConstructAndReferenceConflict applicationAndReferenceConflict;
  private final ConstructAndReferenceConflict parameterisedDetailAndReferenceConflict;
  private final ConstructAndReferenceConflict textAndReferenceConflict;
  private final ConstructAndReferenceConflict textBodyAndReferenceConflict;
  private final ConstructAndReferenceConflict typeAndReferenceConflict;
  private final ConstructAndReferenceConflict variableAndReferenceConflict;
  private final ConstructAndReferenceConflict forVariableAndReferenceConflict;

  /**
   * Next phase after symbol definition, now check for explicit references.
   * Because the reference phase is singled threaded access to compilableProgram is given directly.
   */
  ReferencesPhase1Listener(final CompilableProgram compilableProgram,
                           final ParsedModule parsedModule) {
    
    AssertValue.checkNotNull("CompilableProgramAccess cannot be null", compilableProgram);
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    this.compilableProgram = compilableProgram;
    this.parsedModule = parsedModule;

    this.symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));

    this.checkForInvalidUseOfReference = new CheckForInvalidUseOfReference(parsedModule.getSource().getErrorListener());
    this.duplicateSymbolByReference = new ConstructAndReferenceConflict("reference",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.REFERENCES_CONFLICT);
    this.referenceDoesNotResolve = new ReferenceDoesNotResolve(parsedModule.getSource().getErrorListener());

    //Errors for constructs
    this.constantAndReferenceConflict = new ConstructAndReferenceConflict("constant",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.functionAndReferenceConflict = new ConstructAndReferenceConflict("function",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.recordAndReferenceConflict = new ConstructAndReferenceConflict("record",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.traitAndReferenceConflict = new ConstructAndReferenceConflict("trait",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.classAndReferenceConflict = new ConstructAndReferenceConflict("class",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.methodAndReferenceConflict = new ConstructAndReferenceConflict("method",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.dynamicClassAndReferenceConflict = new ConstructAndReferenceConflict("dynamic class",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.componentAndReferenceConflict = new ConstructAndReferenceConflict("component",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.serviceAndReferenceConflict = new ConstructAndReferenceConflict("service",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.serviceOperationAndReferenceConflict = new ConstructAndReferenceConflict("service operation",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.serviceMethodAndReferenceConflict = new ConstructAndReferenceConflict("service method",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);

    this.applicationAndReferenceConflict = new ConstructAndReferenceConflict("application",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.textAndReferenceConflict = new ConstructAndReferenceConflict("text",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.textBodyAndReferenceConflict = new ConstructAndReferenceConflict("text body",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);

    this.typeAndReferenceConflict = new ConstructAndReferenceConflict("type",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.variableAndReferenceConflict = new ConstructAndReferenceConflict("variable",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.forVariableAndReferenceConflict = new ConstructAndReferenceConflict("for loop variable",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
    this.parameterisedDetailAndReferenceConflict = new ConstructAndReferenceConflict("parameter",
        parsedModule.getSource().getErrorListener(), ErrorListener.SemanticClassification.CONSTRUCT_REFERENCE_CONFLICT);
  }

  /**
   * This is where there are one or more references to types/functions or constants? in other modules
   * that can now be resolved - at least as a name. The full details won't yet be known.
   * But the definition phase will/should have at least defined them to some extent.
   */
  @Override
  public void enterReferencesBlock(final EK9Parser.ReferencesBlockContext ctx) {

    ctx.identifierReference().forEach(this::processIdentifierReference);

    super.enterReferencesBlock(ctx);
  }

  @Override
  public void enterConstantDeclaration(final EK9Parser.ConstantDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, constantAndReferenceConflict);

    super.enterConstantDeclaration(ctx);
  }

  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, functionAndReferenceConflict);

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, recordAndReferenceConflict);

    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, traitAndReferenceConflict);

    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, classAndReferenceConflict);

    super.enterClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, componentAndReferenceConflict);

    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, textAndReferenceConflict);

    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, textBodyAndReferenceConflict);

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, serviceAndReferenceConflict);

    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, serviceOperationAndReferenceConflict);

    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, applicationAndReferenceConflict);

    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, dynamicClassAndReferenceConflict);

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterParameterisedDetail(final EK9Parser.ParameterisedDetailContext ctx) {

    processSymbolAndReferenceClash(ctx.Identifier().getText(), new Ek9Token(ctx.start),
        parameterisedDetailAndReferenceConflict);

    super.enterParameterisedDetail(ctx);
  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    final var symbol = parsedModule.getRecordedSymbol(ctx);
    if (symbol != null) {
      //For types, it is possible for forward declare template type instances and no symbol is recorded.
      processSymbolAndReferenceClash(ctx, typeAndReferenceConflict);
    }

    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    if (ctx.getParent() instanceof EK9Parser.AggregatePartsContext) {
      processSymbolAndReferenceClash(ctx, methodAndReferenceConflict);
    } else if (ctx.getParent() instanceof EK9Parser.ServiceDeclarationContext) {
      processSymbolAndReferenceClash(ctx, serviceMethodAndReferenceConflict);
    }

    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void enterForLoop(final EK9Parser.ForLoopContext ctx) {

    processSymbolAndReferenceClash(ctx, forVariableAndReferenceConflict);

    super.enterForLoop(ctx);
  }

  @Override
  public void enterForRange(final EK9Parser.ForRangeContext ctx) {

    processSymbolAndReferenceClash(ctx, forVariableAndReferenceConflict);

    super.enterForRange(ctx);
  }

  @Override
  public void enterVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, variableAndReferenceConflict);

    super.enterVariableOnlyDeclaration(ctx);
  }

  @Override
  public void enterVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    processSymbolAndReferenceClash(ctx, variableAndReferenceConflict);

    super.enterVariableDeclaration(ctx);
  }

  private void processSymbolAndReferenceClash(final ParseTree node, final Consumer<ConflictingTokens> errorConsumer) {

    final var symbol = parsedModule.getRecordedSymbol(node);
    processSymbolAndReferenceClash(symbol.getName(), symbol.getSourceToken(), errorConsumer);

  }

  private void processSymbolAndReferenceClash(final String unqualifiedName, final IToken token,
                                              final Consumer<ConflictingTokens> errorConsumer) {

    var search = new AnySymbolSearch(ISymbol.getUnqualifiedName(unqualifiedName));

    final var existingReference = compilableProgram.resolveReferenceFromModule(parsedModule.getModuleName(),
        search);
    existingReference.ifPresent(reference -> {
      //There is a conflict
      final var originalLocation = compilableProgram.getOriginalReferenceLocation(parsedModule.getModuleName(), search);
      originalLocation.ifPresent(location -> errorConsumer.accept(
          new ConflictingTokens(token, location, reference)));
    });
  }

  private void processIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    final var identifierReference = ctx.getText();

    checkForInvalidUseOfReference.accept(ctx);
    if (identifierReference.contains("::")) {
      checkIdentifierReference(ctx, identifierReference);
    }

  }

  /**
   * The different scenarios to be catered for.
   * Firstly, is the developer referencing something that does not even exist
   * Secondly, has the developer included the same reference via multiple source files for the same thing?
   * Thirdly, is the developer attempting to reference something where the final unqualified name clashes
   * with an existing reference.
   * Finally, does the developer have a Symbol of that unqualified name available in their module?
   * This could be a module defined type/function, class method/property or even a variable in some scope.
   * The whole idea with references is to enable a developer to use shorthand for some.module::X just as X
   * But if the developer needs both a.module::X and b.module::X then they must use a fully qualified name for
   * at least one. But also if they have an X in their module already then clearly there is a clash.
   * This code will detect and issue errors for these situations.
   */
  private void checkIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx,
                                        final String fullyQualifiedIdentifierReference) {

    final var identifierToken = new Ek9Token(ctx.identifier().start);
    final var search = new AnySymbolSearch(fullyQualifiedIdentifierReference);
    final var resolved = symbolAndScopeManagement.getTopScope().resolve(search);

    if (resolved.isEmpty()) {
      //Not good - it means that during definition time it was not defined in that module. So that's an error.
      referenceDoesNotResolve.accept(identifierToken, fullyQualifiedIdentifierReference);
    } else {

      //Ok so we can add this to our set of references in our module.
      //But only if we have not already got a reference to it through
      //either this source or another source of this same module namespace
      final var existingReference = compilableProgram.resolveReferenceFromModule(parsedModule.getModuleName(),
          search);
      if (existingReference.isEmpty()) {
        parsedModule.getModuleScope().defineReference(identifierToken, resolved.get());
        //Record against the correct context.
        symbolAndScopeManagement.recordSymbol(resolved.get(), ctx);
      } else {
        final var originalLocation = compilableProgram.getOriginalReferenceLocation(parsedModule.getModuleName(), search);
        originalLocation.ifPresent(location -> duplicateSymbolByReference.accept(
            new ConflictingTokens(identifierToken, location, existingReference.get())));
      }
    }
  }
}
