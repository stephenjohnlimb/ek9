package org.company.dept;

/**
 * When the Ek9 compiler actually creates Java classes it will create something like this.
 * This is then used to get the correct language instance.
 * <p>
 *   So basically in Ek9 you would write:
 * </p>
 * <pre>
 *   welcomeText <- WelcomePageText("de")
 * </pre>
 * <p>
 *   Then the Ek9 compiler will convert that into:
 * </p>
 * <pre>
 *   final var welcomeText = WelcomePageText._of("de");
 * </pre>
 * <p>
 *   Note that Ek9 text is more than just a bunch of flat text with placeholders.
 *   It actually enables code to be called via interpolation and it is 'type-safe'.
 *   There are no %s or %d things to get wrong or missing placeholders.
 * </p>
 */
public abstract class WelcomePageText {

  public static WelcomePageText _of(final String lang) {

    return switch (lang) {
      case "en_GB" -> new WelcomePageText_en_GB();
      case "de" -> new WelcomePageText_de();
      default -> throw new RuntimeException("Finite set of WelcomePageText exhausted");
    };

  }

  //Just to ensure that all instances have the right methods on them.
  public abstract String namedWelcome(final String name) ;
}
