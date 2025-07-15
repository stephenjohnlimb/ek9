package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.BASE_NAME;
import static org.ek9lang.compiler.support.CommonValues.LANG;

import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * Deals with the creation of Text parts from the EK9 language.
 */
class TextFactory extends CommonFactory {
  TextFactory(ParsedModule parsedModule) {
    super(parsedModule);
  }

  /**
   * Create a new aggregate that represents EK9 text construct.
   * Note that this also creates a common base for all text with the same name.
   * It ensures that all methods are added to that common base.
   * Then we can check if all those methods on the common base are also defined in each and every concrete language
   * text aggregate.
   */
  public AggregateSymbol newText(final EK9Parser.TextDeclarationContext ctx, final String forLanguage) {

    checkContextNotNull.accept(ctx);
    //an error will have been created for the developer as language does not conform.
    if (forLanguage != null) {
      var baseName = ctx.Identifier().getText();
      Optional<ISymbol> base = parsedModule.getModuleScope().resolve(new TypeSymbolSearch(baseName));

      if (base.isEmpty()) {

        final var textBase = new AggregateSymbol(baseName, parsedModule.getModuleScope());
        configureAggregate(textBase, new Ek9Token(ctx.start));
        textBase.setGenus(SymbolGenus.TEXT);
        //Now for the base - we set this to be an 'Any'.
        //That way the 'en' or 'de' Text that extends the Base text is also an 'Any'.
        textBase.setSuperAggregate(parsedModule.getEk9Types().ek9Any());

        parsedModule.getModuleScope().define(textBase);

        final var constructor = aggregateManipulator
            .addConstructor(textBase, aggregateManipulator.resolveString(parsedModule.getModuleScope()));
        //Text can never mutate anything as construction.
        constructor.setMarkedPure(true);
        base = Optional.of(textBase);
      }

      final var textName = baseName + "_" + forLanguage;
      final var text = new AggregateSymbol(textName, parsedModule.getModuleScope());

      configureAggregate(text, new Ek9Token(ctx.start));
      text.setGenus(SymbolGenus.TEXT);
      //Now ensure it is set up as the 'super'.
      text.setSuperAggregate((IAggregateSymbol) base.get());
      //Store both the language this is for and the base name, this will be useful later
      text.putSquirrelledData(LANG, forLanguage);
      text.putSquirrelledData(BASE_NAME, baseName);
      return text;
    }

    //return a placeholder so error handling can continue
    return new AggregateSymbol(ctx.Identifier().getText(), parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 text body - this is represented by a method.
   */
  public MethodSymbol newTextBody(final EK9Parser.TextBodyDeclarationContext ctx, final IScope scope) {
    checkContextNotNull.accept(ctx);

    final var methodName = ctx.Identifier().getText();

    return makeTextBodyMethod(methodName, new Ek9Token(ctx.start), scope);
  }

  /**
   * Ensures that any method in a specific text block is always added to the base text for that component.
   */
  public void ensureTextBodyIsInSuper(final MethodSymbol textMethodSymbol) {

    final var scope = textMethodSymbol.getParentScope();

    if (scope instanceof IAggregateSymbol textDeclaration
        && textDeclaration.getSuperAggregate().isPresent()) {

      final var textBase = textDeclaration.getSuperAggregate().get();
      final var results = textBase.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch(textMethodSymbol),
          new MethodSymbolSearchResult());

      if (results.isEmpty()) {
        final var textBaseMethod = textMethodSymbol.clone(textBase);
        textBaseMethod.setOverride(false);
        textBase.define(textMethodSymbol);
      }
    } else {
      throw new CompilerException("Only expecting text body to be using within a textDeclaration scope with a super");
    }

  }

  private MethodSymbol makeTextBodyMethod(final String methodName,
                                          final IToken token,
                                          final IScope scope) {

    final var method = new MethodSymbol(methodName, scope);

    configureSymbol(method, token);
    method.setOverride(true);
    method.setMarkedAbstract(false);
    method.setMarkedPure(true);
    method.setOperator(false);

    //Always returns a String - no need or ability to declare it.
    method.setType(aggregateManipulator.resolveString(scope));

    return method;
  }

}
