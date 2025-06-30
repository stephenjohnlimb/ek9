package org.company.dept;

import org.ek9tooling.Ek9EnumType;
import org.ek9tooling.Ek9Property;

/**
 * An EK9 enumerated type is actually just a Java class with specific methods on it.
 * We need to do this because, the EK9 compiler will use a class when it generates an EK9
 * enumeration. It also adds in lots of automatic generated methods, like hashcode, isSet, toString etc.
 */
@Ek9EnumType("CardRank")
public class CardRank {
  private final String value;

  @Ek9Property
  public CardRank Two = new CardRank("Two");

  @Ek9Property
  public CardRank Three = new CardRank("Three");

  @Ek9Property
  public CardRank Four = new CardRank("Four");

  @Ek9Property
  public CardRank Five = new CardRank("Five");

  @Ek9Property
  public CardRank Six = new CardRank("Six");

  @Ek9Property
  public CardRank Seven = new CardRank("Seven");

  @Ek9Property
  public CardRank Eight = new CardRank("Eight");

  @Ek9Property
  public CardRank Nine = new CardRank("Nine");

  @Ek9Property
  public CardRank Ten = new CardRank("Ten");

  @Ek9Property
  public CardRank Jack = new CardRank("Jack");

  @Ek9Property
  public CardRank Queen = new CardRank("Queen");

  @Ek9Property
  public CardRank King = new CardRank("King");

  @Ek9Property
  public CardRank Ace = new CardRank("Ace");

  private CardRank(String value) {
    this.value = value;
  }

  //TODO all of the EK9 built in methods, once the standard library has been created.

  public static CardRank _of(final String value) {
    return new CardRank(value);
  }

  public static String _from(final CardRank cardRank) {
    return cardRank.value;
  }
}
