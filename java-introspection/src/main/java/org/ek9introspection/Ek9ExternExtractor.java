package org.ek9introspection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Function;
import org.ek9tooling.Ek9Application;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Component;
import org.ek9tooling.Ek9Constants;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Package;
import org.ek9tooling.Ek9Record;
import org.ek9tooling.Ek9Service;
import org.ek9tooling.Ek9Trait;

/**
 * Designed to introspect Java based package and produce the 'extern' Ek9 interface for it.
 * This will be used for the built-in provided constructs of org.ek9.lang and org.ek9.math.
 * But will also be used for 'any' provided Java jar as long as it uses the ek9tooling annotations
 * and conventions.
 * <p>
 * Given a Java Package Name (Ek9 module) it will introspect and return the extern interface for those
 * classes/function and components.
 * </p>
 * <p>
 * This is basically the mechanism that is to be used to link EK9 code to java.
 * </p>
 * <p>
 * At some point if I can get through all this, there will be a llvm (binary) equivalent of this.
 * But that's probably years away yet.
 * </p>
 */
public class Ek9ExternExtractor implements Function<String, Ek9InterfaceOrError> {

  private final ClassLister classLister = new ClassLister();
  private final ValidEk9Interface validEk9Interface = new ValidEk9Interface();

  @Override
  public Ek9InterfaceOrError apply(final String packageName) {
    final var packageInfo = "package-info";
    final var ek9ModuleName = packageName + "." + packageInfo;

    final var classes = classLister.findAllClassesUsingClassLoader(packageName);

    try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         PrintStream printStream = new PrintStream(outputStream)) {

      final var moduleIntrospector = new ModuleIntrospector(printStream, ek9ModuleName);

      final var interfaceOrError = moduleIntrospector.apply(classes);
      if (validEk9Interface.test(interfaceOrError)) {
        final var byConstructType = classLister.findByConstruct(classes);

        //The order of these has been designed to pull in constructs in a reasonable way

        new ReferencesIntrospector(printStream).accept(classes);

        new GenericAwareIntrospector(printStream, false, Ek9Class.class).accept(byConstructType);
        new GenericAwareIntrospector(printStream, false, Ek9Function.class).accept(byConstructType);

        new ConstantsIntrospector(printStream).accept(byConstructType.get(Ek9Constants.class));

        new TypeIntrospector(printStream).accept(byConstructType);

        new GenericAwareIntrospector(printStream, true, Ek9Function.class).accept(byConstructType);
        new GenericAwareIntrospector(printStream, true, Ek9Class.class).accept(byConstructType);

        new GeneralConstructIntrospector(printStream, Ek9Trait.class).accept(byConstructType);
        new GeneralConstructIntrospector(printStream, Ek9Record.class).accept(byConstructType);

        new TextIntrospector(printStream).accept(byConstructType);

        new GeneralConstructIntrospector(printStream, Ek9Component.class).accept(byConstructType);
        new GeneralConstructIntrospector(printStream, Ek9Service.class).accept(byConstructType);
        new GeneralConstructIntrospector(printStream, Ek9Application.class).accept(byConstructType);

        new ProgramIntrospector(printStream).accept(byConstructType);
        new PackageIntrospector(printStream).accept(byConstructType.get(Ek9Package.class));

        printStream.printf("%n//EOF%n");

        return new Ek9InterfaceOrError(outputStream.toString(), null);
      }
      return interfaceOrError;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
