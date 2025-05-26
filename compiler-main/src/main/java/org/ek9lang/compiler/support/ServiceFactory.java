package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.HTTP_ACCESS;
import static org.ek9lang.compiler.support.CommonValues.HTTP_SOURCE;
import static org.ek9lang.compiler.support.CommonValues.HTTP_URI;
import static org.ek9lang.compiler.support.CommonValues.HTTP_VERB;
import static org.ek9lang.compiler.support.CommonValues.URI_PROTO;

import java.util.Map;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Deals with the creation of the service parts for the EK9 language.
 */
public class ServiceFactory extends CommonFactory {

  private final CheckForInvalidServiceDefinition checkForInvalidServiceDefinition;
  private final CheckForInvalidServiceOperator checkForInvalidServiceOperator;

  private static final Map<String, String> operatorToHttpVerbMap = Map.of(
      "+", "POST",
      "+=", "POST",
      "-", "DELETE",
      "-=", "DELETE:",
      ":^:", "PUT",
      ":~:", "PATCH",
      "?", "HEAD"
  );

  ServiceFactory(ParsedModule parsedModule) {
    super(parsedModule);
    this.checkForInvalidServiceDefinition
        = new CheckForInvalidServiceDefinition(parsedModule.getSource().getErrorListener());
    this.checkForInvalidServiceOperator
        = new CheckForInvalidServiceOperator(parsedModule.getSource().getErrorListener());
  }

  /**
   * Create a new aggregate that represents an EK9 service.
   */
  public AggregateSymbol newService(final EK9Parser.ServiceDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    final var serviceName = ctx.Identifier().getText();
    final var service = new AggregateSymbol(serviceName, parsedModule.getModuleScope());
    final var uri = ctx.Uriproto().getText();

    configureAggregate(service, new Ek9Token(ctx.start));
    service.setGenus(SymbolGenus.SERVICE);
    service.putSquirrelledData(HTTP_URI, uri);
    checkForInvalidServiceDefinition.accept(service);

    return service;
  }

  /**
   * Create a new aggregate that represents an EK9 service operation - a specialised method.
   */
  public ServiceOperationSymbol newServiceOperation(final EK9Parser.ServiceOperationDeclarationContext ctx,
                                                    final IScope scope) {

    checkContextNotNull.accept(ctx);
    final var operator = ctx.operator() != null;
    final var methodName = operator ? ctx.operator().getText() : ctx.identifier().getText();
    final var httpVerb = ctx.httpVerb() != null ? ctx.httpVerb().getText() : "GET";
    final var serviceOperation = new ServiceOperationSymbol(methodName, scope);

    configureSymbol(serviceOperation, new Ek9Token(ctx.start));
    serviceOperation.setOverride(false);
    serviceOperation.setMarkedAbstract(false);
    serviceOperation.setMarkedPure(false);
    serviceOperation.setOperator(operator);
    serviceOperation.putSquirrelledData(URI_PROTO, ctx.Uriproto().getText());
    serviceOperation.putSquirrelledData(HTTP_VERB, httpVerb);

    if (operator) {
      //Check operator used is valid.
      checkForInvalidServiceOperator.accept(serviceOperation);
      serviceOperation.putSquirrelledData(HTTP_VERB,
          operatorToHttpVerbMap.getOrDefault(serviceOperation.getName(), ""));
    }

    return serviceOperation;
  }

  public void configureWebVariable(final EK9Parser.VariableOnlyDeclarationContext ctx, VariableSymbol variable) {

    checkContextNotNull.accept(ctx);
    //We need to squirrel away the information, so it can be both checked and used elsewhere
    variable.putSquirrelledData(HTTP_ACCESS, ctx.webVariableCorrelation().httpAccess().getText());
    if (ctx.webVariableCorrelation().stringLit() != null) {
      //Where will this be pulled from
      variable.putSquirrelledData(HTTP_SOURCE, ctx.webVariableCorrelation().stringLit().getText());
      //Obviously is httpAccess is HEADER then it must be a valid header name.
      //If httpAccess is PATH then it must exist in the PATH on the Method
      //And if QUERY then it will have to be one of the query parameters
      //These things are checked elsewhere - here we are just gathering info.
    }
  }
}
