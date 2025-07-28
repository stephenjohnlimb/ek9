package org.ek9.lang;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * This is used to model Locale like 'en_GB' or Java version 'en-GB'.
 * <p>
 * SHORT, MEDIUM, LONG and FULL formats. Typically for Dates, Times, DateTimes, Money, Integers and Floats.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Locale as open""")
public class Locale extends SuffixedComponent {
  java.lang.String lang = "en";

  @Ek9Constructor("""
      Locale() as pure""")
  public Locale() {
    unSet();
  }

  @Ek9Constructor("""
      Locale() as pure
        -> languageCodeCountryCode as String""")
  public Locale(String languageCodeCountryCode) {
    this();
    if (isValid(languageCodeCountryCode)) {
      parse(languageCodeCountryCode.state);
    }
  }

  @Ek9Constructor("""
      Locale() as pure
        ->
          languageCode as String
          countryCode as String""")
  public Locale(String languageCode, String countryCode) {
    this();
    if (isValid(languageCode) && isValid(countryCode)) {
      assign(languageCode.state, countryCode.state);
    }
  }

  @Ek9Constructor("""
      Locale() as pure
        -> arg0 as Locale""")
  public Locale(Locale arg0) {
    this();
    if (isValid(arg0)) {
      assign(arg0.lang, arg0.suffix);
    }
  }

  @Ek9Operator("""
      operator |
        -> arg0 as Locale""")
  public void _pipe(Locale arg0) {
    _merge(arg0);
  }

  @Ek9Operator("""
      operator :~:
        -> arg0 as Locale""")
  public void _merge(Locale arg0) {
    if (isValid(arg0)) {
      assign(arg0);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg0 as Locale""")
  public void _replace(Locale arg0) {
    _copy(arg0);
  }

  @Ek9Operator("""
      operator :=:
        -> arg0 as Locale""")
  public void _copy(Locale arg0) {
    if (isValid(arg0)) {
      assign(arg0);
    } else {
      super.unSet();
    }
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg0 as Locale
        <- rtn as Integer?""")
  public Integer _fuzzy(Locale arg0) {
    if (canProcess(arg0)) {
      return Integer._of(this.lang.compareTo(arg0.lang));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg0 as Locale
        <- rtn as Integer?""")
  public Integer _cmp(Locale arg0) {
    if (canProcess(arg0)) {
      return Integer._of(this.compare(arg0));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg0 as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg0) {
    if (arg0 instanceof Locale asLocale) {
      return _cmp(asLocale);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator matches as pure
        -> arg as RegEx
        <- rtn as Boolean?""")
  public Boolean _matches(final RegEx arg) {
    if (canProcess(arg)) {
      return arg._matches(this);
    }

    return new Boolean();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _lt(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _lteq(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _gt(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _gteq(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _eq(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg0 as Locale
        <- rtn as Boolean?""")
  public Boolean _neq(Locale arg0) {
    if (canProcess(arg0)) {
      return Boolean._of(compare(arg0) != 0);
    }
    return new Boolean();
  }

  @Ek9Method("""
      language() as pure
        <- rtn as String?""")
  public String language() {
    return _prefix();
  }

  @Ek9Method("""
      country() as pure
        <- rtn as String?""")
  public String country() {
    return _suffix();
  }

  @Ek9Method("""
      shortFormat() as pure
        -> arg0 as Money
        <- rtn as String?""")
  public String shortFormat(Money arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(moneyFormat(arg0, false, false));
  }

  @Ek9Method("""
      shortFormat() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String shortFormat(Date arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.SHORT, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      shortFormat() as pure
        -> arg0 as DateTime
        <- rtn as String?""")
  public String shortFormat(DateTime arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.SHORT, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      shortFormat() as pure
        -> arg0 as Time
        <- rtn as String?""")
  public String shortFormat(Time arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.SHORT, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      mediumFormat() as pure
        -> arg0 as Money
        <- rtn as String?""")
  public String mediumFormat(Money arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(moneyFormat(arg0, true, false));
  }

  @Ek9Method("""
      mediumFormat() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String mediumFormat(Date arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.MEDIUM, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      mediumFormat() as pure
        -> arg0 as DateTime
        <- rtn as String?""")
  public String mediumFormat(DateTime arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.MEDIUM, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      mediumFormat() as pure
        -> arg0 as Time
        <- rtn as String?""")
  public String mediumFormat(Time arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.MEDIUM, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      longFormat() as pure
        -> arg0 as Money
        <- rtn as String?""")
  public String longFormat(Money arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(moneyFormat(arg0, false, true));
  }

  @Ek9Method("""
      longFormat() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String longFormat(Date arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.LONG, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      longFormat() as pure
        -> arg0 as DateTime
        <- rtn as String?""")
  public String longFormat(DateTime arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.LONG, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      longFormat() as pure
        -> arg0 as Time
        <- rtn as String?""")
  public String longFormat(Time arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.LONG, arg0._getAsJavaTemporalAccessor()));
  }

  @Ek9Method("""
      fullFormat() as pure
        -> arg0 as Money
        <- rtn as String?""")
  public String fullFormat(Money arg0) {
    return format(arg0);
  }

  @Ek9Method("""
      fullFormat() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String fullFormat(Date arg0) {
    return format(arg0);
  }

  @Ek9Method("""
      fullFormat() as pure
        -> arg0 as DateTime
        <- rtn as String?""")
  public String fullFormat(DateTime arg0) {
    return format(arg0);
  }

  @Ek9Method("""
      fullFormat() as pure
        -> arg0 as Time
        <- rtn as String?""")
  public String fullFormat(Time arg0) {
    return format(arg0);
  }

  @Ek9Method("""
      dayOfWeek() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String dayOfWeek(Date arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(arg0.state.getDayOfWeek().getDisplayName(
        java.time.format.TextStyle.FULL, getAsJavaLocale()));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Integer
        <- rtn as String?""")
  public String format(Integer arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(arg0.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Float
        <- rtn as String?""")
  public String format(Float arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(decimalFormat(arg0.state));
  }

  @Ek9Method("""
      format() as pure
        ->
          arg0 as Float
          precision as Integer
        <-
          rtn as String?""")
  public String format(Float arg0, Integer precision) {
    if (!canProcess(arg0) || !canProcess(precision)) {
      return new String();
    }
    return String._of(decimalFormat(arg0.state, (int) precision.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Money
        <- rtn as String?""")
  public String format(Money arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }

    return String._of(moneyFormat(arg0, true, true));
  }

  @Ek9Method("""
      format() as pure
        ->
          arg0 as Money
          showSymbol as Boolean
          showFractionalPart as Boolean
        <-
          rtn as String?""")
  public String format(Money arg0, Boolean showSymbol, Boolean showFractionalPart) {
    if (!canProcess(arg0) || !canProcess(showSymbol) || !canProcess(showFractionalPart)) {
      return new String();
    }

    return String._of(
        moneyFormat(arg0, showSymbol.state, showFractionalPart.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Boolean
        <- rtn as String?""")
  public String format(Boolean arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(java.lang.Boolean.toString(arg0.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Date
        <- rtn as String?""")
  public String format(Date arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.FULL, arg0.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Time
        <- rtn as String?""")
  public String format(Time arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.FULL, arg0.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as DateTime
        <- rtn as String?""")
  public String format(DateTime arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(FormatStyle.FULL, arg0.state));
  }

  @Ek9Method("""
      format() as pure
        -> arg0 as Dimension
        <- rtn as String?""")
  public String format(Dimension arg0) {
    if (!canProcess(arg0)) {
      return new String();
    }
    return String._of(format(arg0.state));
  }

  @Ek9Method("""
      format() as pure
        ->
          arg0 as Dimension
          precision as Integer
        <-
          rtn as String?""")
  public String format(Dimension arg0, Integer precision) {
    if (!canProcess(arg0) || !canProcess(precision)) {
      return new String();
    }
    return String._of(decimalFormat(arg0.state, (int) precision.state));
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as String?""")
  public String _prefix() {
    if (isSet) {
      return String._of(this.lang);
    }
    return new String();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as String?""")
  public String _suffix() {
    if (isSet && suffix != null) {
      return String._of(super.suffix);
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      return new JSON(this);
    }
    return new JSON();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      if (suffix != null && !suffix.isEmpty()) {
        return String._of(lang + "_" + suffix);
      } else {
        return String._of(lang);
      }
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(java.util.Objects.hash(lang, suffix));
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  //Start of Utility methods

  private java.util.Locale getAsJavaLocale() {
    if (suffix != null && !suffix.isEmpty()) {
      return java.util.Locale.forLanguageTag(this.lang + "-" + this.suffix);
    }
    return java.util.Locale.forLanguageTag(this.lang);
  }

  protected void parse(java.lang.String value) {
    //It is possible to just had "en" for example, not just the full format of
    //"en_GB" or "en_US".
    Pattern p = Pattern.compile("^([a-z]{2})([_-][A-Z]{2})?$");
    Matcher m = p.matcher(value);

    if (m.find()) {
      java.lang.String first = m.group(1);
      java.lang.String second = m.group(2);
      if (second != null) {
        assign(first, second.substring(1));
      } else {
        assign(first, "");
      }
    }
  }

  private int compare(Locale value) {

    final var langCmp = this.lang.compareTo(value.lang);
    if (langCmp == 0 && this.suffix != null) {
      return this.suffix.compareTo(value.suffix);
    }
    return langCmp;
  }

  private void assign(Locale value) {
    if (isValid(value)) {
      assign(value.lang, value.suffix);
    }
  }

  private void assign(java.lang.String theLang, java.lang.String theSuffix) {
    if (theSuffix == null) {
      theSuffix = "";
    }
    boolean isSetBefore = isSet;
    java.lang.String langBefore = this.lang;
    java.lang.String suffixBefore = this.suffix;

    this.lang = theLang;
    this.suffix = theSuffix;
    if (!validateConstraints().state) {
      java.lang.String stringTo = this.toString();
      this.lang = langBefore;
      this.suffix = suffixBefore;
      this.isSet = isSetBefore;
      throw Exception._of("Constraint violation can't change " + this + " to " + stringTo);
    }
    set();

  }

  @SuppressWarnings({"checkstyle:PatternVariableName", "checkstyle:OverloadMethodsDeclarationOrder"})
  private java.lang.String format(java.time.format.FormatStyle style, TemporalAccessor temporalItem) {
    //You'd have thought the java API would have been able to handle this but no.
    switch (temporalItem) {
      case LocalDate _ -> {
        return DateTimeFormatter.ofLocalizedDate(style).withLocale(getAsJavaLocale()).format(temporalItem);
      }
      case LocalDateTime _, ZonedDateTime _ -> {
        return DateTimeFormatter.ofLocalizedDateTime(style).withLocale(getAsJavaLocale()).format(temporalItem);
      }
      default -> {
        //Because we use a LocaleTime in our Time component LONG and FULL try to use the zone - which we don't have
        if (style.equals(FormatStyle.LONG) || style.equals(FormatStyle.FULL)) {
          style = FormatStyle.MEDIUM;
        }
        return DateTimeFormatter.ofLocalizedTime(style).withLocale(getAsJavaLocale()).format(temporalItem);
      }
    }
  }

  private java.lang.String format(Number number) {
    return NumberFormat.getNumberInstance(getAsJavaLocale()).format(number);
  }

  private java.lang.String decimalFormat(Number number, int numFractionalDigits) {
    NumberFormat nf = NumberFormat.getNumberInstance(getAsJavaLocale());
    nf.setMinimumFractionDigits(numFractionalDigits);
    nf.setMaximumFractionDigits(numFractionalDigits);
    nf.setRoundingMode(RoundingMode.HALF_UP);
    return nf.format(number);
  }

  private java.lang.String decimalFormat(Number number) {
    NumberFormat nf = NumberFormat.getNumberInstance(getAsJavaLocale());

    nf.setMaximumFractionDigits(300);
    nf.setRoundingMode(RoundingMode.HALF_UP);
    return nf.format(number);
  }

  private java.lang.String moneyFormat(Money value, boolean includeSymbol, boolean includeDefaultFractionalPart) {
    NumberFormat nf = NumberFormat.getNumberInstance(getAsJavaLocale());
    if (includeSymbol) {
      nf = NumberFormat.getCurrencyInstance(getAsJavaLocale());
      nf.setCurrency(value.currency);
    }

    if (!includeDefaultFractionalPart) {
      nf.setMinimumFractionDigits(0);
      nf.setMaximumFractionDigits(0);
    } else {
      int numFractionalDigits = value.currency.getDefaultFractionDigits();
      nf.setMinimumFractionDigits(numFractionalDigits);
      nf.setMaximumFractionDigits(numFractionalDigits);
    }
    nf.setRoundingMode(RoundingMode.HALF_UP);

    return nf.format(value.state);
  }

  public static Locale _of() {
    return new Locale();
  }

  public static Locale _of(java.lang.String value) {
    Locale rtn = new Locale();

    rtn.parse(value);

    return rtn;
  }

  public static Locale _of(java.lang.String language, java.lang.String country) {
    Locale rtn = new Locale();
    rtn.assign(language, country);
    return rtn;
  }

}
