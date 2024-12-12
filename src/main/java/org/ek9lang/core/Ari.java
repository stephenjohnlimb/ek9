package org.ek9lang.core;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Readability check metric.
 */
public class Ari {

  private static final Map<Integer, String> scoreToName = getScoreMap();

  // A bit of a convoluted way because of limited values on Map.of.
  // Basically calculate a score round int and lookup value.
  private static Map<Integer, String> getScoreMap() {

    final var map1 = Map.of(
        1, "5-6 Kindergarten",
        2, "6-7 First/Second Grade",
        3, "7-9 Third Grade",
        4, "9-10 Fourth Grade",
        5, "10-11 Fifth Grade");
    final var map2 = Map.of(
        6, "11-12 Sixth Grade",
        7, "12-13 Seventh Grade",
        8, "14-15 Ninth Grade",
        9, "15-16 Tenth Grade",
        10, "16-17 Eleventh Grade",
        11, "17-18 Twelfth Grade",
        12, "18-24 College student"
    );

    return Stream.of(map1, map2)
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Get readability score, based on values.
   */
  public String getScore(final int numberOfLetters,
                         final int numberOfWords,
                         final int numberOfIndents,
                         final int numberOfNewLines) {

    //Slight modification to the ARI routine here, because source code is not really prose or
    //normal sentences. We have to alter or try and workout what we mean by a sentence
    //and also slightly alter the word count.
    final var numberOfSentences = numberOfIndents + numberOfNewLines / 2;
    final var wordAdjustment = numberOfWords * 1.25;
    final var score = 4.71 * (numberOfLetters / wordAdjustment) + 0.50 * (wordAdjustment / numberOfSentences) - 20.25;

    return scoreToName.getOrDefault((int) Math.round(score), "24+ Graduate");
  }

}
