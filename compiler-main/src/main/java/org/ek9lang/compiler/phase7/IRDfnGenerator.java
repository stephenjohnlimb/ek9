package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * Deals with visiting all parts of the EK9 structure in a module (compilable source).
 * These parts are then added to the appropriate structures (in an IR graph) and then at the
 * top level (aggregate for example) are added to the IRModule.
 * <p>
 * The concept is to move away from the EK9 grammar, syntax and semantics. But does not move too
 * far towards any concrete final output.
 * </p>
 * <p>
 * So for example, everything in the IR will become some form of 'struct with operations'.
 * This includes all types of EK9 aggregates and even EK9 functions (as they can hold state).
 * </p>
 * <p>
 * Constants and actual EK9 functions will just be 'instances' of these 'struct with operations'.
 * </p>
 * <p>
 * Another example is 'for loops', 'while loops' and 'do while loops'. These will all be
 * defined in terms of executable blocks, with a 'condition' (that will appear in different locations)
 * and a 'Marker' for the start and end. So, here this is moving well towards something that can be
 * implemented in some form of machine/byte code.
 * </p>
 * <p>
 * Guards will also be broken down into 'basic blocks' with labels and a condition.
 * </p>
 */
final class IRDfnGenerator {

  private final CompilerFlags compilerFlags;
  private ParsedModule parsedModule;
  private final IRModule irModule;
  private final IRContext baseIRContext;


  IRDfnGenerator(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                 final CompilableSource source,
                 final IRModule irModule,
                 final CompilerFlags compilerFlags) {

    AssertValue.checkNotNull("compilableProgramAccess cannot be null", compilableProgramAccess);
    AssertValue.checkNotNull("source cannot be null", source);
    AssertValue.checkNotNull("irModule cannot be null", irModule);
    AssertValue.checkNotNull("compilerFlags cannot be null", compilerFlags);

    this.compilerFlags = compilerFlags;
    this.irModule = irModule;
    compilableProgramAccess.accept(compilableProgram ->
        this.parsedModule = compilableProgram.getParsedModuleForCompilableSource(source));

    // Create base IRContext once - will be used to create per-construct stack contexts
    this.baseIRContext = new org.ek9lang.compiler.phase7.support.IRContext(parsedModule, compilerFlags);

  }

  /**
   * This is the main entry point into creating the IR fo a particular source file via its
   * compilation unit context.
   *
   * @param compilationUnitContext The main entry point in via the ANTLR grammar.
   */
  void create(final EK9Parser.CompilationUnitContext compilationUnitContext) {

    //Note that each module block can be processed in parallel
    //Access to the irModule (add Construct) is synchronized.
    compilationUnitContext
        .moduleDeclaration()
        .moduleBlock()
        .parallelStream()
        .forEach(this::createIRForModuleBlock);
  }

  /**
   * Processes the specific type of module.
   *
   * @param ctx The context of the particular module to process.
   */
  private void createIRForModuleBlock(final EK9Parser.ModuleBlockContext ctx) {
    switch (ctx.children.getFirst()) {
      case EK9Parser.ProgramBlockContext programs -> createIRForProgram(programs);
      case EK9Parser.TypeBlockContext types -> createIRForTypeBlock(types);
      case EK9Parser.FunctionBlockContext functions -> createIRForFunctionBlock(functions);
      case EK9Parser.RecordBlockContext records -> createIRForRecordBlock(records);
      case EK9Parser.ClassBlockContext classes -> createIRForClassBlock(classes);
      case EK9Parser.TraitBlockContext traits -> createIRForTraitBlock(traits);
      case EK9Parser.ComponentBlockContext components -> createIRForComponentBlock(components);
      case EK9Parser.ConstantBlockContext constants -> createIRForConstantBlock(constants);
      case EK9Parser.PackageBlockContext packages -> createIRForPackageBlock(packages);
      case EK9Parser.TextBlockContext texts -> createIRForTextBlock(texts);
      default -> throw new CompilerException(ctx.getText() + " block type not implemented");
    }
  }

  private void createIRForTypeBlock(final EK9Parser.TypeBlockContext ctx) {
    // Type definitions are metadata only - no IR generation needed
    // Not strictly true we declare enumeration and constrained type here.
    //TODO.
  }

  private void createIRForFunctionBlock(final EK9Parser.FunctionBlockContext ctx) {
    ctx.functionDeclaration().forEach(this::processFunctionDeclaration);
  }

  private void createIRForRecordBlock(final EK9Parser.RecordBlockContext ctx) {
    ctx.recordDeclaration().forEach(this::processRecordDeclaration);
  }

