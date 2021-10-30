package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.files.Module;
import org.ek9lang.compiler.symbol.support.ITokenReference;

import java.util.Optional;

public interface ISymbol extends ITokenReference
{
	/*
	 * Typically used on aggregates because we might use a AggregateSymbol
	 * But when coming to process it we need to ensure other aggregate symbols that extend
	 * it are of compatible genus i.e a class can only extend a base class not a style
	 * but one style could extend another style.
	 * We can also use this information when parsing the structure and then doing the semantic
	 * analysis before the IR node generation. We can modify output by using this information.
	 */
	enum SymbolGenus {
		GENERAL_APPLICATION,
		SERVICE_APPLICATION,
		COMPONENT,
		VALUE,
		CLASS,
		CLASS_TRAIT,
		CLASS_CONSTRAINED,
		CLASS_ENUMERATION,
		RECORD,
		DECORATION,
		MEDIA,
		STYLE,
		TYPE,
		FUNCTION,
		FUNCTION_TRAIT,
		TEXT_BASE,
		TEXT,
		SERVICE
	}
	
	/**
	 * Typically used on symbols so we know their broad use and type.
	 * A Function is both a type and a method because we want to use it as a types variable but also call it.
	 * So we may need to alter these methods below to treat a function also as a type and a method.
	 * A control is an example of a switch statement that can return a value - we may add others.
	 */
	enum SymbolCategory {
		TYPE,
		TEMPLATE_TYPE, //This is a definition of the template once MyTemplate with type of T is made read as a type MyTemplate of Integer it becomes a TYPE
		METHOD,
		TEMPLATE_FUNCTION, // As per the TEMPLATE_TYPE once made concrete becomes a FUNCTION when used with concrete parameters - has unique name in the combination.
		FUNCTION,
		CONTROL,
		VARIABLE
	}

	/**
	 * Typically used with constants some are just general use constants
	 * But others are defined at module scope and we may and do need to treat these differently in some contexts.
	 * @return true if at module scope false otherwise.
	 */
	default boolean isAtModuleScope() { return false; }

	//Used for declarations of variables/params where they can be null.
	boolean isNullAllowed();

	void setNullAllowed(boolean nullAllowed);

	boolean isInjectionExpected();

	void setInjectionExpected(boolean injectionExpected);

	//Bits of information we might need to put away for later processing
	void putSquirrelledData(String key, String value);
		
	String getSquirrelledData(String key);
	
	/**
	 * For some symbols you may wish to specify the parsed module they were defined in.
	 * @param parsedModule The parsedModule the symbol was defined in.
	 */
	default void setParsedModule(Module parsedModule) {}
	
	default Module getParsedModule() { return null ;}
	
	/**
	 * Where was this symbol defined if known.
	 * @return The location maybe a file or a stream.
	 */
	default String getSourceFileLocation() { return ""; }

	default boolean isDevSource() { return false; }
	
	default boolean isLibSource() { return false; }
	
	/**
	 * Clone the symbol and re-parent if this symbol like a method should have a parent.
	 * Other symbols like VariableSymbols are un-parented
	 * @param withParentAsAppropriate
	 * @return a new instance of the symbol.
	 */
	default ISymbol clone(IScope withParentAsAppropriate) {return null; }
	
	/**
	 * So just to add to the confusion.
	 * A type that has been used with a type of a generic nature with some parameters.
	 * So this would normally result in a type that is now concrete. i.e List (generic in nature) and String (concrete) so
	 * This resulting type 'List of String' isAParameterisedType=true and isGenericInNature=false.
	 * But if List were parameterised with another generic parameter it would still be of a generic nature.
	 * For example, consider List<T> and Iterator<I> when we defined List<T> and use the Iterator with it we parameterise Iterator with 'T'. But they are
	 * still both 'isGenericTypeParameter' it only when we use List (and by implication Iterator) with String do they both stop being generic in nature.
	 * @return true if a parameterised type (note parameterised not just of a generic nature).
	 */
	default boolean isAParameterisedType() { return false; }
	
	default boolean isParameterisedWrappingRequired() { return false; }
	
	/**
	 * Is this symbol type of a generic nature i.e can it be parameterised with types.
	 * @return false by default. Aggregate Symbols can be defines to accept one or more parameters ie S and T. 
	 */
	default boolean isGenericInNature() { return false; }
	
	/**
	 * Some symbols are simulated as generic type parameters like T and S and U for example
	 * When they are constructed as AggregateTypes this will be set to true in those classes.
	 * In general this is useful when working with Generic classes and needing types like S and T
	 * But they can never really be generated.
	 * @return by default false.
	 */
	default boolean isGenericTypeParameter() { return false; }
	
	/**
	 * This symbol itself can be marked as pure - i.e an operator with no side effects.
	 * @return true if pure, false otherwise. By default false - lets assume the worst.
	 */
	default boolean isMarkedPure() { return false; }
	
	/**
	 * Not typically used for type symbols - but for Variables and Functions - this does make sense
	 * if the context of the use is 'pure' then only 'pure' calls can be made.
	 * @return true if only pure calls can be made.
	 */
	default boolean isRestrictedToPureCalls() { return false; }
	
	/**
	 * Is this symbol itself restricted to only make pure calls - typically used on an aggregate type variable
	 * So that only pure method calls can be made.
	 */
	default void setRestrictedToPureCalls(boolean restrictedToPureCalls) { };
	
