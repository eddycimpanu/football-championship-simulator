package org.example.betting;

// Everything a bettor can choose, grouped in three markets.
public enum Selection {
    HOME(Market.RESULT, "1"),
    DRAW(Market.RESULT, "X"),
    AWAY(Market.RESULT, "2"),
    OVER_2_5(Market.GOALS, "Over 2.5"),
    UNDER_2_5(Market.GOALS, "Under 2.5"),
    BOTH_SCORE(Market.BOTH_TEAMS_SCORE, "GG"),
    NOT_BOTH_SCORE(Market.BOTH_TEAMS_SCORE, "NG");

    public enum Market { RESULT, GOALS, BOTH_TEAMS_SCORE }

    private final Market market;
    private final String label;

    Selection(Market market, String label) {
        this.market = market;
        this.label = label;
    }

    // Did this selection win, given the final score?
    public boolean wins(int homeGoals, int awayGoals) {
        return switch (this) {
            case HOME -> homeGoals > awayGoals;
            case DRAW -> homeGoals == awayGoals;
            case AWAY -> homeGoals < awayGoals;
            case OVER_2_5 -> homeGoals + awayGoals > 2;
            case UNDER_2_5 -> homeGoals + awayGoals <= 2;
            case BOTH_SCORE -> homeGoals > 0 && awayGoals > 0;
            case NOT_BOTH_SCORE -> homeGoals == 0 || awayGoals == 0;
        };
    }

    public Market getMarket() { return market; }
    public String getLabel() { return label; }
}
