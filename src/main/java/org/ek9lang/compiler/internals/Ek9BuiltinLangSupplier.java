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
    //Note define the package at the end - because we need to define basic types first.
    //Normally the package would come first by convention.
    var sources = List.of(ORG_EK_9_LANG_PREAMBLE,
        DEFINES_CLASS,
        DEFINE_STRING_CLASS,
        DEFINE_BUILT_IN_TYPE_CLASSES,
        DEFINES_FUNCTION,
        DEFINE_BUILT_IN_TEMPLATE_FUNCTIONS,
        DEFINES_CLASS,
        DEFINE_BUILT_IN_TEMPLATE_CLASSES,
        DEFINES_TRAIT,
        DEFINE_BUILT_IN_TRAITS,
        DEFINES_FUNCTION,
        DEFINE_STANDARD_FUNCTIONS,
        DEFINES_CLASS,
        DEFINE_STANDARD_CLASSES,
        DEFINES_FUNCTION,
        DEFINE_NETWORK_FUNCTIONS,
        DEFINES_TRAIT,
        DEFINE_NETWORK_TRAITS,
        DEFINES_CLASS,
        DEFINE_NETWORK_CLASSES,
        DEFINES_RECORD,
        DEFINE_NETWORK_RECORDS,
        DEFINES_CLASS,
        DEFINE_ASPECT_CLASSES,
        DEFINES_LANGUAGE_PACKAGE
    );
    return new ByteArrayInputStream(String.join("", sources).getBytes());
  }

  private InputStream getOrgEk9MathDeclarations() {
    var sources = List.of(ORG_EK9_MATH_PREMABLE, DEFINES_CONSTANT, DEFINE_MATH_CONSTANTS);
    return new ByteArrayInputStream(String.join("", sources).getBytes());
  }

  @SuppressWarnings({"Indentation"})
  private static final String ORG_EK_9_LANG_PREAMBLE = """
#!ek9
defines extern module org.ek9.lang
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_LANGUAGE_PACKAGE = """
  defines package
  
    version <- 0.0.1-0
  
    description <- "Builtin EK9 language constructs."
  
    tags <- [
      "types"
      ]
  
    license <- "MIT"
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_CLASS = """
  defines class
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_TRAIT = """
  defines trait
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_FUNCTION = """
  defines function
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_RECORD = """
  defines record
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINES_CONSTANT = """
  defines constant
""";

  /**
   * Ready to start fleshing out String.
   */
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_STRING_CLASS = """

    String
      String()    
""";

  /**
   * As each type is fleshed out pull it out of the list and create a new full signature.
   */
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TYPE_CLASSES = """

    Binary
      Binary()
      
    Boolean
      Boolean()

    Character
      Character()

    Integer
      Integer()

    Float
      Float()

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

    Resolution
      Resolution()
      
    Path
      Path()

    JSON
      JSON()

    JSONInput as abstract

    RegEx
      RegEx()
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_FUNCTIONS = """
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

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_CLASSES = """
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

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TRAITS = """
    Clock

    StringInput

    StringOutput
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_STANDARD_FUNCTIONS = """
    SignalHandler() as abstract
      -> value as String
      <- result as Integer

    <!- By its nature it is abstract -!>
    MutexKey() of type T
      -> value as T
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_STANDARD_CLASSES = """
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

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_FUNCTIONS = """
    TCPHandler
      ->
        input as StringInput
        output as SpringOutput
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_TRAITS = """
    HTTPRequest

    HTTPResponse

    TCPConnection
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_CLASSES = """
    UDP
      UDP()

    TCP
      TCP()
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_RECORDS = """
    UDPPacket
      UDPPacket()
""";

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_ASPECT_CLASSES = """
    Aspect
      Aspect()

    JoinPoint
      JoinPoint()

    PreparedMetaData
      PreparedMetaData()
""";

  @SuppressWarnings({"Indentation"})
  private static final String ORG_EK9_MATH_PREMABLE = """
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

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_MATH_CONSTANTS = """
    PI <- 3.141592653589793238
    e <- 2.7182818284
    root2 <- 1.41421356237309504880
""";
}