	default boolean isMutable() { return false; }
	
	default void setNotMutable() {};

	default boolean isPrivate() { return false; }
	
	default boolean isProtected() { return false; }
	
	default boolean isPublic() { return true; }
	
	default public boolean isIncomingParameter() { return false; }
	
	default void setIncomingParameter(boolean incomingParameter) { };
	
	default public boolean isReturningParameter() { return false; }
	
	default void setReturningParameter(boolean returningParameter) {};
	
	default boolean isATemplateType() { return getCategory().equals(SymbolCategory.TEMPLATE_TYPE); }
	
	default boolean isATemplateFunction() { return getCategory().equals(SymbolCategory.TEMPLATE_FUNCTION); }
	
	default boolean isAType() { return getCategory().equals(SymbolCategory.TYPE); }
	
	default boolean isAVariable() { return getCategory().equals(SymbolCategory.VARIABLE); }
	
	//Special type of variable one that is a property on an aggregate.
	default boolean isAggregatePropertyField() { return false; }
	
	default boolean isAnApplication()
	{
		switch(getGenus())
		{
			case GENERAL_APPLICATION:
			case SERVICE_APPLICATION:
				return true;
			default:
				return false;
		}		
	}
	
	default boolean isAMethod() { return getCategory().equals(SymbolCategory.METHOD); }
	
	default boolean isAFunction() { return getCategory().equals(SymbolCategory.FUNCTION); }
	
	default boolean isAControl() { return getCategory().equals(SymbolCategory.CONTROL); }
	
	default boolean isAConstant() { return false; }
	
	default boolean isFromLiteral() { return false; }
	
	default boolean isMarkedAbstract() { return false; }
	
	/**
	 * Some classes generated can be injected and others not.
	 * 
	 * @return true if this can be injected, false if not.
	 */
	default boolean isInjectable() { return false; }
	
	/**
	 * While this aggregate itself might not able been marked as injectable
	 * Does this extend an aggregate that is marked as injectable.
	 * @return true if this is injectable or any of its supers area
	 */
	default boolean isExtensionOfInjectable() { return false ; }

	/**
	 * By default isExactSameType, but can be used if symbols are a conceptual match like generics can be.
	 */
	default boolean isSymbolTypeMatch(ISymbol symbolType) { return isExactSameType(symbolType); }
	
	/**
	 * If is the symbol is an exact match.
	 */
	boolean isExactSameType(ISymbol symbolType) ;
	
	/**
	 * For some symbols we might support the _promote method via coercion.
	 * i.e Long -> Float for example
	 * But with class structures and traits/interfaces there is a time when
	 * objects are assignable because of base and super classes/types but don't need coercion.
	 * The isAssignable deals with mingling both coercion and super/trait type compatibility.
	 * But we need to know when it comes to IR generation whether or not to include a PromoteNode
	 * or whether the code generated will just work because of class inheritance/interface implementation.
	 */
	default boolean isPromotionSupported(ISymbol s) { return false; }
	
	default boolean isAssignableTo(ISymbol s) { return false; }
	
	default boolean isAssignableTo(Optional<ISymbol> s) { return false; }	
	
	default double getAssignableWeightTo(Optional<ISymbol> s) { return -1.0; }
		
	default double getAssignableWeightTo(ISymbol s) { return -1.0;}
	
	default double getUnCoercedAssignableWeightTo(ISymbol s) { return -1.0;}
	
	/**
	 * Assuming you've already checked that this isAssignableTo (s)
	 * Then you can call this to check if this ability was dues to coercion. 
	 */
	default boolean isOnlyAssignableByCoercion(ISymbol s)
	{
		double uncoercedWeight = getUnCoercedAssignableWeightTo(s);
		return uncoercedWeight < 0 && isPromotionSupported(s);
	}
	
	void setGenus(SymbolGenus genus);
	
	default SymbolGenus getGenus() { return SymbolGenus.CLASS; };
	
	SymbolCategory getCategory();
	
	/**
	 * Was the symbol defined from the outset in a fully qualified manner.
	 * i.e was is item as 'com.items.specials:GoodThing' or just 'GoodThing'
	 * @return
	 */
	default boolean isDefinedInAFullyQualifiedManner()
	{
		return getName().contains("::");
	}
	
	/**
	 * Provide the internal fully qualified name of this symbol - defaults to just the name unless overridden
	 * @return The fully qualified internal name - useful for generating output where you want to ensure fully qualified names are used.
	 */
	default String getFullyQualifiedName()
	{
		return getName();
	}
	
	/**
	 * Provide the name an end user would need to see on the screen. Normally this is just 'getName' but
	 * in the case of Templates We use a very nasty internal naming for List of SomeClass - which will probably be
	 * something like _List_hashed_version_of_ComeClass and the end user needs to see 'List of SomeClass' for it to be meaningful.
	 *  
	 * @return a user presentable of the symbol name.
	 */
	default String getFriendlyName()
	{
		return getName();
	}
	
	/**
	 * Provide the internal name of this symbol - not fully qualified in terms of the module it is in.
	 * @return The basic internal name we'd use to resolve the symbol
	 */
	String getName();

	Optional<ISymbol> getType();

	default ISymbol setType(ISymbol type) { return setType(Optional.ofNullable(type)); }
		
	ISymbol setType(Optional<ISymbol> type);
}
