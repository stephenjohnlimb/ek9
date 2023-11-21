grammar EK9;
options
{
  tokenVocab=EK9LexerRules;
  language = Java;
}

//Also note that in most grammars new lines/tabs are just guff and '{' '}' are the keys to scopes and parsing
//But similar to Python INDENT, DEDENT and NEWLINE are critical scope marking that's why they are everywhere in the
//grammar - white space and newlines are significant.

//Note that identifier is basically an unqualified name. IdentifierReference is an optionally fully qualified identifier.
//Also note that Identifier (not identifier) is the lex token, identifier includes additional key words.

compilationUnit
    : sheBang? moduleDeclaration NL* EOF
    ;

sheBang
    : SHEBANG NL+
    ;

//Provides a way to embed directives into EK9 source for various reasons to check/alter compilation/tests/generation
directive
    : AT identifier (COLON directivePart)* NL?
    ;

directivePart
    : identifier
    | stringLit
    ;

moduleDeclaration
    : directive? DEFINES EXTERN? MODULE dottedName NL+ (INDENT NL* (directive? referencesBlock NL+)? (directive? moduleBlock NL+)* DEDENT)?
    ;

moduleBlock
    : constantBlock
    | programBlock
    | typeBlock
    | functionBlock
    | recordBlock
    | classBlock
    | traitBlock
    | packageBlock
    | textBlock
    | componentBlock
    | applicationBlock
    | serviceBlock
    ;

referencesBlock
    : REFERENCES NL+ INDENT NL* (directive? identifierReference NL+)+ DEDENT
    ;

typeBlock
    : DEFINES TYPE NL+ INDENT NL* (directive? typeDeclaration NL+)+ DEDENT
    ;

packageBlock
    : DEFINES PACKAGE NL+ INDENT NL* (directive? variableDeclaration NL+)+ DEDENT
    ;

constantBlock
    : DEFINES CONSTANT NL+ INDENT NL* (directive? constantDeclaration NL+)+ DEDENT
    ;

recordBlock
    : DEFINES RECORD NL+ INDENT NL* (directive? recordDeclaration NL+)+ DEDENT
    ;

traitBlock
    : DEFINES TRAIT NL+ INDENT NL* (directive? traitDeclaration NL+)+ DEDENT
    ;

classBlock
    : DEFINES CLASS NL+ INDENT NL* (directive? classDeclaration NL+)+ DEDENT
    ;

componentBlock
    : DEFINES COMPONENT NL+ INDENT NL* (directive? componentDeclaration NL+)+ DEDENT
    ;

textBlock
    : DEFINES TEXT FOR stringLit NL+ INDENT NL* (directive? textDeclaration NL+)+ DEDENT
    ;

serviceBlock
    : DEFINES SERVICE NL+ INDENT NL* (directive? serviceDeclaration NL+)+ DEDENT
    ;

applicationBlock
    : DEFINES APPLICATION NL+ INDENT NL* (directive? applicationDeclaration NL+)+ DEDENT
    ;

functionBlock
    : DEFINES FUNCTION NL+ INDENT NL* (directive? functionDeclaration)+ DEDENT
    ;

programBlock
    : DEFINES PROGRAM NL+ INDENT NL* (directive? methodDeclaration)+ DEDENT
    ;

//End of main blocks

constantDeclaration
    : Identifier LEFT_ARROW constantInitialiser
    ;

//By adding Open, it means we can define a function with a body, but also override functionality and pass it around.
//None dynamic functions can extend other functions (but not generic ones), generic functions cannot extend anything
//See dynamic functions for further extending of functionality.
functionDeclaration
    : Identifier (LPAREN RPAREN)? (extendPreamble identifierReference (LPAREN RPAREN)?)? AS? PURE? (ABSTRACT | OPEN)? NL+ operationDetails
    | Identifier (LPAREN RPAREN)? parameterisedParams AS? PURE? (ABSTRACT | OPEN)? NL+ operationDetails
    ;

