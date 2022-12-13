package org.ek9lang.compiler.internals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Just loads the ek9 language builtin ek9 source code and supplies it as Compilable Source.
 * I had intended to use a resource with the source code in. But when I tried a native build
 * with 'native-image --no-fallback -jar ek9.jar' and ran the executable I got a nullPointer.
 * So I guessed it with the resource loading
 * 'Ek9BuiltinLangSupplier.class.getResource("/builtin/org/ek9/lang/builtin.ek9")'.
 * Now I just hold the text as static strings and build up the source. May stick with this!
 * This does mean that the formatted String in here will become numerous as each class is fleshed out.
 * So may need to use other classes to hold related stuff to keep this down - but this would have
 * happened in an external resource anyway. I quite like the idea of the source build deep in the
 * code here.
 */
public class Ek9BuiltinLangSupplier implements Supplier<List<CompilableSource>> {

  ///Obviously with ek9 the indentation is important.

  @Override
  public List<CompilableSource> get() {

    return List.of(new CompilableSource("org-ek9-lang.ek9", getOrgEk9LangDeclarations()),
        new CompilableSource("org-ek9-math.ek9", getOrgEk9MathDeclarations()));
  }

  private InputStream getOrgEk9LangDeclarations() {
    var sources = List.of(orgEk9LangPreamble,
        definesClass,
        defineStringClass,
        defineBuiltInTypeClasses,
        definesFunction,
        defineBuiltInTemplateFunctions,
        definesClass,
        defineBuiltInTemplateClasses,
        definesTrait,
        defineBuiltInTraits,
        definesFunction,
        defineStandardFunctions,
        definesClass,
        defineStandardClasses,
        definesFunction,
        defineNetworkFunctions,
        definesTrait,
        defineNetworkTraits,
        definesClass,
        defineNetworkClasses,
        definesRecord,
        defineNetworkRecords,
        definesClass,
        defineAspectClasses
    );
    return new ByteArrayInputStream(String.join("", sources).getBytes());
  }

  private InputStream getOrgEk9MathDeclarations() {
    var sources = List.of(orgEk9MathPreamble, definesConstant, defineMathConstants);
    return new ByteArrayInputStream(String.join("", sources).getBytes());
  }

  private final String orgEk9LangPreamble = """
#!ek9
defines extern module org.ek9.lang

  defines package
  
    version <- 0.0.1-0
  
    description <- "Builtin EK9 language constructs."
  
    tags <- [
      "types"
      ]
  
    license <- "MIT"
""";
  
  private final String definesClass = """
  defines class
""";

  private final String definesTrait = """
  defines trait
""";

  private final String definesFunction = """
  defines function
""";

  private final String definesRecord = """
  defines function
""";

  private final String definesConstant = """
  defines constant
""";

  /**
   * Ready to start fleshing out String.
   */
  private final String defineStringClass = """

    String
      String()    
""";

  /**
   * As each type is fleshed out pull it out of the list and create a new full signature.
   */
  private final String defineBuiltInTypeClasses = """

    Boolean
      Boolean()

    Character
      Character()

    Integer
      Integer()

    Float
      Float()

    Bits
      Bits()

    Time
      Time()

    Duration
      Duration()

    Millisecond
      Millisecond()

    Date
      Date()

    DateTime
      DateTime()

    Money
      Money()

    Locale
      Locale()

    Colour
      Colour()

    Dimension
      Dimension()

    Path
      Path()

    JSON
      JSON()

    JSONInput as abstract

    RegEx
      RegEx()
""";

  private final String defineBuiltInTemplateFunctions = """
    Supplier of type T
      <- r as T

    Consumer of type T
      -> t as T

    BiConsumer of type (T, U)
      ->
        t as T
        u as U

    UnaryOperator of type T
      -> t as T
      <- r as T

    Function of type (T, R)
      -> t as T
      <- r as R

    Predicate of type T
      -> t as T
      <- r as Boolean

    BiPredicate of type (T, U)
      ->
        t as T
        u as U
      <-
        r as Boolean

    Comparator of type T
      ->
        t1 as T
        t2 as T
      <-
        r as Integer
""";

  private final String defineBuiltInTemplateClasses = """
    List of type T
      List()

    Optional of type T
      Optional()

    PriorityQueue of type T
      PriorityQueue()

    Dict of type (K, V)
      Dict()

    DictEntry of type (K, V)
      DictEntry()

    Iterator of type T
      Iterator()
""";

  private final String defineBuiltInTraits = """
    Clock

    StringInput

    StringOutput
""";

  private final String defineStandardFunctions = """
    SignalHandler() as abstract
      -> value as String
      <- result as Integer

    <!- By its nature it is abstract -!>
    MutexKey() of type T
      -> value as T
""";

  private final String defineStandardClasses = """
    SystemClock with trait of Clock
      SystemClock()

    Stdin with trait of StringInput
      Stdin()

    Stdout with trait of StringOutput
      Stdout()

    Stderr with trait of StringOutput
      Stderr()

    TextFile
      TextFile()

    FileSystem
      FileSystem()

    FileSystemPath
      FileSystemPath()

    <!- Operating System -!>
    OS
      OS()

    GUID
      GUID()

    HMAC
      HMAC()

    Signals
      Signals()

    EnvVars
      EnvVars()

    GetOpt
      GetOpt()

    Version
      Version()

    MutexLock of type T
      MutexLock()
""";

  private final String defineNetworkFunctions = """
    TCPHandler
      ->
        input as StringInput
        output as SpringOutput
""";

  private final String defineNetworkTraits = """
    HTTPRequest

    HTTPResponse

    TCPConnection
""";

  private final String defineNetworkClasses = """
    UDP
      UDP()

    TCP
      TCP()
""";

  private final String defineNetworkRecords = """
    UDPPacket
      UDPPacket()
""";

  private final String defineAspectClasses = """
    Aspect
      Aspect()

    JoinPoint
      JoinPoint()

    PreparedMetaData
      PreparedMetaData()
""";

  private final String orgEk9MathPreamble = """
#!ek9
defines extern module org.ek9.math

  defines package
  
    version <- 0.0.1-0
  
    description <- "Builtin EK9 mathematics constructs."
  
    tags <- [
      "constants",
      "algorithms"
      ]
  
    license <- "MIT"
""";

  private final String defineMathConstants = """
    PI <- 3.141592653589793238
    e <- 2.7182818284
    root2 <- 1.41421356237309504880
""";
}
