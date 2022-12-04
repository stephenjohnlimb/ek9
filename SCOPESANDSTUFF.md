# Scopes Symbols and Other Compiler type stuff

Firstly compilers are complex, so let's just put that out there.

The EK9 compiler uses ANTLR4 to do all the lexing and parsing. So that's one job we don't need to do.
The initial output with be Java byte codes using ASM (so there's another job we don't have to do.)

## Symbols and Scopes

We need to define a range of Symbols - these are used both for `types` and `variables`, just the context of
use is different.

There are different type of `scope` object as well. These tend to be `BLOCKS', methods, aggregates and
modules. So variable can come in and out of `scope`.

## How does the EK9 Compiler visit the AST?

It actually uses the ANTLR visitor pattern and uses a `stack of scopes` pushing and popping these scopes
on entering and leaving EK9 scope boundaries. But at the same time it records scopes in a `parsedModule` against
an ANTLR TreeNode.

This can (and does) take a bit of getting your head around. Especially with the EK9 `tuple`/`dynamic classes and functions`
as these can be defined well down in the middle of other classes/functions - but get hoisted up to module
level scope.

Now this sounds quite weird as it looks like they've 'popped' out of a scope. In some ways they have!

