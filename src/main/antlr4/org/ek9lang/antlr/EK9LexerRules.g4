lexer grammar EK9LexerRules;

//Synthetic tokens for indenting and dedenting.
tokens { INDENT, DEDENT }

@lexer::members {
  //It is necessary to declare this here to be used with predicates.
  //As this call is used in the baase lexer antlr generates.
  protected boolean isRegexPossible() { return true; }
}

//The order is important in antlr first and longest match.

MODULE : 'module';
//Type of module
DEFINES : 'defines';
CONSTANT : 'constant';
TYPE : 'type';
RECORD: 'record';
SERVICE : 'service';
PURE : 'pure' ;
FUNCTION : 'function';
PROGRAM : 'program';
APPLICATION: 'application';
ASPECT : 'aspect';
REGISTER : 'register';
ISOLATED : 'isolated';
ALLOW : 'allow';
EMPTY : 'empty';
CONSTRAIN : 'constrain';
TEXT : 'text';
AS : 'as';
OF : 'of';
URI : 'uri';

MAP : 'map';
HEAD : 'head';
TAIL : 'tail';
TEE : 'tee';
COLLECT : 'collect';
UNIQ : 'uniq';
CAT : 'cat';
SORT : 'sort';
FILTER : 'filter';
SELECT : 'select';
CALL: 'call';
ASYNC: 'async';
FLATTEN: 'flatten';
GROUP: 'group';
JOIN: 'join';
SPLIT: 'split';
SKIPPING: 'skip';

RANGE : 'range';
WITH : 'with';
THEN : 'then';
TRAIT: 'trait';
OPEN : 'open';
DECORATION : 'decoration';
WHEN : 'when';
REFERENCES: 'references';
LENGTH : 'length';
ONLY : 'only';
BY : 'by';

//Not all may be used - but are reserved
DISPATCHER : 'dispatcher'; //For certain classes etc we can support double dispatch if method marked with this
ABSTRACT : 'abstract';
ASSERT : 'assert';
BOOLEAN : 'Boolean'; //ek9 boolean
CASE : 'case';
CATCH : 'catch';
HANDLE : 'handle';
CHARACTER : 'Character'; //ek9 character
CLASS : 'class';
COMPONENT: 'component';
CONST : 'const';
DO : 'do';
ELSE : 'else';
EXTENDS : 'extends';
FINAL : 'final';
FINALLY : 'finally';
FLOAT : 'Float'; //ek9 float
FOR : 'for';
IF : 'if';
IS : 'is';
INTEGER : 'Integer'; //ek9 Integer
PACKAGE : 'package';
PRIVATE : 'private';
PROTECTED : 'protected';
OVERRIDE : 'override';
PUBLIC : 'public';
OPERATOR: 'operator';
SUPER : 'super';
SWITCH : 'switch';
GIVEN : 'given' ;
STRING : 'String';
REGEX : 'RegEx';
DATE : 'Date';
DATETIME : 'DateTime';
DURATION : 'Duration';
TIME : 'Time';
THIS : 'this';
THROW : 'throw';
TRY : 'try';
WHILE : 'while';
COLOUR: 'Colour';
DIMENSION: 'Dimension';
RESOLUTION: 'Resolution';
MONEY: 'Money';
VERSION: 'Version';

HTTP_GET: 'GET';
HTTP_DELETE: 'DELETE';
HTTP_HEAD: 'HEAD';
HTTP_POST: 'POST';
HTTP_PUT: 'PUT';
HTTP_PATCH: 'PATCH';
HTTP_OPTIONS: 'OPTIONS';

HTTP_PATH: 'PATH';
HTTP_HEADER: 'HEADER';
HTTP_QUERY: 'QUERY';
HTTP_REQUEST: 'REQUEST';
HTTP_CONTENT: 'CONTENT';
HTTP_CONTEXT: 'CONTEXT';

Uriproto
    : ':' + UriprotoPart+
    ;

