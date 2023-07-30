package org.ek9lang.compiler.symbols.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * An initial stuff for match results.
 * Typically, this will be used for symbol searching if we
 * cannot resolve a symbol of some sort.
 * This would normally be some type of general or fuzzy search.
 */
public class MatchResults {
  private final int maxResults;
  private final PriorityQueue<MatchResult> actualQueue;

  public MatchResults(int maxResults) {
    this.maxResults = maxResults;
    actualQueue = new PriorityQueue<>(maxResults, MatchResult.getComparator());
  }

  /**
   * Add a new match result to the set of match results. Subject to the max allowed.
   */
  public MatchResults add(MatchResult result) {
    actualQueue.add(result);
    if (actualQueue.size() > maxResults) {
      actualQueue.remove();
    }
    return this;
  }

  /**
   * Provide a list of the results in priority order.
   *
   * @return The list of match results in priority order.
   */
  public List<MatchResult> list() {
    List<MatchResult> rtn = new ArrayList<>(size());
    var copyQueue = new PriorityQueue<>(actualQueue);
    while (!copyQueue.isEmpty()) {
      rtn.add(copyQueue.poll());
    }
    Collections.reverse(rtn);
    return rtn;
  }

  /**
   * An iterator of the results in order of best match.
   *
   * @return The iterator.
   */
  public Iterator<MatchResult> iterator() {
    return list().iterator();
  }

  public int size() {
    return actualQueue.size();
  }

  @Override
  public String toString() {
    return list().stream().map(result -> "'" + result + "'").collect(Collectors.joining(", "));
  }
}
