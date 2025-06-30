package org.company.dept;

import org.ek9tooling.Ek9EnumType;
import org.ek9tooling.Ek9Property;

/**
 * Another example of a enumeration.
 */
@Ek9EnumType("CardSuit")
public class CardSuit {
  private final String value;

  @Ek9Property
  public CardSuit Hearts = new CardSuit("Hearts");

  @Ek9Property
  public CardSuit Diamonds = new CardSuit("Diamonds");

  @Ek9Property
  public CardSuit Clubs = new CardSuit("Clubs");

  @Ek9Property
  public CardSuit Spades = new CardSuit("Spades");

  private CardSuit(String value) {
    this.value = value;
  }

  //TODO all of the EK9 built in methods, once the standard library has been created.

  public static CardSuit _of(final String value) {
    return new CardSuit(value);
  }

  public static String _from(final CardSuit cardSuit) {
    return cardSuit.value;
  }
}
