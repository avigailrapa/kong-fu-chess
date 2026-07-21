package src.server;

public final class EloCalculator {

    private static final int K = 32;

    private EloCalculator() {
    }

    public static int updatedRating(int rating, int opponentRating, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - rating) / 400.0));
        return (int) Math.round(rating + K * (score - expected));
    }
}
