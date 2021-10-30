package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

import org.ek9lang.compiler.files.Module;
import org.ek9lang.compiler.symbol.support.TypeCoercions;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.ek9lang.core.exception.AssertValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Symbol implements ISymbol
{
	//This also covers constants - change mutability for those.
	private SymbolCategory category = SymbolCategory.VARIABLE;
	
	private SymbolGenus genus = SymbolGenus.CLASS;
	
	private String name;
	
	private Optional<ISymbol> type = Optional.ofNullable(null);

	private boolean hasBeenSet = false;

	/**
	 * The idea is to assume that symbol cannot be null by default.
	 * i.e a variable cannot null or a function/method cannot return a null value
	 * If the developer wants to support then ? has to be used to make that explicit. 
	 */
	private boolean nullAllowed = false;

	//For components we need to know if injection is exected.
	private boolean injectionExpected = false;

	/** 
	 * Used to mark a symbol as ek9 core or not.
	 * Normally used to drive the generation of fully qualified names.
	 */
	private boolean ek9Core = false;
		
	private boolean produceFullyQualifiedName = false;
	
	/**
	 * Where this this symbol come from - not always set for every symbol.
	 */
	private Module parsedModule;
	
	/**
	 * Not always set if ek9 core.
	 * But is the token where this symbol was defined.
	 */
	private Token sourceToken = null;

	/**
	 * This is the token that initialised the sysmbol.
	 * Typically ony valid for Variables and expressions.
	 */
	private Token initialisedBy = null;

	//Sometimes it is necessary to pick data and hold it in the symbol for later.
	private Map<String, String> squirrelledAway = new HashMap<String, String>();

	public Symbol(String name)
	{
		this.setName(name);
	}
	
	public Symbol(String name, ISymbol type)
	{
		this(name);
		this.setType(type);
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
		newCopy.initialisedBy = initialisedBy;
		newCopy.category = this.category;
		newCopy.genus = this.genus;
		newCopy.name = this.name;
		newCopy.hasBeenSet = this.hasBeenSet;
		newCopy.nullAllowed = this.nullAllowed;
		newCopy.injectionExpected = this.injectionExpected;
		newCopy.ek9Core = this.ek9Core;
		newCopy.produceFullyQualifiedName = this.produceFullyQualifiedName;
		newCopy.parsedModule = this.parsedModule;
		newCopy.sourceToken = this.sourceToken;
		newCopy.squirrelledAway.putAll(this.squirrelledAway);
		return newCopy;
	}

	public Token getInitialisedBy()
	{
		return initialisedBy;
	}

	public void setInitialisedBy(Token initialisedBy)
	{
		this.initialisedBy = initialisedBy;
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
		setSourceToken(new SyntheticToken());
	}
	
	public boolean isDevSource()
	{
		if(parsedModule != null)
			return parsedModule.getSource().isDev();
		return false;
	}

	public boolean isLibSource()
	{
		if(parsedModule != null)
			return parsedModule.getSource().isLib();
		return false;
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
	
	public Module getParsedModule()
	{
		return parsedModule;
	}

	public void setParsedModule(Module parsedModule)
	{		
		//only set if not already set and not ek9 core.
		if(this.parsedModule == null && !isEk9Core())
			this.parsedModule = parsedModule;
	}
	
	public String getFullyQualifiedName()
	{
		String rtn = getName();
		if(rtn.contains("::"))
			return rtn; //already qualified in how it was constructed.
		
		if(produceFullyQualifiedName && parsedModule != null)
			return parsedModule.getScopeName() + "::" + getName();
		return rtn;
	}
	
	@Override
	public String getSourceFileLocation()
	{
		if(isEk9Core())
			return "_ek9/global/builtin.ek9";
		
		if(parsedModule != null && parsedModule.getSource() != null)
			return parsedModule.getSource().getFileName();
	
		return ISymbol.super.getSourceFileLocation();
	}

	public boolean isExactSameType(ISymbol symbolType)
	{
		String thisName = this.getName();
		String symbolTypeName = symbolType.getName();
		
		if(this.isAType() && symbolType.isAType())
		{
			//if the same symbol physical memory or name is the same
			return this == symbolType || thisName.equals(symbolTypeName);							
		}
		else if(this.isATemplateType() && symbolType.isATemplateType())
		{
			//if the same symbol physical memory or name is the same
			return this == symbolType || thisName.equals(symbolTypeName);							
		}
		else if(this.isATemplateFunction() && symbolType.isATemplateFunction())
		{
			//if the same symbol physical memory or name is the same
			return this == symbolType || thisName.equals(symbolTypeName);							
		}
		else if(this.isAFunction() && symbolType.isAFunction())
		{
			//if the same symbol physical memory or name is the same
			return this == symbolType || thisName.equals(symbolTypeName);							
		}
		return false;
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
		if(!s.isPresent())
			return -1000000.0;
		return getAssignableWeightTo(s.get());
	}
	
	@Override
	public boolean isPromotionSupported(ISymbol s)
	{
		//Check if any need to promote might be same type
		if(isExactSameType(s))
			return false;
		
		return TypeCoercions.get().isCoercible(this, s);
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

	public String getName()
	{
		return name;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ISymbol)
			return getName().equals(((ISymbol)obj).getName());
		return false;
	}

	@Override
	public String getFriendlyName()
	{
		StringBuffer rtn = new StringBuffer(getName());
		
		if(this.getType().isPresent())
		{
			ISymbol theType = getType().get();
			//avoid self
			if(this != theType)
			{
				rtn.append(" as ");				
				rtn.append(theType.getFriendlyName());
			}
		}
		return rtn.toString();
	}
	
	@Override
	public String toString()
	{
		return getFriendlyName();
	}

	public void setName(String name)
	{
		AssertValue.checkNotNull("Name cannot be null", name);
		this.name = name;
	}

	public Optional<ISymbol> getType()
	{
		return type;
	}
	
	public ISymbol setType(Optional<ISymbol> type)
	{
		AssertValue.checkNotNull("SymbolType cannot be null", type);
		this.type = type;
		return this;
	}
}
