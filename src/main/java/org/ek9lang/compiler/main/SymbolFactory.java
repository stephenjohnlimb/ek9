package org.ek9lang.compiler.main;

import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * Just a factory for all types of EK9 symbol.
 * Ensures that newly created symbols are initialised correctly.
 */
public class SymbolFactory {

  private final ParsedModule parsedModule;

  /**
   * Create a new symbol factory for use with the parsedModule.
   */
  public SymbolFactory(ParsedModule parsedModule) {
    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);
    this.parsedModule = parsedModule;
  }

  /**
   * Create a new aggregate that represents an EK9 program.
   */
  public AggregateSymbol newProgram(EK9Parser.MethodDeclarationContext ctx, IScope scope) {
    AssertValue.checkNotNull("Failed to locate program name", ctx.identifier());

    //TODO Need to deal with the application setup part.
    //We underscore to class name and main processing method don't clash and look like a constructor.
    String programName = "_" + ctx.identifier().getText();

    AggregateSymbol program = new AggregateSymbol(programName, scope);
    configureAggregate(program, ctx.start);

    return program;
  }

  /**
   * Create a new aggregate that represents an EK9 class.
   */

  public AggregateWithTraitsSymbol newClass(EK9Parser.ClassDeclarationContext ctx) {
    String className = ctx.Identifier().getText();
    return newAggregateWithTraitsSymbol(className, ctx.start, parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(EK9Parser.TraitDeclarationContext ctx) {
    String traitName = ctx.Identifier().getText();
    AggregateWithTraitsSymbol trait = newAggregateWithTraitsSymbol(traitName, ctx.start, parsedModule.getModuleScope());
    trait.setGenus(ISymbol.SymbolGenus.CLASS_TRAIT);
    //All traits are designed to be open to extending and use/override.
    trait.setOpenForExtension(true);

    return trait;
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(EK9Parser.RecordDeclarationContext ctx) {
    String recordName = ctx.Identifier().getText();
    AggregateSymbol newRecord = new AggregateSymbol(recordName, parsedModule.getModuleScope());
    configureAggregate(newRecord, ctx.start);
    newRecord.setGenus(ISymbol.SymbolGenus.RECORD);
    return newRecord;
  }

  /**
   * Create a new aggregate that represents an EK9 function.
   */
  public FunctionSymbol newFunction(EK9Parser.FunctionDeclarationContext ctx) {
    String functionName = ctx.Identifier().getText();
    FunctionSymbol function = new FunctionSymbol(functionName, parsedModule.getModuleScope());
    function.setModuleScope(parsedModule.getModuleScope());
    function.setParsedModule(Optional.of(parsedModule));
    function.setSourceToken(ctx.start);

    //A function can be both pure and abstract - in this case it is establishing a 'contract' that the
    //implementation must also be pure!
    //This is so that uses of abstract concepts can be used to ensure that there are no side effects.
    if (ctx.PURE() != null) {
      function.setMarkedPure(true);
    }

    //While it maybe a function we need to know if it is abstract or not
    if (ctx.ABSTRACT() == null) {
      function.setGenus(ISymbol.SymbolGenus.FUNCTION);
    } else {
      function.setGenus(ISymbol.SymbolGenus.FUNCTION_TRAIT);
      function.setMarkedAbstract(true);
    }

    //make a note of this - could be null
    function.setReturningParamContext(ctx.operationDetails().returningParam());

    return function;
  }

  /**
   * Create a new aggregate that represents an EK9 literal value.
   */
  public ConstantSymbol newLiteral(Token start, String name) {
    AssertValue.checkNotNull("Start token cannot be null", start);
    AssertValue.checkNotNull("Name cannot be null", name);

    ConstantSymbol literal = new ConstantSymbol(name, true);

    literal.setSourceToken(start);
    literal.setParsedModule(Optional.of(parsedModule));
    //You cannot set this to any other value.
    literal.setNotMutable();
    return literal;
  }

  private AggregateWithTraitsSymbol newAggregateWithTraitsSymbol(String className, Token start, IScope scope) {
    AggregateWithTraitsSymbol clazz = new AggregateWithTraitsSymbol(className, scope);
    configureAggregate(clazz, start);
    clazz.setGenus(ISymbol.SymbolGenus.CLASS);
    return clazz;
  }

  private void configureAggregate(AggregateSymbol aggregate, Token start) {
    aggregate.setModuleScope(parsedModule.getModuleScope());
    aggregate.setParsedModule(Optional.of(parsedModule));
    aggregate.setSourceToken(start);
  }

}
