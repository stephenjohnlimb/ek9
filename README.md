# The EK9 Programming Language

![GitHub top language](https://img.shields.io/github/languages/top/stephenjohnlimb/ek9)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/stephenjohnlimb/ek9)
![GitHub Workflow Status (event)](https://img.shields.io/github/workflow/status/stephenjohnlimb/ek9/Java%20CI?event=push)
[![codecov](https://codecov.io/gh/stephenjohnlimb/ek9/branch/main/graph/badge.svg?token=F8MMCBREMB)](https://codecov.io/gh/stephenjohnlimb/ek9)
![GitHub](https://img.shields.io/github/license/stephenjohnlimb/ek9)
![Twitter Follow](https://img.shields.io/twitter/follow/stephenjohnlimb?label=Follow%20EK9&style=social)

## Overview
Focused on the development a new programming language (yes another one).
Much as been done already in terms of research/development and the definition of the language grammar (ANTLR4).
Lots of [worked examples](https://www.ek9lang.org/index.html#examples) have been created to get the feel of what the
language will be like once the first reference implementation of the compiler is complete.

### Hello World Example
The [EK9](https://www.ek9lang.org) website has many more examples, but this short sample should give you a feel for the language.

    #!ek9
    defines module introduction
      defines program
        HelloWorld()
          stdout <- Stdout()
          stdout.println("Hello, World")
    //EOF

### Structure and Source Code Layout
Broadly speaking layout is a mix of Python and YAML style with indentation and whitespace rather than **{** **}**.
This also includes the **Pascal** type approach of creating sections for **constructs** (i.e., as section for constants).

### Source Files
EK9 source files can contain any number of **constructs** and files must have a name ending in **.ek9**.
External build/make/poms are redundant by including a [package construct](https://www.ek9lang.org/packaging.html)
into the language itself.

## Contributing

See [Contributing](CONTRIBUTING.md) and the [WIKI](https://github.com/stephenjohnlimb/ek9/wiki/EK9-Development) for details.

## Why create another programming language?
Frustration with other languages, like C, C++, Pascal, Java and so some extent Python. I love them all in
the way they are - but really wanted feature X from one language in another (and add a few new things in)!

The aim is a language that is easy (trivial) to get started with, but can scale in terms of **constructs** and
more advanced **software design concepts**. EK9 has been designed to make easy things; 'easy' and harder things;
'structured and coherent'. This approach has been taken to facilitate EK9 language use right through a developers career.

The aim is for very young developers (aged ten or so) to be able to use some language features immediately.
As developers become more experienced they can employ more advanced capabilities (in the same language).

EK9 is not a 'teaching language', but it does have characteristics that make it very suitable as an introduction
to programming. It builds upon a variety of software concepts in layers each of which is progressively more
sophisticated.

Capabilities like **functions** are treated as peers of **classes**. **Composition** can be used just as easily as
**inheritance** .
Excessive use of **< >** and **{ }** when mixed with **( )** in some languages can be confusing;
hence the adoption of indentation for block structure. 

The language retains the structural/controlling mechanisms from Java and Scala where **Interfaces and Traits** enable
strong polymorphic typing.

It includes **Type Inference**, [Operators](https://www.ek9lang.org/operators.html) and **design patterns** in the language itself. It does not use
**Primitives**, only **Objects**; enabling more consistency.

**Null/Nill** does not exist in the EK9 language, neither does any form of **casting**.

The features and characteristics above were the main drivers for creating a new language.

### Constructs
With EK9; **construct** means software design ideas like:
  - [Constants](https://www.ek9lang.org/constants.html)
  - [Enumerations](https://www.ek9lang.org/enumerations.html)  
  - [Records](https://www.ek9lang.org/records.html)    
  - [Functions](https://www.ek9lang.org/functions.html)    
  - [Classes](https://www.ek9lang.org/classes.html)    
  - [Traits/Interfaces](https://www.ek9lang.org/traits.html)    
  - [Components](https://www.ek9lang.org/components.html)    
  - [Applications](https://www.ek9lang.org/dependencyInjection.html)
  - [Services](https://www.ek9lang.org/webServices.html)

EK9 has most of the standard [flow control](https://www.ek9lang.org/flowControl.html) mechanisms of **for loops, if/else** and **switch statements**,
but also includes [pipeline processing](https://www.ek9lang.org/streamsAndPipelines.html); similar to the Unix/Linux
'**pipe**' mechanism.

## The Result
It has taken quite a long time just to get to the point where the language has found balance, there have been several
prototypes and proto-compilers to date. Now the grammar and the language (at least for the first release) does hang
together and there aren't that many inconsistencies.

### How does it feel?
If you take a look at any of the [examples](https://www.ek9lang.org/index.html#examples) you can draw your own
conclusions. Is it the language I set out to design; - probably not! It is the one I iterated to
by trying to balance feature X over feature Y. It does need more contributions from other people now though.

In my opinion it feels 'light', 'scripty', 'dynamic', 'balanced', 'coherent' and 'symmetrical'. It is not 'dense' in
terms of source code line length, program flow is very easy to comprehend. The EK9 language it is opinionated and
forces a specific development approach in certain circumstances.

The EK9 language forces 'white space' through its layout; it has the following
[features](https://www.ek9lang.org/introduction.html#main_features).

### What's it like to code in?
As EK9 facilitates both a **functional** and an **object** oriented style; you can use it in a variety of ways.
You can just code like you would in Java or C/C++/C# or you could take a totally functional approach. Really the value
and power comes when you force yourself to combine the two approaches. Initially this took me some time to 'understand',
if you look at [this example](https://www.ek9lang.org/standardTypes.html#worked_example) (which is quite long);
you will probably see what I mean.

While it might sound strange as I created the language;
but took me quite some time to find the right blend and balance of using the different **constructs**.
The main reason for this was the adoption of a more **functional** approach
(which is not my main background - I'm not a devout Haskell or Lisp programmer). However, having tried this more
**functional** approach I can see real benefits of it rather than just using an **'OO'** approach.

There is another [example](https://www.ek9lang.org/composition.html/composition_example) that is based on promoting a
**compositional** design. Originally the external site example was in C++ and then adapted to
[Python](https://realpython.com/inheritance-composition-python/). If you compare the EK9 and Python code you'll see it
is very similar. So you can just code EK9 in a sort of Python style if you wish - then slowly start to incorporate
the new features of EK9 as and when you feel the need.

So as EK9 has been inspired by a range of different languages, you have the opportunity to use many of the same ideas
from those languages (but the layout is most similar to Python).
This makes for an easy transition should you elect to adopt EK9.

EK9 adds additional structure and type safety to Python (as well as being compiled). It adds simplicity/clarity of syntax, 
**functions** and **operators** to languages like Java. More over it adds a standard **fluent** type **functional**
processing pipeline flow that is not available in most languages.

## Features
If you've read the sections above and followed any of the links to the [EK9 web site](https://www.ek9lang.org);
you'll see that EK9 is an amalgamation of several existing language ideas and scripting/shell concepts.

EK9 is **compiled** but has the light feeling of being **interpreted**.

## License and Trademark
It is licensed under the MIT license; see [License](LICENSE) and
[Trademark](https://www.ek9lang.org/tradeMarkPolicy.html).

