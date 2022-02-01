package org.ek9lang.compiler.symbol.support.search;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An initial stuff for match results.
 * Typically, this will be used for symbol searching if we
 * cannot resolve a symbol of some sort.
 * This would normally be some type of general or fuzzy search.
 */
public class MatchResults
{
	private final int maxResults;
	private final PriorityQueue<MatchResult> actualQueue;

	public MatchResults(int maxResults)
	{
		this.maxResults = maxResults;
		actualQueue = new PriorityQueue<>(maxResults, MatchResult.getComparator());
	}

	public MatchResults add(MatchResult result)
	{
		actualQueue.add(result);
		if(actualQueue.size() > maxResults)
			actualQueue.remove();
		return this;
	}

	/**
	 * Provide a list of the results in priority order.
	 *
	 * @return The list of match results in priority order.
	 */
	public List<MatchResult> list()
	{
		List<MatchResult> rtn = new ArrayList<>(size());
		var copyQueue = new PriorityQueue<>(actualQueue);
		while(!copyQueue.isEmpty())
			rtn.add(copyQueue.poll());
		Collections.reverse(rtn);
		return rtn;
	}

	/**
	 * An iterator of the results in order of best match.
	 *
	 * @return The iterator.
	 */
	public Iterator<MatchResult> iterator()
	{
		return list().iterator();
	}

	public int size()
	{
		return actualQueue.size();
	}

	@Override
	public String toString()
	{
		return list().stream().map(MatchResult::toString).collect(Collectors.joining(", "));
	}
}
