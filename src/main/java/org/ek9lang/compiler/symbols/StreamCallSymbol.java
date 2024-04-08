package org.ek9lang.compiler.symbols;

import java.io.Serial;

/**
 * Just re-uses the bulk of method symbol. This is only used in part of the stream pipeline.
 * We need additional information surrounding the type that can be accepted and the type that
 * is produced (chained).
 * In some cases (like a call to head) we can accept anything in and will produce the same back out.
 * But a filter can only accept a certain type in - but will then also product that same type out.
 * A map on the other hand will only accept a certain type and will product another type
 * (maybe - depending on the map).
 * Flatten can only accept something is can iterate over and will then product whatever that type
 * is that is provided by the iterator.
 * This is the 'glue' between streaming commands and functions.
 */
public class StreamCallSymbol extends MethodSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  //For mid-part of a stream pipeline both of these types will be set.
  //Not sure about 'tee' it is an anomaly in some ways - consumes yes
  //but also can map to a terminal, but also pass on the type it consumes!

  //Maybe the match on the '|' pipe requires a promotion.
  private boolean consumesSymbolPromotionRequired = false;

  /**
   * Typically used in terminal but also tee.
   * This indicates that when a pipeline type is known in the pipeline itself we really
   * want to check if this sort of call is capable of receiving it in some way via
   * the '|' pipe operator.
   * Originally I was only going to allow one type of object to be received via pipe,
   * but if we can support multiple '|' pipe operators it means we have more flexibility and
   * can reduce the number of map functions required.
   */
  private boolean sinkInNature = false;

  /**
   * For head, skip, tail, flatten, call and async - we're not bothered what type we get
   * indeed this 'command' has no way of knowing what it will/should be passed.
   * So it does need to be told, so it can also say what it produces.
   * So I'm expecting the pipeline to go through and tell these type of calls what they will be
   * consuming.
   * So that logic will need to also check a couple of things for flatten, call and async as those
   * do have constraints.
   */
  private boolean capableOfConsumingAnything = false;

  /**
   * For flatten the consumes must have an iterator and the call would produce N of the type
   * the iterator supplies.
   */
  private boolean derivesProducesTypeFromConsumesType = false;

  /**
   * Should the upstream producer be a function or not.
   */
  private boolean producesTypeMustBeAFunction = false;

  /**
   * In the case of something like filter, head, tail, skip whatever is consumed is also
   * produced (conditionally and maybe in limited volume).
   */
  private boolean producerSymbolTypeSameAsConsumerSymbolType = false;

  //Not always set for a main stream head consumes will be null
  private ISymbol consumesSymbolType = null;
  //Not always set for a terminal node it does not produce anything.
  private ISymbol producesSymbolType = null;

  public StreamCallSymbol(final String name, final IScope enclosingScope) {

    super(name, enclosingScope);

  }

  @Override
  public StreamCallSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoStreamCallSymbol(new StreamCallSymbol(getName(), withParentAsAppropriate));
  }

  protected StreamCallSymbol cloneIntoStreamCallSymbol(final StreamCallSymbol newCopy) {

    super.cloneIntoMethodSymbol(newCopy);
    newCopy.consumesSymbolPromotionRequired = consumesSymbolPromotionRequired;
    newCopy.consumesSymbolType = consumesSymbolType;
    newCopy.producesSymbolType = producesSymbolType;
    newCopy.capableOfConsumingAnything = capableOfConsumingAnything;
    newCopy.derivesProducesTypeFromConsumesType = derivesProducesTypeFromConsumesType;
    newCopy.producesTypeMustBeAFunction = producesTypeMustBeAFunction;
    newCopy.producerSymbolTypeSameAsConsumerSymbolType = producerSymbolTypeSameAsConsumerSymbolType;
    newCopy.sinkInNature = sinkInNature;

    return newCopy;
  }

  public boolean isConsumesSymbolPromotionRequired() {

    return consumesSymbolPromotionRequired;
  }

  public void setConsumesSymbolPromotionRequired(final boolean consumesSymbolPromotionRequired) {

    this.consumesSymbolPromotionRequired = consumesSymbolPromotionRequired;
  }

  public boolean isSinkInNature() {

    return sinkInNature;
  }

  public void setSinkInNature(final boolean sinkInNature) {

    this.sinkInNature = sinkInNature;

  }

  public boolean isCapableOfConsumingAnything() {

    return capableOfConsumingAnything;
  }

  public void setCapableOfConsumingAnything(final boolean capableOfConsumingAnything) {

    this.capableOfConsumingAnything = capableOfConsumingAnything;
  }

  public void setProducerSymbolTypeSameAsConsumerSymbolType(final boolean producerSymbolTypeSameAsConsumerSymbolType) {

    this.producerSymbolTypeSameAsConsumerSymbolType = producerSymbolTypeSameAsConsumerSymbolType;

  }

  public boolean isDerivesProducesTypeFromConsumesType() {

    return derivesProducesTypeFromConsumesType;
  }

  public void setDerivesProducesTypeFromConsumesType(final boolean derivesProducesTypeFromConsumesType) {

    this.derivesProducesTypeFromConsumesType = derivesProducesTypeFromConsumesType;

  }

  public boolean isProducesTypeMustBeAFunction() {

    return producesTypeMustBeAFunction;
  }

  public void setProducesTypeMustBeAFunction(final boolean producesTypeMustBeAFunction) {

    this.producesTypeMustBeAFunction = producesTypeMustBeAFunction;

  }

  public ISymbol getConsumesSymbolType() {

    return consumesSymbolType;
  }

  /**
   * This sets the type of symbol that stream can consume.
   */
  public void setConsumesSymbolType(final ISymbol consumesSymbolType) {

    this.consumesSymbolType = consumesSymbolType;
    if (producerSymbolTypeSameAsConsumerSymbolType) {
      setProducesSymbolType(consumesSymbolType);
    }

  }

  public ISymbol getProducesSymbolType() {

    return producesSymbolType;
  }

  /**
   * This sets the type of symbol that stream can produce.
   */
  public void setProducesSymbolType(final ISymbol producesSymbolType) {

    this.producesSymbolType = producesSymbolType;
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    if ((o instanceof StreamCallSymbol that)
        && super.equals(o)
        && isConsumesSymbolPromotionRequired() == that.isConsumesSymbolPromotionRequired()
        && isCapableOfConsumingAnything() == that.isCapableOfConsumingAnything()
        && isDerivesProducesTypeFromConsumesType() == that.isDerivesProducesTypeFromConsumesType()
        && isProducesTypeMustBeAFunction() == that.isProducesTypeMustBeAFunction()
        && isSinkInNature() == that.isSinkInNature()
        && producerSymbolTypeSameAsConsumerSymbolType == that.producerSymbolTypeSameAsConsumerSymbolType) {
      if (getConsumesSymbolType() != null ? !getConsumesSymbolType().equals(that.getConsumesSymbolType()) :
          that.getConsumesSymbolType() != null) {
        return false;
      }

      return getProducesSymbolType() != null ? getProducesSymbolType().equals(that.getProducesSymbolType()) :
          that.getProducesSymbolType() == null;
    }
    return false;
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + (isConsumesSymbolPromotionRequired() ? 1 : 0);
    result = 31 * result + (getConsumesSymbolType() != null ? getConsumesSymbolType().hashCode() : 0);
    result = 31 * result + (getProducesSymbolType() != null ? getProducesSymbolType().hashCode() : 0);
    result = 31 * result + (isCapableOfConsumingAnything() ? 1 : 0);
    result = 31 * result + (isDerivesProducesTypeFromConsumesType() ? 1 : 0);
    result = 31 * result + (isProducesTypeMustBeAFunction() ? 1 : 0);
    result = 31 * result + (producerSymbolTypeSameAsConsumerSymbolType ? 1 : 0);
    result = 31 * result + (isSinkInNature() ? 1 : 0);

    return result;
  }
}