//Records can extend other records (if needed).
recordDeclaration
    : Identifier extendDeclaration? (AS? (ABSTRACT | OPEN))? aggregateParts?
    ;

//Include ABSTRACT and OPEN just for syntax consistency.
//Added a choice of using extends, is, trait of for extending a trait. I just keep typing 'extends'.
traitDeclaration
    : Identifier ((extendPreamble | traitPreamble) traitsList)? allowingOnly? (AS? (ABSTRACT | OPEN))? aggregateParts?
    ;

//Classes can extend other classes (including generic ones), generic classes cannot extend anything.
//See dynamic classes for further extending classes.
classDeclaration
    : Identifier extendDeclaration? (traitPreamble traitsList)? (AS? (ABSTRACT | OPEN))? aggregateParts?
    | Identifier parameterisedParams (AS? (ABSTRACT | OPEN))? aggregateParts
    ;

//Components can extend other component, and cannot be polymorphic parametric.
componentDeclaration
    : Identifier extendDeclaration? (AS? (ABSTRACT | OPEN))? aggregateParts?
    ;

textDeclaration
    : Identifier AS? NL+ INDENT NL* (directive? textBodyDeclaration NL+)+ DEDENT
    ;

serviceDeclaration
    : Identifier FOR? Uriproto AS? NL+ INDENT NL* (directive? (methodDeclaration | serviceOperationDeclaration))* DEDENT
    ;

applicationDeclaration
    : Identifier (LPAREN RPAREN)? AS? NL+ INDENT NL* (directive? (blockStatement | registerStatement) NL+)+ DEDENT
    ;

registerStatement
    : REGISTER call (AS (ISOLATED? identifierReference (LPAREN RPAREN)? aspectDeclaration?))?
    ;

aspectDeclaration
    : WITH? ASPECT OF call (COMMA call)*
    ;

textBodyDeclaration
    : Identifier (LPAREN RPAREN)? NL+ INDENT NL* argumentParam? directive? stringLit NL+ DEDENT
    ;

//End of main construct declarations

//A dynamic class can only have traits, be 'Tuple' or extend a parameterised generic class.
dynamicClassDeclaration
    : Identifier? dynamicVariableCapture traitPreamble traitsList AS? CLASS? aggregateParts?
    | Identifier dynamicVariableCapture AS? CLASS aggregateParts
    | Identifier? dynamicVariableCapture extendPreamble parameterisedType AS? CLASS aggregateParts?
    ;

//A dynamic function can extend other functions including parameterised generic functions.
dynamicFunctionDeclaration
    : dynamicVariableCapture extendPreamble identifierReference (LPAREN RPAREN)? AS? PURE? FUNCTION? dynamicFunctionBody
    | dynamicVariableCapture extendPreamble parameterisedType AS? PURE? FUNCTION dynamicFunctionBody?
    ;

dynamicFunctionBody
    : singleStatementBlock
    | block
    ;

singleStatementBlock
    : LPAREN blockStatement RPAREN
    ;

constantInitialiser
    : literal
    ;

dynamicVariableCapture
    : paramExpression
    ;

parameterisedParams
    : OF? TYPE parameterisedDetail 
    | OF? TYPE LPAREN parameterisedDetail (COMMA parameterisedDetail)+ RPAREN
    ;

parameterisedDetail
    : Identifier (CONSTRAIN BY typeDef)? //ie. String or whatever
    ;

extendDeclaration
    : extendPreamble typeDef
    ;

extendPreamble
    : EXTENDS
    | IS
    ;

allowingOnly
    : ALLOW? ONLY identifierReference (COMMA identifierReference)*
    ;

typeDeclaration
    : Identifier AS? typeDef constrainDeclaration?
    | Identifier AS? NL+ INDENT NL* enumerationDeclaration DEDENT
    | parameterisedType
    ;

typeDef
    : identifierReference
    | parameterisedType
    ;

//Added optional paramExpression here, to simplify grammar and processing
//But now needs coding check to ensure only used in the correct context.
parameterisedType
    : identifierReference paramExpression? OF LPAREN parameterisedArgs RPAREN
    | identifierReference paramExpression? OF typeDef
    ;