fragment
UriprotoPart
    : '/'
    | '/' Identifier ((SUB|DOT) Identifier)?
    | '/' LBRACE Identifier (SUB Identifier)? RBRACE
    ;

IntegerLiteral
    :          DecimalIntegerLiteral
    |          HexIntegerLiteral
    |          OctalIntegerLiteral
    ;

BinaryLiteral
    : BinaryIntegerLiteral
    ;

DateTimeLiteral
	: Digit Digit Digit Digit '-' Digit Digit '-' Digit Digit 'T' Digit Digit ':' Digit Digit ':' Digit Digit ( ((('-'|'+') Digit Digit COLON Digit Digit)) | 'Z')
	;

DateLiteral
    : Digit Digit Digit Digit '-' Digit Digit '-' Digit Digit
    ;

TimeLiteral
    : Digit Digit COLON Digit Digit (COLON Digit Digit)?
    ;

//ISO 8601 i.e P[n]Y[n]M[n]W[n]DT[n]H[n]M[n]S
DurationLiteral
    : 'P' ('-'? Digit+ ('Y' | 'M' | 'W' | 'D') )* ('T' ('-'? Digit+ ('H' | 'M' | 'S') )* )?
    ;

MillisecondLiteral
    : Digit+ 'ms'
    ;

DecorationDimensionLiteral
    : Digit+ '.' Digit+ DimensionType
    | Digit+ DimensionType
    ;

fragment
DimensionType
    :  ('km' | 'm' | 'cm' | 'mm' | 'mile' | 'in' | 'pc' | 'pt' | 'px' | 'em' | 'ex' | 'ch' | 'rem' | 'vw' | 'vh' | 'vmin' | 'vmax' | '%' )
    ;

DecorationResolutionLiteral
    : Digit+ ('dpi' | 'dpc')
    ;

MoneyLiteral
    : Digits ('.' Digits)? HASH [A-Z][A-Z][A-Z]
    ;

//With optional alpha, if present the alpha is first two hex digits
ColourLiteral
    : HASH HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit (HexDigit HexDigit)?
    ;

fragment
DecimalIntegerLiteral
    : DecimalNumeral
    ;

fragment
HexIntegerLiteral
    : HexNumeral
    ;

fragment
OctalIntegerLiteral
    : OctalNumeral
    ;

fragment
BinaryIntegerLiteral
    : BinaryNumeral
    ;

fragment
DecimalNumeral
    : '0'
    | NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    : Digit (DigitsAndUnderscores? Digit)?
    ;

fragment
Digit
    : '0'
    | NonZeroDigit
    ;

fragment
NonZeroDigit
    : [1-9]
    ;

fragment
DigitsAndUnderscores
    : DigitOrUnderscore+
    ;

fragment
DigitOrUnderscore
    : Digit
    | '_'
    ;

fragment
Underscores
    : '_'+
    ;

fragment
HexNumeral
    : '0' [xX] HexDigits
    ;

fragment
HexDigits
    : HexDigit (HexDigitsAndUnderscores? HexDigit)?
    ;

fragment
HexDigit
    : [0-9a-fA-F]
    ;

fragment
HexDigitsAndUnderscores
    : HexDigitOrUnderscore+
    ;

fragment
HexDigitOrUnderscore
    : HexDigit
    | '_'
    ;

fragment
OctalNumeral
    : '0' Underscores? OctalDigits
    ;

fragment
OctalDigits
    : OctalDigit (OctalDigitsAndUnderscores? OctalDigit)?
    ;

fragment
OctalDigit
    : [0-7]
    ;

fragment
OctalDigitsAndUnderscores
    : OctalDigitOrUnderscore+
    ;

fragment
OctalDigitOrUnderscore
    : OctalDigit
    | '_'
    ;

fragment
BinaryNumeral
    : '0' [bB] BinaryDigits
    ;

fragment
BinaryDigits
    : BinaryDigit (BinaryDigitsAndUnderscores? BinaryDigit)?
    ;

fragment
BinaryDigit
    : [01]
    ;

