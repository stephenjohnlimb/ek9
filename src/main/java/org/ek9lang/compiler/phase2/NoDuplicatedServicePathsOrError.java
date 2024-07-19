package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.support.SymbolFactory.HTTP_VERB;
import static org.ek9lang.compiler.support.SymbolFactory.URI_PROTO;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;

/**
 * Examines all the service operations on a service and looks at the verbs used and the uti proto paths.
 * Checks for duplications and issues errors if paths (irrespective of variable names) are duplicated.
 */
final class NoDuplicatedServicePathsOrError extends RuleSupport implements Consumer<IAggregateSymbol> {

  NoDuplicatedServicePathsOrError(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final IAggregateSymbol service) {

    final Map<String, Map<String, ServiceOperationSymbol>> verbToPathToOperation = new HashMap<>();

    getServiceOperations(service).forEach(operation -> {
      final var verb = operation.getSquirrelledData(HTTP_VERB);
      final var path = removedParameterNames(operation.getSquirrelledData(URI_PROTO));

      var verbMap = verbToPathToOperation.get(verb);
      if (verbMap == null) {
        verbMap = new HashMap<>();
        verbMap.put(path, operation);
        verbToPathToOperation.put(verb, verbMap);
      } else {
        var existingPathToService = verbMap.get(path);
        if (existingPathToService == null) {
          verbMap.put(path, operation);
        } else {
          emitDuplicatePathError(existingPathToService, operation);
        }
      }
    });

  }

  private void emitDuplicatePathError(final ServiceOperationSymbol existingPathToService,
                                      final ServiceOperationSymbol operation) {

    final var verb = existingPathToService.getSquirrelledData(HTTP_VERB);
    final var msg = "HTTP verb: '"
        + verb
        + "' '"
        + existingPathToService.getSquirrelledData(URI_PROTO)
        + "' and '"
        + operation.getSquirrelledData(URI_PROTO)
        + "':";

    //Output two errors one of each of the clashing values.
    errorListener.semanticError(existingPathToService.getSourceToken(), msg,
        ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_DUPLICATED);
    errorListener.semanticError(operation.getSourceToken(), msg,
        ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_DUPLICATED);

  }

  private Stream<ServiceOperationSymbol> getServiceOperations(final IAggregateSymbol service) {

    return service.getAllNonAbstractMethods()
        .stream()
        .filter(ServiceOperationSymbol.class::isInstance)
        .map(ServiceOperationSymbol.class::cast);
  }

  private String removedParameterNames(final String utiProtoPath) {

    return utiProtoPath.replaceAll("\\{.*?}", "{}");

  }
}