  private void createIRForClassBlock(final EK9Parser.ClassBlockContext ctx) {
    ctx.classDeclaration().forEach(this::processClassDeclaration);
  }

  private void createIRForTraitBlock(final EK9Parser.TraitBlockContext ctx) {
    ctx.traitDeclaration().forEach(this::processTraitDeclaration);
  }

  private void createIRForComponentBlock(final EK9Parser.ComponentBlockContext ctx) {
    ctx.componentDeclaration().forEach(this::processComponentDeclaration);
  }

  private void createIRForConstantBlock(final EK9Parser.ConstantBlockContext ctx) {
    ctx.constantDeclaration().forEach(this::processConstantDeclaration);
  }

  private void createIRForPackageBlock(final EK9Parser.PackageBlockContext ctx) {
    // Package declarations are metadata only - no IR generation needed
    //The we may provide that meta data in some way TODO consider this.
  }

  private void createIRForTextBlock(final EK9Parser.TextBlockContext ctx) {
    ctx.textDeclaration().forEach(this::processTextDeclaration);
  }

  private void createIRForProgram(final EK9Parser.ProgramBlockContext ctx) {
    ctx.methodDeclaration().forEach(this::processProgramDeclaration);
  }

  /**
   * Deals with method declarations and the specific case of EK9 programs.
   *
   * @param ctx the parse tree context that defines the method details.
   */
  public void processProgramDeclaration(final EK9Parser.MethodDeclarationContext ctx) {
    // Create isolated stack context for this program
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var programCreator = new ProgramDfnGenerator(stackContext);
    
    irModule.add(programCreator.apply(ctx));
  }

  // Declaration processing methods with per-construct stack context
  private void processFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {
    // Create isolated stack context for this function
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var functionCreator = new FunctionDfnGenerator(stackContext);
    
    // Create the IR construct for this function and add to module
    final var construct = functionCreator.apply(ctx);
    irModule.add(construct);
  }

  private void processRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {
    // Create isolated stack context for this record
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var recordCreator = new RecordDfnGenerator(stackContext);
    
    final var construct = recordCreator.apply(ctx);
    irModule.add(construct);
  }

  private void processClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {
    // Create isolated stack context for this class
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var classCreator = new ClassDfnGenerator(stackContext);
    
    final var construct = classCreator.apply(ctx);
    irModule.add(construct);
  }

  private void processTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {
    // Create isolated stack context for this trait
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var traitCreator = new TraitDfnGenerator(stackContext);
    
    final var construct = traitCreator.apply(ctx);
    irModule.add(construct);
  }

  private void processComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {
    // Create isolated stack context for this component
    var isolatedIRContext = new IRContext(baseIRContext);  // Creates fresh counters
    var stackContext = new IRGenerationContext(isolatedIRContext);
    var componentCreator = new ComponentDfnGenerator(stackContext);
    
    final var construct = componentCreator.apply(ctx);
    irModule.add(construct);
  }

  private void processConstantDeclaration(final EK9Parser.ConstantDeclarationContext ctx) {
    // Constants may have initialization expressions - need IRGenerationContext
  }

  private void processTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {
    // Text templates have multiple text body declarations, each needs its own IRGenerationContext
    for (final var textBodyCtx : ctx.textBodyDeclaration()) {
      final var context = new IRContext(parsedModule, compilerFlags);
      processTextBodyDeclaration(textBodyCtx, context);
    }
  }

  public void processTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx,
                                         final IRContext context) {
    // Text bodies contain string literals with potential embedded expressions
    // Grammar: Identifier (LPAREN RPAREN)? NL+ INDENT NL* argumentParam? directive? stringLit NL+ DEDENT

    // Process argumentParam if present - text methods can take parameters
    if (ctx.argumentParam() != null) {
      // Process each parameter declaration 
      for (final var paramCtx : ctx.argumentParam().variableOnlyDeclaration()) {
        processVariableOnlyDeclarationWithContext(paramCtx, context, "text_param");
      }
    }

    // String literals in text declarations may contain embedded expressions
    // These would be processed when the text template is instantiated/rendered
    // For now, just note that the string literal is present
    //TODO - Process embedded expressions in stringLit when string interpolation is implemented
  }


  private void processVariableOnlyDeclarationWithContext(final EK9Parser.VariableOnlyDeclarationContext ctx,
                                                         final IRContext context,
                                                         final String scopePrefix) {
    //TODO - Add these instructions to the appropriate BasicBlock
  }
}
