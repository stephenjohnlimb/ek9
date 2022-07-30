package org.ek9lang.compiler.symbol;

/**
 * Just re-uses the bulk of method symbol. This is only used in part of the stream pipeline.
 * We need additional information surrounding the type that can be accepted and the type that is produced (chained).
 * In some cases (like a call to head) we can accept anything in and will produce the same back out.
 * But a filter can only accept a certain type in - but will then also product that same type out.
 * A map on the other hand will only accept a certain type and will product another type (maybe - depending on the map).
 * Flatten can only accept something is can iterate over and will then product whatever that type is that is provided by the iterator.
 */
public class StreamCallSymbol extends MethodSymbol implements IAssignableSymbol
{
	//For mid-part of a stream pipeline both of these types will be set.
	//Not sure about 'tee' it is an anomaly in some ways - consumes yes
	//but also can map to a terminal, but also pass on the type it consumes!

	//Maybe the match on the '|' pipe requires a promotion.
	private boolean consumesSymbolPromotionRequired = false;

	//Not always set for a main stream head consumes will be null
	private ISymbol consumesSymbolType = null;
	//Not always set for a terminal node it does not produce anything.
	private ISymbol producesSymbolType = null;

	//For head, skip, tail, flatten, call and async - we're not bothered what type we get
	//indeed this 'command' has no way of knowing what it will/should be passed.
	//So it does need to be told, so it can also say what it produces.
	//So I'm expecting the pipeline to go through and tell these type of calls what they will be consuming.
	//So that logic will need to also check a couple of things for flatten, call and async as those do have constraints.
	private boolean capableOfConsumingAnything = false;

	/**
	 * For flatten the consumes must have an iterator and the call would produce N of the type the iterator supplies.
	 */
	private boolean derivesProducesTypeFromConsumesType = false;

	private boolean producesTypeMustBeAFunction = false;

	/**
	 * In the case of something like filter, head, tail, skip whatever is consumed is also produced (conditionally and maybe in limited volume)
	 */
	private boolean producerSymbolTypeSameAsConsumerSymbolType = false;

	/**
	 * Typically used in terminal but also tee.
	 * This indicates that when a pipeline type is known in the pipeline itself we really
	 * want to check if this sort of call is capable of receiving it in some way via the '|' pipe operator.
	 * Originally I was only going to allow one type of object to be received via pipe, but if we can support
	 * multiple '|' pipe operators it means we have more flexibility and can reduce the number of map functions required.
	 */
	private boolean sinkInNature = false;

	public StreamCallSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
	}

	@Override
	public StreamCallSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoStreamCallSymbol(new StreamCallSymbol(getName(), withParentAsAppropriate));
	}

	protected StreamCallSymbol cloneIntoStreamCallSymbol(StreamCallSymbol newCopy)
	{
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

	public boolean isConsumesSymbolPromotionRequired()
	{
		return consumesSymbolPromotionRequired;
	}

	public void setConsumesSymbolPromotionRequired(boolean consumesSymbolPromotionRequired)
	{
		this.consumesSymbolPromotionRequired = consumesSymbolPromotionRequired;
	}

	public boolean isSinkInNature()
	{
		return sinkInNature;
	}

	public void setSinkInNature(boolean sinkInNature)
	{
		this.sinkInNature = sinkInNature;
	}

	public boolean isCapableOfConsumingAnything()
	{
		return capableOfConsumingAnything;
	}

	public void setCapableOfConsumingAnything(boolean capableOfConsumingAnything)
	{
		this.capableOfConsumingAnything = capableOfConsumingAnything;
	}

	public void setProducerSymbolTypeSameAsConsumerSymbolType(boolean producerSymbolTypeSameAsConsumerSymbolType)
	{
		this.producerSymbolTypeSameAsConsumerSymbolType = producerSymbolTypeSameAsConsumerSymbolType;
	}

	public boolean isDerivesProducesTypeFromConsumesType()
	{
		return derivesProducesTypeFromConsumesType;
	}

	public void setDerivesProducesTypeFromConsumesType(boolean derivesProducesTypeFromConsumesType)
	{
		this.derivesProducesTypeFromConsumesType = derivesProducesTypeFromConsumesType;
	}

	public boolean isProducesTypeMustBeAFunction()
	{
		return producesTypeMustBeAFunction;
	}

	public void setProducesTypeMustBeAFunction(boolean producesTypeMustBeAFunction)
	{
		this.producesTypeMustBeAFunction = producesTypeMustBeAFunction;
	}

	public ISymbol getConsumesSymbolType()
	{
		return consumesSymbolType;
	}

	public void setConsumesSymbolType(ISymbol consumesSymbolType)
	{
		this.consumesSymbolType = consumesSymbolType;

		if(producerSymbolTypeSameAsConsumerSymbolType)
			setProducesSymbolType(consumesSymbolType);
	}

	public ISymbol getProducesSymbolType()
	{
		return producesSymbolType;
	}

	public void setProducesSymbolType(ISymbol producesSymbolType)
	{
		this.producesSymbolType = producesSymbolType;
	}
}