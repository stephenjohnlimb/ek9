# The EK9 Programming Language

## Overview
This is project is aimed at developing a new programming language (yes another one).
Much as been done already in terms of research and the definition of the grammar (ANTLR4).
With lots of [worked examples](https://www.ek9lang.org/index.html#examples) - so as to get the feel of what the language will be like once the first reference implementation of the compiler is complete.

### Why create another programming language?
Frustration with other languages, like C, C++, Pascal and Java and so some extent Python. I love them all in
the way they are - but really wanted feature X from one language in another!

But I also wanted a language that was easy (trivial) to get started with, but could scale in terms of **constructs** and
more advanced **software design concepts**. So it has been designed to make easy things; 'easy' and harder things; 'structured and coherent'. This approach has been taken to facilitate EK9 language use right through a developers career.

The aim is for very young developers (aged ten or so) to be able to use some of the language features immediately; but then as they become more experienced employ some of the more advanced capabilities (in the same language).

Capabilities like **functions** should be treated as peers of **classes**. **Composition** should be used in more places where **inheritance** is currently employed.
Also I found the excessive use of **< >** and **{ }** when mixed with **( )** confusing - hence the adoption of indentation for structure (like Python). 

The language retains the structural/controlling mechanisms from Java and Scala where **Interfaces and Traits** enables strong polymorphic typing.

Including **Type Inference** and **design patterns** into the language itself; but not using **primitives** and only using **Objects** enables more consistent code.

Finally 'Symmetry'.

### Source Files
EK9 source files can contain any number of **constructs** and can have any name ending in **.ek9**.
External build/make/poms are made redundant by including a [package construct](https://www.ek9lang.org/packaging.html) into the language itself.

### Structure and Source Code Layout
Broadly speaking a mix of Python and YAML style layout with indentation and whitespace rather than **{** **}**. This also
includes the **Pascal** type approach of creating sections for **constructs** (i.e as section for constants).

### Constructs
With EK9 this means software design ideas like:
  - [Constants](https://www.ek9lang.org/constants.html)
  - [Enumerations](https://www.ek9lang.org/enumerations.html)  
  - [Records](https://www.ek9lang.org/records.html)    
  - [Functions](https://www.ek9lang.org/functions.html)    
  - [Classes](https://www.ek9lang.org/classes.html)    
  - [Traits/Interfaces](https://www.ek9lang.org/traits.html)    
  - [Components](https://www.ek9lang.org/components.html)    
  - [Applications](https://www.ek9lang.org/dependencyInjection.html)
  - [Services](https://www.ek9lang.org/webServices.html)

But EK9 also includes [pipeline processing](https://www.ek9lang.org/streamsAndPipelines.html)  similar to the Unix/Linux '**|**' mechanism.

### Hello World Example
The [EK9](https://www.ek9lang.org) web site has many more examples, but this should give you a feel.

    #!ek9
    defines module introduction
      defines program
        HelloWorld()
          stdout <- Stdout()
          stdout.println("Hello, World")
    //EOF
## The Result
It has taken quite a long time just to get to the point where the language has found balance, there have been several prototypes
and compilers to date. But now the grammar and the language (at least for the first release) does hang together and there aren't too many inconsistencies.

### How does it feel?
If you take a look at any of the [examples](https://www.ek9lang.org/index.html#examples) you will be able to draw your own
conclusions. Is it the language I set out to design; - probably not! It is the one I iterated to by trying to balance feature X over feature Y. It does need more contributions for other people now though.

In my opinion it feels 'light', 'scripty', 'dynamic', 'balanced' and 'coherent'. It is not 'dense' in terms of source code
line length, program flow is very easy to comprehend. But it is opinionated and forces a specific development approach in
certain circumstances.

The EK9 language forces 'white space' through its layout it has the following
[features](https://www.ek9lang.org/introduction.html#main_features).

### What's it like to code in?
As EK9 facilitates both a **functional** and an **object** oriented style; you can use it in a variety of ways. You can just code like
you would in Java or C/C++/C# or you could take a totally functional approach. But really the value and power comes when you force
yourself to combine the two approaches. Initially this took me some time to 'understand', if you look at
[this example](https://www.ek9lang.org/standardTypes.html#worked_example) (which is quite long); you will probably see what I mean.

There is quite an involved explanation of the code in the example, but the example does start out with a documented requirement and covers alternative approaches.

There is another [example](https://www.ek9lang.org/composition.html/composition_example) that is based on promoting a
**compositional** design. Originally the external site example was in C++ and then adapted to
[Python](https://realpython.com/inheritance-composition-python/). If you compare the EK9 and Python code you'll see if is very
similar. So you can just code EK9 in a sort of Python style if you wish - then slowly start to incorporate the new features of EK9 in as and when you feel the need.

So as EK9 has been inspired by a range of different languages, you have the opportunity to use many of the same ideas from those
languages (but the layout is most similar to Python). This makes for an easy transition should you elect to adopt EK9.

EK9 adds additional structure and type safety to Python (as well as being compiled). It adds simplicity/clarity of syntax, 
**functions** and **operators** to languages like Java. Moreover it adds a standard **fluent** type **functional** processing
pipeline flow not avialable in most languages.

## Features
If you've read the sections above and followed any of the links to the [EK9 web site](https://www.ek9lang.org); you'll see that
it is an amalgamation of several existing language ideas and scripting/shell concepts with just a few new (and some very old) ideas included.

It is designed to be **compiled** and have the light feeling of being **interpreted** for details.

## License and Trademark
It is licensed under the MIT license; see [License](https://www.ek9lang.org/LICENSE.txt) and
[Trademark](https://www.ek9lang.org/tradeMarkPolicy.html).

## Contributing

See [Contributing](CONTRIBUTING.md) for details on what we need next.