fragment
BinaryDigitsAndUnderscores
    : BinaryDigitOrUnderscore+
    ;

fragment
BinaryDigitOrUnderscore
    : BinaryDigit
    | '_'
    ;

FloatingPointLiteral
    : DecimalFloatingPointLiteral
    | HexadecimalFloatingPointLiteral
    ;

//Versions number must look like this major.minor.patch-buildNo is 3.2.1-9
//Or look like this major.minor.patch-buildNo is 3.2.1-feature12-9 for a feature based version
VersionNumberLiteral
    : Digits ('.' Digits) ('.' Digits) ('-' Digits)
    | Digits ('.' Digits) ('.' Digits) ('-' StringCharacter+ Digits* StringCharacter*) ('-' Digits)
    ;

fragment
DecimalFloatingPointLiteral
    : Digits '.' Digits? ExponentPart?
    | '.' Digits ExponentPart?
    | Digits ExponentPart
    | Digits
    ;

fragment
ExponentPart
    : ExponentIndicator SignedInteger
    ;

fragment
ExponentIndicator
    : [eE]
    ;

fragment
SignedDecimal
    : Sign? DecimalFloatingPointLiteral
    ;

fragment
SignedInteger
    : Sign? Digits
    ;

fragment
Sign
    : [+-]
    ;

fragment
HexadecimalFloatingPointLiteral
    : HexSignificand BinaryExponent
    ;

fragment
    HexSignificand
    : HexNumeral '.'?
    | '0' [xX] HexDigits? '.' HexDigits
    ;

fragment
BinaryExponent
    : BinaryExponentIndicator SignedInteger
    ;

fragment
BinaryExponentIndicator
    : [pP]
    ;

BooleanLiteral
    : 'true'
    | 'false'
    ;

CharacterLiteral
    : '\'' SingleCharacter '\''
    | '\'' EscapeSequence '\''
    ;

