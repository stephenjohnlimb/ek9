package org.ek9lang.lsp;


import java.util.*;
import java.util.function.Function;

import org.ek9lang.compiler.tokenizer.TokenResult;

/**
 * Designed for us in the LSP only as a set of words that are part of the language.
 * But there is also some descriptive text that is provided with the work.
 * These can be used for code completion or for the hover functionality.
 * <p>
 * This might seem like a duplication of what ANTLR has generated, but it we need to supply
 * much more information around each word. This is to really help developers get started even very new developers.
 * <p>
 * In the future might make this functional optional as you get used to it you might now want the hover/help.
 * <p>
 * This only deals with simple single word completion and simple cases.
 */
public class EK9LanguageWords
{
	private Map<String, KeyWordInformation> keywordMap = new HashMap<>();

	public EK9LanguageWords()
	{
		setupKeyWords();
	}

	private void setupKeyWords()
	{
		keywordMap.put("#!ek9",
				new KeyWordInformation("An EK9 source code file, https://www.ek9.io/structure.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);

		keywordMap.put("defines",
				new KeyWordInformation("Define a module or construct block, https://www.ek9.io/structure.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);

		keywordMap.put("references",
				new KeyWordInformation("Reference construct from a module, https://www.ek9.io/structure.html#references",
						Arrays.asList("references\n"
						),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("module",
				new KeyWordInformation("Module declaration block, https://www.ek9.io/structure.html#module",
						Arrays.asList("module"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("constant",
				new KeyWordInformation("Constant declaration, https://www.ek9.io/constants.html",
						Arrays.asList("constant\n"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("program",
				new KeyWordInformation("Program declaration / use with applications, https://www.ek9.io/programs.html",
						Arrays.asList("program\n",
								"program application\n"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("type",
				new KeyWordInformation("Type/Enumeration/Generic declaration, https://www.ek9.io/enumerations.html",
						Arrays.asList("type",
								"type T",
								"type T constrain by"
						),
						search -> !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("function",
				new KeyWordInformation("Function declaration , https://www.ek9.io/functions.html",
						Arrays.asList("function"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("record",
				new KeyWordInformation("Record declaration, https://www.ek9.io/records.html",
						Arrays.asList("record"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("class",
				new KeyWordInformation("Class declaration, https://www.ek9.io/classes.html",
						Arrays.asList("class"
						),
						search -> !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("trait",
				new KeyWordInformation("Trait declaration/use, https://www.ek9.io/traits.html",
						Arrays.asList("trait"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("package",
				new KeyWordInformation("Package build/publishing control, https://www.ek9.io/packaging.html",
						Arrays.asList("package\n"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("text",
				new KeyWordInformation("Text construct for output, https://www.ek9.io/textProperties.html",
						Arrays.asList("text\n"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("component",
				new KeyWordInformation("Component declaration, https://www.ek9.io/components.html",
						Arrays.asList("component\n"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		keywordMap.put("application",
				new KeyWordInformation("Application declaration, https://www.ek9.io/structure.html#application",
						Arrays.asList("application\n",
								"application of"
						),
						search -> !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("service",
				new KeyWordInformation("Service declaration / use with application, https://www.ek9.io/webServices.html",
						Arrays.asList("service",
								"service application"
						),
						search -> search.previousTokenIsDefines()
				)
		);
		//Just joining words
		keywordMap.put("with",
				new KeyWordInformation("Linking stream mapping/if statements, use of traits/applications and aspects",
						Arrays.asList("with",
								"with trait of",
								"with application of",
								"with aspect of"
						),
						search -> !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("as",
				new KeyWordInformation("Declaration or linking",
						Arrays.asList("as",
								"as\n",
								"as open\n",
								"as abstract\n",
								"as function\n",
								"as class\n",
								"as dispatcher\n",
								"as pure\n",
								"as pure abstract\n",
								"as pure function\n",
								"as pure abstract\n",
								"as pure dispatcher\n",
								"as GET for :/",
								"as DELETE for :/",
								"as HEAD for :/",
								"as POST for :/",
								"as PUT for :/",
								"as PATCH for :/",
								"as OPTIONS for :/"
						),
						search -> !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("of",
				new KeyWordInformation("Used with Generic types, applications, aspects, traits and conditionally with streams, length/abs/sqrt",
						Arrays.asList("of",
								"of type"
						),
						search -> !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("override",
				new KeyWordInformation("Used when overriding methods/operators, https://www.ek9.io/methods.html",
						Arrays.asList("override"
						),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("public",
				new KeyWordInformation("Public declarations not required in EK9, https://www.ek9.io/basics.html#visibility",
						Arrays.asList(""
						),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("protected",
				new KeyWordInformation("Protected method modifier on classes, https://www.ek9.io/basics.html#class_visibility",
						Arrays.asList("protected"
						),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("private",
				new KeyWordInformation("Private method modifier, https://www.ek9.io/basics.html#visibility",
						Arrays.asList("private"
						),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("operator",
				new KeyWordInformation("Declaration of an operator (such as +-*/), https://www.ek9.io/operators.html",
						Arrays.asList("operator",
								"operator $",
								"operator $$",
								"operator >",
								"operator <",
								"operator :=",
								"operator !=",
								"operator <=",
								"operator >=",
								"operator <=>",
								"operator <~>",
								"operator :~:",
								"operator :=:",
								"operator :^:",
								"operator !",
								"operator ?",
								"operator ~",
								"operator ++",
								"operator --",
								"operator +",
								"operator -",
								"operator *",
								"operator /",
								"operator +=",
								"operator -=",
								"operator *=",
								"operator /=",
								"operator ^",
								"operator |",
								"operator #^",
								"operator #?",
								"operator #<",
								"operator #>",
								"operator mod",
								"operator rem",
								"operator abs",
								"operator sqrt",
								"operator contains",
								"operator matches",
								"operator empty",
								"operator length"
						),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsOverride()
				)
		);

		keywordMap.put("?",
				new KeyWordInformation("Is set, https://www.ek9.io/operators.html#ternary",
						Arrays.asList(
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("??",
				new KeyWordInformation("Null coalescing, https://www.ek9.io/operators.html#ternary",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("?:",
				new KeyWordInformation("Null is set coalescing (Elvis), https://www.ek9.io/operators.html#ternary",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("?=",
				new KeyWordInformation("Guarded assignment (if/when conditionals), https://www.ek9.io/flowControl.html#if_elseif_else",
						Arrays.asList(""),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("!=",
				new KeyWordInformation("Not Equals, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<>",
				new KeyWordInformation("Not Equals, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("=",
				new KeyWordInformation("Assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("==",
				new KeyWordInformation("Equals, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put(":",
				new KeyWordInformation("Assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(":=",
				new KeyWordInformation("Assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("::",
				new KeyWordInformation("References construct, https://www.ek9.io/basics.html#references_example",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(":=:",
				new KeyWordInformation("Deep copy, https://www.ek9.io/operators.html#modification",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(":~:",
				new KeyWordInformation("Merge, https://www.ek9.io/operators.html#modification",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(":^:",
				new KeyWordInformation("Replace, https://www.ek9.io/operators.html#modification",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(":=?",
				new KeyWordInformation("Assign if unset, https://www.ek9.io/operators.html#ternary",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("$",
				new KeyWordInformation("String conversion, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("$$",
				new KeyWordInformation("JSON conversion, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("->",
				new KeyWordInformation("Incoming parameter(s)",
						Arrays.asList(),
						search -> !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("<",
				new KeyWordInformation("Less than, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<-",
				new KeyWordInformation("Returning / declaration/ out of",
						Arrays.asList(),
						search -> !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<?",
				new KeyWordInformation("Null safe less than, https://www.ek9.io/operators.html#assignment_coalescing",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<=",
				new KeyWordInformation("Less than or equal to, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<=>",
				new KeyWordInformation("Comparison (space ship), https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<~>",
				new KeyWordInformation("Fuzzy Comparison, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<=?",
				new KeyWordInformation("Null safe less than or equal to, https://www.ek9.io/operators.html#assignment_coalescing",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("<<",
				new KeyWordInformation("Shift bits left",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put(">",
				new KeyWordInformation("Greater than / Add to a collection out of stream, https://www.ek9.io/operators.html#comparison ",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(">?",
				new KeyWordInformation("Null safe greater than, https://www.ek9.io/operators.html#assignment_coalescing",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(">=",
				new KeyWordInformation("Greater than or equal to, https://www.ek9.io/operators.html#comparison",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(">=?",
				new KeyWordInformation("Null safe greater than or equal to, https://www.ek9.io/operators.html#assignment_coalescing",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put(">>",
				new KeyWordInformation("Shift bits right / Append to a collection out of stream",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("+",
				new KeyWordInformation("Mathematical addition, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("-",
				new KeyWordInformation("Mathematical subtraction, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("*",
				new KeyWordInformation("Mathematical multiplication, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("/",
				new KeyWordInformation("Mathematical division, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("+=",
				new KeyWordInformation("Mathematical addition and assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("-=",
				new KeyWordInformation("Mathematical subtraction and assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("*=",
				new KeyWordInformation("Mathematical multiplication and assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("/=",
				new KeyWordInformation("Mathematical division and assignment, https://www.ek9.io/operators.html#assignment",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("++",
				new KeyWordInformation("Mathematical increment, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("--",
				new KeyWordInformation("Mathematical decrement, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("^",
				new KeyWordInformation("Mathematical power",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("|",
				new KeyWordInformation("Stream pipelines linkage",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("#^",
				new KeyWordInformation("Promotion, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("#?",
				new KeyWordInformation("Get hash code, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("#<",
				new KeyWordInformation("Get first, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("#>",
				new KeyWordInformation("Get last, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("mod",
				new KeyWordInformation("Mathematical modulus, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("rem",
				new KeyWordInformation("Remainder, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("abs",
				new KeyWordInformation("Absolute value, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("sqrt",
				new KeyWordInformation("mathematical Square Root, https://www.ek9.io/operators.html#mathematical",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("contains",
				new KeyWordInformation("Does container contain and item",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("matches",
				new KeyWordInformation("Does one item match another",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("empty",
				new KeyWordInformation("Has no content, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("length",
				new KeyWordInformation("Length, https://www.ek9.io/operators.html#functional",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("and",
				new KeyWordInformation("Boolean 'and' / Bitwise 'and'",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("or",
				new KeyWordInformation("Boolean 'or' / Bitwise 'or'",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("xor",
				new KeyWordInformation("Boolean 'exclusive or' / Bitwise 'exclusive or'",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("not",
				new KeyWordInformation("Boolean 'not' / Bitwise 'not'",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("~",
				new KeyWordInformation("Boolean 'not' /Bitwise 'not' / reverse a list",
						Arrays.asList(),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("assert",
				new KeyWordInformation("Assert a statement is true, Exception is thrown when false",
						Arrays.asList("assert", "assert()"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("by",
				new KeyWordInformation("Generic/Type constraint. For loop incrementer. Trait delegation, Pipeline",
						Arrays.asList("by"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		//Now the flow control items
		keywordMap.put("if",
				new KeyWordInformation("Conditional if/when, https://www.ek9.io/flowControl.html#switch",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("switch",
				new KeyWordInformation("Conditional switch/given, https://www.ek9.io/flowControl.html#switch",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("given",
				new KeyWordInformation("Conditional switch/given, https://www.ek9.io/flowControl.html#switch",
						Arrays.asList("given"),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("case",
				new KeyWordInformation("Conditional switch/case condition, https://www.ek9.io/flowControl.html#switch",
						Arrays.asList("case"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("default",
				new KeyWordInformation("Default switch/given condition, https://www.ek9.io/flowControl.html#switch",
						Arrays.asList("default\n"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("when",
				new KeyWordInformation("Conditional if/when/case",
						Arrays.asList("when")
				)
		);
		keywordMap.put("else",
				new KeyWordInformation("Conditional else, https://www.ek9.io/flowControl.html#if_elseif_else",
						Arrays.asList("else"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("do",
				new KeyWordInformation("Do loop, https://www.ek9.io/flowControl.html#do_while_loop",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("while",
				new KeyWordInformation("While loop, https://www.ek9.io/flowControl.html#while_loop",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);

		keywordMap.put("try",
				new KeyWordInformation("Try/catch expection block, https://www.ek9.io/exceptions.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("catch",
				new KeyWordInformation("Catch expection block, https://www.ek9.io/exceptions.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("handle",
				new KeyWordInformation("Handle exception block, https://www.ek9.io/exceptions.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("finally",
				new KeyWordInformation("Finally exception block, https://www.ek9.io/exceptions.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst()
				)
		);

		keywordMap.put("for",
				new KeyWordInformation("For loop / for stream / text for language / service for URI",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("pure",
				new KeyWordInformation("Limit reassignment of variables/side effects, https://www.ek9.io/basics.html#pure",
						Arrays.asList("pure"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("abstract",
				new KeyWordInformation("Non concrete functions/components/records/classes/methods/operators",
						Arrays.asList("abstract"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);

		keywordMap.put("allow",
				new KeyWordInformation("Limit reuse by extension, https://www.ek9.io/traits.html",
						Arrays.asList("allow"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("is",
				new KeyWordInformation("Extend function/class/component/record (or logic), https://www.ek9.io/inheritance.html",
						Arrays.asList("is", "is not", "is in", "is not in"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("open",
				new KeyWordInformation("Leave open for extension, https://www.ek9.io/inheritance.html",
						Arrays.asList("open"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("extends",
				new KeyWordInformation("Extend a function/class/component/record, https://www.ek9.io/inheritance.html",
						Arrays.asList("extends"),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("constrain",
				new KeyWordInformation("Restrict an existing type / Control generic type",
						Arrays.asList("constrain by", "constrain as"),
						search -> !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("register",
				new KeyWordInformation("Register a component instance for injection, https://www.ek9.io/dependencyInjection.html",
						Arrays.asList("register"),
						search -> search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("dispatcher",
				new KeyWordInformation("Dispatcher method for calling polymorphic objects, https://www.ek9.io/advancedClassMethods.html",
						Arrays.asList("dispatcher"),
						search -> !search.previousTokensIndentsOrFirst()
				)
		);
		keywordMap.put("cat",
				new KeyWordInformation("Catenate stream (start streaming), https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList(),
						search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()
				)
		);
		keywordMap.put("filter",
				new KeyWordInformation("Filter/Select items to be passed through stream, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("filter",
								"filter with",
								"filter by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("select",
				new KeyWordInformation("Filter/Select items to be passed through, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("select",
								"select with",
								"select by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("map",
				new KeyWordInformation("Map item into different type, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("map",
								"map with",
								"map by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("sort",
				new KeyWordInformation("Sort items with a sorting function, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("sort",
								"sort with",
								"sort by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("group",
				new KeyWordInformation("Group on an item property, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("group",
								"group with",
								"group by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("join",
				new KeyWordInformation("Join items, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("join",
								"join with",
								"join by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("split",
				new KeyWordInformation("Split on an item property, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("split",
								"split with",
								"split by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("uniq",
				new KeyWordInformation("Unique on an item property, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("uniq",
								"uniq with",
								"uniq by"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("tee",
				new KeyWordInformation("Tee off items to separate output, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("tee",
								"tee with",
								"tee by",
								"tee in"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("flatten",
				new KeyWordInformation("Flatten - take items out of collections and stream, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("flatten"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("call",
				new KeyWordInformation("Call delegate, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("call"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("async",
				new KeyWordInformation("Asynchronously call delegate, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("async"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("skip",
				new KeyWordInformation("Skip one or more items by removing from the stream, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("skip",
								"skip by",
								"skip of",
								"skip only"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("head",
				new KeyWordInformation("Head only - stop streaming after N, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("head",
								"head by",
								"head of",
								"head only"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("tail",
				new KeyWordInformation("Tail only last N, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("tail",
								"tail by",
								"tail of",
								"tail only"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		keywordMap.put("collect",
				new KeyWordInformation("Collect items from stream into a collection, https://www.ek9.io/streamsAndPipelines.html",
						Arrays.asList("collect as"
						),
						search -> search.previousTokenIsPipe()
				)
		);
		//Web Services
		keywordMap.put("GET",
				new KeyWordInformation("HTTP GET verb (idempotent)",
						Arrays.asList("GET :/", "GET for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("DELETE",
				new KeyWordInformation("HTTP DELETE verb (requires pre-condition checks)",
						Arrays.asList("DELETE :/", "DELETE for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("HEAD",
				new KeyWordInformation("HTTP HEAD verb (idempotent)",
						Arrays.asList("HEAD :/", "HEAD for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("POST",
				new KeyWordInformation("HTTP POST verb (create new content)",
						Arrays.asList("POST :/", "POST for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("PUT",
				new KeyWordInformation("HTTP PUT verb (update/replace existing content)",
						Arrays.asList("PUT :/", "PUT for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("PATCH",
				new KeyWordInformation("HTTP PATCH verb (partial update to existing content)",
						Arrays.asList("PATCH :/", "PATCH for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
		keywordMap.put("OPTIONS",
				new KeyWordInformation("HTTP OPTIONS verb (check what verbs are supported)",
						Arrays.asList("OPTIONS :/", "OPTIONS for :/"
						),
						search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()
				)
		);
	}

	public KeyWordInformation exactMatch(TokenResult search)
	{
		KeyWordInformation rtn = keywordMap.get(search.getToken().getText());
		return rtn;
	}

	/**
	 * Typically used for completion, where string is partial.
	 * Receive a TokenResults so we can also see position in the line.
	 * This will affect some of the search results.
	 */
	public List<String> fuzzyMatch(TokenResult search)
	{
		List<String> rtn = new ArrayList<>();

		//System.err.println("Searching for [" + search.getToken().getText() + "]");
		for(String word : keywordMap.keySet())
		{
			if(word.startsWith(search.getToken().getText()))
			{
				KeyWordInformation info = keywordMap.get(word);
				if(info.isValidInThisContext(search))
				{
					info.completionText.forEach(match -> {
						if(!rtn.contains(match))
							rtn.add(match);
					});
				}
			}
		}
		//System.err.println("Returning List");
		//rtn.forEach(item -> System.err.println(item));
		return rtn;
	}

	public static class KeyWordInformation
	{
		//The hover text to be shown
		public String hoverText;
		//One or more bits of text that could complete the partial keyword.
		public List<String> completionText = new ArrayList<>();
		//An optional function that can be used to indicate if this keyword is appropriate in the context of use
		//i.e. defines is only appropriate is the previous tokens were only indents.
		public Function<TokenResult, Boolean> inContext = null;

		public KeyWordInformation(String hover, List<String> completions, Function<TokenResult, Boolean> inContext)
		{
			this(hover, completions);
			this.inContext = inContext;
		}

		public KeyWordInformation(String hover, List<String> completions)
		{
			this.hoverText = hover;
			this.completionText = completions;
		}

		public boolean isValidInThisContext(TokenResult tokenResult)
		{
			if(inContext != null)
				return inContext.apply(tokenResult);
			return true;
		}
	}
}
