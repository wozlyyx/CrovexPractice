package com.crovex.practice.util;

public final class EloCalculator {

    public static final int DEFAULT_K_FACTOR = 32;
    public static final int DEFAULT_MIN_ELO = 100;

    private EloCalculator() {}

    /**
     * Calculates the ELO change for the winner given the winner's and loser's current ratings.
     *
     * @param eloWinner Current rating of the winner
     * @param eloLoser  Current rating of the loser
     * @param kFactor   K-factor multiplier
     * @return ELO points to be awarded to the winner (and deducted from the loser)
     */
    public static int calculateEloChange(int eloWinner, int eloLoser, int kFactor) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (eloLoser - eloWinner) / 400.0));
        int change = (int) Math.round(kFactor * (1.0 - expectedScore));
        return Math.max(1, change); // Minimum 1 point gained on win
    }

    public static int calculateEloChange(int eloWinner, int eloLoser) {
        return calculateEloChange(eloWinner, eloLoser, DEFAULT_K_FACTOR);
    }

    /**
     * Computes the new loser ELO ensuring it does not drop below the minimum threshold.
     */
    public static int calculateNewLoserElo(int oldElo, int eloChange, int minElo) {
        return Math.max(minElo, oldElo - eloChange);
    }
}