parameterisedArgs
    : typeDef (COMMA typeDef)*
    ;

traitsList
    : traitReference (COMMA traitReference)*
    | LPAREN traitReference (COMMA traitReference)* RPAREN
    ;

traitPreamble
    : WITH? TRAIT OF?
    ;

traitReference
    : identifierReference (BY identifier)?
    ;

aggregateParts
    : NL+ INDENT NL* (directive? aggregateProperty)* (directive? methodDeclaration)* (directive? operatorDeclaration)* defaultOperator? DEDENT
    ;

defaultOperator
    : directive? DEFAULT OPERATOR NL+
    ;

aggregateProperty
    : variableDeclaration NL+
    | variableOnlyDeclaration NL+
    ;

methodDeclaration
    : (OVERRIDE | DEFAULT)? accessModifier? identifier (LPAREN RPAREN)? (WITH APPLICATION OF? identifierReference (LPAREN RPAREN)?)? AS? PURE? (ABSTRACT | DISPATCHER)? NL+ operationDetails?
    ;

operatorDeclaration
    : (OVERRIDE | DEFAULT)? OPERATOR operator AS? PURE? ABSTRACT? NL+ operationDetails?
    ;

serviceOperationDeclaration
    : ((identifier (LPAREN RPAREN)? (AS? httpVerb)?) | (OPERATOR operator)) FOR? Uriproto NL+ operationDetails
    ;

operationDetails
    : INDENT NL* argumentParam? returningParam? instructionBlock? DEDENT NL+
    ;

httpVerb
    : (HTTP_GET | HTTP_DELETE | HTTP_HEAD | HTTP_POST | HTTP_PUT | HTTP_PATCH | HTTP_OPTIONS)
    ;

enumerationDeclaration
    : (directive? Identifier COMMA? NL+)+
    ;

constrainDeclaration
    : CONSTRAIN (BY|AS)? NL+ INDENT constrainType NL+ DEDENT
    ;

//When a literal by itself - short hand for equals.
constrainType
    : directive? op=(GT | GE | LT | LE | EQUAL | NOTEQUAL | NOTEQUAL2 | MATCHES | CONTAINS)? literal
    | constrainType op=(AND | OR) constrainType
    | LPAREN constrainType RPAREN
    ;

httpAccess
    : (HTTP_PATH | HTTP_HEADER | HTTP_QUERY | HTTP_REQUEST | HTTP_CONTENT | HTTP_CONTEXT)
    ;

//End of main declaration parts

block
    : NL+ INDENT NL* instructionBlock DEDENT
    ;

instructionBlock
    : (directive? blockStatement NL+)+
    ;

blockStatement
    : variableDeclaration
    | variableOnlyDeclaration
    | statement
    ;

variableOnlyDeclaration
    : identifier AS? typeDef (QUESTION | BANG)? webVariableCorrelation?
    ;

webVariableCorrelation
    : COPY httpAccess stringLit?
    ;

variableDeclaration
    : identifier AS? typeDef QUESTION? op=(ASSIGN | ASSIGN2 | COLON | MERGE | ASSIGN_UNSET) assignmentExpression
    | identifier LEFT_ARROW assignmentExpression
    ;

statement
    : ifStatement
    | whileStatement
    | forStatement
    | assertStatement
    | assignmentStatement
    | identifierReference op=(INC | DEC)
    | call
    | stream
    | objectAccessExpression
    | switchStatementExpression
    | tryStatementExpression
    | throwStatement
    ;

ifStatement
    : (IF | WHEN) preFlowStatement? control=expression block (NL+ directive? ELSE block)?
    | (IF | WHEN) preFlowStatement? control=expression block NL+ directive? ELSE ifStatement
    ;

whileStatement
    : WHILE preFlowStatement? control=expression block
    | DO preFlowStatement? block NL+ directive? WHILE control=expression
    ;

forStatement
    : (forLoop | forRange) block
    ;

