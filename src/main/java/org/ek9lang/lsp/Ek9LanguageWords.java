package org.ek9lang.lsp;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ek9lang.compiler.tokenizer.TokenResult;

/**
 * Designed for us in the LSP only as a set of words that are part of the language.
 * But there is also some descriptive text that is provided with the work.
 * These can be used for code completion or for the hover functionality.
 * This might seem like a duplication of what ANTLR has generated, but we need to supply
 * much more information around each word. This is to really help developers get started
 * even very new developers.
 * In the future might make this functional optional as you get used to it, you might now
 * want the hover/help.
 * This only deals with simple single word completion and simple cases.
 */
final class Ek9LanguageWords {
  private final Map<String, KeyWordInformation> keywordMap = new HashMap<>();

  Ek9LanguageWords() {
    setupKeyWords();
  }

  List<String> getAllKeyWords() {
    return keywordMap.keySet().stream().sorted().toList();
  }

  private void setupKeyWords() {
    keywordMap.put("#!ek9",
        new KeyWordInformation("An EK9 source code file, https://www.ek9.io/structure.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("defines", new KeyWordInformation(
        "Define a module or construct block, https://www.ek9.io/structure.html", List.of(),
        TokenResult::previousTokensIndentsOrFirst));

    //Just joining words
    keywordMap.put("with", new KeyWordInformation(
        "Linking stream mapping/if statements, use of traits/applications and aspects",
        Arrays.asList("with", "with trait of", "with application of", "with aspect of"),
        search -> !search.previousTokenIsPipe()));

    keywordMap.put("as", new KeyWordInformation("Declaration or linking",
        Arrays.asList("as", "as\n", "as open\n", "as abstract\n", "as function\n", "as class\n",
            "as dispatcher\n", "as pure\n", "as pure abstract\n", "as pure function\n",
            "as pure abstract\n", "as pure dispatcher\n", "as GET for :/", "as DELETE for :/",
            "as HEAD for :/", "as POST for :/", "as PUT for :/", "as PATCH for :/",
            "as OPTIONS for :/"), search -> !search.previousTokenIsPipe()));

    keywordMap.put("of", new KeyWordInformation(
        "Used with Generic types, applications, aspects, traits"
            + " and conditionally with streams, length/abs/sqrt", Arrays.asList("of", "of type"),
        search -> !search.previousTokenIsPipe()));

    keywordMap.put("assert",
        new KeyWordInformation("Assert a statement is true, Exception is thrown when false",
            Arrays.asList("assert", "assert()"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("by", new KeyWordInformation(
        "Generic/Type constraint. For loop incrementer. Trait delegation, Pipeline", List.of("by"),
        TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("is", new KeyWordInformation(
        "Extend function/class/component/record (or logic), https://www.ek9.io/inheritance.html",
        Arrays.asList("is", "is not", "is in", "is not in"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put("register", new KeyWordInformation(
        "Register a component instance for injection, https://www.ek9.io/dependencyInjection.html",
        List.of("register"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("dispatcher", new KeyWordInformation(
        "Dispatcher method for calling polymorphic objects, https://www.ek9.io/advancedClassMethods.html",
        List.of("dispatcher"), search -> !search.previousTokensIndentsOrFirst()));

    populateConstructs(keywordMap);
    populateModifiers(keywordMap);
    populateOperators(keywordMap);
    populateFlowControl(keywordMap);
    populateLoops(keywordMap);
    populateTryCatch(keywordMap);
    populateStreaming(keywordMap);
    populateWebServices(keywordMap);

  }

  private void populateConstructs(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("references", new KeyWordInformation(
        "Reference construct from a module, https://www.ek9.io/structure.html#references",
        List.of("references\n"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("module",
        new KeyWordInformation("Module declaration block, https://www.ek9.io/structure.html#module",
            List.of("module"), TokenResult::previousTokenIsDefines));
    keywordMap.put("constant",
        new KeyWordInformation("Constant declaration, https://www.ek9.io/constants.html",
            List.of("constant\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("program", new KeyWordInformation(
        "Program declaration / use with applications, https://www.ek9.io/programs.html",
        Arrays.asList("program\n", "program application\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("type", new KeyWordInformation(
        "Type/Enumeration/Generic declaration, https://www.ek9.io/enumerations.html",
        Arrays.asList("type", "type T", "type T constrain by"),
        search -> !search.previousTokenIsPipe()));
    keywordMap.put("function",
        new KeyWordInformation("Function declaration , https://www.ek9.io/functions.html",
            List.of("function"), TokenResult::previousTokenIsDefines));
    keywordMap.put("record",
        new KeyWordInformation("Record declaration, https://www.ek9.io/records.html",
            List.of("record"), TokenResult::previousTokenIsDefines));
    keywordMap.put("class",
        new KeyWordInformation("Class declaration, https://www.ek9.io/classes.html",
            List.of("class"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("trait",
        new KeyWordInformation("Trait declaration/use, https://www.ek9.io/traits.html",
            List.of("trait"), TokenResult::previousTokenIsDefines));
    keywordMap.put("package", new KeyWordInformation(
        "Package build/publishing control, https://www.ek9.io/packaging.html", List.of("package\n"),
        TokenResult::previousTokenIsDefines));
    keywordMap.put("text",
        new KeyWordInformation("Text construct for output, https://www.ek9.io/textProperties.html",
            List.of("text\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("component",
        new KeyWordInformation("Component declaration, https://www.ek9.io/components.html",
            List.of("component\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("application", new KeyWordInformation(
        "Application declaration, https://www.ek9.io/structure.html#application",
        Arrays.asList("application\n", "application of"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("service", new KeyWordInformation(
        "Service declaration / use with application, https://www.ek9.io/webServices.html",
        Arrays.asList("service", "service application"), TokenResult::previousTokenIsDefines));
  }

  private void populateModifiers(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("override", new KeyWordInformation(
        "Used when overriding methods/operators, https://www.ek9.io/methods.html",
        List.of("override"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("public", new KeyWordInformation(
        "Public declarations not required in EK9, https://www.ek9.io/basics.html#visibility",
        List.of(""), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("protected", new KeyWordInformation(
        "Protected method modifier on classes, https://www.ek9.io/basics.html#class_visibility",
        List.of("protected"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("private",
        new KeyWordInformation("Private method modifier, https://www.ek9.io/basics.html#visibility",
            List.of("private"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("open",
        new KeyWordInformation("Leave open for extension, https://www.ek9.io/inheritance.html",
            List.of("open"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("extends", new KeyWordInformation(
        "Extend a function/class/component/record, https://www.ek9.io/inheritance.html",
        List.of("extends"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("constrain",
        new KeyWordInformation("Restrict an existing type / Control generic type",
            Arrays.asList("constrain by", "constrain as"),
            search -> !search.previousTokenIsPipe()));
    keywordMap.put("pure", new KeyWordInformation(
        "Limit reassignment of variables/side effects, https://www.ek9.io/basics.html#pure",
        List.of("pure"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("abstract", new KeyWordInformation(
        "Non concrete functions/components/records/classes/methods/operators", List.of("abstract"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("allow",
        new KeyWordInformation("Limit reuse by extension, https://www.ek9.io/traits.html",
            List.of("allow"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }


  private void populateOperators(Map<String, KeyWordInformation> keywordMap) {
    populateListOfOperators(keywordMap);
    populateAssignmentTypeOperators(keywordMap);
    populateEqualityOperators(keywordMap);
    populateMathematicsOperators(keywordMap);
    populatesInOutOperators(keywordMap);
    populateConversionOperators(keywordMap);
    populateAccessOperators(keywordMap);
    populateBooleanOperators(keywordMap);
  }

  private void populateAccessOperators(Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("contains", new KeyWordInformation("Does container contain and item", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("matches", new KeyWordInformation("Does one item match another", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("empty",
        new KeyWordInformation("Has no content, https://www.ek9.io/operators.html#functional",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("length",
        new KeyWordInformation("Length, https://www.ek9.io/operators.html#functional", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populateConversionOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("$",
        new KeyWordInformation("String conversion, https://www.ek9.io/operators.html#functional",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("$$",
        new KeyWordInformation("JSON conversion, https://www.ek9.io/operators.html#functional",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put("#^",
        new KeyWordInformation("Promotion, https://www.ek9.io/operators.html#functional", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("#?",
        new KeyWordInformation("Get hash code, https://www.ek9.io/operators.html#functional",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("#<",
        new KeyWordInformation("Get first, https://www.ek9.io/operators.html#functional", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("#>",
        new KeyWordInformation("Get last, https://www.ek9.io/operators.html#functional", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populatesInOutOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("->", new KeyWordInformation("Incoming parameter(s)", List.of(),
        search -> !search.previousTokenIsPipe()));

    keywordMap.put("<-", new KeyWordInformation("Returning / declaration/ out of", List.of(),
        search -> !search.previousTokenIsPipe()));
  }

  private void populateBooleanOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("and", new KeyWordInformation("Boolean 'and' / Bitwise 'and'", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("or", new KeyWordInformation("Boolean 'or' / Bitwise 'or'", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("xor",
        new KeyWordInformation("Boolean 'exclusive or' / Bitwise 'exclusive or'", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("not", new KeyWordInformation("Boolean 'not' / Bitwise 'not'", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("~",
        new KeyWordInformation("Boolean 'not' /Bitwise 'not' / reverse a list", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populateMathematicsOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("+", new KeyWordInformation(
        "Mathematical addition, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("-", new KeyWordInformation(
        "Mathematical subtraction, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("*", new KeyWordInformation(
        "Mathematical multiplication, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("/", new KeyWordInformation(
        "Mathematical division, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("+=", new KeyWordInformation(
        "Mathematical addition and assignment, https://www.ek9.io/operators.html#assignment",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("-=", new KeyWordInformation(
        "Mathematical subtraction and assignment, https://www.ek9.io/operators.html#assignment",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("*=", new KeyWordInformation(
        "Mathematical multiplication and assignment, https://www.ek9.io/operators.html#assignment",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("/=", new KeyWordInformation(
        "Mathematical division and assignment, https://www.ek9.io/operators.html#assignment",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("++", new KeyWordInformation(
        "Mathematical increment, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("--", new KeyWordInformation(
        "Mathematical decrement, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("^", new KeyWordInformation("Mathematical power", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put("mod", new KeyWordInformation(
        "Mathematical modulus, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("rem",
        new KeyWordInformation("Remainder, https://www.ek9.io/operators.html#mathematical",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("abs",
        new KeyWordInformation("Absolute value, https://www.ek9.io/operators.html#mathematical",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("sqrt", new KeyWordInformation(
        "mathematical Square Root, https://www.ek9.io/operators.html#mathematical", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populateEqualityOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("<",
        new KeyWordInformation("Less than, https://www.ek9.io/operators.html#comparison", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<?", new KeyWordInformation(
        "Null safe less than, https://www.ek9.io/operators.html#assignment_coalescing", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<=", new KeyWordInformation(
        "Less than or equal to, https://www.ek9.io/operators.html#comparison", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<=>", new KeyWordInformation(
        "Comparison (space ship), https://www.ek9.io/operators.html#comparison", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<~>",
        new KeyWordInformation("Fuzzy Comparison, https://www.ek9.io/operators.html#comparison",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<=?", new KeyWordInformation(
        "Null safe less than or equal to, https://www.ek9.io/operators.html#assignment_coalescing",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<<", new KeyWordInformation("Shift bits left", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put(">", new KeyWordInformation(
        "Greater than / Add to a collection out of stream, https://www.ek9.io/operators.html#comparison ",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(">?", new KeyWordInformation(
        "Null safe greater than, https://www.ek9.io/operators.html#assignment_coalescing",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(">=", new KeyWordInformation(
        "Greater than or equal to, https://www.ek9.io/operators.html#comparison", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(">=?", new KeyWordInformation(
        "Null safe greater than or equal to, https://www.ek9.io/operators.html#assignment_coalescing",
        List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(">>",
        new KeyWordInformation("Shift bits right / Append to a collection out of stream", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populateAssignmentTypeOperators(Map<String, KeyWordInformation> keywordMap) {
    var assignmentText = "Assignment, https://www.ek9.io/operators.html#assignment";

    keywordMap.put("?",
        new KeyWordInformation("Is set, https://www.ek9.io/operators.html#ternary", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("??",
        new KeyWordInformation("Null coalescing, https://www.ek9.io/operators.html#ternary",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("?:", new KeyWordInformation(
        "Null is set coalescing (Elvis), https://www.ek9.io/operators.html#ternary", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("?=", new KeyWordInformation(
        "Guarded assignment (if/when conditionals), https://www.ek9.io/flowControl.html#if_elseif_else",
        List.of(""),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("!=",
        new KeyWordInformation("Not Equals, https://www.ek9.io/operators.html#comparison",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("<>",
        new KeyWordInformation("Not Equals, https://www.ek9.io/operators.html#comparison",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("=", new KeyWordInformation(assignmentText, List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("==",
        new KeyWordInformation("Equals, https://www.ek9.io/operators.html#comparison", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put(":", new KeyWordInformation(assignmentText, List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(":=", new KeyWordInformation(assignmentText, List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("::", new KeyWordInformation(
        "References construct, https://www.ek9.io/basics.html#references_example", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(":=:",
        new KeyWordInformation("Deep copy, https://www.ek9.io/operators.html#modification",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(":~:",
        new KeyWordInformation("Merge, https://www.ek9.io/operators.html#modification", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(":^:",
        new KeyWordInformation("Replace, https://www.ek9.io/operators.html#modification", List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put(":=?",
        new KeyWordInformation("Assign if unset, https://www.ek9.io/operators.html#ternary",
            List.of(),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  private void populateListOfOperators(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("operator", new KeyWordInformation(
        "Declaration of an operator (such as +-*/), https://www.ek9.io/operators.html",
        Arrays.asList("operator", "operator $", "operator $$", "operator >", "operator <",
            "operator :=", "operator !=", "operator <=", "operator >=", "operator <=>",
            "operator <~>", "operator :~:", "operator :=:", "operator :^:", "operator !",
            "operator ?", "operator ~", "operator ++", "operator --", "operator +", "operator -",
            "operator *", "operator /", "operator +=", "operator -=", "operator *=", "operator /=",
            "operator ^", "operator |", "operator #^", "operator #?", "operator #<", "operator #>",
            "operator mod", "operator rem", "operator abs", "operator sqrt", "operator contains",
            "operator matches", "operator empty", "operator length"),
        search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsOverride()));
  }

  private void populateFlowControl(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("if",
        new KeyWordInformation("Conditional if/when, https://www.ek9.io/flowControl.html#switch",
            List.of(),
            search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));
    keywordMap.put("switch", new KeyWordInformation(
        "Conditional switch/given, https://www.ek9.io/flowControl.html#switch", List.of(),
        search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));
    keywordMap.put("given", new KeyWordInformation(
        "Conditional switch/given, https://www.ek9.io/flowControl.html#switch", List.of("given"),
        search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));
    keywordMap.put("case", new KeyWordInformation(
        "Conditional switch/case condition, https://www.ek9.io/flowControl.html#switch",
        List.of("case"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("default", new KeyWordInformation(
        "Default switch/given condition, https://www.ek9.io/flowControl.html#switch",
        List.of("default\n"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("when", new KeyWordInformation("Conditional if/when/case", List.of("when")));
    keywordMap.put("else", new KeyWordInformation(
        "Conditional else, https://www.ek9.io/flowControl.html#if_elseif_else", List.of("else"),
        TokenResult::previousTokensIndentsOrFirst));
  }

  private void populateLoops(Map<String, KeyWordInformation> keywordMap) {
    keywordMap.put("do",
        new KeyWordInformation("Do loop, https://www.ek9.io/flowControl.html#do_while_loop",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("while",
        new KeyWordInformation("While loop, https://www.ek9.io/flowControl.html#while_loop",
            List.of(), TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("for",
        new KeyWordInformation("For loop / for stream / text for language / service for URI",
            List.of(),
            search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));
  }

  private void populateTryCatch(Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {
    keywordMap.put("try",
        new KeyWordInformation("Try/catch exception block, https://www.ek9.io/exceptions.html",
            List.of(),
            search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));
    keywordMap.put("catch",
        new KeyWordInformation("Catch exception block, https://www.ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("handle",
        new KeyWordInformation("Handle exception block, https://www.ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("finally",
        new KeyWordInformation("Finally exception block, https://www.ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
  }

  private void populateStreaming(Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {
    keywordMap.put("cat", new KeyWordInformation(
        "Catenate stream (start streaming), https://www.ek9.io/streamsAndPipelines.html", List.of(),
        search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment()));

    keywordMap.put("|", new KeyWordInformation("Stream pipelines linkage", List.of(),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));

    keywordMap.put("filter", new KeyWordInformation(
        "Filter/Select items to be passed through stream, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("filter", "filter with", "filter by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("select", new KeyWordInformation(
        "Filter/Select items to be passed through, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("select", "select with", "select by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("map", new KeyWordInformation(
        "Map item into different type, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("map", "map with", "map by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("sort", new KeyWordInformation(
        "Sort items with a sorting function, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("sort", "sort with", "sort by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("group", new KeyWordInformation(
        "Group on an item property, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("group", "group with", "group by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("join",
        new KeyWordInformation("Join items, https://www.ek9.io/streamsAndPipelines.html",
            Arrays.asList("join", "join with", "join by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("split", new KeyWordInformation(
        "Split on an item property, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("split", "split with", "split by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("uniq", new KeyWordInformation(
        "Unique on an item property, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("uniq", "uniq with", "uniq by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("tee", new KeyWordInformation(
        "Tee off items to separate output, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("tee", "tee with", "tee by", "tee in"), TokenResult::previousTokenIsPipe));
    keywordMap.put("flatten", new KeyWordInformation(
        "Flatten - take items out of collections and stream, https://www.ek9.io/streamsAndPipelines.html",
        List.of("flatten"), TokenResult::previousTokenIsPipe));
    keywordMap.put("call",
        new KeyWordInformation("Call delegate, https://www.ek9.io/streamsAndPipelines.html",
            List.of("call"), TokenResult::previousTokenIsPipe));
    keywordMap.put("async", new KeyWordInformation(
        "Asynchronously call delegate, https://www.ek9.io/streamsAndPipelines.html",
        List.of("async"), TokenResult::previousTokenIsPipe));
    keywordMap.put("skip", new KeyWordInformation(
        "Skip one or more items by removing from the stream, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("skip", "skip by", "skip of", "skip only"),
        TokenResult::previousTokenIsPipe));
    keywordMap.put("head", new KeyWordInformation(
        "Head only - stop streaming after N, https://www.ek9.io/streamsAndPipelines.html",
        Arrays.asList("head", "head by", "head of", "head only"),
        TokenResult::previousTokenIsPipe));
    keywordMap.put("tail",
        new KeyWordInformation("Tail only last N, https://www.ek9.io/streamsAndPipelines.html",
            Arrays.asList("tail", "tail by", "tail of", "tail only"),
            TokenResult::previousTokenIsPipe));
    keywordMap.put("collect", new KeyWordInformation(
        "Collect items from stream into a collection, https://www.ek9.io/streamsAndPipelines.html",
        List.of("collect as"), TokenResult::previousTokenIsPipe));
  }

  private void populateWebServices(Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {
    keywordMap.put("GET",
        new KeyWordInformation("HTTP GET verb (idempotent)", Arrays.asList("GET :/", "GET for :/"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("DELETE",
        new KeyWordInformation("HTTP DELETE verb (requires pre-condition checks)",
            Arrays.asList("DELETE :/", "DELETE for :/"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("HEAD", new KeyWordInformation("HTTP HEAD verb (idempotent)",
        Arrays.asList("HEAD :/", "HEAD for :/"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("POST", new KeyWordInformation("HTTP POST verb (create new content)",
        Arrays.asList("POST :/", "POST for :/"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("PUT", new KeyWordInformation("HTTP PUT verb (update/replace existing content)",
        Arrays.asList("PUT :/", "PUT for :/"),
        search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("PATCH",
        new KeyWordInformation("HTTP PATCH verb (partial update to existing content)",
            Arrays.asList("PATCH :/", "PATCH for :/"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
    keywordMap.put("OPTIONS",
        new KeyWordInformation("HTTP OPTIONS verb (check what verbs are supported)",
            Arrays.asList("OPTIONS :/", "OPTIONS for :/"),
            search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe()));
  }

  /**
   * Get only an exact match for this search.
   */
  public KeyWordInformation exactMatch(TokenResult search) {
    KeyWordInformation rtn = null;
    if (search.isPresent()) {
      rtn = keywordMap.get(search.getToken().getText());
    }
    return rtn;
  }

  /**
   * Typically used for completion, where string is partial.
   * Receive a TokenResults, so we can also see position in the line.
   * This will affect the search results.
   */
  public List<String> fuzzyMatch(TokenResult search) {
    List<String> rtn = new ArrayList<>();
    if (search.isPresent()) {
      keywordMap.entrySet().stream()
          .filter(entry -> entry.getKey().startsWith(search.getToken().getText()))
          .filter(entry -> entry.getValue().isValidInThisContext(search))
          .forEach(entry -> entry.getValue().completionText.forEach(match -> {
            if (!rtn.contains(match)) {
              rtn.add(match);
            }
          }));
    }
    return rtn;
  }

  /**
   * Holds information relating to the EK9 language keywords.
   */
  public static class KeyWordInformation {
    //The hover text to be shown
    public final String hoverText;
    //One or more bits of text that could complete the partial keyword.
    private final List<String> completionText;
    //An optional function that can be used to indicate if this keyword is appropriate in the
    //context of use i.e. defines is only appropriate is the previous tokens were only indents.
    private final Function<TokenResult, Boolean> inContext;

    /**
     * Create key word info in terms of hover text, completions for a context of use.
     */
    public KeyWordInformation(String hover, List<String> completions,
                              Function<TokenResult, Boolean> inContext) {
      this.hoverText = hover;
      this.completionText = completions;
      this.inContext = inContext;
    }

    public KeyWordInformation(String hover, List<String> completions) {
      this(hover, completions, null);

    }

    /**
     * Check if a token is valid in a particular context of use.
     */
    public boolean isValidInThisContext(TokenResult tokenResult) {
      if (inContext != null) {
        return inContext.apply(tokenResult);
      }
      return true;
    }
  }
}
