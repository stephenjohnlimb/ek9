package org.ek9lang.compiler.support;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.UniqueIdGenerator;

/**
 * Deals with the creation of various aggregate types.
 */
public class AggregateFactory extends CommonFactory {
  AggregateFactory(ParsedModule parsedModule) {
    super(parsedModule);
  }

  /**
   * Create a new aggregate that represents an EK9 class.
   * A bit tricky when it comes to parameterised (generic/template classes).
   */
  public AggregateWithTraitsSymbol newClass(final EK9Parser.ClassDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate class name", ctx.Identifier());

    final var moduleScope = parsedModule.getModuleScope();
    final var className = ctx.Identifier().getText();
    final var newClass = newAggregateWithTraitsSymbol(className, new Ek9Token(ctx.start));

    newClass.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    newClass.setGenus(SymbolGenus.CLASS);
    newClass.setMarkedAbstract(ctx.ABSTRACT() != null);

    final var parameterisedSymbols =
        createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), newClass, moduleScope);

    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newClass::addTypeParameterOrArgument);
    }

    return newClass;
  }

  /**
   * Create a new aggregate that represents an EK9 component.
   */
  public AggregateWithTraitsSymbol newComponent(final EK9Parser.ComponentDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate component name", ctx.Identifier());

    final var componentName = ctx.Identifier().getText();
    final var component = newAggregateWithTraitsSymbol(componentName, new Ek9Token(ctx.start));

    component.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    component.setGenus(SymbolGenus.COMPONENT);
    component.setMarkedAbstract(ctx.ABSTRACT() != null);

    return component;
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(final EK9Parser.TraitDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate trait name", ctx.Identifier());

    final var traitName = ctx.Identifier().getText();
    final var trait = newAggregateWithTraitsSymbol(traitName, new Ek9Token(ctx.start));

    configureAggregate(trait, new Ek9Token(ctx.start));
    trait.setGenus(SymbolGenus.CLASS_TRAIT);

    //All traits are designed to be open to extending and use/override.
    //Even though trait can have abstract and open - this is just for syntax consistency
    //So a developer can write it in the same way as a class or component - but they are all open and abstract.
    trait.setOpenForExtension(true);
    trait.setMarkedAbstract(true);

    return trait;
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(final EK9Parser.RecordDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate record name", ctx.Identifier());

    final var recordName = ctx.Identifier().getText();
    final var newRecord = new AggregateSymbol(recordName, parsedModule.getModuleScope());

    configureAggregate(newRecord, new Ek9Token(ctx.start));
    newRecord.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    newRecord.setGenus(SymbolGenus.RECORD);
    newRecord.setMarkedAbstract(ctx.ABSTRACT() != null);

    return newRecord;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic class.
   */
  public AggregateWithTraitsSymbol newDynamicClass(final IScopedSymbol enclosingMainTypeOrFunction,
                                                   final EK9Parser.DynamicClassDeclarationContext ctx) {

    //Name is optional - if not present then generate a dynamic value.
    final var dynamicClassName = ctx.Identifier() != null
        ? ctx.Identifier().getText()
        : "_Class_" + UniqueIdGenerator.getNewUniqueId();

    final var rtn = newAggregateWithTraitsSymbol(dynamicClassName, new Ek9Token(ctx.start));
    rtn.setOuterMostTypeOrFunction(enclosingMainTypeOrFunction);
    rtn.setScopeType(IScope.ScopeType.DYNAMIC_BLOCK);
    rtn.setReferenced(true);

    return rtn;
  }

  private AggregateWithTraitsSymbol newAggregateWithTraitsSymbol(final String className, final IToken start) {

    final var scope = parsedModule.getModuleScope();
    final var clazz = new AggregateWithTraitsSymbol(className, scope);

    configureAggregate(clazz, start);
    clazz.setGenus(SymbolGenus.CLASS);

    return clazz;
  }
}