fragment
SingleCharacter
    : ~['\\]
    ;

//semantic predecate to limit when regular expression token can be provided.
//Also the order of this the line comment and the un-closed regex is important and antlr processes them in order and tries to find longest match
//The ~ means 'not' so " | ~[\r\n]" means any char that is not new line or line feed.
// Note that we use normal reg ex like \d, \D, \s, \S, \w, \W, \b so EK9 will escape these to \\d for example, to use  a literal \ you need to escape to \\ and ek9 will escape to \\\\

//The ~ means 'not' so " | ~[\r\n/]" means any char that is not new line or line feed and not / - but then ends with a new line '\n'.
//The *? is the non greedy any number of!
RegExLiteral
    : {isRegexPossible()}? '/' ( '\\/' | ~[\r\n] )*?  '/'
    ;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
//html style for developers with that back ground
BLOCK_COMMENT1: '<!--' .*? '-->' -> skip;
//These two are more consistent ek9 type first is just to comment out
BLOCK_COMMENT2: '<!-' .*? '-!>' -> skip;
//But this comments out put is intended to be attached to the source as helpful doc comments.
CODE_COMMENT: '<?-' .*? '-?>' -> skip;

StringLiteral
    : '"' StringCharacters? '"'
    ;

fragment
StringCharacters
    : StringCharacter+
    ;

fragment
StringCharacter
    : ~["]
    | EscapeSequence
    ;

fragment
EscapeSequence
    : '\\' [btnfr"'\\]
    | OctalEscape
    | UnicodeEscape
    ;

fragment
OctalEscape
    : '\\' OctalDigit
    | '\\' OctalDigit OctalDigit
    | '\\' ZeroToThree OctalDigit OctalDigit
    ;

fragment
ZeroToThree
    : [0-3]
    ;

fragment
UnicodeEscape
    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
//Also acts as end of interpolation - Lexer must override popMode to be safe on empty stack
RBRACE : '}' -> popMode;
//Start of a String literal so go into STRING interpolation MODE
OPEN_STRING: '`' -> pushMode(STRING_MODE); // Switch context

LBRACK : '[';
RBRACK : ']';
SEMI : ';';
COMMA : ',';
DOT : '.';
HASH : '#';
TOJSON : '$$';
DOLLAR : '$';
SHEBANG : '#!ek9';
PROMOTE : '#^'; //ek9 promote operator
HASHCODE : '#?'; //ek9 hashcode operator
PREFIX : '#<'; //ek9 prefix/first operator
SUFFIX : '#>'; //ek9 suffix/last operator
PIPE : '|'; //Used in streams but also as operator on collect into a type.

DEFAULT : 'default';
GT : '>';
LT : '<';
QUESTION : '?'; // used in syntax and as an operator for isSet
CHECK : '??'; //null coalescing
ELVIS: '?:'; //null and isSet coalescing
GUARD : '?='; // a special type of assign that also checks if null on assignment used in if statements
TERNARY_LT : '<?'; //ternary operator
TERNARY_LE : '<=?'; //ternary operator
TERNARY_GT : '>?'; //ternary operator
TERNARY_GE : '>=?'; //ternary operator
ASSIGN_UNSET : ':=?'; //Assign to the left hand side if left handside is null or unset
COLON : ':';
EQUAL : '==';
LE : '<=';
GE : '>=';
NOT : 'not'; //ek9 not
IN : 'in'; //ek9 in
MATCHES : 'matches' ; //also matching
CONTAINS : 'contains'; // ek9 contains
NOTEQUAL : '<>'; //SQL styles of not equals
NOTEQUAL2 : '!='; //ek9 not equals more like C C++ and Java
AND : 'and'; //ek9 and
OR : 'or'; //ek9 or
XOR : 'xor'; //ek9 X or
CMP : '<=>'; //ek9 compare
FUZ : '<~>'; //ek9 fuzzy compare
INC : '++';
DEC : '--';
ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
SHFTL : '<<'; //The logical left shift
SHFTR : '>>' ; //The logical right shift
BANG : '!';  //Factorial/Clear operator or requires injection
TILDE : '~'; //ek9 not/negate operator
CARET : '^'; //ek9 power
MOD : 'mod'; //ek9 modulus for values
REM : 'rem'; //ek9 remainder check sign on MOD and REM
ABS : 'abs'; //EK9 absolute removes negative
SQRT : 'sqrt';
RIGHT_ARROW : '->';
LEFT_ARROW : '<-';
COLONCOLON : '::';
MERGE : ':~:'; //ek9 merge right item into left
COPY : ':=:'; //ek9 make a copy/clone - this is different to assignment.
REPLACE : ':^:'; //ek9 replace something if possible ideal for lists of things
ASSIGN : ':='; //ek9 assign - this copies the variable pointer over - above does a copy of the contents.
ASSIGN2 : '='; //Also allow '=' as assignment
ADD_ASSIGN : '+=';
SUB_ASSIGN : '-=';
MUL_ASSIGN : '*=';
DIV_ASSIGN : '/=';

Identifier
    : Letter LetterOrDigit*
    ;

fragment
LowerCase
    : [a-z]
    ;

fragment
Letter
    : [a-zA-Z]
    ;

fragment
LetterOrDigit
    : [a-zA-Z0-9]
    ;

AT : '@';
ELLIPSIS : '...';

NL: ('\r'? '\n' ' '*); // note the ' '*
TAB: '\t';
WS: [ ]+ -> skip;

ANY: . ;

mode STRING_MODE;

//Only when inside a String i.e STRING_MODE - do with push interpolation
//which is just normal processing again
ENTER_EXPR_INTERPOLATION: INTERP_START -> pushMode(DEFAULT_MODE);

STRING_TEXT: TEXT_CHAR+;

TEXT_CHAR
    : ~[$`]
    | '\\$'
    | '\\`'
    | EscapeSequence
    ;

fragment
INTERP_START: '$' '{';

//End of the String when in string mode - pop out of STRING_MODE
CLOSE_STRING: '`' -> popMode;

//EOF
