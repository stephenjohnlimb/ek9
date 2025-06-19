package org.ek9lang.compiler.support;

import java.util.Arrays;
import java.util.function.Function;
import org.ek9lang.core.Digest;

/**
 * Useful function that generates the decorated/internal name for generic types that have been
 * parameterized. Uses a mix of fully qualified and basic names and then a hashing function.
 * <p>
 *   Note that this also has a 'main' entry point so that it can be used by developers when looking
 *   to create parameterised generic types.
 * </p>
 * <p>
 *   So it can be run from the Java command line as follows (from the project root):
 * </p>
 * <pre>
 *   java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName List org.ek9.lang::List org.ek9.lang::String
 *   # Response will be
 *   _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1
 * </pre>
 */
@SuppressWarnings("checkstyle:LineLength")
public class DecoratedName implements Function<InternalNameDetails, String> {

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println(
          "Expecting primaryName, fully qualified genericName, any number of fully qualified parameterising types");
      System.err.println(
          "EG: List org.ek9.lang::List org.ek9.lang::String");

      System.err.println("So the above fully qualified name would be as follows:");
      System.err.println("org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1");

      System.err.println("Just the name would be:");
      System.err.println("_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1");
    }

    final var details = new InternalNameDetails(args[0], args[1], Arrays.stream(args).skip(2).toList());
    System.out.println(new DecoratedName().apply(details));
  }

  @Override
  public String apply(final InternalNameDetails details) {
    final var toDigest = details.genericName() + "_" + String.join("_", details.parameters());
    return "_" + details.primaryName() + "_" + Digest.digest(toDigest);
  }
}
