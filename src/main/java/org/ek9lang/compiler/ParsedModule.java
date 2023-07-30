package org.ek9lang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.directives.Directive;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.definition.Ek9Types;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Once we have parsed a module (source file) we need to keep track of the
 * results. Now there is the normal CompilationUnitContext which has the entry
 * point into the compiled file. But there are also a set of scopes we create
 * when we visit the context with the visitor.
 * The scopes are critical to being able to check for multiple definitions of
 * the same identity (and type) but also when it comes to resolving names.
 * Importantly because we can define the same 'module name' in more than one
 * source file and also because we can 'reference' other modules by name it
 * means we have to be able to resolve names across multiple ParsedModules to
 * find the scope they are in.
 * It also means that if a single source code file is altered we need to remove
 * the related ParsedModule (for that source file) but keep the many others we
 * have - because these will not have changed. But the identities they reference
 * may now have been removed (if we alter a source file).
 * As there will be very many source files we should only parse them when we
 * really have to - the reading and parsing is the most expensive part.
 * But we may still have to re-run the definition and resolving phases for all
 * ParsedModules. Importantly if we have a module that is parsed but is marked
 * as being in error (i.e. duplicate definition or missing reference) we also
 * need to re-run the redefinition on these modules as well (even though their
 * source code might not have changed). But we might be able to get around unnecessary
 * definition and resolving if we keep a reference to the compilation unit where we
 * resolved the definition. Clearly when adding new compilation units we may have to
 * complete a resolve again.
 * All access to symbol tables of anything shared is via SharedThreadContext.
 * So this will be created on a per source file basis and then can be parsed with a listener
 * or a visitor. An instance of this parsedModule will be given to the visitor/listener
 * then the compilationUnitContext can be set and the scope tree built up.
 * So the listener/visitor during the first parse will need to keep a local variable
 * of the current scope and deal with interacting with this object to put both the
 * contexts and scopes into the ParseTreeProperty scopes object.
 */
public class ParsedModule implements Module {

  /**
   * The source file that is parsed module has loaded (or failed to load).
   */
  private final CompilableSource source;

  /**
   * The actual whole program, or set of programs that a developer is trying to develop.
   */
  private final SharedThreadContext<CompilableProgram> compilableProgram;

  /**
   * Now we also need to hold a set of scopes for each context.
   * This is where we store the scopes in a map with the contexts
   * We use this for the multiple passes we need to do.
   * But remember there will be other parsed modules with their scopes
   * and important some with the same module name! so need to go to compilable program to access.
   * As the 'listener' goes through the ek9 source we register these scopes here.
   */
  protected ParseTreeProperty<IScope> scopes = new ParseTreeProperty<>();

  /**
   * These are the directives that can be added to EK9 source.
   * These can be used for testing the compiler, but also enabled instrumentation and reification.
   */
  protected List<Directive> directives = new ArrayList<>();

  /**
   * On first pass through the code we don't know the types or other items relating to a symbol
   * So we keep the map of the context and the symbol, so we can augment information on the symbol
   * Then when we are ready we can attempt to add the symbols to the correct scopes during a visit
   * But we may find there are duplicate and the like so that is semantic analysis.
   * So it is probable that something like a class will be both recorded as a symbol and also a scope.
   */
  protected ParseTreeProperty<ISymbol> symbols = new ParseTreeProperty<>();

  /**
   * The name of the module as defined in the EK9 source code. But remember this same module name
   * can be used in other 'ParsedModules' any number of source files can make up a 'module' with a
   * distinct name.
   */
  private String moduleName;

  /**
   * Once the source is parsed, this will hold the main entry point into the EK9 source as a parsed structure.
   */
  private EK9Parser.CompilationUnitContext compilationUnitContext = null;

  //We also need to keep a record of the ModuleScope so that when we come to resolve across modules we can
  //Set after the first definition phase.
  private ModuleScope moduleScope;

  /**
   * Is the parsed module an EK9 implementation or externally linked.
   * So the built-in EK9 types will be externally linked either java/jar or binary '*.so' for example.
   * But there is a plugin mechanism - designed to enable third party 'adaptors' and binary linked code.
   */
  private boolean externallyImplemented = false;

  private Ek9Types ek9Types;

  /**
   * We may hold Nodes in here - but not sure yet.
   */
  public ParsedModule(CompilableSource source, SharedThreadContext<CompilableProgram> compilableProgram) {
    AssertValue.checkNotNull("CompilableSource cannot be null", source);
    AssertValue.checkNotNull("CompilableProgram cannot be null", compilableProgram);
    this.source = source;
    this.compilableProgram = compilableProgram;
  }

  public ModuleScope getModuleScope() {
    return moduleScope;
  }

  private void setModuleScope(ModuleScope moduleScope) {
    AssertValue.checkNotNull("ModuleScope cannot be null", moduleScope);
    this.moduleScope = moduleScope;
  }