forLoop
    : FOR preFlowStatement? identifier IN expression
    ;

forRange
    : FOR preFlowStatement? identifier IN range (BY (literal | identifier))?
    ;

assignmentStatement
    : (primaryReference | identifier | objectAccessExpression) op=(ASSIGN | ASSIGN2 | COLON | ASSIGN_UNSET | ADD_ASSIGN | SUB_ASSIGN | DIV_ASSIGN | MUL_ASSIGN | MERGE | REPLACE | COPY) assignmentExpression
    ;

assignmentExpression
    : expression
    | guardExpression
    | switchStatementExpression
    | tryStatementExpression
    | dynamicClassDeclaration
    | stream
    ;

throwStatement
    : THROW (call | identifierReference)
    ;

//The order here is important, order of precedence for processing
expression
    : expression op=(INC | DEC | BANG)
    | op=SUB expression
    | expression op=QUESTION
    | op=TOJSON expression
    | op=DOLLAR expression
    | op=PROMOTE expression
    | op=LENGTH OF? expression
    | op=PREFIX expression
    | op=SUFFIX expression
    | op=HASHCODE expression
    | op=ABS OF? expression
    | op=SQRT OF? expression
    | <assoc=right> left=expression coalescing=(CHECK | ELVIS) right=expression
    | primary
    | call
    | objectAccessExpression
    | list
    | dict
    | expression IS? neg=NOT? op=EMPTY
    | op=(NOT | TILDE) expression
    | left=expression op=CARET right=expression
    | left=expression op=(DIV | MUL | MOD | REM ) NL? right=expression
    | left=expression op=(ADD | SUB) NL? right=expression
    | left=expression op=(SHFTL | SHFTR) right=expression
    | left=expression op=(CMP | FUZ) NL? right=expression
    | left=expression op=(LE | GE | GT | LT) NL? right=expression
    | left=expression op=(EQUAL | NOTEQUAL | NOTEQUAL2) NL? right=expression
    | <assoc=right> left=expression coalescing_equality=(COALESCE_LE | COALESCE_GE | COALESCE_GT | COALESCE_LT) right=expression
    | left=expression neg=NOT? op=MATCHES right=expression
    | left=expression neg=NOT? op=CONTAINS right=expression
    | left=expression IS? neg=NOT? IN right=expression
    | left=expression op=(AND | XOR | OR) NL? right=expression
    | expression IS? neg=NOT? IN range
    | <assoc=right> control=expression LEFT_ARROW left=expression (COLON|ELSE) right=expression
    ;

call
    : identifierReference paramExpression
    | parameterisedType
    | primaryReference paramExpression
    | dynamicFunctionDeclaration
    | call paramExpression
    ;

tryStatementExpression
    : TRY preFlowStatement? NL+ INDENT NL* declareArgumentParam? instructionBlock DEDENT catchStatementExpression? finallyStatementExpression?
    | TRY preFlowStatement? NL+ INDENT NL* declareArgumentParam? returningParam instructionBlock? DEDENT catchStatementExpression? finallyStatementExpression?
    ;

catchStatementExpression
    : NL+ directive? (CATCH|HANDLE) NL+ INDENT NL* argumentParam instructionBlock DEDENT
    ;

finallyStatementExpression
    : NL+ directive? FINALLY block
    ;

switchStatementExpression
    : (SWITCH|GIVEN) preFlowStatement? control=expression NL+ INDENT (NL* returningParam)? caseStatement+ (directive? DEFAULT block NL+)?  DEDENT
    ;

caseStatement
    : directive? (CASE|WHEN) caseExpression (COMMA caseExpression)* block NL+
    ;

caseExpression
    : call
    | objectAccessExpression
    | op=(LE | GE | GT | LT) expression
    | MATCHES expression
    | primary
    ;

preFlowStatement
    : (variableDeclaration | assignmentStatement | guardExpression) (WITH|THEN)
    ;

guardExpression
    : identifier op=GUARD expression
    ;

