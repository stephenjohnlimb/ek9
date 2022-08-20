package org.ek9lang.core.metrics;

/**
 * Readability check metric.
 */
public class Ari {

  /**
   * Get readability score, based on values.
   */
  public String getScore(int numberOfLetters, int numberOfWords, int numberOfIndents,
                         int numberOfNewLines) {
    //Slight modification to the ARI routine here, because source code is not really prose or
    //normal sentences. We have to alter or try and workout what we mean by a sentence
    //and also slightly alter the word count.
    int numberOfSentences = numberOfIndents + numberOfNewLines / 2;
    double wordAdjustment = numberOfWords * 1.25;
    double score = 4.71 * (numberOfLetters / wordAdjustment)
        + 0.50 * (wordAdjustment / numberOfSentences) - 20.25;

    if (score < 2) {
      return "5-6 Kindergarten";
    }
    if (score < 3) {
      return "6-7 First/Second Grade";
    }
    if (score < 4) {
      return "7-9 Third Grade";
    }
    if (score < 5) {
      return "9-10 Fourth Grade";
    }
    if (score < 6) {
      return "10-11 Fifth Grade";
    }
    if (score < 7) {
      return "11-12 Sixth Grade";
    }
    if (score < 8) {
      return "12-13 Seventh Grade";
    }
    if (score < 9) {
      return "13-14 Eighth Grade";
    }
    if (score < 10) {
      return "14-15 Ninth Grade";
    }
    if (score < 11) {
      return "15-16 Tenth Grade";
    }
    if (score < 12) {
      return "16-17 Eleventh Grade";
    }
    if (score < 13) {
      return "17-18 Twelfth Grade";
    }
    if (score < 14) {
      return "18-24 College student";
    }

    return "24+ Graduate";
  }
}