  /**
   * If cached ek9 types have been provided, they can be accessed here.
   */
  public Ek9Types getEk9Types() {
    if (ek9Types == null) {
      AtomicReference<Ek9Types> ref = new AtomicReference<>();
      compilableProgram.accept(program -> ref.set(program.getEk9Types()));
      ek9Types = ref.get();
    }
    return ek9Types;
  }

  public boolean isExternallyImplemented() {
    return externallyImplemented;
  }

  public void setExternallyImplemented(boolean externallyImplemented) {
    this.externallyImplemented = externallyImplemented;
  }

  @Override
  public boolean isEk9Core() {
    //Any module that start with this is deemed core.
    return this.moduleName.startsWith("org.ek9");
  }

  /**
   * Once the source code has been parsed by one of the stages in the compiler, the CompilationUnitContext
   * can be provided to this Parsed module. It will then have the second part of its initialisation complete.
   * It will create a module scope and that can be used to add and define symbols.
   */
  public ModuleScope acceptCompilationUnitContext(EK9Parser.CompilationUnitContext compilationUnitContext) {
    AssertValue.checkNotNull("CompilationUnitContext cannot be null", compilationUnitContext);
    this.compilationUnitContext = compilationUnitContext;
    var theModuleName = compilationUnitContext.moduleDeclaration().dottedName().getText();
    AssertValue.checkNotEmpty("ModuleName must have a value", theModuleName);
    setModuleName(theModuleName);
    //Only set if a module scope is not yet present.
    //Typically, on the definition phase will set this.
    if (getModuleScope() == null) {
      setModuleScope(new ModuleScope(theModuleName, compilableProgram));
    }

    return getModuleScope();
  }

  public boolean isForThisCompilableSource(CompilableSource compilableSource) {
    AssertValue.checkNotNull("CompilableSource cannot be null", compilableSource);
    return source.equals(compilableSource);
  }

  /**
   * When processing EK9 source code the developer now has some ability to use
   * '@directives'. These are aimed at code compilation, instrumentation and error checking.
   * This means that for compiler development, we can reduce the number of Java unit tests and
   * specific coding (to some extent), by adding in our expectations just before we write
   * so erroneous code (to check the compiler).
   * This means that the test and the deliberated defective code as co-located.
   */
  public void recordDirective(final Directive directive) {
    AssertValue.checkNotNull("Directive cannot be null", directive);
    directives.add(directive);
  }

  /**
   * Provide access to any directives recorded of a specific type and compilation phase.
   */
  public List<Directive> getDirectives(final DirectiveType type, final CompilationPhase phase) {
    return directives.stream().filter(directive -> directive.type() == type)
        .filter(directive -> directive.isForPhase(phase)).toList();
  }

  /**
   * Provide access to any directives recorded.
   */
  public List<Directive> getDirectives(final DirectiveType type) {
    return directives.stream().filter(directive -> directive.type() == type).toList();
  }

  /**
   * Record a particular node context during listen/visit of a context with a particular scope.
   */
  public void recordScope(ParseTree node, IScope withScope) {
    AssertValue.checkNotNull("WithScope cannot be null", withScope);
    scopes.put(node, withScope);
  }

  /**
   * Locate and return a recorded scope against part of the parse tree,
   * this may return null if nothing has been recorded.
   */
  public IScope getRecordedScope(ParseTree node) {
    return scopes.get(node);
  }

  /**
   * Record a particular node context with a particular symbol.
   */
  public void recordSymbol(ParseTree node, ISymbol symbol) {
    AssertValue.checkNotNull("Node cannot be null", node);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    //Let the symbol know where it is defined.
    //But it can only be defined in one place - in case of references we record in other locations
    //We only want its actual module recorded the first time it is encountered.
    symbol.setParsedModule(Optional.of(this));
    symbols.put(node, symbol);

  }

  /**
   * Locate and return a recorded symbol against part of the parse tree,
   * this may return null if nothing has been recorded.
   */
  public ISymbol getRecordedSymbol(ParseTree node) {
    return symbols.get(node);
  }

  @Override
  public boolean equals(Object obj) {
    //Only if the compilation units match is this equal
    var rtn = false;
    if (obj instanceof ParsedModule module) {
      rtn = this.compilationUnitContext == module.compilationUnitContext;
    }
    return rtn;
  }

  @Override
  public int hashCode() {
    var rtn = super.hashCode();
    if (compilationUnitContext != null) {
      rtn = compilationUnitContext.hashCode();
    }
    return rtn;
  }

  @Override
  public String toString() {
    return getModuleName();
  }

  public String getModuleName() {
    return moduleName;
  }

  private void setModuleName(String moduleName) {
    AssertValue.checkNotNull("ModuleName cannot be null", moduleName);
    this.moduleName = moduleName;
  }

  @Override
  public CompilableSource getSource() {
    return this.source;
  }

  @Override
  public String getScopeName() {
    return getModuleName();
  }
}
