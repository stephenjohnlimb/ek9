package org.ek9lang.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Just loads the ek9 language builtin ek9 source code and supplies it as Compilable Source.
 * I had intended to use a resource with the source code in. But when I tried a native build
 * with 'native-image --no-fallback -jar ek9c-jar-with-dependencies.jar' and ran the executable
 * I got a nullPointer.
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
  public static final int NUMBER_OF_EK9_SYMBOLS = 113;

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
        
          tags as List of String: [
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
          
            String() as pure
            
            String() as pure
              -> arg0 as String

            String() as pure
              -> arg0 as Optional of String
              
            trim() as pure
              <- rtn as String?
            
            trim() as pure
              -> char as Character
              <- rtn as String?
                            
            upperCase() as pure
              <- rtn as String?

            lowerCase() as pure
              <- rtn as String?

            iterator() as pure
              <- rtn as Iterator of Character?

            first() as pure
              <- rtn as Character?
              
            last() as pure
              <- rtn as Character?
            
            rightPadded() as pure
              -> width as Integer
              <- rtn as String?

            leftPadded() as pure
              -> width as Integer
              <- rtn as String?
            
            count() as pure
              -> char as Character
              <- rtn as Integer?

            split() as pure
              -> pattern as RegEx
              <- rtn as List of String?
              
            operator < as pure
              -> arg as String
              <- rtn as Boolean?

            operator <= as pure
              -> arg as String
              <- rtn as Boolean?

            operator > as pure
              -> arg as String
              <- rtn as Boolean?

            operator >= as pure
              -> arg as String
              <- rtn as Boolean?

            operator == as pure
              -> arg as String
              <- rtn as Boolean?

            operator <> as pure
              -> arg as String
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as String
              <- rtn as Integer?

            operator <~> as pure
              -> arg as String
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as String
              <- rtn as String?

            operator + as pure
              -> arg as Character
              <- rtn as String?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Character?
              
            operator #> as pure
              <- rtn as Character?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator :~:
              -> arg as String

            operator :^:
              -> arg as String
              
            operator :=:
              -> arg as String

            operator |
              -> arg as String

            operator |
              -> arg as JSON

            operator +=
              -> arg as String

            operator +=
              -> arg as Character

            operator contains as pure
              -> arg as String
              <- rtn as Boolean?

            operator matches as pure
              -> arg as RegEx
              <- rtn as Boolean?

      """;

  /**
   * As each type is fleshed out pull it out of the list and create a new full signature.
   */
  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TYPE_CLASSES = """

          Void
            Void() as pure

          AnyClass as open
            AnyClass() as pure
            
          Bits as open
            Bits() as pure

            Bits() as pure
              -> arg0 as Bits
            
            Bits() as pure
              -> arg0 as String

            Bits() as pure
              -> arg0 as Boolean

            Bits() as pure
              -> arg0 as Colour
              
            iterator() as pure
              <- rtn as Iterator of Boolean?

            operator < as pure
              -> arg as Bits
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Bits
              <- rtn as Boolean?

            operator > as pure
              -> arg as Bits
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Bits
              <- rtn as Boolean?

            operator == as pure
              -> arg as Bits
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Bits
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Bits
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Bits
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator ~ as pure
              <- rtn as Bits?

            operator + as pure
              -> arg as Bits
              <- rtn as Bits?

            operator + as pure
              -> arg as Boolean
              <- rtn as Bits?

            operator - as pure
              -> arg as Bits
              <- rtn as Bits?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Boolean?
              
            operator #> as pure
              <- rtn as Boolean?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator >> as pure
              -> arg as Integer
              <- rtn as Bits?

            operator << as pure
              -> arg as Integer
              <- rtn as Bits?

            operator and as pure
              -> arg as Bits
              <- rtn as Bits?

            operator or as pure
              -> arg as Bits
              <- rtn as Bits?

            operator xor as pure
              -> arg as Bits
              <- rtn as Bits?

            operator :~:
              -> arg as Bits

            operator :^:
              -> arg as Bits
              
            operator :=:
              -> arg as Bits

            operator |
              -> arg as Bits

            operator +=
              -> arg as Bits

            operator +=
              -> arg as Boolean

            operator -=
              -> arg as Bits

          Boolean
            Boolean() as pure

            Boolean() as pure
              -> arg0 as Boolean

            Boolean() as pure
              -> arg0 as String

            operator == as pure
              -> arg as Boolean
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Boolean
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Boolean
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Boolean
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator ~ as pure
              <- rtn as Boolean?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator and as pure
              -> arg as Boolean
              <- rtn as Boolean?

            operator or as pure
              -> arg as Boolean
              <- rtn as Boolean?

            operator xor as pure
              -> arg as Boolean
              <- rtn as Boolean?

            operator :~:
              -> arg as Boolean

            operator :^:
              -> arg as Boolean
              
            operator :=:
              -> arg as Boolean

            operator |
              -> arg as Boolean

            operator |
              -> arg as JSON
              
          Character
            Character() as pure

            Character() as pure
              -> arg0 as String

            upperCase() as pure
              <- rtn as Character?

            lowerCase() as pure
              <- rtn as Character?
              
            operator < as pure
              -> arg as Character
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Character
              <- rtn as Boolean?

            operator > as pure
              -> arg as Character
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Character
              <- rtn as Boolean?

            operator == as pure
              -> arg as Character
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Character
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Character
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Character
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator #^ as pure
              <- rtn as String?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator length as pure
              <- rtn as Integer?

            operator :~:
              -> arg as Character

            operator :^:
              -> arg as Character
              
            operator :=:
              -> arg as Character

            operator |
              -> arg as Character

            operator |
              -> arg as JSON
              
            operator ++
              <- rtn as Character?

            operator --
              <- rtn as Character?

          Integer as open
            <?-
              Just an unset Integer
            -?>
            Integer() as pure

            <?-
              Parse a String as an Integer, unset if not parsable.
            -?>
            Integer() as pure
              -> arg0 as String
          
            <?-
              Copy construction from parameter passed in
            -?>
            Integer() as pure
              -> arg0 as Integer

            operator < as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator > as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator == as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Integer
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Integer
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Integer
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Float?

            operator ! as pure
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator ~ as pure
              <- rtn as Integer?

            operator + as pure
              -> arg as Integer
              <- rtn as Integer?

            operator - as pure
              -> arg as Integer
              <- rtn as Integer?

            operator - as pure
              <- rtn as Integer?
              
            operator * as pure
              -> arg as Integer
              <- rtn as Integer?

            operator / as pure
              -> arg as Integer
              <- rtn as Integer?

            operator + as pure
              -> arg as Float
              <- rtn as Float?

            operator - as pure
              -> arg as Float
              <- rtn as Float?
              
            operator * as pure
              -> arg as Float
              <- rtn as Float?

            operator / as pure
              -> arg as Float
              <- rtn as Float?

            operator ^ as pure
              -> arg as Integer
              <- rtn as Integer?

            operator #^ as pure
              <- rtn as Float?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator abs as pure
              <- rtn as Integer?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator and as pure
              -> arg as Integer
              <- rtn as Integer?

            operator or as pure
              -> arg as Integer
              <- rtn as Integer?

            operator xor as pure
              -> arg as Integer
              <- rtn as Integer?

            operator mod as pure
              -> arg as Integer
              <- rtn as Integer?

            operator rem as pure
              -> arg as Integer
              <- rtn as Integer?

            operator contains as pure
              -> arg as Integer
              <- rtn as Boolean?

            operator :~:
              -> arg as Integer

            operator :^:
              -> arg as Integer
              
            operator :=:
              -> arg as Integer

            operator |
              -> arg as Integer

            operator |
              -> arg as JSON

            operator +=
              -> arg as Integer

            operator -=
              -> arg as Integer

            operator *=
              -> arg as Integer

            operator /=
              -> arg as Integer

            operator ++
              <- rtn as Integer?

            operator --
              <- rtn as Integer?
              
          Float as open
            Float() as pure

            Float() as pure
              -> arg0 as Float

            Float() as pure
              -> arg0 as String

            operator < as pure
              -> arg as Float
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Float
              <- rtn as Boolean?

            operator > as pure
              -> arg as Float
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Float
              <- rtn as Boolean?

            operator == as pure
              -> arg as Float
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Float
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Float
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Float
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Float?

            operator ? as pure
              <- rtn as Boolean?

            operator ~ as pure
              <- rtn as Float?

            operator + as pure
              -> arg as Float
              <- rtn as Float?

            operator - as pure
              <- rtn as Float?

            operator - as pure
              -> arg as Float
              <- rtn as Float?
              
            operator * as pure
              -> arg as Float
              <- rtn as Float?

            operator / as pure
              -> arg as Float
              <- rtn as Float?

            operator ^ as pure
              -> arg as Float
              <- rtn as Float?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator abs as pure
              <- rtn as Float?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator :~:
              -> arg as Float

            operator :^:
              -> arg as Float
              
            operator :=:
              -> arg as Float

            operator |
              -> arg as Float

            operator |
              -> arg as JSON

            operator +=
              -> arg as Float

            operator -=
              -> arg as Float

            operator *=
              -> arg as Float

            operator /=
              -> arg as Float

            operator ++
              <- rtn as Float?

            operator --
              <- rtn as Float?

          Time as open
            Time() as pure

            Time() as pure
              -> arg0 as Time

            Time() as pure
              -> arg0 as String

            Time() as pure
              -> hour as Integer

            Time() as pure
              ->
                hour as Integer
                minute as Integer

            Time() as pure
              ->
                hour as Integer
                minute as Integer
                second as Integer
                
            now() as pure
              <- rtn as Time?
              
            startOfDay() as pure
              <- rtn as Time?

            endOfDay() as pure
              <- rtn as Time?

            hour() as pure
              <- rtn as Integer?

            minute() as pure
              <- rtn as Integer?

            second() as pure
              <- rtn as Integer?
              
            operator < as pure
              -> arg as Time
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Time
              <- rtn as Boolean?

            operator > as pure
              -> arg as Time
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Time
              <- rtn as Boolean?

            operator == as pure
              -> arg as Time
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Time
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Time
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Time
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Duration
              <- rtn as Time?

            operator - as pure
              -> arg as Duration
              <- rtn as Time?

            operator - as pure
              -> arg as Time
              <- rtn as Duration?

            operator - as pure
              <- rtn as Time?
              
            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator :~:
              -> arg as Time

            operator :^:
              -> arg as Time
              
            operator :=:
              -> arg as Time

            operator |
              -> arg as Time

            operator |
              -> arg as Duration

            operator |
              -> arg as JSON

            operator +=
              -> arg as Duration

            operator -=
              -> arg as Duration

          Duration as open
            Duration() as pure

            Duration() as pure
              -> arg0 as Duration

            Duration() as pure
              -> arg0 as String

            hours() as pure
              <- rtn as Integer?

            minutes() as pure
              <- rtn as Integer?

            seconds() as pure
              <- rtn as Integer?
                            
            operator < as pure
              -> arg as Duration
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Duration
              <- rtn as Boolean?

            operator > as pure
              -> arg as Duration
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Duration
              <- rtn as Boolean?

            operator == as pure
              -> arg as Duration
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Duration
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Duration
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Duration
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Duration
              <- rtn as Duration?

            operator - as pure
              <- rtn as Duration?

            operator - as pure
              -> arg as Duration
              <- rtn as Duration?
              
            operator * as pure
              -> arg as Integer
              <- rtn as Duration?

            operator * as pure
              -> arg as Float
              <- rtn as Duration?
              
            operator / as pure
              -> arg as Integer
              <- rtn as Duration?

            operator / as pure
              -> arg as Float
              <- rtn as Duration?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator abs as pure
              <- rtn as Duration?

            operator mod as pure
              -> arg as Duration
              <- rtn as Integer?

            operator rem as pure
              -> arg as Duration
              <- rtn as Integer?

            operator :~:
              -> arg as Duration

            operator :^:
              -> arg as Duration
              
            operator :=:
              -> arg as Duration

            operator |
              -> arg as Duration

            operator |
              -> arg as JSON

            operator +=
              -> arg as Duration

            operator -=
              -> arg as Duration

            operator *=
              -> arg as Integer

            operator /=
              -> arg as Integer

          Millisecond as open
            Millisecond() as pure

            Millisecond() as pure
              -> arg0 as Millisecond

            Millisecond() as pure
              -> arg0 as String

            Millisecond() as pure
              -> arg0 as Integer

            Millisecond() as pure
              -> arg0 as Duration
              
            <?-
              Converts to a duration (must be greater than a second).
              i.e. it rounds up above milliseconds.
            -?>
            duration() as pure
              <- rtn as Duration?
              
            operator < as pure
              -> arg as Millisecond
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Millisecond
              <- rtn as Boolean?

            operator > as pure
              -> arg as Millisecond
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Millisecond
              <- rtn as Boolean?

            operator == as pure
              -> arg as Millisecond
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Millisecond
              <- rtn as Boolean?
            
            operator <=> as pure
              -> arg as Millisecond
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Millisecond
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Float?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Millisecond
              <- rtn as Millisecond?

            operator - as pure
              <- rtn as Millisecond?

            operator - as pure
              -> arg as Millisecond
              <- rtn as Millisecond?

            operator * as pure
              -> arg as Integer
              <- rtn as Millisecond?

            operator * as pure
              -> arg as Float
              <- rtn as Millisecond?

            operator / as pure
              -> arg as Integer
              <- rtn as Millisecond?

            operator #^ as pure
              <- rtn as Duration?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator abs as pure
              <- rtn as Millisecond?

            operator mod as pure
              -> arg as Millisecond
              <- rtn as Integer?

            operator rem as pure
              -> arg as Millisecond
              <- rtn as Integer?

            <?-
              Inverts the value.
            -?>
            operator ~ as pure
              <- rtn as Millisecond?
              
            operator :~:
              -> arg as Millisecond

            operator :^:
              -> arg as Millisecond
              
            operator :=:
              -> arg as Millisecond

            operator |
              -> arg as Millisecond

            operator |
              -> arg as JSON

            operator +=
              -> arg as Millisecond

            operator -=
              -> arg as Millisecond

            operator *=
              -> arg as Integer

            operator /=
              -> arg as Integer

            operator ++
              <- rtn as Millisecond?

            operator --
              <- rtn as Millisecond?

          Date as open
            Date() as pure

            Date() as pure
              -> arg0 as Date

            Date() as pure
              -> arg0 as String

            Date() as pure
              ->
                year as Integer
                month as Integer
                day as Integer

            today() as pure
              <- rtn as Date?
            
            year() as pure
              <- rtn as Integer?

            month() as pure
              <- rtn as Integer?

            day() as pure
              <- rtn as Integer?

            dayOfYear() as pure
              <- rtn as Integer?
              
            dayOfMonth() as pure
              <- rtn as Integer?
              
            <?-
              1 is Sunday and 7 is Saturday. But see Locale for a spoken language way to do this.
            -?>
            dayOfWeek() as pure
              <- rtn as Integer?
                                                          
            operator < as pure
              -> arg as Date
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Date
              <- rtn as Boolean?

            operator > as pure
              -> arg as Date
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Date
              <- rtn as Boolean?

            operator == as pure
              -> arg as Date
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Date
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Date
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Date
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Duration
              <- rtn as Date?

            operator - as pure
              -> arg as Duration
              <- rtn as Date?

            operator - as pure
              -> arg as Date
              <- rtn as Duration?
              
            operator #^ as pure
              <- rtn as DateTime?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator :~:
              -> arg as Date

            operator :^:
              -> arg as Date
              
            operator :=:
              -> arg as Date

            operator |
              -> arg as Date

            operator |
              -> arg as JSON

            <?-
              Can accept multiple durations and build up from base date.
            -?>
            operator |
              -> arg as Duration
              
            operator +=
              -> arg as Duration

            operator -=
              -> arg as Duration

            operator ++
              <- rtn as Date?

            operator --
              <- rtn as Date?

          DateTime as open
            DateTime() as pure

            DateTime() as pure
              -> arg0 as DateTime

            DateTime() as pure
              -> arg0 as String

            <?-
              Will default time to start of day in UTC timezone
            -?>
            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer

            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer
                offset as Duration
                
            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer
                hour as Integer

            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer
                hour as Integer
                minute as Integer

            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer
                hour as Integer
                minute as Integer
                second as Integer

            DateTime() as pure
              ->
                year as Integer
                month as Integer
                day as Integer
                hour as Integer
                minute as Integer
                second as Integer
                offset as Duration
              
            now() as pure
              <- rtn as DateTime?

            <?-
              The start of the day.
            -?>
            today() as pure
              <- rtn as DateTime: startOfDay()
              
            startOfDay() as pure
              <- rtn as DateTime?

            endOfDay() as pure
              <- rtn as DateTime?

            year() as pure
              <- rtn as Integer?

            month() as pure
              <- rtn as Integer?

            day() as pure
              <- rtn as Integer?

            hour() as pure
              <- rtn as Integer?

            minute() as pure
              <- rtn as Integer?

            second() as pure
              <- rtn as Integer?

            dayOfYear() as pure
              <- rtn as Integer?
              
            dayOfMonth() as pure
              <- rtn as Integer?
              
            <?-
              1 is Sunday and 7 is Saturday. But see Locale for a spoken language way to do this.
            -?>
            dayOfWeek() as pure
              <- rtn as Integer?
            
            offSetFromUTC() as pure
              <- rtn as Duration?
            
            <?-
            If UTC this will be "Z", other known specific zones i.e. EST,CST or String of "-03" for un-named zones.
            -?>
            zone() as pure
              <- rtn as String?
                  
            operator < as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator <= as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator > as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator >= as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator == as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator <> as pure
              -> arg as DateTime
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as DateTime
              <- rtn as Integer?

            operator <~> as pure
              -> arg as DateTime
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Duration
              <- rtn as DateTime?

            operator - as pure
              -> arg as Duration
              <- rtn as DateTime?

            operator - as pure
              -> arg as DateTime
              <- rtn as Duration?
              
            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Integer?
              
            operator #> as pure
              <- rtn as Integer?

            operator :~:
              -> arg as DateTime

            operator :^:
              -> arg as DateTime
              
            operator :=:
              -> arg as DateTime

            operator |
              -> arg as DateTime

            operator |
              -> arg as Duration

            operator |
              -> arg as JSON
              
            operator +=
              -> arg as Duration

            operator -=
              -> arg as Duration

            operator ++
              <- rtn as DateTime?

            operator --
              <- rtn as DateTime?

          Money as open
            Money() as pure

            Money() as pure
              -> arg0 as Money

            Money() as pure
              -> arg0 as String

            convert() as pure
              -> arg0 as Money
              <- rtn as Money?

            convert() as pure
              ->
                multiplier as Float
                currencyCode as String
              <-
                rtn as Money?

            operator < as pure
              -> arg as Money
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Money
              <- rtn as Boolean?

            operator > as pure
              -> arg as Money
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Money
              <- rtn as Boolean?

            operator == as pure
              -> arg as Money
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Money
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Money
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Money
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Money?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Money
              <- rtn as Money?

            operator - as pure
              <- rtn as Money?
              
            operator - as pure
              -> arg as Money
              <- rtn as Money?

            operator * as pure
              -> arg as Integer
              <- rtn as Money?

            operator * as pure
              -> arg as Float
              <- rtn as Money?

            operator / as pure
              -> arg as Integer
              <- rtn as Money?

            operator / as pure
              -> arg as Float
              <- rtn as Money?

            operator / as pure
              -> arg as Money
              <- rtn as Float?

            operator ^ as pure
              -> arg as Integer
              <- rtn as Money?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Float?
              
            operator #> as pure
              <- rtn as String?

            operator abs as pure
              <- rtn as Money?

            operator mod as pure
              -> arg as Money
              <- rtn as Integer?

            operator rem as pure
              -> arg as Money
              <- rtn as Integer?

            operator :~:
              -> arg as Money

            operator :^:
              -> arg as Money
              
            operator :=:
              -> arg as Money

            operator |
              -> arg as Money

            operator |
              -> arg as JSON

            operator +=
              -> arg as Money

            operator -=
              -> arg as Money

            operator *=
              -> arg as Integer

            operator *=
              -> arg as Float

            operator /=
              -> arg as Integer

            operator /=
              -> arg as Float

            operator ++
              <- rtn as Money?

            operator --
              <- rtn as Money?

          Locale as open
            Locale() as pure

            Locale() as pure
              -> arg0 as Locale

            Locale() as pure
              -> languageCodeCountryCode as String

            Locale() as pure
              ->
                languageCode as String
                countryCode as String

            <?-
              Given a date, this provides a spoken language version of the day of the week as a String.
            -?>
            dayOfWeek() as pure
              -> arg0 as Date
              <- rtn as String?
              
            format() as pure
              -> arg0 as Boolean
              <- rtn as String?
              
            format() as pure
              -> arg0 as Integer
              <- rtn as String?
                          
            format() as pure
              -> arg0 as Float
              <- rtn as String?

            format() as pure
              ->
                arg0 as Float
                precision as Integer
              <-
                rtn as String?

            format() as pure
              -> arg0 as Date
              <- rtn as String?

            format() as pure
              -> arg0 as Time
              <- rtn as String?

            format() as pure
              -> arg0 as DateTime
              <- rtn as String?

            format() as pure
              -> arg0 as Money
              <- rtn as String?

            format() as pure
              ->
                arg0 as Money
                showSymbol as Boolean
                showFractionalPart as Boolean
              <-
                rtn as String?
              
            format() as pure
              -> arg0 as Dimension
              <- rtn as String?

            format() as pure
              ->
                arg0 as Dimension
                precision as Integer
              <-
                rtn as String?
              
            shortFormat() as pure
              -> arg0 as Date
              <- rtn as String?

            shortFormat() as pure
              -> arg0 as Time
              <- rtn as String?

            shortFormat() as pure
              -> arg0 as DateTime
              <- rtn as String?

            shortFormat() as pure
              -> arg0 as Money
              <- rtn as String?

            mediumFormat() as pure
              -> arg0 as Date
              <- rtn as String?

            mediumFormat() as pure
              -> arg0 as Time
              <- rtn as String?

            mediumFormat() as pure
              -> arg0 as DateTime
              <- rtn as String?

            mediumFormat() as pure
              -> arg0 as Money
              <- rtn as String?
              
            longFormat() as pure
              -> arg0 as Date
              <- rtn as String?

            longFormat() as pure
              -> arg0 as Time
              <- rtn as String?

            longFormat() as pure
              -> arg0 as DateTime
              <- rtn as String?

            longFormat() as pure
              -> arg0 as Money
              <- rtn as String?
              
            fullFormat() as pure
              -> arg0 as Date
              <- rtn as String?

            fullFormat() as pure
              -> arg0 as Time
              <- rtn as String?

            fullFormat() as pure
              -> arg0 as DateTime
              <- rtn as String?

            fullFormat() as pure
              -> arg0 as Money
              <- rtn as String?
                            
            operator == as pure
              -> arg as Locale
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Locale
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Locale
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Locale
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Locale?
              
            operator #> as pure
              <- rtn as Locale?

            operator contains as pure
              -> arg as Locale
              <- rtn as Boolean?

            operator matches as pure
              -> arg as Locale
              <- rtn as Boolean?

            operator matches as pure
              -> arg as RegEx
              <- rtn as Boolean?

            operator :~:
              -> arg as Locale

            operator :^:
              -> arg as Locale
              
            operator :=:
              -> arg as Locale

            operator |
              -> arg as Locale

          Colour as open
            Colour() as pure

            Colour() as pure
              -> arg0 as Colour

            Colour() as pure
              -> arg0 as String

            Colour() as pure
              -> arg0 as Bits
              
            bits() as pure
              <- rtn as Bits?
            
            hue() as pure
              <- rtn as Integer?

            saturation() as pure
              <- rtn as Float?

            lightness() as pure
              <- rtn as Float?
            
            withOpaque() as pure
              -> arg0 as Integer
              <- rtn as Colour?

            withHue() as pure
              -> arg0 as Integer
              <- rtn as Colour?

            withLightness() as pure
              -> arg0 as Float
              <- rtn as Colour?

            withSaturation() as pure
              -> arg0 as Float
              <- rtn as Colour?

            RGB() as pure
              <- rtn as String?

            RGBA() as pure
              <- rtn as String?
            
            ARGB() as pure
              <- rtn as String?
                                            
            operator < as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator > as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator == as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Colour
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Colour
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Colour
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator ~ as pure
              <- rtn as Colour?

            operator + as pure
              -> arg as Colour
              <- rtn as Colour?

            operator - as pure
              -> arg as Colour
              <- rtn as Colour?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator and as pure
              -> arg as Colour
              <- rtn as Colour?

            operator or as pure
              -> arg as Colour
              <- rtn as Colour?

            operator xor as pure
              -> arg as Colour
              <- rtn as Colour?

            operator :~:
              -> arg as Colour

            operator :^:
              -> arg as Colour
              
            operator :=:
              -> arg as Colour

            operator |
              -> arg as Colour

            operator |
              -> arg as JSON

            operator +=
              -> arg as Colour

            operator -=
              -> arg as Colour

          Dimension as open
            Dimension() as pure

            Dimension() as pure
              -> arg0 as Dimension

            Dimension() as pure
              -> arg0 as String

            <?-
              Converts one type of dimension to another (if possible).
              For example 1.0km would be 0.621371mile
            -?>
            convert() as pure
              -> arg0 as Dimension
              <- rtn as Dimension?

            convert() as pure
              ->
                multiplier as Float
                typeOfDimension as String
              <-
                rtn as Dimension?
              
            operator < as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator > as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator == as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Dimension
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Dimension
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Dimension
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Dimension?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Dimension
              <- rtn as Dimension?
              
            operator + as pure
              -> arg as Float
              <- rtn as Dimension?

            operator - as pure
              <- rtn as Dimension?

            operator - as pure
              -> arg as Dimension
              <- rtn as Dimension?
              
            operator * as pure
              -> arg as Integer
              <- rtn as Dimension?

            operator * as pure
              -> arg as Float
              <- rtn as Dimension?

            operator / as pure
              -> arg as Integer
              <- rtn as Dimension?

            operator / as pure
              -> arg as Float
              <- rtn as Dimension?

            operator / as pure
              -> arg as Dimension
              <- rtn as Float?

            operator ^ as pure
              -> arg as Integer
              <- rtn as Dimension?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Float?
              
            operator #> as pure
              <- rtn as String?

            operator abs as pure
              <- rtn as Dimension?

            operator mod as pure
              -> arg as Dimension
              <- rtn as Integer?

            operator rem as pure
              -> arg as Dimension
              <- rtn as Integer?

            operator :~:
              -> arg as Dimension

            operator :^:
              -> arg as Dimension
              
            operator :=:
              -> arg as Dimension

            operator |
              -> arg as Dimension

            operator |
              -> arg as JSON

            operator +=
              -> arg as Dimension

            operator +=
              -> arg as Float

            operator -=
              -> arg as Float

            operator -=
              -> arg as Dimension
              
            operator *=
              -> arg as Integer

            operator *=
              -> arg as Float

            operator /=
              -> arg as Integer

            operator /=
              -> arg as Float

            operator ++
              <- rtn as Dimension?

            operator --
              <- rtn as Dimension?
              
          Resolution as open
            Resolution() as pure

            Resolution() as pure
              -> arg0 as Resolution

            Resolution() as pure
              -> arg0 as String

            operator < as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator <= as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator > as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator >= as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator == as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Resolution
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Resolution
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Resolution
              <- rtn as Integer?

            operator sqrt as pure
              <- rtn as Resolution?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Resolution
              <- rtn as Resolution?

            operator - as pure
              -> arg as Resolution
              <- rtn as Resolution?

            operator * as pure
              -> arg as Integer
              <- rtn as Resolution?

            operator * as pure
              -> arg as Float
              <- rtn as Resolution?

            operator / as pure
              -> arg as Integer
              <- rtn as Resolution?

            operator / as pure
              -> arg as Float
              <- rtn as Resolution?

            operator ^ as pure
              -> arg as Integer
              <- rtn as Resolution?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as Float?
              
            operator #> as pure
              <- rtn as String?

            operator abs as pure
              <- rtn as Resolution?

            operator mod as pure
              -> arg as Resolution
              <- rtn as Integer?

            operator rem as pure
              -> arg as Resolution
              <- rtn as Integer?

            operator :~:
              -> arg as Resolution

            operator :^:
              -> arg as Resolution
              
            operator :=:
              -> arg as Resolution

            operator |
              -> arg as Resolution

            operator |
              -> arg as JSON

            operator +=
              -> arg as Resolution

            operator -=
              -> arg as Resolution

            operator *=
              -> arg as Integer

            operator *=
              -> arg as Float

            operator /=
              -> arg as Integer

            operator /=
              -> arg as Float

            operator ++
              <- rtn as Resolution?

            operator --
              <- rtn as Resolution?
           
          Path as open
            Path() as pure

            Path() as pure
              -> arg0 as Path

            Path() as pure
              -> arg0 as String

            operator == as pure
              -> arg as Path
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Path
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Path
              <- rtn as Integer?

            operator <~> as pure
              -> arg as Path
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Path
              <- rtn as Path?

            operator + as pure
              -> arg as String
              <- rtn as Path?

            operator + as pure
              -> arg as Character
              <- rtn as Path?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as String?
              
            operator #> as pure
              <- rtn as String?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator contains as pure
              -> arg as Path
              <- rtn as Boolean?

            operator matches as pure
              -> arg as RegEx
              <- rtn as Boolean?

            operator :~:
              -> arg as Path

            operator :^:
              -> arg as Path
              
            operator :=:
              -> arg as Path

            operator |
              -> arg as Path

            operator +=
              -> arg as Path

            operator +=
              -> arg as String

            operator *=
              -> arg as Path

            operator +=
              -> arg as Character

          JSON
            JSON() as pure

            JSON() as pure
              -> arg0 as JSON

            JSON() as pure
              -> arg0 as String

            JSON() as pure
              -> arg0 as Integer

            JSON() as pure
              -> arg0 as Float

            JSON() as pure
              -> arg0 as Boolean
            
            JSON() as pure
              ->
                key as String
                value as JSON
                
            iterator() as pure
              <- rtn as Iterator of JSON?
              
            operator == as pure
              -> arg as JSON
              <- rtn as Boolean?

            operator <> as pure
              -> arg as JSON
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as JSON
              <- rtn as Integer?

            operator <~> as pure
              -> arg as JSON
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator #^ as pure
              <- rtn as String?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator contains as pure
              -> arg as JSON
              <- rtn as Boolean?

            operator :~:
              -> arg as JSON

            operator :^:
              -> arg as JSON
              
            operator :=:
              -> arg as JSON

            operator |
              -> arg as JSON

          JSONInput as abstract

          RegEx as open
            RegEx() as pure

            RegEx() as pure
              -> arg0 as RegEx

            RegEx() as pure
              -> arg0 as String
            
            split() as pure
              -> toSplit as String
              <- rtn as List of String?
              
            operator == as pure
              -> arg as RegEx
              <- rtn as Boolean?

            operator <> as pure
              -> arg as RegEx
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as RegEx
              <- rtn as Integer?

            operator <~> as pure
              -> arg as RegEx
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as RegEx
              <- rtn as RegEx?

            operator + as pure
              -> arg as String
              <- rtn as RegEx?

            operator + as pure
              -> arg as Character
              <- rtn as RegEx?

            operator #^ as pure
              <- rtn as String?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator matches as pure
              -> arg as String
              <- rtn as Boolean?

            operator matches as pure
              -> arg as Path
              <- rtn as Boolean?

            operator matches as pure
              -> arg as Locale
              <- rtn as Boolean?

            operator :~:
              -> arg as RegEx

            operator :^:
              -> arg as RegEx
              
            operator :=:
              -> arg as RegEx

            operator |
              -> arg as RegEx

            operator +=
              -> arg as RegEx

            operator +=
              -> arg as String

            operator +=
              -> arg as Character

          Exception as open
            Exception() as pure

            Exception() as pure
              -> reason as Exception
            
            Exception() as pure
              -> reason as String
            
            Exception() as pure
              ->
                reason as String
                exitCode as Integer
            
            reason() as pure
              <- rtn as String?
            
            exitCode() as pure
              <- rtn as Integer?

            operator ? as pure
              <- rtn as Boolean?

            operator $ as pure
              <- rtn as String?
                          
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_FUNCTIONS = """
          Supplier of type T as pure abstract
            <- r as T?

          Producer of type T as abstract
            <- r as T?

          Consumer of type T as pure abstract
            -> t as T

          BiConsumer of type (T, U) as pure abstract
            ->
              t as T
              u as U

          Acceptor of type T as abstract
            -> t as T

          BiAcceptor of type (T, U) as abstract
            ->
              t as T
              u as U

          UnaryOperator of type T as pure abstract
            -> t as T
            <- r as T?

          Function of type (T, R) as pure abstract
            -> t as T
            <- r as R?

          BiFunction of type (T, U, R) as pure abstract
            ->
              t as T
              u as U
            <- r as R?
          
          Predicate of type T as pure abstract
            -> t as T
            <- r as Boolean?
          
          <?-
            Similar in concept to a Predicate, but not pure. Can mutate data.
          -?>
          Assessor of type T as abstract
            -> t as T
            <- r as Boolean?
            
          BiPredicate of type (T, U) as pure abstract
            ->
              t as T
              u as U
            <-
              r as Boolean?

          Comparator of type T as pure abstract
            ->
              t1 as T
              t2 as T
            <-
              r as Integer?
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TEMPLATE_CLASSES = """
          List of type T as open
            List() as pure
            
            List() as pure
              -> arg0 as T

            <?-
              Check bounds or use iterator, else if out of bounds and exception will be thrown.
            -?>
            get() as pure
              -> index as Integer
              <- rtn as T?

            first() as pure
              <- rtn as T?

            last() as pure
              <- rtn as T?
            
            reverse() as pure
              <- rtn as List of T?
                              
            iterator() as pure
              <- rtn as Iterator of T?

            <!-
            TODO sort out generics error: 'List of type T of type T' is not 'List of type T
            operator ~ as pure
              <- rtn as List of T?
            -!>
                            
            operator == as pure
              -> arg as List of T
              <- rtn as Boolean?

            operator <> as pure
              -> arg as List of T
              <- rtn as Boolean?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as List of T
              <- rtn as List of T?

            operator + as pure
              -> arg as T
              <- rtn as List of T?

            operator - as pure
              -> arg as List of T
              <- rtn as List of T?

            operator - as pure
              -> arg as T
              <- rtn as List of T?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as T?
              
            operator #> as pure
              <- rtn as T?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator contains as pure
              -> arg as T
              <- rtn as Boolean?

            operator :~:
              -> arg as List of T

            operator :^:
              -> arg as List of T
              
            operator :=:
              -> arg as List of T

            <!-
            Generics issue in here: 'List of type T of type T' is not 'List of type T'
            operator ~ as pure
              <- rtn as List of T?
            -!>
              
            operator |
              -> arg as T
              
            operator +=
              -> arg as List of T

            operator +=
              -> arg as T
              
            operator -=
              -> arg as List of T

            operator -=
              -> arg as T
              
          Optional of type T
            Optional() as pure
            
            Optional() as pure
              -> arg0 as T

            get() as pure
              <- rtn as T?
              
            iterator() as pure
              <- rtn as Iterator of T?

            <?-
              When the Optional has a value the acceptor will be called.
            -?>
            whenPresent()
              -> acceptor as Acceptor of T

            <?-
              When the Optional has a value the consumer will be called.
            -?>
            whenPresent() as pure
              -> consumer as Consumer of T
              
            operator == as pure
              -> arg as Optional of T
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Optional of T
              <- rtn as Boolean?

            operator <=> as pure
              -> arg as Optional of T
              <- rtn as Integer?

            <?-
              Does this optional have a valid value in it.
            -?>
            operator ? as pure
              <- rtn as Boolean?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator empty as pure
              <- rtn as Boolean?

            operator contains as pure
              -> arg as T
              <- rtn as Boolean?

            operator :~:
              -> arg as Optional of T

            operator :^:
              -> arg as Optional of T
              
            operator :=:
              -> arg as Optional of T

            operator |
              -> arg as T

          Result of type (O, E)
          
            //default constructor without an ok value or an error
            Result() as pure

            Result() as pure
              ->
                ok as O
                error as E

            <?-
              Check if the result is Ok or not.
            -?>
            isOk() as pure
              <- rtn as Boolean?

            <?-
              Get the OK value, if not present Exception, so check.
              This is the same as get().
            -?>
            ok() as pure
              <- rtn as O?

            <?-
              Get the OK value, if not present Exception, so check.
              This is the same as ok().
            -?>
            get() as pure
              <- rtn as O?

            <?-
              If the result has a valid ok value the acceptor will be called.
            -?>
            whenOK()
              -> acceptor as Acceptor of O

            <?-
              If the result has a valid ok value the consumer will be called.
            -?>
            whenOK() as pure
              -> consumer as Consumer of O

            <?-
              Check if the result is Error or not.
            -?>
            isError() as pure
              <- rtn as Boolean?

            <?-
              Get the Error value, if not present Exception, so check.
            -?>
            error() as pure
              <- rtn as E?

            <?-
              If the result is in error then the acceptor will be called.
            -?>
            whenError()
              -> acceptor as Acceptor of E

            <?-
              If the result is in error then the consumer will be called.
            -?>
            whenError() as pure
              -> consumer as Consumer of E

            iterator() as pure
              <- rtn as Iterator of O?
                            
            operator == as pure
              -> arg as Result of (O, E)
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Result of (O, E)
              <- rtn as Boolean?

            <?-
              Check if this Result has an ok value.
            -?>
            operator ? as pure
              <- rtn as Boolean?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as O?
              
            operator #> as pure
              <- rtn as E?

            operator empty as pure
              <- rtn as Boolean?

            operator contains as pure
              -> arg as O
              <- rtn as Boolean?

            operator :~:
              -> arg as Result of (O, E)

            operator :^:
              -> arg as Result of (O, E)
              
            operator :=:
              -> arg as Result of (O, E)

            operator |
              -> arg as O
                
          PriorityQueue of type T as open
            PriorityQueue() as pure

            PriorityQueue() as pure
              -> arg0 as T

            useComparator() as pure
              -> comparator as Comparator of T
              <- rtn as PriorityQueue of T?
            
            useSize() as pure
              -> size as Integer
              <- rtn as PriorityQueue of T?
            
            list() as pure
              <- rtn as List of T?
                  
            iterator() as pure
              <- rtn as Iterator of T?

            operator == as pure
              -> arg as PriorityQueue of T
              <- rtn as Boolean?

            operator <> as pure
              -> arg as PriorityQueue of T
              <- rtn as Boolean?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as T
              <- rtn as PriorityQueue of T?

            operator + as pure
              -> arg as List of T
              <- rtn as PriorityQueue of T?

            operator +=
              -> arg as T

            operator +=
              -> arg as List of T
              
            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as T?
              
            operator #> as pure
              <- rtn as T?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator contains as pure
              -> arg as T
              <- rtn as Boolean?

            operator :~:
              -> arg as PriorityQueue of T

            operator :^:
              -> arg as PriorityQueue of T
              
            operator :=:
              -> arg as PriorityQueue of T

            operator |
              -> arg as T

          DictEntry of type (K, V) as open
            DictEntry() as pure

            DictEntry() as pure
              ->
                k as K
                v as V

            key() as pure
              <- rtn as K?

            value() as pure
              <- rtn as V?
                            
            operator ? as pure
              <- rtn as Boolean?
            
          Dict of type (K, V) as open
            Dict() as pure

            Dict() as pure
              ->
                k as K
                v as V
                        
            get() as pure
              -> arg0 as K
              <- rtn as V?
              
            iterator() as pure
              <- rtn as DictEntry of (K, V)?

            keys() as pure
              <- rtn as Iterator of K?

            values() as pure
              <- rtn as Iterator of V?
                            
            operator == as pure
              -> arg as Dict of (K, V)
              <- rtn as Boolean?

            operator <> as pure
              -> arg as Dict of (K, V)
              <- rtn as Boolean?

            operator ? as pure
              <- rtn as Boolean?

            operator + as pure
              -> arg as Dict of (K, V)
              <- rtn as Dict of (K, V)?

            operator + as pure
              -> arg as DictEntry of (K, V)
              <- rtn as Dict of (K, V)?
              
            operator - as pure
              -> arg as Dict of (K, V)
              <- rtn as Dict of (K, V)?

            operator $$ as pure
              <- rtn as JSON?

            operator $ as pure
              <- rtn as String?

            operator #? as pure
              <- rtn as Integer?

            operator #< as pure
              <- rtn as DictEntry of (K, V)?
              
            operator #> as pure
              <- rtn as DictEntry of (K, V)?

            operator empty as pure
              <- rtn as Boolean?

            operator length as pure
              <- rtn as Integer?

            operator contains as pure
              -> arg as K
              <- rtn as Boolean?

            operator :~:
              -> arg as Dict of (K, V)

            operator :^:
              -> arg as Dict of (K, V)
              
            operator :=:
              -> arg as Dict of (K, V)

            operator |
              -> arg as DictEntry of (K, V)

            operator +=
              -> arg as Dict of (K, V)

            operator +=
              -> arg as DictEntry of (K, V)
              
            operator -=
              -> arg as Dict of (K, V)
              
            operator -=
              -> arg as DictEntry of (K, V)

            operator -=
              -> arg as K

          Iterator of type T as abstract
            Iterator() as pure
            
            Iterator() as pure
              -> arg0 as T

            hasNext() as pure abstract
              <- rtn as Boolean?
              
            next() as abstract
              <- rtn as T?
              
            operator empty as pure
              <- rtn as Boolean: not hasNext()
                
            operator ? as pure
              <- rtn as Boolean: hasNext()

      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_BUILT_IN_TRAITS = """
          Clock
            millisecond() as pure
              <- rtn as Millisecond?
            
            time() as pure
              <- rtn as Time := Time()
              
            date() as pure
              <- rtn as Date := Date()
              
            dateTime() as pure
              <- rtn as DateTime := DateTime()

            operator ? as pure
              <- rtn as Boolean := Boolean()

          StringInput
            next() as pure
              <- rtn as String := String()

            hasNext() as pure
              <- rtn as Boolean := Boolean()

            operator close as pure
            
            operator ? as pure
              <- rtn as Boolean := Boolean()
          
          <!-
            Just added in a body so it is not marked as abstract.
          -!>
          StringOutput
            println() as pure
              -> arg0 as String
              assert arg0?

            print() as pure
              -> arg0 as String
              assert arg0?

            print() as pure
              -> arg0 as Character
              assert arg0?

            operator |
              -> arg0 as String
              assert arg0?
              
            operator ? as pure
              <- rtn as Boolean?
          
          PipedOutput
              
            operator |
              -> arg0 as Bits
              assert arg0?

            operator |
              -> arg0 as Boolean
              assert arg0?

            operator |
              -> arg0 as Character
              assert arg0?

            operator |
              -> arg0 as Integer
              assert arg0?

            operator |
              -> arg0 as Float
              assert arg0?

            operator |
              -> arg0 as Time
              assert arg0?

            operator |
              -> arg0 as Duration
              assert arg0?

            operator |
              -> arg0 as Millisecond
              assert arg0?

            operator |
              -> arg0 as Date
              assert arg0?

            operator |
              -> arg0 as DateTime
              assert arg0?

            operator |
              -> arg0 as Money
              assert arg0?

            operator |
              -> arg0 as Locale
              assert arg0?

            operator |
              -> arg0 as Colour
              assert arg0?

            operator |
              -> arg0 as Dimension
              assert arg0?

            operator |
              -> arg0 as Resolution
              assert arg0?

            operator |
              -> arg0 as Path
              assert arg0?

            operator |
              -> arg0 as JSON
              assert arg0?

            operator |
              -> arg0 as RegEx
              assert arg0?

            operator |
              -> arg0 as Exception
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
            SystemClock() as pure
            
            override millisecond() as pure
              <- rtn as Millisecond?
                          
            override operator ? as pure
              <- rtn as Boolean?

          Stdin with trait of StringInput
            Stdin() as pure
            
            override operator close as pure

            override operator ? as pure
              <- rtn as Boolean?

          Stdout with trait of StringOutput, PipedOutput
            Stdout() as pure

            override operator ? as pure
              <- rtn as Boolean?
              
          Stderr with trait of StringOutput, PipedOutput
            Stderr() as pure

            override operator ? as pure
              <- rtn as Boolean?

          TextFile
            TextFile() as pure

            TextFile() as pure
              -> fileName as String
            
            TextFile() as pure
              -> fileSystemPath as FileSystemPath
              
            input() as pure
              <- rtn as StringInput?
            
            isReadable() as pure
              <- rtn as Boolean?
            
            isFile() as pure
              <- rtn as Boolean?

            isDirectory() as pure
              <- rtn as Boolean?
                            
            lastModified() as pure
              <- rtn as DateTime?

            operator $ as pure
              <- rtn as String?

            operator ? as pure
              <- rtn as Boolean?

          FileSystem
            FileSystem() as pure

            <?-
              Get the current working directory
            -?>
            cwd() as pure
              <- rtn as FileSystemPath?

            operator ? as pure
              <- rtn as Boolean?

          FileSystemPath
            FileSystemPath() as pure

            FileSystemPath() as pure
              -> pathName as String
            
            isAbsolute() as pure
              <- rtn as Boolean?
                            
            operator + as pure
              -> addition as FileSystemPath
              <- rtn as FileSystemPath?
                
            operator ? as pure
              <- rtn as Boolean?

          <!- Operating System -!>
          OS
            OS() as pure

            <?-
              This process id
            -?>
            pid() as pure
              <- rtn as Integer?
              
            operator ? as pure
              <- rtn as Boolean?

          GUID
            GUID() as pure

            operator #^ as pure
              <- rtn as String?
              
            operator ? as pure
              <- rtn as Boolean?

          HMAC
            HMAC() as pure

            SHA256() as pure
              -> arg0 as String
              <- rtn as String?

            SHA256() as pure
              -> arg0 as GUID
              <- rtn as String?
              
            operator ? as pure
              <- rtn as Boolean?

          Signals
            Signals() as pure

            <?-
              Register a handler for one or more signals.
              Returns the list of signals that have been applied.
            -?>
            register()
              ->
                signals as List of String
                handler as SignalHandler
              <-
                rtn as List of String?
                
            <?-
              Register a handler for one signal
            -?>
            register()
              ->
                signal as String
                handler as SignalHandler

            operator ? as pure
              <- rtn as Boolean?
                 
          EnvVars
            EnvVars() as pure

            get() as pure
              -> environmentVariableName as String
              <- environmentVariableValue as String?
            
            operator contains as pure
              -> environmentVariableName as String
              <- rtn as Boolean?
                
            operator ? as pure
              <- rtn as Boolean?

          GetOpt of type T
            GetOpt() as pure

            GetOpt() as pure
              -> value as T
              
            make() as pure
              ->
                value as T
                pattern as Dict of (String, T)
                usage as String
              <-
                rtn as GetOpt of T?

            options() as pure
              -> arguments as List of String
              <- rtn as Dict of (String, String)?
              
            operator ? as pure
              <- rtn as Boolean?
                
          Version
            Version() as pure

            operator ? as pure
              <- rtn as Boolean?

            operator $ as pure
              <- rtn as String?

          MutexLock of type T
            MutexLock() as pure
            
            MutexLock() as pure
              -> value as T
            
            enter() as pure
              -> withKey as MutexKey of T
              
            tryEnter() as pure
              -> withKey as MutexKey of T

            operator ? as pure
              <- rtn as Boolean?
              
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_FUNCTIONS = """
          
          <?-
            Applies each of the configurations to a new NetworkProperties record and returns that
            mutated NetworkProperties record.
          -?>
          ConfigureNetworkProperties()
            -> configurations as List of Acceptor of NetworkProperties
            <- networkProperties as NetworkProperties?

          TCPHandler as abstract
            ->
              input as StringInput
              output as StringOutput
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_TRAITS = """
          HTTPRequest as open
            <?-
              Typically if the request what a POST, PUT or PATCH
            -?>
            content() as pure
              <- rtn as String: ""
              
          HTTPResponse as open
          
            etag()
              <- rtn as String: String()
              
            cacheControl() as pure
              <- rtn as String: "public,max-age=3600,must-revalidate"
            
            contentType() as pure
              <- rtn as String: "text/plain"
            
            contentLanguage() as pure
              <- rtn as String: "en"
            
            contentLocation() as pure
              <- rtn as String: String()
              
            content()
              <- rtn as String: ""
            
            lastModified() as pure
              <- rtn as DateTime: DateTime()
              
            status() as pure
              <- rtn as Integer: 404

            operator ? as pure
              <- rtn <- Boolean()

          TCPConnection as open
            output() as pure
              <- rtn as StringOutput?
              
            input() as pure
              <- rtn as StringInput?

            operator ? as pure
              <- rtn as Boolean := Boolean()

      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_CLASSES = """
          UDP
            UDP() as pure
            
            UDP() as pure
              -> properties as NetworkProperties
            
            timeout() as pure
              -> duration as Millisecond
              <- rtn as UDP?
            
            send()
              -> packet as UDPPacket
            
            hasNext() as pure
              <- rtn as Boolean?
            
            <?-
              Same functionality as receive.
            -?>
            next()
              <- packet as UDPPacket?
            
            receive()
              <- packet as UDPPacket?
            
            lastErrorMessage() as pure
              <- rtn as String?
            
            operator |
              -> packet as UDPPacket
               
            operator close as pure

            <?-
              Same functionality as hasNext()
            -?>
            operator ? as pure
              <- rtn as Boolean?
                           
          TCP
            TCP() as pure
            
            TCP() as pure
              -> properties as NetworkProperties
              
            connect()
              <- rtn as TCPConnection?
              
            accept()
              -> handler as TCPHandler
              <- rtn as Boolean?

            lastErrorMessage() as pure
              <- rtn as String?
               
            operator close as pure

            operator ? as pure
              <- rtn as Boolean?
            
            operator $ as pure
              <- rtn as String?
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_NETWORK_RECORDS = """

          AnyRecord as open
            AnyRecord() as pure
          
          <?-
            Used for various network communications.
            See 'ConfigureNetworkProperties' on how to create a bespoke or specific
            network property configuration - rather than using a specific constructor.
          -?>
          NetworkProperties
            host as String: String()
            port as Integer: Integer()
            packetSize as Integer: Integer()
            timeout as Millisecond: Millisecond()
            backlog as Integer: Integer()
            maxConcurrent as Integer: Integer()
            localOnly as Boolean: Boolean()
            
            NetworkProperties() as pure

            NetworkProperties() as pure
              ->
                duration as Millisecond
            
            NetworkProperties() as pure
              ->
                host as String

            NetworkProperties() as pure
              ->
                host as String
                port as Integer

            NetworkProperties() as pure
              ->
                port as Integer

            NetworkProperties() as pure
              ->
                port as Integer
                packetSize as Integer

            NetworkProperties() as pure
              ->
                port as Integer
                localOnly as Boolean
                
            NetworkProperties() as pure
              ->
                host as String
                port as Integer
                packetSize as Integer

            NetworkProperties() as pure
              ->
                host as String
                port as Integer
                timeout as Millisecond

            NetworkProperties() as pure
              ->
                host as String
                port as Integer
                packetSize as Integer
                timeout as Millisecond

            NetworkProperties() as pure
              ->
                port as Integer
                backlog as Integer
                maxConcurrent as Integer
                localOnly as Boolean

            NetworkProperties() as pure
              ->
                port as Integer
                timeout as Millisecond
                backlog as Integer
                maxConcurrent as Integer
                localOnly as Boolean

            operator ? as pure
              <- rtn as Boolean?
                
            operator $ as pure
              <- rtn as String?
                                                   
          UDPPacket
            properties as NetworkProperties: NetworkProperties()
            content as String: String()
            
            UDPPacket() as pure
            
            UDPPacket() as pure
              ->
                properties as NetworkProperties
                content as String
            
            <?-
              Allow to be promoted to a String for output.
            -?>
            operator #^ as pure
              <- rtn as String: $this
              
            operator ? as pure
              <- rtn as Boolean?
                
            operator $ as pure
              <- rtn as String?
                            
      """;

  @SuppressWarnings({"Indentation"})
  private static final String DEFINE_ASPECT_CLASSES = """
          Aspect as open
            Aspect() as pure

            beforeAdvice()
              -> joinPoint as JoinPoint
              <- rtn as PreparedMetaData?
            
            afterAdvice()
              -> preparedMetaData as PreparedMetaData

            operator ? as pure
              <- rtn as Boolean?
            
          JoinPoint as open
            JoinPoint() as pure

            componentName() as pure
              <- rtn as String?

            methodName() as pure
              <- rtn as String?
              
            operator ? as pure
              <- rtn as Boolean?

          PreparedMetaData as open
            PreparedMetaData() as pure
            
            PreparedMetaData() as pure
              -> joinPoint as JoinPoint

            joinPoint() as pure
              <- rtn as JoinPoint?
              
            operator ? as pure
              <- rtn as Boolean?

      """;

  @SuppressWarnings({"Indentation"})
  private static final String ORG_EK9_MATH_PREMABLE = """
      #!ek9
      defines extern module org.ek9.math

        defines package
        
          version <- 0.0.1-0
        
          description <- "Builtin EK9 mathematics constructs."
        
          tags as List of String: [
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
