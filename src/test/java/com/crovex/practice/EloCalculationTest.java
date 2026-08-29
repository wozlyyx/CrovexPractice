package com.crovex.practice;

import com.crovex.practice.util.EloCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EloCalculationTest {

    @Test
    @DisplayName("Equal ELO ratings should produce exactly half K-factor points on win")
    void testEqualRatingEloGain() {
        int eloChange = EloCalculator.calculateEloChange(1000, 1000, 32);
        assertThat(eloChange).isEqualTo(16);
    }

    @Test
    @DisplayName("Underdog beating higher rated player should gain substantial ELO")
    void testUnderdogVictory() {
        // Winner has 1000, Loser has 1400 (400 points difference)
        int eloChange = EloCalculator.calculateEloChange(1000, 1400, 32);
        // Expected score ~0.09 -> gain ~29
        assertThat(eloChange).isGreaterThanOrEqualTo(28);
    }

    @Test
    @DisplayName("High rated player beating underdog should gain minimal ELO but at least 1 point")
    void testHeavyFavoriteVictory() {
        // Winner has 2500, Loser has 500
        int eloChange = EloCalculator.calculateEloChange(2500, 500, 32);
        assertThat(eloChange).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Loser ELO should never fall below minimum threshold")
    void testLoserEloMinimumClamp() {
        int minElo = 100;
        int oldElo = 110;
        int eloChange = 25;

        int newElo = EloCalculator.calculateNewLoserElo(oldElo, eloChange, minElo);
        assertThat(newElo).isEqualTo(100);
    }

    @Test
    @DisplayName("Custom K-factor should scale ELO change proportionately")
    void testCustomKFactorScaling() {
        int changeK32 = EloCalculator.calculateEloChange(1000, 1000, 32);
        int changeK64 = EloCalculator.calculateEloChange(1000, 1000, 64);
        int changeK16 = EloCalculator.calculateEloChange(1000, 1000, 16);

        assertThat(changeK32).isEqualTo(16);
        assertThat(changeK64).isEqualTo(32);
        assertThat(changeK16).isEqualTo(8);
    }
}
