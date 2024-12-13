package org.ek9lang.compiler.support;

import java.util.Set;
import java.util.function.Predicate;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.compiler.symbols.StreamPipeLineSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Just deals with the creation of Streams parts.
 */
class StreamFactory extends CommonFactory {

  //Note that uniq and sort might be able to consume anything if they aren't given a function.
  //But there are limits, there would be to be hashCode, comparators, plus in some cases types being consumed
  //must be functions (call and async).
  private static final Set<String> streamPartCanConsumeAnything = Set.of("flatten",
      "call", "async", "skipping", "head", "tail");

  private static final Set<String> streamPartProducerAndConsumerTypeSame = Set.of(
      "skipping", "head", "tail", "filter", "select", "sort", "group",
      "join", "uniq", "tee");

  private static final Predicate<String> canConsumeAnything
      = streamPartCanConsumeAnything::contains;

  private static final Predicate<String> isProducerAndConsumerSameType
      = streamPartProducerAndConsumerTypeSame::contains;

  private static final Predicate<String> isProducerDerivedFromConsumerType = "flatten"::equals;

  private static final Predicate<String> isASinkInNature = "tee"::equals;

  private static final Predicate<String> isFunctionRequired
      = operation -> "call".equals(operation) || "async".equals(operation);


  StreamFactory(final ParsedModule parsedModule) {
    super(parsedModule);

  }

  /**
   * Create a new symbol that represents an EK9 concept of a stream pipeline.
   */
  public StreamPipeLineSymbol newStream(final ParserRuleContext ctx) {

    checkContextNotNull.accept(ctx);
    final var pipeLine = new StreamPipeLineSymbol("stream");

    configureSymbol(pipeLine, new Ek9Token(ctx.start));
    pipeLine.setReferenced(true);
    pipeLine.setNotMutable();

    return pipeLine;
  }

  /**
   * Create a new symbol that represents an EK9 'cat' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamCat(final EK9Parser.StreamCatContext ctx, final IScope scope) {

    checkContextNotNull.accept(ctx);
    final var call = new StreamCallSymbol("cat", scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 'for' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamFor(final EK9Parser.StreamForContext ctx, final IScope scope) {

    checkContextNotNull.accept(ctx);
    final var call = new StreamCallSymbol("for", scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 stream function part of a stream pipeline.
   */
  public StreamCallSymbol newStreamPart(final EK9Parser.StreamPartContext ctx, final IScope scope) {
    checkContextNotNull.accept(ctx);
    final var operation = ctx.op.getText();
    final var call = new StreamCallSymbol(operation, scope);

    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    //It is necessary to correctly configure the stream part for later processing.
    //This enables type inference and also other logic checks.
    //May need to revisit this once addressing later phases.
    call.setCapableOfConsumingAnything(canConsumeAnything.test(operation));
    call.setProducerSymbolTypeSameAsConsumerSymbolType(isProducerAndConsumerSameType.test(operation));
    call.setSinkInNature(isASinkInNature.test(operation));
    call.setProducesTypeMustBeAFunction(isFunctionRequired.test(operation));
    call.setDerivesProducesTypeFromConsumesType(isProducerDerivedFromConsumerType.test(operation));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 terminal part of a stream pipeline.
   */
  public StreamCallSymbol newStreamTermination(final ParserRuleContext ctx,
                                               final String operation,
                                               final IScope scope) {
    checkContextNotNull.accept(ctx);
    final var call = new StreamCallSymbol(operation, scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));
    call.setSinkInNature(true);

    return call;
  }

  private void configureStreamCallSymbol(final StreamCallSymbol call, final IToken token) {

    configureSymbol(call, token);
    call.setReferenced(true);
    call.setNotMutable();

  }

}
