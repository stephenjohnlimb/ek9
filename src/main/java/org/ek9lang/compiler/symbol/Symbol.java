package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

import org.ek9lang.compiler.files.Module;
import org.ek9lang.compiler.symbol.support.TypeCoercions;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.ek9lang.core.exception.AssertValue;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A bit of a beast.
 * Is extended widely for particular types of symbol.
 */
public class Symbol implements ISymbol
{
	//This also covers constants - change mutability for those.
	private SymbolCategory category = SymbolCategory.VARIABLE;
	
	private SymbolGenus genus = SymbolGenus.CLASS;
	
	private String name;
	
	private Optional<ISymbol> type = Optional.empty();

	private boolean hasBeenSet = false;

	/**
	 * Not always set if ek9 core.
	 * But is the token where this symbol was defined.
	 */
	private Token sourceToken = null;

	/**
	 * This is the token that initialised the symbol.
	 * Typically, ony valid for Variables and expressions.
	 */
	private Token initialisedBy = null;

	/** Was this symbol referenced. */
	private boolean referenced = false;

	/**
	 * The idea is to assume that symbol cannot be null by default.
	 * i.e a variable cannot null or a function/method cannot return a null value
	 * If the developer wants to support then ? has to be used to make that explicit. 
	 */
	private boolean nullAllowed = false;

	//For components, we need to know if injection is expected.
	private boolean injectionExpected = false;

	/** 
	 * Used to mark a symbol as ek9 core or not.
	 * Normally used to drive the generation of fully qualified names.
	 */
	private boolean ek9Core = false;

	/**
	 * Is this symbol marked as being pure.
	 */
	private boolean markedPure = false;

	private boolean produceFullyQualifiedName = false;
	
	/**
	 * Where this symbol come from - not always set for every symbol.
	 */
	private Optional<Module> parsedModule = Optional.empty();
	

	//Sometimes it is necessary to pick data and hold it in the symbol for later.
	private final Map<String, String> squirrelledAway = new HashMap<>();

	public Symbol(String name)
	{
		this.setName(name);
	}

	public Symbol(String name, Optional<ISymbol> type)
	{
		this(name);
		this.setType(type);
	}

