package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.ParameterDetails;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Extracts metadata from EK9 program symbols to create ProgramDetails objects
 * for the PROGRAM_ENTRY_POINT_BLOCK. Reuses existing patterns from the codebase
 * to avoid code duplication.
 */
public final class ProgramMetadataExtractor implements Function<AggregateSymbol, ProgramDetails> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  @Override
  public ProgramDetails apply(final AggregateSymbol programSymbol) {

    AssertValue.checkNotNull("Program symbol cannot be null", programSymbol);

    // Extract qualified name (REUSE existing pattern)
    final var qualifiedName = programSymbol.getFullyQualifiedName();

    // Extract parameter signature (REUSE existing patterns from ConstructorCallProcessor)
    final var parameterSignature = extractParameterSignature(programSymbol);

    final var applicationName = programSymbol
        .getApplication()
        .map(AggregateSymbol::getFullyQualifiedName)
        .orElse(null);

    return new ProgramDetails(
        qualifiedName,
        parameterSignature,
        applicationName
    );
  }

  /**
   * Extract parameter signature from program symbol.
   * REUSES the exact pattern from ConstructorCallProcessor:73-75.
   */
  private List<ParameterDetails> extractParameterSignature(final AggregateSymbol programSymbol) {
    // REUSE existing pattern: getAllMethods().stream().findFirst()
    AssertValue.checkTrue("Expecting only one method on program",
        programSymbol.getAllMethods().size() == 1);

    final var method = programSymbol.getAllMethods().stream().findFirst().orElseThrow();

    // REUSE existing pattern: getCallParameters().stream().map(typeNameOrException)
    final var parameters = method.getCallParameters();

    return IntStream.range(0, parameters.size())
        .mapToObj(i -> {
          final var param = parameters.get(i);
          // REUSE existing patterns
          final var name = param.getName();                    // Standard getName() pattern
          final var type = typeNameOrException.apply(param);   // REUSE TypeNameOrException
          return new ParameterDetails(name, type, i);
        })
        .toList();
  }


}