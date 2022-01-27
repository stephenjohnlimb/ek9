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

The initial implementation will be written in Java (17), all code must have unit tests and code coverage analysis is employed.

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


### Phase of development

#### Phase 1
The first version of the grammar is being finalised. All the EK9 source code examples, together with a Lexer/Parser in
Java will be contributed.

'Fuzzer' contributions required to automatically generate code that won't quite 'parse'. Ensure parsing and lexing
gives suitable and accurate locations of where the errors are.

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
Code generation, initially produce Java source code an an output from the Intermediate Representation.
If there is sufficient interest in generating different output formats, for example LLVM or MSIL or even Java Byte code then
those projects can be run in parallel to the main focus of generating Java source code for the compiler.

#### Phase 7
Wrap the resulting compiler (java jar) in native executables/packages for Linux, Windows and MacOS

#### Phase 8
Get version 0.1.0 out of the door so that defects and issues can be found with a range of real code by as many
developers as possible.


### Phase 9 
Fix more problems
...
