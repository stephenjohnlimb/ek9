# The EK9 Programming Language

![GitHub top language](https://img.shields.io/github/languages/top/stephenjohnlimb/ek9)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/stephenjohnlimb/ek9)
![GitHub Workflow Status (event)](https://img.shields.io/github/actions/workflow/status/stephenjohnlimb/ek9/build.yml?branch=main)
[![codecov](https://codecov.io/gh/stephenjohnlimb/ek9/branch/main/graph/badge.svg?token=F8MMCBREMB)](https://codecov.io/gh/stephenjohnlimb/ek9)
![GitHub](https://img.shields.io/github/license/stephenjohnlimb/ek9)
![Twitter Follow](https://img.shields.io/twitter/follow/stephenjohnlimb?label=Follow%20EK9&style=social)

## Overview
Focused on the development a new programming language (yes another one).
Much as been done already in terms of research/development and the definition of the language grammar (ANTLR4).
Lots of [worked examples](https://www.ek9lang.org/index.html#examples) have been created to get the feel of what the
language will be like once the first reference implementation of the compiler is complete.

### Thanks to JetBrains for making the full version of IDEA available
Jetbrains have supported the development of EK9 for several years now, see
[JetBrains opensource support](https://www.jetbrains.com/community/opensource/#support) for more details.
This is much appreciated as IntelliJ IDEA is without doubt one of the best development tools there is, it has made the development
of EK9 much easier.

### Java and binaries
The base version of Java for this project is now Java 21. I'm now using the lightweight threading mechanism in
Java 21 to make the compiler as multithreaded as possible (I mean how hard can it be!).

I've also taken a look at [Graalvm](https://www.graalvm.org) to see if I can create a native application from the
Java compiler. My main reason for doing this - is general interest. But a native application should also start and
run faster. See the [instructions](https://www.graalvm.org/java/quickstart/) and
[how to install native-image](https://www.graalvm.org/22.3/reference-manual/native-image/#install-native-image).

For this project, once you've built the 'fat jar' (`mvn clean install`), you can use the following command:
`native-image --no-fallback  -jar ek9c-jar-with-dependencies.jar` to create a native binary.

You can then just run that application `./ek9` or send it to someone; and they can run it (must be same OS);
but they don't need Java installed (i.e. it is a full standalone binary).

Now this is early days and there's some gotchas with reflection and proxies
(mainly in the lsp libraries being used). But I'm currently focussing on the command-line compiler.
So for now the language-server still has to be run with java as a jar. But I'll get there.

### Main Entry Point

The Java source `org.ek9lang.cli.Ek9.java` is the main entry point in and
`org.ek9lang.compiler.Ek9Compiler.java` is where the actual compiler is.
Though this is split into many 'phases' as the EK9 compile is a multi-pass compiler.
Some of these phases are single-threaded and some are multi-threaded.
See [JavaDoc](https://repo.ek9lang.org/apidocs/0.0.1-SNAPSHOT/index.html) for detils on the layout.

### Blog
If you want to see why specific decisions have been made on the details of the language then you can follow the [EK9 blog](https://blog.ek9.io/).

### Hello World Example
The [EK9](https://www.ek9lang.org) website has many more examples; but this short sample should give you a feel for the language.

    #!ek9
    defines module introduction
      defines program
        HelloWorld()
          stdout <- Stdout()
          stdout.println("Hello, World")
    //EOF

### Development Environment
To help developers get started there is a [VSCode Extension](https://github.com/stephenjohnlimb/vscode-ek9-ls) that has code snippets and syntax highlighting.

Once the compiler has been fully developed, the extension will be published, but at present it is only available as source code from GitHub.

### Structure and Source Code Layout
Broadly speaking EK9 layout is a mix of Python and YAML style with indentation and whitespace rather than **{** **}**.
This also includes the **Pascal** type approach of creating sections for **constructs** (i.e., as section for constants).

### Source Files
EK9 source files can contain any number of **constructs** and files must have a name ending in '**.ek9**'.
External build/make/poms are redundant by including a [package construct](https://www.ek9lang.org/packaging.html)
into the language itself.

## Contributing

See [Contributing](CONTRIBUTING.md) for lots of detail on where EK9 is at the moment and supporting technologies.
The [WIKI](https://github.com/stephenjohnlimb/ek9/wiki/EK9-Development) for details.

## Why create another programming language?
Frustration with other languages, like C, C++, Pascal, Java and to some extent Python. I love them all in
the way they are - but really wanted feature X from one language in another (and add a few new things in as well)!

The aim is a language that is easy (trivial) to get started with, but can scale in terms of **constructs** and
more advanced **software design concepts**. EK9 has been designed to make easy things; 'easy' and harder things;
'structured and coherent'. This approach has been taken to facilitate EK9 language use right through a developers career.

The aim is for very young developers (aged ten or so) to be able to use some language features immediately.
As developers become more experienced they can employ more advanced capabilities (in the same language).

EK9 is not a 'teaching language', but it does have characteristics that make it very suitable as an introduction
to programming. It builds upon a variety of software concepts in layers; each of which is progressively more
sophisticated.

Capabilities like **functions** are treated as peers of **classes**. 
[Composition](https://www.ek9lang.org/composition.html#composition_example) can be used just as easily as
[Inheritance](https://www.ek9lang.org/inheritance.html) .
The adoption of indentation block structure is employed to remove the excessive use of **< >** and **{ }** when
mixed with **( )** in some languages can be confusing. 

The language retains the structural/controlling mechanisms from Java and Scala where **Interfaces and Traits** enable
strong polymorphic typing.

It includes **Type Inference**, [Operators](https://www.ek9lang.org/operators.html) and
[design patterns](https://www.ek9lang.org/advancedClassMethods.html) in the language itself. It does not use
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
If you take a look at any of the examples in the [web site](https://www.ek9lang.org/index.html#examples)
or from the [examples source](https://github.com/stephenjohnlimb/ek9/tree/main/src/test/resources/examples); you can draw your own
conclusions. Is it the language I set out to design; - probably not! It is the one I iterated to
by trying to balance feature X over feature Y.

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

There is another [example](https://www.ek9lang.org/composition.html#composition_example) that is based on promoting a
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

Even though I've been tinkering with this for many years, I'm more excited and enthusiastic about it now than I've
ever been. Having taken some quite long layoffs from it has actually helped me come back to it with fresh eyes and
ideas.

For example - I've just added a sort of [jsonPath](https://www.ek9.io/builtInTypes.html#path) built-in type with
compiler checked literal syntax checking (nice). Why add this in? Accessing paths through structured graphs of
objects/arrays is pretty much a fundamental programming need! Be it JSON, XML, or any object graph. 

## License and Trademark
It is licensed under the MIT license; see [License](LICENSE) and
[Trademark](https://www.ek9lang.org/tradeMarkPolicy.html).

