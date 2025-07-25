package org.ek9.lang;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 Money type implementation.
 * Represents monetary values with currency codes using BigDecimal for precision.
 * Currency-safe operations only work with same currency - different currencies result in unset.
 * Format: amount#CurrencyCode (e.g., 10.50#USD, 99.99#GBP)
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Money as open""")
public class Money extends BuiltinType {

  BigDecimal state;
  Currency currency;

  @Ek9Constructor("""
      Money() as pure""")
  public Money() {
    unSet();
  }

  @Ek9Constructor("""
      Money() as pure
        -> arg0 as Money""")
  public Money(Money arg0) {
    unSet();
    assign(arg0);
  }

  @Ek9Constructor("""
      Money() as pure
        -> arg0 as String""")
  public Money(String arg0) {
    unSet();
    if (isValid(arg0)) {
      parseMoneyString(arg0.state);
    }
  }

  @Ek9Method("""
      convert() as pure
        -> arg0 as Money
        <- rtn as Money?""")
  public Money convert(Money arg0) {
    if (canProcess(arg0)) {
      // Convert using 1:1 exchange rate to target currency
      return convertInternal(BigDecimal.ONE, arg0.currency);
    }
    return _new();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      convert() as pure
        ->
          multiplier as Float
          currencyCode as String
        <-
          rtn as Money?""")
  public Money convert(Float multiplier, String currencyCode) {
    if (canProcess(multiplier) && canProcess(currencyCode)) {
      try {
        Currency targetCurrency = Currency.getInstance(currencyCode.state);
        return convertInternal(BigDecimal.valueOf(multiplier.state), targetCurrency);
      } catch (IllegalArgumentException _) {
        // Invalid currency code
        return _new();
      }
    }
    return _new();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _lt(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _lteq(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _gt(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _gteq(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _eq(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) == 0);
    }
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof Money asMoney) {
      return _eq(asMoney);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Money
        <- rtn as Boolean?""")
  public Boolean _neq(Money arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Money
        <- rtn as Integer?""")
  public Integer _cmp(Money arg) {
    if (canProcessSameCurrency(arg)) {
      return Integer._of(this.state.compareTo(arg.state));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Money asMoney) {
      return _cmp(asMoney);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Money
        <- rtn as Integer?""")
  public Integer _fuzzy(Money arg) {
    if (canProcessSameCurrency(arg)) {
      // For money, fuzzy match is just comparison
      return _cmp(arg);
    }
    return new Integer();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Operator("""
      operator sqrt as pure
        <- rtn as Money?""")
  public Money _sqrt() {
    if (isSet && state.compareTo(BigDecimal.ZERO) >= 0) {
      try {
        // Calculate square root using Newton's method for BigDecimal
        BigDecimal sqrtValue = sqrt(state);
        Money result = _new();
        result.assign(sqrtValue, currency);
        return result;
      } catch (ArithmeticException _) {
        return _new();
      }
    }
    return _new();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Money
        <- rtn as Money?""")
  public Money _add(Money arg) {
    if (canProcessSameCurrency(arg)) {
      Money result = _new();
      result.assign(this.state.add(arg.state), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator - as pure
        <- rtn as Money?""")
  public Money _negate() {
    if (isSet) {
      Money result = _new();
      result.assign(this.state.negate(), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Money
        <- rtn as Money?""")
  public Money _sub(Money arg) {
    if (canProcessSameCurrency(arg)) {
      Money result = _new();
      result.assign(this.state.subtract(arg.state), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Money?""")
  public Money _mul(Integer arg) {
    if (canProcess(arg)) {
      Money result = _new();
      BigDecimal multiplier = BigDecimal.valueOf(arg.state);
      result.assign(this.state.multiply(multiplier), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Money?""")
  public Money _mul(Float arg) {
    if (canProcess(arg)) {
      Money result = _new();
      final var scale = currency.getDefaultFractionDigits();
      final var multiplier = BigDecimal.valueOf(arg.state);
      final var product = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
      result.assign(product, this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Money?""")
  public Money _div(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
      Money result = _new();
      BigDecimal divisor = BigDecimal.valueOf(arg.state);
      int scale = currency.getDefaultFractionDigits();
      BigDecimal quotient = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
      result.assign(quotient, this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Money?""")
  public Money _div(Float arg) {
    if (canProcess(arg) && arg.state != 0.0) {
      Money result = _new();
      BigDecimal divisor = BigDecimal.valueOf(arg.state);
      int scale = currency.getDefaultFractionDigits();
      BigDecimal quotient = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
      result.assign(quotient, this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Money
        <- rtn as Float?""")
  public Float _div(Money arg) {
    if (canProcessSameCurrency(arg) && arg.state.compareTo(BigDecimal.ZERO) != 0) {
      BigDecimal ratio = this.state.divide(arg.state, 10, RoundingMode.HALF_UP);
      return Float._of(ratio.doubleValue());
    }
    return new Float();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Operator("""
      operator ^ as pure
        -> arg as Integer
        <- rtn as Money?""")
  public Money _pow(Integer arg) {
    if (canProcess(arg)) {
      try {
        Money result = _new();
        BigDecimal power = this.state.pow((int) arg.state);
        int scale = currency.getDefaultFractionDigits();
        power = power.setScale(scale, RoundingMode.HALF_UP);
        result.assign(power, this.currency);
        return result;
      } catch (ArithmeticException _) {
        return _new();
      }
    }
    return _new();
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Money?""")
  public Money _abs() {
    if (isSet) {
      Money result = _new();
      result.assign(this.state.abs(), this.currency);
      return result;
    }
    return _new();
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
      return String._of(state.toPlainString() + "#" + currency.getCurrencyCode());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      int result = Objects.hash(state, currency);
      return Integer._of(result);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Float?""")
  public Float _amount() {
    if (isSet) {
      return Float._of(state.doubleValue());
    }
    return new Float();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as String?""")
  public String _currency() {
    if (isSet) {
      return String._of(currency.getCurrencyCode());
    }
    return new String();
  }

  @Ek9Operator("""
      operator ++
        <- rtn as Money?""")
  public Money _inc() {
    if (isSet) {
      Money result = _new();
      BigDecimal increment = getSmallestUnit();
      result.assign(this.state.add(increment), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator --
        <- rtn as Money?""")
  public Money _dec() {
    if (isSet) {
      Money result = _new();
      BigDecimal decrement = getSmallestUnit();
      result.assign(this.state.subtract(decrement), this.currency);
      return result;
    }
    return _new();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(state.compareTo(BigDecimal.ZERO) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator +=
        -> arg as Money""")
  public void _addAss(Money arg) {
    if (canProcessSameCurrency(arg)) {
      this.state = this.state.add(arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as Money""")
  public void _subAss(Money arg) {
    if (canProcessSameCurrency(arg)) {
      this.state = this.state.subtract(arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Integer""")
  public void _mulAss(Integer arg) {
    if (canProcess(arg)) {
      BigDecimal multiplier = BigDecimal.valueOf(arg.state);
      this.state = this.state.multiply(multiplier);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Float""")
  public void _mulAss(Float arg) {
    if (canProcess(arg)) {
      final var multiplier = BigDecimal.valueOf(arg.state);
      final var scale = currency.getDefaultFractionDigits();
      this.state = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator /=
        -> arg as Integer""")
  public void _divAss(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
      final var divisor = BigDecimal.valueOf(arg.state);
      final var scale = currency.getDefaultFractionDigits();
      this.state = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator /=
        -> arg as Float""")
  public void _divAss(Float arg) {
    if (canProcess(arg) && !nearEnoughToZero(arg.state)) {
      final var divisor = BigDecimal.valueOf(arg.state);
      final var scale = currency.getDefaultFractionDigits();
      this.state = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Money""")
  public void _copy(Money arg) {
    assign(arg);
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Money""")
  public void _merge(Money arg) {
    if (isValid(arg)) {
      if (!isSet) {
        //If this is not set then just assign and take new value
        assign(arg.state, arg.currency);
      } else if (this.currency.equals(arg.currency)) {
        // Merge by addition if same currency
        this.state = this.state.add(arg.state);
      } else {
        unSet();
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Money""")
  public void _replace(Money arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator |
        -> arg as Money""")
  public void _pipe(Money arg) {
    _merge(arg);
  }

  // Utility methods

  @Override
  protected Money _new() {
    return new Money();
  }

  private void assign(Money arg) {
    if (isValid(arg)) {
      assign(arg.state, arg.currency);
    } else {
      unSet();
    }
  }

  private void assign(BigDecimal amount, Currency curr) {
    this.state = amount;
    this.currency = curr;
    set();
  }

  private boolean canProcessSameCurrency(Money arg) {
    return canProcess(arg) && this.currency.equals(arg.currency);
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private void parseMoneyString(java.lang.String input) {
    try {
      // Format: amount#CurrencyCode (e.g., "10.50#USD", "99.99#GBP")
      int hashIndex = input.indexOf('#');
      if (hashIndex == -1 || hashIndex == 0 || hashIndex == input.length() - 1) {
        return; // Invalid format
      }

      final var amountStr = input.substring(0, hashIndex);
      final var currencyStr = input.substring(hashIndex + 1);

      if (currencyStr.length() != 3) {
        return; // Currency code must be 3 characters
      }

      final var amount = new BigDecimal(amountStr);
      final var curr = Currency.getInstance(currencyStr);

      // Set scale to currency's default fraction digits
      final int scale = curr.getDefaultFractionDigits();
      final var scaled = amount.setScale(scale, RoundingMode.HALF_UP);

      assign(scaled, curr);
    } catch (IllegalArgumentException _) {
      // Invalid number format
      unSet();
    }
  }

  private Money convertInternal(BigDecimal exchangeRate, Currency targetCurrency) {
    final var result = _new();
    final var scale = targetCurrency.getDefaultFractionDigits();
    final var convertedAmount = this.state.multiply(exchangeRate).setScale(scale, RoundingMode.HALF_UP);
    result.assign(convertedAmount, targetCurrency);
    return result;
  }

  // BigDecimal square root using Newton's method
  private BigDecimal sqrt(BigDecimal value) {

    BigDecimal x = value;
    BigDecimal previous;
    BigDecimal two = BigDecimal.valueOf(2);
    int scale = currency.getDefaultFractionDigits() + 10; // Extra precision for calculation

    do {
      previous = x;
      x = x.add(value.divide(x, scale, RoundingMode.HALF_UP)).divide(two, scale, RoundingMode.HALF_UP);
    } while (x.subtract(previous).abs().compareTo(BigDecimal.valueOf(0.0001)) > 0);

    return x.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
  }

  // Get smallest unit for currency (e.g., 0.01 for USD, 1 for JPY)
  private BigDecimal getSmallestUnit() {
    int scale = currency.getDefaultFractionDigits();
    if (scale == 0) {
      return BigDecimal.ONE;
    }
    return BigDecimal.ONE.scaleByPowerOfTen(-scale);
  }

  // Factory methods
  public static Money _of() {
    return new Money();
  }

  public static Money _of(java.lang.String moneyString) {
    return new Money(String._of(moneyString));
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static Money _of(BigDecimal amount, java.lang.String currencyCode) {
    try {
      final var currency = Currency.getInstance(currencyCode);
      final var result = new Money();
      final var scale = currency.getDefaultFractionDigits();
      final var scaled = amount.setScale(scale, RoundingMode.HALF_UP);
      result.assign(scaled, currency);
      return result;
    } catch (IllegalArgumentException _) {
      return new Money();
    }
  }

  public static Money _of(double amount, java.lang.String currencyCode) {
    return _of(BigDecimal.valueOf(amount), currencyCode);
  }
}