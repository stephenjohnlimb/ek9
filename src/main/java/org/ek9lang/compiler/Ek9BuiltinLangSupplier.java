package org.ek9lang.compiler;

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

  /**
   * As we add more, update this.
   */
  public static final int NUMBER_OF_EK9_SYMBOLS = 75;

  //Obviously with ek9 the indentation is important.
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

          String as open
            String()
            
            String()
              -> arg0 as String

            trim() as pure
              <- rtn as String: String()
              
            upperCase() as pure
              <- rtn as String: String()

            lowerCase() as pure
              <- rtn as String: String()
                                       
            operator <=> as pure
              -> arg as String
              <- rtn as Integer?

            operator :=:
              -> arg as String

            operator +=
              -> arg as String
              
            operator + as pure
              -> arg as String
              <- rtn as String: String()
      """;
  /**
   * As each type is fleshed out pull it out of the list and create a new full signature.
   */
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TYPE_CLASSES = """

          Void
            Void()
            
          Bits as open
            Bits()
            
            Bits()
              -> arg0 as String

            operator <=> as pure
              -> arg0 as Bits
              <- rtn as Integer?

            operator :=:
              -> arg0 as Bits

            operator and as pure
              -> arg0 as Bits
              <- rtn as Bits?

            operator or as pure
              -> arg0 as Bits
              <- rtn as Bits?

            operator xor as pure
              -> arg0 as Bits
              <- rtn as Bits?

          Boolean
            Boolean()

            Boolean()
              -> arg0 as String

            operator :=:
              -> arg0 as Boolean

            operator and as pure
              -> arg0 as Boolean
              <- rtn as Boolean?

            operator or as pure
              -> arg0 as Boolean
              <- rtn as Boolean?

            operator xor as pure
              -> arg0 as Boolean
              <- rtn as Boolean?
              
          Character
            Character()

            Character()
              -> arg0 as String

            operator <=> as pure
              -> arg as Character
              <- rtn as Integer?

            operator :=:
              -> arg as Character

            operator ++
              <- rtn as Character?

            operator --
              <- rtn as Character?

          Integer as open
            Integer()

            Integer()
              -> arg0 as String

            operator #^ as pure
              <- rtn as Float?
            
            operator <=> as pure
              -> arg as Integer
              <- rtn as Integer?

            operator :=:
              -> arg as Integer

            operator +=
              -> arg as Integer

            operator ++
              <- rtn as Integer?

            operator --
              <- rtn as Integer?
                
          Float as open
            Float()

            Float()
              -> arg0 as String

            operator <=> as pure
              -> arg as Float
              <- rtn as Integer?

            operator :=:
              -> arg as Float

            operator +=
              -> arg as Float

            operator ++
              <- rtn as Float?

            operator --
              <- rtn as Float?

          Time as open
            Time()

            Time()
              -> arg0 as String

            operator <=> as pure
              -> arg as Time
              <- rtn as Integer?

            operator :=:
              -> arg as Time

            operator +=
              -> arg as Duration

            operator ++
              <- rtn as Time?

            operator --
              <- rtn as Time?

          Duration as open
            Duration()

            Duration()
              -> arg0 as String

            operator <=> as pure
              -> arg as Duration
              <- rtn as Integer?

            operator :=:
              -> arg as Duration

            operator +=
              -> arg as Duration

            operator ++
              <- rtn as Duration?

            operator --
              <- rtn as Duration?

          Millisecond as open
            Millisecond()

            Millisecond()
              -> arg0 as String

            operator <=> as pure
              -> arg as Millisecond
              <- rtn as Integer?

            operator :=:
              -> arg as Millisecond

            operator +=
              -> arg as Millisecond

            operator ++
              <- rtn as Millisecond?

            operator --
              <- rtn as Millisecond?

          Date as open
            Date()

            Date()
              -> arg0 as String

            operator <=> as pure
              -> arg as Date
              <- rtn as Integer?

            operator :=:
              -> arg as Date

            operator +=
              -> arg as Duration

            operator ++
              <- rtn as Date?

            operator --
              <- rtn as Date?

          DateTime as open
            DateTime()

            DateTime()
              -> arg0 as String

            operator <=> as pure
              -> arg as DateTime
              <- rtn as Integer?

            operator :=:
              -> arg as DateTime

            operator +=
              -> arg as Duration

            operator ++
              <- rtn as DateTime?

            operator --
              <- rtn as DateTime?

          Money as open
            Money()

            Money()
              -> arg0 as String

            operator <=> as pure
              -> arg as Money
              <- rtn as Integer?

            operator :=:
              -> arg as Money

            operator +=
              -> arg as Money

            operator ++
              <- rtn as Money?

            operator --
              <- rtn as Money?

          Locale as open
            Locale()

            Locale()
              -> arg0 as String

            operator <=> as pure
              -> arg as Locale
              <- rtn as Integer?

            operator :=:
              -> arg as Locale

          Colour as open
            Colour()

            Colour()
              -> arg0 as String

            operator <=> as pure
              -> arg as Colour
              <- rtn as Integer?

            operator :=:
              -> arg as Colour

            operator +=
              -> arg as Colour

          Dimension as open
            Dimension()

            Dimension()
              -> arg0 as String

            operator <=> as pure
              -> arg as Dimension
              <- rtn as Integer?

            operator :=:
              -> arg as Dimension

            operator +=
              -> arg as Dimension

            operator ++
              <- rtn as Dimension?

            operator --
              <- rtn as Dimension?

          Resolution as open
            Resolution()

            Resolution()
              -> arg0 as String

            operator <=> as pure
              -> arg as Resolution
              <- rtn as Integer?

            operator :=:
              -> arg as Resolution
            
          Path as open
            Path()

            Path()
              -> arg0 as String

            operator <=> as pure
              -> arg as Path
              <- rtn as Integer?

            operator :=:
              -> arg as Path

          JSON
            JSON()

            JSON()
              -> arg0 as String

            operator <=> as pure
              -> arg as JSON
              <- rtn as Integer?

            operator :=:
              -> arg as JSON

          JSONInput as abstract

          RegEx as open
            RegEx()

            RegEx()
              -> arg0 as String

            operator :=:
              -> arg as RegEx

          Exception as open
            Exception()
            
            Exception()
              -> reason as String
            
            Exception()
              ->
                reason as String
                exitCode as Integer
            
            information()
              <- rtn as String?
            
            exitCode()
              <- rtn as Integer?
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_FUNCTIONS = """
          Supplier of type T as abstract
            <- r as T?

          Consumer of type T as abstract
            -> t as T

          BiConsumer of type (T, U) as abstract
            ->
              t as T
              u as U

          UnaryOperator of type T as abstract
            -> t as T
            <- r as T?

          Function of type (T, R) as abstract
            -> t as T
            <- r as R?

          Predicate of type T as abstract
            -> t as T
            <- r as Boolean?

          BiPredicate of type (T, U) as abstract
            ->
              t as T
              u as U
            <-
              r as Boolean?

          Comparator of type T as abstract
            ->
              t1 as T
              t2 as T
            <-
              r as Integer?
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_CLASSES = """
          List of type T as open
            List()
            
            List()
              -> arg0 as T

            operator + as pure
              -> arg as T
              <- rtn as List of T?

            operator +=
              -> arg as T              
              
          Optional of type T
            Optional()
            
            Optional()
              -> arg0 as T

          Result of type (O, E)
            //default constructor without an ok value or an error
            Result()
                
            <?-
              Use above and call ok with the OK value.
            -?>
            ok()
              -> arg0 as O
            
            <?-
              Get the OK value, if not present Exception, so check.
            -?>
            ok()
              <- rtn as O?
            
            <?-
              Check if the result is Ok or not.
            -?>
            isOk()
              <- rtn as Boolean?
              
            <?-
              Use above and call error with the error value.
            -?>
            error()
              -> arg0 as E

            <?-
              Get the Error value, if not present Exception, so check.
            -?>
            error()
              <- rtn as E?
              
            <?-
              Check if the result is Error or not.
            -?>
            isError()
              <- rtn as Boolean?
            
            <?-
              Check if this Result has and Ok value or an error - it might have neither.
              That's not always wrong, there may be no Ok value, but also nothing in error.
            -?>
            operator ? as pure
              <- rtn as Boolean?
                
          PriorityQueue of type T as open
            PriorityQueue()

            PriorityQueue()
              -> arg0 as T
            
          Dict of type (K, V) as open
            Dict()

            Dict()
              ->
                k as K
                v as V
                
            operator +=
              -> arg as DictEntry of (K, V)
                
          DictEntry of type (K, V) as open
            DictEntry()

            DictEntry()
              ->
                k as K
                v as V

          Iterator of type T as open
            Iterator()
            
            Iterator()
              -> arg0 as T

      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TRAITS = """
          Clock
            dateTime() as pure
              <- rtn as DateTime: DateTime()

          StringInput
            next() as pure
              <- rtn as String: String()

            hasNext() as pure
              <- rtn as Boolean: Boolean()

            operator close as pure
            
            operator ? as pure
              <- rtn as Boolean: Boolean()
          
          <!-
            Just added in a body so it is not marked as abstract.
          -!>
          StringOutput
            println() as pure
              -> arg0 as String
              assert arg0?
                            
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_STANDARD_FUNCTIONS = """
          SignalHandler() as abstract
            -> signal as String
            <- result as Integer?

          <!- By its nature it is abstract -!>
          MutexKey() of type T as abstract
            -> value as T
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_STANDARD_CLASSES = """
          SystemClock with trait of Clock
            SystemClock()

          Stdin with trait of StringInput
            Stdin()
            
            override operator close as pure

          Stdout with trait of StringOutput
            default Stdout()
              
          Stderr with trait of StringOutput
            default Stderr()

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

            <?-
              Register a handler for one or more signals
            -?>
            register()
              ->
                signals as List of String
                handler as SignalHandler

            <?-
              Register a handler for one signal
            -?>
            register()
              ->
                signal as String
                handler as SignalHandler
                 
          EnvVars
            EnvVars()

          GetOpt of type T
            GetOpt()

            GetOpt()
              -> value as T
              
            make() as pure
              ->
                value as T
                pattern as Dict of (String, T)
                usage as String
              <-
                rtn as GetOpt of T?
                
          Version
            Version()

          MutexLock of type T
            MutexLock()
            
            MutexLock()
              -> value as T
            
            enter()
              -> withKey as MutexKey of T
              
            tryEnter()
              -> withKey as MutexKey of T
              
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_FUNCTIONS = """
          TCPHandler as abstract
            ->
              input as StringInput
              output as StringOutput
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_TRAITS = """
          HTTPRequest as open

          HTTPResponse as open
            contentType()
              <- rtn as String: "text/plain"
            content()
              <- rtn as String: ""
            status()
              <- rtn as Integer: 404

          TCPConnection as open
            output() as pure
              <- rtn as StringOutput?
              
            input() as pure
              <- rtn as StringInput?
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_CLASSES = """
          UDP
            default UDP()
            
            UDP()
              -> properties as NetworkProperties
            
            timeout() as pure
              -> duration as Millisecond
              <- rtn as UDP?
            
            send()
              -> packet as UDPPacket
            
            hasNext() as pure
              <- rtn as Boolean: Boolean()
            
            <?-
              Same functionality as receive.
            -?>
            next()
              <- packet as UDPPacket: UDPPacket()
            
            receive()
              <- packet as UDPPacket: UDPPacket()
            
            lastErrorMessage() as pure
              <- rtn as String: String()
               
            operator close as pure

            <?-
              Same functionality as hasNext()
            -?>
            operator ? as pure
              <- rtn as Boolean: Boolean()
                           
          TCP
            default TCP()
            
            TCP()
              -> properties as NetworkProperties
              
            connect()
              <- rtn as TCPConnection?
              
            accept()
              -> handler as TCPHandler

            lastErrorMessage() as pure
              <- rtn as String: String()
               
            operator close as pure

            operator ? as pure
              <- rtn as Boolean: Boolean()
              
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_RECORDS = """
          NetworkProperties
            host as String: String()
            port as Integer: Integer()
            packetSize as Integer: Integer()
            timeout as Millisecond: Millisecond()
            backlog as Integer: Integer()
            maxConcurrent as Integer: Integer()
            localOnly as Boolean: Boolean()
            
            default NetworkProperties()

            NetworkProperties()
              ->
                duration as Millisecond
            
            NetworkProperties()
              ->
                host as String

            NetworkProperties()
              ->
                host as String
                port as Integer

            NetworkProperties()
              ->
                port as Integer

            NetworkProperties()
              ->
                port as Integer
                packetSize as Integer

            NetworkProperties()
              ->
                port as Integer
                localOnly as Boolean
                
            NetworkProperties()
              ->
                host as String
                port as Integer
                packetSize as Integer

            NetworkProperties()
              ->
                host as String
                port as Integer
                timeout as Millisecond

            NetworkProperties()
              ->
                host as String
                port as Integer
                packetSize as Integer
                timeout as Millisecond

            NetworkProperties()
              ->
                port as Integer
                backlog as Integer
                maxConcurrent as Integer
                localOnly as Boolean

            NetworkProperties()
              ->
                port as Integer
                timeout as Millisecond
                backlog as Integer
                maxConcurrent as Integer
                localOnly as Boolean
                
            operator $ as pure
              <- rtn as String: String()
                                                   
          UDPPacket
            properties as NetworkProperties: NetworkProperties()
            content as String: String()
            
            UDPPacket()
              ->
                properties as NetworkProperties
                content as String
                
            operator $ as pure
              <- rtn as String: String()
                            
      """;
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_ASPECT_CLASSES = """
          Aspect as open
            Aspect()

          JoinPoint as open
            JoinPoint()

          PreparedMetaData as open
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
}