	@Override
	public Symbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoSymbol(new Symbol(this.getName(), this.getType()));
	}

	protected Symbol cloneIntoSymbol(Symbol newCopy)
	{
		newCopy.setInitialisedBy(this.getInitialisedBy());
		newCopy.setCategory(this.getCategory());
		newCopy.setGenus(this.getGenus());
		newCopy.setName(this.getName());
		newCopy.hasBeenSet = this.hasBeenSet;
		newCopy.setNullAllowed(this.isNullAllowed());
		newCopy.setInjectionExpected(this.isInjectionExpected());
		newCopy.setEk9Core(this.isEk9Core());
		newCopy.setMarkedPure(this.isMarkedPure());
		newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
		parsedModule.ifPresent(newCopy::doSetModule);
		newCopy.setSourceToken(this.getSourceToken());
		newCopy.squirrelledAway.putAll(this.squirrelledAway);
		newCopy.setReferenced(this.isReferenced());
		if(this instanceof AggregateSymbol)
		{
			//We don't copy over any type as the aggregate IS the type
		}
		else
		{
			getType().ifPresent(newCopy::setType);
		}
		return newCopy;
	}

	public boolean isMarkedPure()
	{
		return markedPure;
	}

	public void setMarkedPure(boolean markedPure)
	{
		this.markedPure = markedPure;
	}

	public Token getInitialisedBy()
	{
		return initialisedBy;
	}

	public void setInitialisedBy(Token initialisedBy)
	{
		this.initialisedBy = initialisedBy;
	}

	public boolean isReferenced()
	{
		return referenced;
	}

	public void setReferenced(boolean referenced)
	{
		this.referenced = referenced;
	}

	public void putSquirrelledData(String key, String value)
	{
		if(value.startsWith("\"") && value.endsWith("\""))
		{
			//We only store the value that is in the Quotes not the quotes themselves
			Pattern p = Pattern.compile("\"(.*)\""); //just the contents in the quotes
			Matcher m = p.matcher(value);
			if (m.find())
				value = m.group(1);
		}
		squirrelledAway.put(key, value);
	}
	
	public String getSquirrelledData(String key)
	{
		return squirrelledAway.get(key);
	}

	@Override
	public boolean isMutable()
	{
		return !hasBeenSet;
	}

	@Override
	public void setNotMutable()
	{
		hasBeenSet = true;
	}

	public boolean isNullAllowed()
	{
		return nullAllowed;
	}

	public void setNullAllowed(boolean nullAllowed)
	{
		this.nullAllowed = nullAllowed;
	}

	public boolean isInjectionExpected()
	{
		return injectionExpected;
	}

	public void setInjectionExpected(boolean injectionExpected)
	{
		this.injectionExpected = injectionExpected;
	}

	public boolean isEk9Core()
	{
		return ek9Core;
	}

	public void setEk9Core(boolean ek9Core)
	{
		this.ek9Core = ek9Core;
		if(ek9Core)
			setSourceToken(new SyntheticToken());
	}
	
	public boolean isDevSource()
	{
		return parsedModule.map(m -> m.getSource().isDev()).orElseGet(() -> false);
	}

	public boolean isLibSource()
	{
		return parsedModule.map(m -> m.getSource().isLib()).orElseGet(() -> false);
	}

	public void setProduceFullyQualifiedName(boolean produceFullyQualifiedName)
	{
		this.produceFullyQualifiedName = produceFullyQualifiedName;
	}
	
	public boolean getProduceFullyQualifiedName()
	{
		return produceFullyQualifiedName;
	}

	public Token getSourceToken()
	{
		return sourceToken;
	}

	public void setSourceToken(Token sourceToken)
	{
		this.sourceToken = sourceToken;
	}
	
	public Optional<Module> getParsedModule()
	{
		return parsedModule;
	}

	public void setParsedModule(Optional<Module> module)
	{
		module.ifPresent(this::doSetModule);
	}

	private void doSetModule(Module module)
	{
		if(parsedModule.isEmpty() && !isEk9Core())
			parsedModule = Optional.of(module);
	}

	public String getFullyQualifiedName()
	{
		String rtn = getName();
		if(rtn.contains("::"))
			return rtn; //already qualified in how it was constructed.

		return parsedModule.map(m -> m.getScopeName() + "::" + rtn).orElse(rtn);
	}

	public boolean isExactSameType(ISymbol symbolType)
	{
		return sameCategory(this, symbolType) && getFullyQualifiedName().equals(symbolType.getFullyQualifiedName());
	}

	private boolean sameCategory(ISymbol c1, ISymbol c2)
	{
		return c1.getCategory().equals(c2.getCategory());
	}

	public boolean isAssignableTo(ISymbol s)
	{
		return getAssignableWeightTo(s) >= 0.0;
	}
	
	public boolean isAssignableTo(Optional<ISymbol> s)
	{
		return getAssignableWeightTo(s) >= 0.0;
	}
	
	public double getAssignableWeightTo(Optional<ISymbol> s)
	{
		return s.map(this::getAssignableWeightTo).orElse(-1000000.0);
	}
	
	@Override
	public boolean isPromotionSupported(ISymbol s)
	{
		//Check if any need to promote might be same type
		if(!isExactSameType(s))
			return TypeCoercions.get().isCoercible(this, s);
		return false;
	}
	
	public double getAssignableWeightTo(ISymbol s)
	{
		double canAssign = getUnCoercedAssignableWeightTo(s);
		//Well if not the same symbol can we coerce/promote?
		if(canAssign < 0.0 && TypeCoercions.get().isCoercible(this, s))
			canAssign = 0.5;
		
		return canAssign;
	}
	
	public double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		double canAssign = -1000000.0;
		AssertValue.checkNotNull("Symbol cannot be null", s);
		
		if(isExactSameType(s))
			canAssign = 0.0;			
		return canAssign;
	}
	
	public SymbolCategory getCategory()
	{
		return category;
	}

	protected void setCategory(SymbolCategory category)
	{
		this.category = category;
	}
	
	public SymbolGenus getGenus()
	{
		return genus;
	}

	public void setGenus(SymbolGenus genus) 
	{
		this.genus = genus;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ISymbol)
			return getFriendlyName().equals(((ISymbol)obj).getFriendlyName());
		return false;
	}

	@Override
	public String getFriendlyName()
	{
		String theType = getSymbolTypeAsString(this.getType());
		if(theType.length() > 0)
			return  getName() + " as " + theType;
		return getName();
	}

	protected String getSymbolTypeAsString(Optional<ISymbol> aType)
	{
		if(aType.isPresent())
		{
			ISymbol theType = aType.get();
			//avoid self
			if(this != theType)
				return theType.getFriendlyName();
			return "";
		}
		return "Unknown";
	}

	@Override
	public String toString()
	{
		return getFriendlyName();
	}

	@Override
	public void setName(String name)
	{
		AssertValue.checkNotNull("Name cannot be null", name);
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public Optional<ISymbol> getType()
	{
		return type;
	}

	@Override
	public ISymbol setType(Optional<ISymbol> type)
	{
		AssertValue.checkNotNull("SymbolType cannot be null", type);
		this.type = type;
		return this;
	}

	@Override
	public boolean isAParameterisedType()
	{
		return false;
	}

	@Override
	public boolean isGenericInNature()
	{
		return false;
	}

	@Override
	public boolean isGenericTypeParameter()
	{
		return false;
	}

}
