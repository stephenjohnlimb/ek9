# Contributing to EK9

Any sort of positive contribution is welcome. Whether it is improvements to documentation, testing, **coding** and
especially:
  - 'fuzzing' as this is a major knowledge gap at present.
  - 'ANTL4 grammar' this is an area where many eyes make a big difference

On the documentation side, any improvements to the website or these "*.md" documents in terms of:
  - typo corrections
  - better more succinct grammar
  - clearer wording and explanations

## Code of Conduct
See contribution [ code of conduct](CODE_OF_CONDUCT.md) for details of what conduct is acceptable.

## What/How to contribute?
The development of reference implementation of the compiler is under way and is open for contributions.
See the [WIKI](https://github.com/stephenjohnlimb/ek9/wiki/EK9-Development) to see how the project is structured and
how you can run and debug the grammar with some EK9 source code.

The initial implementation will be written in Java (19), all code must have unit tests and code coverage analysis is employed.

At this point in time; focus is on getting the existing grammar robust and resilient -
rather than adding more features or capabilities in.

Having written all the examples and actually used the EK9 language to accomplish a few things
(even with just the basic prototype); the reference implementation can now be built.

Ideally I'd like a hand doing this, as it's a very big job. More importantly I'd like to share the experience with
like-minded people (from wherever they are in the world and whatever background they come from).

I've found using the EK9 language has enabled me to review how I code and produce structures and mechanisms that
I would not normally have employed. I've found this a breath of fresh air. I'd like as many people as possible
to at least try EK9 out as an alternative to other main stream languages.

I've spent about ten years on and off getting this far - most of that time has been spent throwing away code!
I can now see a linear (but hard) path to building the reference implementation of the compiler.

I'm expecting this next phase to take about another ten years - maybe with some help we could get that time down!

### Supporting tooling
Having grappled with lexing and parsing issues - I've built on top of Antlr and included:
- [Ek9Support](src/test/java/org/ek9lang/compiler/common/Ek9Support.java) - this show a graphical AST of an EK9 source file.
- [ASMSupport](src/test/java/org/ek9lang/compiler/common/ASMSupport.java) - this shows the ASM code (java bytecode) you'd need to write for a class.

There's also a bit on the [Symbols and Scopes](SCOPESANDSTUFF.md) - I'll probably add a bit more to this as
I find this area quite confusing (even though I'm writing it - I am a bit beyond my capabilities TBH).

### Phase of development

During the previous prototype compiler work, I focussed on broad areas of functionality, trying to
tease out all the intricacies. I'm now focussing on getting something fully end-to-end, folding in the lessons of the
prototypes. So I'll start with the full grammar, but will cut thin slices through the whole compiler to go
fully end to end with each slice.

With this in mind, I'll start with a single program, statement and expression (`Hello World`), but actually
integrate the whole compiler to produce Java byte code directly (for the Java target).
I'm planning on using `graalvm` to produce binaries of the ek9 compiler.

#### Phase 1
The first version of the grammar ~~is being~~ has been finalised. All the EK9 source code examples,
together with a Lexer/Parser in Java ~~will be~~ have been contributed.

#### Phase 2
Symbol table creation, phased parsing to populate symbol tables

#### Phase 3
Revisit phased parsing add in error check that can be completed early in compilation phase
More 'Fuzzer' contributions required. Aimed at code the will Lex and Parse but should not compile
(i.e., missing declarations, etc).

#### Phase 4
Expand parsing phases to deal with Templates/Generics

#### Phase 5
Create an Intermediate Representation that is sufficiently detailed that any number and range or target output
languages can be generated. 

#### Phase 6
Code generation, initially produce Java source code an output from the Intermediate Representation.
If there is sufficient interest in generating different output formats, for example LLVM or MSIL or even Java Byte code then
those projects can be run in parallel to the main focus of generating Java source code for the compiler.

#### Phase 7
Wrap the resulting compiler (java jar) in native executables/packages for Linux, Windows and MacOS. Graalvm.

#### Phase 8
Get version 0.1.0 out of the door so that defects and issues can be found with a range of real code by as many
developers as possible.

### Phase 9 
'Fuzzer' contributions required to automatically generate code that won't quite 'parse'. Ensure parsing and lexing
gives suitable and accurate locations of where the errors are.

I see phases 2-9 being repeated over and over as I add functionality in an incremental manner. i.e.
thin slices through a 'walking skeleton'. The exception being the grammar which is pretty much fully complete for the
first release. I've done this because I see that many language fail to gracefully incorporate new syntax in
once the 'die is cast'.
