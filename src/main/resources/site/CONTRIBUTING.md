# Contributing to EK9

We welcome any sort of positive contribution, whether is is improvements to documentation, testing, **coding** and
especially 'fuzzing' as this is a major knowledge gap at present.

## Code of Conduct

See [contribution code of conduct](CODE_OF_CONDUCT.md) for details of what conduct is acceptable.

## What to contribute

The development of reference implementation of the compiler is under way and is open for contributions.

The initial implementation will be written in Java, all code must have unit tests.

Corrections and improvements to the EK9 documentation web site are welcome as are reviews and ideas surrounding the grammar.
But at this point in time we're really focused on getting the existing grammar robust and resilient - rather than adding more features or capabilities in.

Having written all the examples and actually used the EK9 language to accomplish a few things (even with just the basic prototype).
I now feel ready to built the reference implementation.

Ideally I'd like a hand doing this, as it's a very big job. But more importantly I'd like to share the experience with like
minded people (from wherever they are are in the world and whatever background they come from).

I've found using the EK9 language has enabled me to review how I code and produce structures and mechanisms that I would not
normally have employed. I've found this a breath of fresh air. I'd like as many people as possible to at least try EK9 out as an
alternative to other main stream languages.

I've spent about ten years on and off getting this far - most of that time has been spent throwing away code! But I can now see
a linear (but hard) path to building the reference implementation of the compiler.

I'm expecting this next phase to take about another ten years - maybe with some help we could get that time down!

### Phase of development

#### Phase 1

The first grammar has now been finalised and all the EK9 source code examples, together with a Lexer/Parser in Java
will be contributed.

'Fuzzer' contributions required to automatically generate code that won't quite 'parse'.
Ensure parsing and lexing gives suitable and accurate locations of where the errors are and importantly the compiler does not crash.

#### Phase 2

Symbol table creation, phased parsing to populate symbol tables, this is now pretty much complete.

#### Phase 3

Revisit phased parsing add in error checks that can be completed early in compilation phase, this has been done for the first
three phases of the compiler. But I'm sure additional 'checks' will be required as more examples and defects are found.

#### Phase 4

Expand parsing phases to deal with Templates/Generics. This was particularly challenging and there may still be some 'edge cases' that
will cause issues. Only time and more examples will tell.

#### Phase 5

Complete all parts of 'calls' and 'expressions', this is likely to take some time as there are a lot of paths to cover.

#### Phase 6

Create an Intermediate Representation that is sufficiently detailed that any number and range or target output languages can be
generated.

#### Phase 7

Code generation, initially produce Java source code an an output from the Intermediate Representation.
If there is sufficient interest in generating different output formats, for example LLVM or MSIL or even Java Byte code then
those projects can be run in parallel to the main focus of generating Java source code for the compiler.

#### Phase 8

Wrap the resulting compiler (java jar) in native executables/packages for Linux, Windows and MacOS (Graalvm for example).
Maybe also consider using 'jpackage' to create a complete deployable Java compiler deliverable that includes the JVM. 