stream
    : (streamCat | streamFor) streamPart* streamTermination
    | (streamCat | streamFor) NL+ INDENT NL* (streamPart NL+)+ streamTermination NL+ DEDENT
    ;

streamCat
    : CAT expression (COMMA expression)*
    ;

streamFor
    : forRange
    ;

//Note that sort and unique can just use comparator and hashcode - respectively if present.
//Also skip, head and tail now default to 1 is nothing is specified.
streamPart
    : PIPE op=FILTER (WITH | BY)? pipelinePart
    | PIPE op=SELECT (WITH | BY)? pipelinePart
    | PIPE op=MAP (WITH | BY)? pipelinePart
    | PIPE op=SORT ((WITH | BY)? pipelinePart)?
    | PIPE op=GROUP (WITH | BY)? pipelinePart
    | PIPE op=JOIN (WITH | BY)? pipelinePart
    | PIPE op=SPLIT (WITH | BY)? pipelinePart
    | PIPE op=UNIQ ((WITH | BY)? pipelinePart)?
    | PIPE op=TEE (WITH | BY | IN)? pipelinePart
    | PIPE op=FLATTEN
    | PIPE op=CALL
    | PIPE op=ASYNC
    | PIPE op=SKIPPING ((BY | OF | ONLY)? (pipelinePart | integerLit))?
    | PIPE op=HEAD ((BY | OF | ONLY)? (pipelinePart | integerLit))?
    | PIPE op=TAIL ((BY | OF | ONLY)? (pipelinePart | integerLit))?
    ;

streamTermination
    : op=GT pipelinePart
    | op=SHFTR pipelinePart
    | PIPE op=COLLECT AS typeDef
    ;

pipelinePart
    : identifier
    | objectAccessExpression
    | call
    ;

objectAccessExpression
    : objectAccessStart objectAccess
    ;

objectAccessStart
    :  (primaryReference | identifier | call)
    ;

objectAccess
    : objectAccessType objectAccess?
    ;

objectAccessType
    : DOT (identifier | operationCall)
    ;

operationCall
    : identifier paramExpression
    | operator paramExpression
    ;

paramExpression
    : LPAREN RPAREN
    | LPAREN expressionParam (COMMA expressionParam)* RPAREN
    ;

expressionParam
    : directive? (identifier (ASSIGN | ASSIGN2 | COLON))? expression
    ;

assertStatement
    : ASSERT expression
    | ASSERT LPAREN expression RPAREN
    ;

list
    : LBRACK expression (COMMA expression)* RBRACK
    ;

dict
    : LBRACE initValuePair (COMMA initValuePair)* RBRACE
    ;

initValuePair
    : expression COLON expression
    ;

primary
    : LPAREN expression RPAREN
    | primaryReference
    | literal
    | identifierReference
    ;

primaryReference
    : THIS
    | SUPER
    ;

declareArgumentParam
    : RIGHT_ARROW NL+ INDENT NL* (variableDeclaration NL+)+ DEDENT NL+
    | RIGHT_ARROW variableDeclaration NL+
    ;

argumentParam
    : directive? RIGHT_ARROW NL+ INDENT NL* (directive? variableOnlyDeclaration NL+)+ DEDENT NL+
    | directive? RIGHT_ARROW variableOnlyDeclaration NL+
    ;

returningParam
    : directive? LEFT_ARROW NL+ INDENT directive? NL* (variableDeclaration | variableOnlyDeclaration) NL DEDENT NL+
    | directive? LEFT_ARROW (variableDeclaration | variableOnlyDeclaration) NL+
    ;

//Simple stuff from here on

operator
    : GT
    | LT
    | EQUAL
    | LE
    | GE
    | BANG
    | QUESTION
    | TILDE
    | NOTEQUAL
    | NOTEQUAL2
    | CMP
    | FUZ
    | MERGE
    | REPLACE
    | COPY
    | INC
    | DEC
    | ADD
    | SUB
    | MUL
    | DIV
    | CARET
    | NOT
    | AND
    | OR
    | XOR
    | MOD
    | REM
    | ABS
    | SQRT
    | CONTAINS
    | MATCHES
    | PIPE
    | ADD_ASSIGN
    | SUB_ASSIGN
    | MUL_ASSIGN
    | DIV_ASSIGN
    | TOJSON
    | DOLLAR
    | PROMOTE
    | HASHCODE
    | PREFIX
    | SUFFIX
    | CLOSE
    | EMPTY
    | LENGTH
    | SHFTL
    | SHFTR
    ;

