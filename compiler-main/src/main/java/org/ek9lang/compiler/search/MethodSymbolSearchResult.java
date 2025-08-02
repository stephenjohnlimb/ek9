package org.ek9lang.compiler.search;

import static org.ek9lang.compiler.support.AggregateManipulator.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Models a set of result for a search for methods.
 * This uses a percentage match mechanism to find the most appropriate match.
 */
public class MethodSymbolSearchResult {

  /**
   * The actual results of the method search.
   * It's a bit tricky matching methods in aggregate hierarchies and with method overloading
   * but also with class compatibility or parameters and possible coercions.
   */
  private final List<PercentageMethodSymbolMatch> results = new ArrayList<>();
  private boolean accessModifierIncompatible = false;
  private boolean methodNotMarkedWithOverride = false;

  public MethodSymbolSearchResult() {

  }

  /**
   * New results that contain the result passed in.
   */
  public MethodSymbolSearchResult(final MethodSymbolSearchResult startWithResults) {

    add(startWithResults.results);
    setAccessModifierIncompatible(startWithResults.accessModifierIncompatible);
    setMethodNotMarkedWithOverride(startWithResults.methodNotMarkedWithOverride);

  }

  public boolean isAccessModifierIncompatible() {

    return accessModifierIncompatible;
  }

  public void setAccessModifierIncompatible(final boolean accessModifierIncompatible) {

    //once true always true
    this.accessModifierIncompatible |= accessModifierIncompatible;
  }

  public boolean isMethodNotMarkedWithOverride() {

    return methodNotMarkedWithOverride;
  }

  public void setMethodNotMarkedWithOverride(final boolean methodNotMarkedWithOverride) {

    //once true always true
    this.methodNotMarkedWithOverride |= methodNotMarkedWithOverride;
  }

  /**
   * Typically used with traits and class/trait combinations so that multiple methods of the
   * same name and parameters. These are merged together this may then mean that the results
   * have two results of the same percentage match and this indicates ambiguity.
   * So for example if you had ClassA implementing interfaceZ (lets say default method methodBoo
   * in interfaceZ) all is good.
   * Now say we have interfaceP (also with method methodBoo) and now add ClassK that extends
   * ClassA and implements interfaceP. We now have a situation where 'methodBoo' has two default
   * implementations (diamond-ish); and therefore we must define one in ClassK.
   * We need to know that there are two methods that would match via different routes.
   * it's not the case where one has overridden the other
   * that's what default methods in traits/interfaces has introduced.
   *
   * @param withResults The results to merge in
   * @return A new set of results - does not alter either of the two sets being merged.
   */
  public MethodSymbolSearchResult mergePeerToNewResult(final MethodSymbolSearchResult withResults) {

    final var rtn = new MethodSymbolSearchResult();

    rtn.setAccessModifierIncompatible(withResults.isAccessModifierIncompatible());
    rtn.setMethodNotMarkedWithOverride(withResults.isMethodNotMarkedWithOverride());
    rtn.add(this.results);
    rtn.add(withResults.results);

    return rtn;
  }

  /**
   * So now let's imagine we have a set of results from various interfaces and classes (maybe
   * using method above to accumulate the methods)
   * But now you are dealing with a Class or a Trait - now we need to know that for all the
   * methods that could be called we only have one
   * single one that would be resolved. If this is not the case then it is an indicator that
   * the method should be implemented in the class
   * so as tobe explicit which implementation is to be used.
   * So we will be checking for matching method name and parameters and compatible covariance
   * return types.
   *
   * @param withResults The set of methods that can be used to override one or more results.
   * @return A new set of results - does not alter either of the two sets being merged.
   */
  public MethodSymbolSearchResult overrideToNewResult(MethodSymbolSearchResult withResults) {

    //Always include new results - but we may not always include the set we have already gathered
    final var buildResult = new MethodSymbolSearchResult(withResults);

    //But if with result is empty then we can just include all our current set or results
    if (withResults.isEmpty()) {
      buildResult.add(results);
    }

    for (PercentageMethodSymbolMatch newResult : withResults.results) {
      for (PercentageMethodSymbolMatch result : results) {
        final var methodSymbol = newResult.getMethodSymbol();
        //You need to get this the right way around in terms of checking params
        //and compatible return types.
        if (!methodSymbol.isSignatureMatchTo(result.getMethodSymbol())) {
          //So not a signature match then just add in result
          buildResult.add(result);
        } else {
          if (!methodSymbol.getAccessModifier()
              .equals(result.getMethodSymbol().getAccessModifier())) {
            //OK so signature is a match but the access modifier is different
            //so this is ambiguous but because the access
            //is either stricter or more lax.
            buildResult.add(result);
            buildResult.setAccessModifierIncompatible(true);
          } else if (!methodSymbol.getAccessModifier().equals(PRIVATE)
              && !methodSymbol.isOverride()) {
            //So we are here and the method signatures are the same, so it's an
            //override but has not been marked as such
            buildResult.setMethodNotMarkedWithOverride(true);
          }
        }
      }
    }

    return buildResult;
  }

