package org.ek9lang.compiler.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.antlr.EK9BaseVisitor;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.main.resolvedefine.GeneralTypeResolver;
import org.ek9lang.compiler.main.resolvedefine.SymbolSearchConfiguration;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Pass a string of en ek9 typeDef in and use the
 * EK9 parser to just handle the breaking up of a String of a TypeDef into
 * a structure and names.
 * Then use a resolver to see if they type or parameterized type exists.
 */
public class JustTypeDef extends EK9BaseVisitor<SymbolSearchConfiguration> {
  private final GeneralTypeResolver generalTypeResolver;

  private final PartialEk9StringToTypeDef partialEk9StringToTypeDef = new PartialEk9StringToTypeDef();

  public JustTypeDef(final IScope scopeForResolution) {
    this.generalTypeResolver = new GeneralTypeResolver(scopeForResolution);
  }

  /**
   * Use an ek9 parser to accept a String of a typeDef and convert to a symbol (if it can be found).
   */
  public Optional<ISymbol> typeDefToSymbol(final String typeDefinition) {

    var context = partialEk9StringToTypeDef.apply(typeDefinition);
    if (context != null) {
      return generalTypeResolver.apply(visit(context));
    }
    return Optional.empty();
  }

  /**
   * This is really the main entry point to build a structure of SymbolSearchConfiguration
   * that is based on the structure in the ek9 parse tree from typDef.
   */
  @Override
  public SymbolSearchConfiguration visitTypeDef(EK9Parser.TypeDefContext ctx) {
    return byTypeDef(ctx);
  }

  private SymbolSearchConfiguration byTypeDef(EK9Parser.TypeDefContext ctx) {
    if (ctx.identifierReference() != null) {
      return byIdentifierReference(ctx.identifierReference());
    } else {
      return byParameterizedType(ctx.parameterisedType());
    }
  }

  private SymbolSearchConfiguration byIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    var identifierReferenceName = ctx.getText();
    return new SymbolSearchConfiguration(identifierReferenceName);
  }

  private SymbolSearchConfiguration byParameterizedType(EK9Parser.ParameterisedTypeContext ctx) {
    var genericTypeName = ctx.identifierReference().getText();
    return byParameterizingDetails(genericTypeName, ctx);
  }

  private SymbolSearchConfiguration byParameterizingDetails(final String genericTypeName,
                                                            final EK9Parser.ParameterisedTypeContext ctx) {
    //So trigger the recursive call back to the top most method.
    //This is just a single parameterizing parameter.
    if (ctx.typeDef() != null) {
      var parameterizingName = byTypeDef(ctx.typeDef());
      return new SymbolSearchConfiguration(genericTypeName, List.of(parameterizingName));
    } else {
      //This is for multiple parameterizing parameters.
      //Multiple type defs - so we need to try and get them all and hold in a list.
      var genericParameterizingNames = new ArrayList<SymbolSearchConfiguration>();
      for (var typeDefCtx : ctx.parameterisedArgs().typeDef()) {
        //Multiple recursive calls back around the loop.
        var parameterizingName = byTypeDef(typeDefCtx);
        genericParameterizingNames.add(parameterizingName);
      }
      return new SymbolSearchConfiguration(genericTypeName, genericParameterizingNames);
    }
  }
}