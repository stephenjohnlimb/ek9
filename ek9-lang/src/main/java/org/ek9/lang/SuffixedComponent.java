package org.ek9.lang;

import java.util.Objects;

/**
 * Used as an abstract base for several classes that have a prefix and a suffix.
 * These are typically fairly simple classes that have a value but also some type of
 * sub typing. For example Millisecond might just be a number but becomes stronger types when given
 * the suffix of 'ms'. This is more important for classes like dimension and money where the suffix can change.
 * <p>
 * So while the values 'could be used mathematically' really you cannot add $ to £ without conversion.
 * </p>
 */
public abstract class SuffixedComponent extends BuiltinType {
  java.lang.String suffix;

  protected SuffixedComponent() {
    //Default constructor
  }


  protected abstract void parse(java.lang.String value);


  protected void assertValidSuffix(java.lang.String suffixToTest) {
    if (!suffix.equals(suffixToTest)) {
      throw new RuntimeException("For type " + this.getClass().getSimpleName() + " " + suffix + " <> " + suffixToTest);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final SuffixedComponent that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    if (isSet) {
      return Objects.equals(suffix, that.suffix);
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(suffix);
    return result;
  }
}
