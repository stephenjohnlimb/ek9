package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR instructions for list literal expressions.
 * <p>
 * Handles the syntax: {@code ["element1", "element2", ...]}
 * </p>
 * <p>
 * <b>IR Generation Strategy:</b><br>
 * List literals are desugared into:
 * </p>
 * <ol>
 *   <li>List&lt;T&gt; constructor call (default constructor)</li>
 *   <li>Memory management (RETAIN/SCOPE_REGISTER) for list object</li>
 *   <li>For each element:
 *     <ul>
 *       <li>Evaluate element expression</li>
 *       <li>Memory management for element</li>
 *       <li>Call {@code _addAss(element)} mutation operator</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * <b>Example:</b><br>
 * {@code ["one", "two"]} generates:
 * </p>
 * <pre>
 * _temp1 = CALL List&lt;String&gt;.&lt;init&gt;()
 * RETAIN _temp1
 * SCOPE_REGISTER _temp1
 * _temp2 = LOAD_LITERAL "one"
 * RETAIN _temp2
 * SCOPE_REGISTER _temp2
 * CALL _temp1._addAss(_temp2)
 * _temp3 = LOAD_LITERAL "two"
 * RETAIN _temp3
 * SCOPE_REGISTER _temp3
 * CALL _temp1._addAss(_temp3)
 * </pre>
 *
 * <p>
 * <b>Memory Management:</b><br>
 * - List object retained and registered to current scope<br>
 * - Each element retained and registered before _addAss call<br>
 * - No LOAD/STORE between elements (direct operation on constructor result)
 * </p>
 */
public final class ListLiteralGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final GeneratorSet generators;

  /**
   * Constructor for list literal IR generator.
   *
   * @param stackContext IRGenerationContext for scope and symbol access
   * @param generators   GeneratorSet for accessing other generators (expression processor, etc.)
   */
  ListLiteralGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();
    final var ctx = details.ctx();
    final var listContext = ctx.list();
    final var resultVariable = details.variableDetails().resultVariable();
    final var currentScopeId = stackContext.currentScopeId();

    // Get resolved list type from symbol table (includes decorated parameterized name)
    // List literals are represented as CallSymbols in phase 1, with the resolved
    // parameterized type (e.g., List<String>) set as the CallSymbol's TYPE in phase 3
    final var symbolFromContext = getRecordedSymbolOrException(listContext);
    final ISymbol listSymbol;

    if (symbolFromContext instanceof CallSymbol callSymbol) {
      // Extract the aggregate type from the CallSymbol's type field
      listSymbol = callSymbol.getType().orElseThrow(() ->
          new CompilerException("List literal has no resolved type"));
    } else {
      // Shouldn't happen for list literals, but handle gracefully
      listSymbol = symbolFromContext;
    }

    // Get fully qualified type name with module prefix (e.g., org.ek9.lang::_List_HASH)
    final var listTypeName = listSymbol.getFullyQualifiedName();

    // Debug info from list literal start token
    final var listDebugInfo = stackContext.createDebugInfo(listContext.getStart());

    // Step 1: Generate List<T> constructor call
    final var listConstructor = findDefaultConstructor(listSymbol);
    final var callMetaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    final var constructorMetaData = callMetaDataExtractor.apply(listConstructor);

    final var constructorCallDetails = new CallDetails(
        listTypeName,               // targetType (fully qualified: org.ek9.lang::_List_HASH)
        listTypeName,               // targetTypeName (fully qualified)
        IRConstants.INIT_METHOD,    // methodName: "<init>"
        List.of(),                  // parameterTypes (no params for default constructor)
        listTypeName,               // returnType (fully qualified)
        List.of(),                  // argumentVariables (no args)
        constructorMetaData,        // metaData (purity, complexity, effects)
        false                       // isMethod (false for constructors)
    );

    instructions.add(CallInstr.constructor(resultVariable, listDebugInfo, constructorCallDetails));

    // Step 2: Memory management for list object (must happen immediately after construction)
    instructions.add(MemoryInstr.retain(resultVariable, listDebugInfo));
    instructions.add(ScopeInstr.register(resultVariable, currentScopeId, listDebugInfo));

    // Step 3: Process each element expression
    final var expressions = listContext.expression();
    for (final EK9Parser.ExpressionContext exprCtx : expressions) {
      // Generate temporary variable for element evaluation
      final var elementDetails = createTempVariableFromContext(exprCtx);
      final var elementTemp = elementDetails.resultVariable();
      final var elementDebugInfo = elementDetails.debugInfo();

      // Recursively evaluate element expression (handles literals, calls, etc.)
      // Use exprGenerator from generators struct for recursive processing
      final var elementProcessing = new ExprProcessingDetails(exprCtx, elementDetails);
      instructions.addAll(generators.exprGenerator.apply(elementProcessing));

      // Get element type from resolved symbol
      final var elementSymbol = getRecordedSymbolOrException(exprCtx);
      final var elementType = elementSymbol.getType().orElseThrow(() ->
          new CompilerException("Element expression has no type: " + elementSymbol.getName()));

      // Memory management for element
      instructions.add(MemoryInstr.retain(elementTemp, elementDebugInfo));
      instructions.add(ScopeInstr.register(elementTemp, currentScopeId, elementDebugInfo));

      // Step 4: Generate _addAss call: list._addAss(element)
      // Use call resolution builder for proper method resolution with cost-based matching
      final var voidType = stackContext.getParsedModule().getEk9Types().ek9Void();
      final var callContext = CallContext.forBinaryOperation(
          listSymbol,      // Target type (List<String>)
          elementType,     // Argument type (String)
          voidType,        // Return type (Void for assignment operators)
          "_addAss",       // Method name
          resultVariable,  // Target variable (list instance)
          elementTemp,     // Argument variable (element)
          currentScopeId  // Current scope ID
      );

      // Use call details builder for method resolution and CallDetails creation
      final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

      // Add any promotion instructions generated by the builder
      instructions.addAll(callDetailsResult.allInstructions());

      // Add the _addAss operator call (returns Void, so no result variable)
      instructions.add(CallInstr.operator(null, elementDebugInfo, callDetailsResult.callDetails()));
    }

    // Result is the populated list in resultVariable
    return instructions;
  }

  /**
   * Find default (no-argument) constructor for aggregate type.
   *
   * @param typeSymbol The list type symbol (must be IAggregateSymbol)
   * @return The default constructor MethodSymbol
   * @throws CompilerException if not an aggregate or no default constructor found
   */
  private MethodSymbol findDefaultConstructor(final ISymbol typeSymbol) {
    if (!(typeSymbol instanceof IAggregateSymbol aggregate)) {
      throw new CompilerException("Expected aggregate type for list literal, got: "
          + typeSymbol.getClass().getSimpleName());
    }

    // Look for constructor with no parameters
    return aggregate.getConstructors().stream()
        .filter(ctor -> ctor.getCallParameters().isEmpty())
        .findFirst()
        .orElseThrow(() -> new CompilerException(
            "No default constructor found for list type: " + typeSymbol.getName()));
  }
}
