package server;

import org.junit.jupiter.api.Test;
import src.server.EloCalculator;

import static org.junit.jupiter.api.Assertions.*;

public class EloCalculatorTest {

    @Test
    public void testEqualRatingsWinnerGainsSixteen() {
        assertEquals(1216, EloCalculator.updatedRating(1200, 1200, 1.0));
    }

    @Test
    public void testEqualRatingsLoserLosesSixteen() {
        assertEquals(1184, EloCalculator.updatedRating(1200, 1200, 0.0));
    }

    @Test
    public void testEqualRatingsDrawIsUnchanged() {
        assertEquals(1200, EloCalculator.updatedRating(1200, 1200, 0.5));
    }

    @Test
    public void testUnderdogWinGainsMoreThanSixteen() {
        int updated = EloCalculator.updatedRating(1200, 1400, 1.0);

        assertTrue(updated - 1200 > 16, "expected the underdog's win to gain more than an even-match win");
    }

    @Test
    public void testFavoriteWinGainsLessThanSixteen() {
        int updated = EloCalculator.updatedRating(1400, 1200, 1.0);

        assertTrue(updated - 1400 < 16, "expected the favorite's win to gain less than an even-match win");
    }

    @Test
    public void testFavoriteLossLosesMoreThanSixteen() {
        int updated = EloCalculator.updatedRating(1400, 1200, 0.0);

        assertTrue(1400 - updated > 16, "expected the favorite's loss to lose more than an even-match loss");
    }
}
