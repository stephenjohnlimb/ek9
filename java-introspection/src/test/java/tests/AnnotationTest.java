package tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9introspection.Ek9ExternExtractor;
import org.junit.jupiter.api.Test;


class AnnotationTest {
  final Ek9ExternExtractor underTest = new Ek9ExternExtractor();

  final List<String> mustContain = List.of(
      "net.customer.geometry::PI",
      "EventHandler of StringExample",
      "IteratorExample of StringExample",
      "ListExample of StringExample",
      "CardRank",
      "CardSuit",
      "CostAssessment",
      "DemoApp",
      "EmployeeId as Integer constrain",
      "EventHandler of type T",
      "General() as pure",
      "IteratorExample of type T as abstract",
      "ListExample of type T as open",
      "Processor with trait of Monitorable, CostAssessment",
      "Program1",
      "Program2",
      "Site :/site",
      "StderrInterface is OutputInterface",
      "StringExample",
      "WelcomePageText",
      "defines text for \"de\"",
      "defines text for \"en_GB\""
  );

  @Test
  void testValidEk9Annotations() {
    final var packageName = "org.company.dept";

    final var possibleEk9Interface = underTest.apply(packageName);
    assertNotNull(possibleEk9Interface.ek9Interface());
    assertNull(possibleEk9Interface.errorMessage());

    //Simple check to see if the annotations have been detected.
    assertCorrectConstructs(possibleEk9Interface.ek9Interface());
  }

  void assertCorrectConstructs(final String ek9Interface) {

    mustContain
        .forEach(construct -> assertTrue(ek9Interface.contains(construct), "Missing [" + construct +"]"));

  }

  @Test
  void testInvalidEk9Annotations() {
    final var packageName = "org.company.nosuchdept";

    final var possibleEk9Interface = underTest.apply(packageName);
    assertNull(possibleEk9Interface.ek9Interface());
    assertNotNull(possibleEk9Interface.errorMessage());
  }
}
