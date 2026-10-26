package org.example.betting.agents;

// Goals in a football match follow a Poisson distribution quite well: if a team scores 1.6 goals
// on average, P(it scores k goals) = e^-1.6 x 1.6^k / k!. With the two teams' averages
// we get the chance of every score, and from them the chance of any market.
public final class PoissonModel {

    private static final int MAX_GOALS = 10;

    private final double[][] scores = new double[MAX_GOALS + 1][MAX_GOALS + 1];

    public PoissonModel(double homeGoals, double awayGoals) {
        double home = Math.max(0.15, homeGoals);
        double away = Math.max(0.15, awayGoals);
        for (int h = 0; h <= MAX_GOALS; h++) {
            for (int a = 0; a <= MAX_GOALS; a++) {
                scores[h][a] = poisson(home, h) * poisson(away, a);
            }
        }
    }

    static double poisson(double mean, int k) {
        double p = Math.exp(-mean);
        for (int i = 1; i <= k; i++) {
            p *= mean / i;
        }
        return p;
    }

    public double homeWin() {
        return sum((h, a) -> h > a);
    }

    public double draw() {
        return sum((h, a) -> h == a);
    }

    public double awayWin() {
        return sum((h, a) -> h < a);
    }

    public double over25() {
        return sum((h, a) -> h + a > 2);
    }

    public double bothScore() {
        return sum((h, a) -> h > 0 && a > 0);
    }

    @FunctionalInterface
    private interface ScoreTest {
        boolean test(int home, int away);
    }

    private double sum(ScoreTest test) {
        double total = 0;
        double all = 0;
        for (int h = 0; h <= MAX_GOALS; h++) {
            for (int a = 0; a <= MAX_GOALS; a++) {
                all += scores[h][a];
                if (test.test(h, a)) {
                    total += scores[h][a];
                }
            }
        }
        return total / all;   // the scores above 10 goals are left out, so the rest is scaled back to 100%
    }
}