//Now we also add in some words that act as operators in some ways but we want the developer to be able to use them as identifiers.
//Seems strange but it means that words like sort, group etc can be used as identifiers as well as in pipeline processing.
identifierReference
    : (dottedName COLONCOLON)? identifier
    ;

dottedName
    : identifier (DOT identifier)*
    ;
 
identifier
    : Identifier
    | SORT
    | CAT
    | FILTER
    | SELECT
    | COLLECT
    | FLATTEN
    | CALL
    | ASYNC
    | MAP
    | GROUP
    | JOIN
    | SPLIT
    | SKIPPING
    | HEAD
    | TAIL
    | CLOSE
    | CONTAINS
    | MATCHES
    | EMPTY
    | LENGTH
    | REGISTER
    | CONSTRAIN
    | ALLOW
    | MODULE
    | REFERENCES
    | CONSTANT
    | TYPE 
    | RECORD
    | FUNCTION
    | CLASS
    | SERVICE
    | APPLICATION
    | PROGRAM
    | COMPONENT
    | TRAIT
    | TEXT
    | HANDLE
    | ISOLATED
    | RANGE
    | IS
    | BY
    | WITH
    | ONLY
    | PURE
    | HTTP_QUERY
    | HTTP_PATH
    | HTTP_HEADER
    | HTTP_REQUEST
    | HTTP_CONTENT
    | HTTP_CONTEXT
    ;

range
    : (expression) ELLIPSIS (expression)
    ;

literal
    : SUB? integerLit #integerLiteral
    | SUB? floatingPointLit #floatingPointLiteral
    | binaryLit #binaryLiteral
    | booleanLit #booleanLiteral
    | characterLit #characterLiteral
    | stringLit #stringLiteral
    | timeLit #timeLiteral
    | dateLit #dateLiteral
    | dateTimeLit #dateTimeLiteral
    | durationLit #durationLiteral
    | SUB? millisecondLit #millisecondLiteral
    | SUB? dimensionLit #decorationDimensionLiteral
    | decorationResolutionLit #decorationResolutionLiteral
    | colourLit #colourLiteral
    | SUB? moneyLit #moneyLiteral
    | regExLit #regularExpressionLiteral
    | versionNumberLit #versionNumberLiteral
    | pathLit #pathLiteral
    ;

integerLit
    : IntegerLiteral
    ;

binaryLit
    : BinaryLiteral
    ;
        
floatingPointLit
    : FloatingPointLiteral
    ;
    
booleanLit
    : BooleanLiteral
    ;
    
characterLit
    : CharacterLiteral
    ;
    
stringLit
    : StringLiteral
    | OPEN_STRING stringPart* CLOSE_STRING
    ;

stringPart
    : STRING_TEXT
    | ENTER_EXPR_INTERPOLATION expression RBRACE
    ;

timeLit
    : TimeLiteral
    ;
    
dateLit
    : DateLiteral
    ;
    
dateTimeLit
    : DateTimeLiteral
    ;
    
durationLit
    : DurationLiteral
    ;

millisecondLit
    : MillisecondLiteral
    ;

regExLit
    : RegExLiteral
    ;

versionNumberLit
    : VersionNumberLiteral
    ;

decorationResolutionLit
    : DecorationResolutionLiteral
    ;
    
dimensionLit
    : DecorationDimensionLiteral
    ;
    
colourLit
    : ColourLiteral
    ;

pathLit
    : PathLiteral
    ;

moneyLit
    : MoneyLiteral
    ;

accessModifier
    : PUBLIC
    | PROTECTED
    | PRIVATE
    ;

//EOF
