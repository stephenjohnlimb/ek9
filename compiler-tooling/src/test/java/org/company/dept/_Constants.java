package org.company.dept;


import org.ek9tooling.Ek9Constant;
import org.ek9tooling.Ek9Constants;

/**
 * The class must be named _Constants as well, this so the EK9 compiler can find it.
 * So when the EK9 code of:
 * <pre>
 *   ...
 *   someValue := MyPi
 *   ...
 * </pre>
 * <p>
 *   The EK9 compiler will generate.
 * </p>
 * <p>
 *   ...
 *   someValue = the.package.name._Constants.MyPi._clone()
 * </p>
 * <p>
 *   So in effect the constants are just used to be cloned and then the variables
 *   that now have that value can be manipulated (without altering the original constant).
 * </p>
 */
@Ek9Constants
public class _Constants {

  @Ek9Constant("MyPi <- 3.142")
  public static Float MyPi = 3.142f;

  @Ek9Constant("Mye <- 2.7182818284")
  public static Float Mye  = 2.7182818284f;

  @Ek9Constant("MyRoot2 <- 1.41421356237309504880")
  public static Float MyRoot2 = 1.41421356237309504880f;
}