  /**
   * If there is a single best match for the search; then return it.
   */
  public Optional<MethodSymbol> getSingleBestMatchSymbol() {

    if (isSingleBestMatchPresent()) {
      return Optional.of(results.getFirst().getMethodSymbol());
    }

    return Optional.empty();
  }

  /**
   * Is there a single best match available.
   * Could be there are no results, could be a single (very good result - perfect match),
   * Could be a match but not perfect but acceptable.
   * But could also be several results. Of equal percentage match; in which case we have an ambiguity.
   *
   * @return true if there is a single best match.
   */
  public boolean isSingleBestMatchPresent() {

    if (!isEmpty()) {
      if (results.size() > 1) {
        //need to check first and second to see if percentage match is same.
        final var firstPercentage = results.get(0).getPercentageMatch();
        final var secondPercentage = results.get(1).getPercentageMatch();

        //are these two within a tolerance of each other - if so ambiguous.
        return Math.abs(firstPercentage - secondPercentage) >= 0.001;
        //No one is better than the other.
      }
      //yes we have one best match.
      return true;
    }

    //no there is no single best match
    return false;
  }

  /**
   * Found more than one matching method.
   */
  public boolean isAmbiguous() {

    return !isEmpty() && !isSingleBestMatchPresent();
  }

  /**
   * Get the parameters that are considered ambiguous and the line they are on.
   */
  public String getAmbiguousMethodParameters() {

    final var buffer = new StringBuilder();

    if (results.size() > 1) {
      final var symbol1 = results.getFirst().getMethodSymbol();
      buffer.append(symbol1.toString()).append(" line ").append(symbol1.getSourceToken().getLine());

      //need to check first and second to see if percentage match is same.
      final var firstPercentage = results.getFirst().getPercentageMatch();
      for (int i = 1; i < results.size(); i++) {
        if (Math.abs(firstPercentage - results.get(i).getPercentageMatch()) < 0.001) {
          final var symbol2 = results.get(i).getMethodSymbol();
          buffer.append(" , ");
          buffer.append(symbol2.toString()).append(" line ").append(symbol2.getSourceToken().getLine());
        } else {
          break;
        }
      }
    }

    return buffer.toString();
  }

  /**
   * Converts the results to a list of match results that are ordered by cost and can be displayed.
   */
  public MatchResults toMatchResults() {

    final var rtn = new MatchResults(results.size());
    results.forEach(
        result -> rtn.add(new MatchResult((int) (result.getPercentageMatch() * 10), result.getMethodSymbol())));

    return rtn;
  }

  @Override
  public String toString() {

    final var buffer = new StringBuilder();
    buffer.append("[");
    boolean first = true;
    for (PercentageMethodSymbolMatch result : results) {
      if (!first) {
        buffer.append(", ");
      }
      buffer.append(result.toString());
      first = false;
    }
    buffer.append("]");
    return buffer.toString();
  }


  public boolean isEmpty() {

    return results.isEmpty();
  }

  /**
   * Add more results and sort the list held.
   */
  public MethodSymbolSearchResult add(final List<PercentageMethodSymbolMatch> moreResults) {

    results.addAll(moreResults);
    sortResults();

    return this;
  }

  /**
   * Add a result and sort the list held.
   */
  public MethodSymbolSearchResult add(final PercentageMethodSymbolMatch percentageMethodSymbolMatch) {

    results.add(percentageMethodSymbolMatch);
    sortResults();

    return this;
  }

  private void sortResults() {

    results.sort((o1, o2) -> Double.compare(o2.getPercentageMatch(), o1.getPercentageMatch()));
  }
}
