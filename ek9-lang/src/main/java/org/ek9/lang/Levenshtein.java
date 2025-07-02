package org.ek9.lang;

import java.util.Arrays;

/**
 * As per the name the mechanism of fuzzy matching.
 */
public class Levenshtein {

  private int cost(char a, char b) {
    //Slight tweek on standard cost if the change is just a case change.
    if (a == b) {
      return 0;
    }
    if (java.lang.Character.toLowerCase(a) == java.lang.Character.toLowerCase(b)) {
      return 1;
    }
    return 2;
  }

  int min(int... numbers) {
    return Arrays.stream(numbers).min().orElse(java.lang.Integer.MAX_VALUE);
  }

  public int costOfMatch(java.lang.String entered, java.lang.String toBeCheckedAgainst) {
    //If there are no common characters the cost is MAX!
    int lenAgainst = toBeCheckedAgainst.length();
    int numCommonCharacters = 0;

    int[][] dp = new int[entered.length() + 1][toBeCheckedAgainst.length() + 1];

    for (int i = 0; i <= entered.length(); i++) {
      for (int j = 0; j <= toBeCheckedAgainst.length(); j++) {
        if (i == 0) {
          dp[i][j] = j;
        } else if (j == 0) {
          dp[i][j] = i;
        } else {
          //Slight tweek on standard cost if the change is just a case change.
          //Use 2 as the extra cost as we use 1 now just for a change in case.
          int theCost = cost(entered.charAt(i - 1), toBeCheckedAgainst.charAt(j - 1));
          if (theCost < 2) {
            numCommonCharacters++;
          }
          dp[i][j] = min(dp[i - 1][j - 1] + theCost,
              dp[i - 1][j] + 2,
              dp[i][j - 1] + 2);
        }
      }
    }

    if (numCommonCharacters > lenAgainst / 2) {
      return dp[entered.length()][toBeCheckedAgainst.length()];
    }
    return java.lang.Integer.MAX_VALUE;
  }
}
