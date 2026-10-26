package org.example.betting.agents;

import org.example.betting.MatchOdds;
import org.example.betting.Pick;
import org.example.betting.Selection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategiesTest {

    private static final BigDecimal BALANCE = new BigDecimal("1000.00");
    private static final TeamForm AVERAGE = new TeamForm(10, 1.4, 1.4, 1.4);

    // A match where the home team wins with this chance; draw 15%, the rest away; goals markets 50/50
    private static MatchView match(int id, double homeChance, TeamForm home, TeamForm away) {
        Map<Selection, Double> chances = new EnumMap<>(Selection.class);
        chances.put(Selection.HOME, homeChance);
        chances.put(Selection.DRAW, 0.15);
        chances.put(Selection.AWAY, 0.85 - homeChance);
        chances.put(Selection.OVER_2_5, 0.5);
        chances.put(Selection.UNDER_2_5, 0.5);
        chances.put(Selection.BOTH_SCORE, 0.5);
        chances.put(Selection.NOT_BOTH_SCORE, 0.5);
        return new MatchView(id, "Home " + id, "Away " + id, home, away,
                MatchOdds.fromProbabilities(id, 1000, chances, 0.05));
    }

    private static List<MatchView> round(double... homeChances) {
        List<MatchView> matches = new ArrayList<>();
        for (int i = 0; i < homeChances.length; i++) {
            matches.add(match(i + 1, homeChances[i], AVERAGE, AVERAGE));
        }
        return matches;
    }

    // ----- Poisson and Kelly -----

    @Test
    void poissonResultChancesAddUpToOne() {
        PoissonModel model = new PoissonModel(1.7, 1.1);
        assertEquals(1.0, model.homeWin() + model.draw() + model.awayWin(), 1e-9);
        assertTrue(model.homeWin() > model.awayWin());
    }

    @Test
    void equalTeamsHaveEqualChances() {
        PoissonModel model = new PoissonModel(1.4, 1.4);
        assertEquals(model.homeWin(), model.awayWin(), 1e-9);
    }

    @Test
    void poissonMatchesTheFormula() {
        // P(2 goals | mean 1.5) = e^-1.5 x 1.5^2 / 2 = 0.2510
        assertEquals(0.2510, PoissonModel.poisson(1.5, 2), 1e-4);
    }

    @Test
    void kellyStakesNothingWithoutAnEdgeAndAQuarterWithOne() {
        assertEquals(0.0, AnalystStrategy.kelly(0.30, 3.0), 1e-9);   // 0.30 x 3.0 = 0.9: no edge
        assertEquals(0.25, AnalystStrategy.kelly(0.50, 3.0), 1e-9);  // (2 x 0.5 - 0.5) / 2
    }

    // ----- Strategies -----

    @Test
    void theFavouriteBacksTheLowestOddsWithFivePercent() {
        List<TicketPlan> plans = new FavouriteStrategy().decide(round(0.45, 0.80, 0.60), BALANCE);

        assertEquals(1, plans.size());
        assertEquals(List.of(new Pick(2, Selection.HOME)), plans.get(0).picks());
        assertEquals(new BigDecimal("50.00"), plans.get(0).stake());
    }

    @Test
    void theDreamerPlaysFourUpsetsFromFourMatches() {
        List<TicketPlan> plans = new DreamerStrategy().decide(round(0.6, 0.62, 0.58, 0.55, 0.65, 0.5), BALANCE);

        assertEquals(1, plans.size());
        List<Pick> picks = plans.get(0).picks();
        assertEquals(4, picks.size());
        Set<Integer> matches = new HashSet<>();
        picks.forEach(p -> matches.add(p.matchId()));
        assertEquals(4, matches.size());
        assertTrue(picks.stream().allMatch(p -> p.selection() == Selection.AWAY));   // the home sides are favourites
        assertEquals(new BigDecimal("20.00"), plans.get(0).stake());
    }

    @Test
    void theDreamerWaitsWhenThereAreNotEnoughUpsets() {
        assertTrue(new DreamerStrategy().decide(round(0.6, 0.62), BALANCE).isEmpty());
    }

    @Test
    void theSteadyHandPlaysADoubleOfTwentyCredits() {
        List<TicketPlan> plans = new SteadyStrategy().decide(round(0.45, 0.80, 0.70), BALANCE);

        assertEquals(1, plans.size());
        assertEquals(2, plans.get(0).picks().size());
        assertTrue(plans.get(0).picks().get(0).matchId() != plans.get(0).picks().get(1).matchId());
        assertEquals(new BigDecimal("20.00"), plans.get(0).stake());
        assertTrue(new SteadyStrategy().decide(round(0.45, 0.80), new BigDecimal("19.99")).isEmpty());
    }

    @Test
    void theStatisticianOnlyBetsOnGoals() {
        // Two attacking teams that concede a lot: its model expects goals, the odds say 50/50
        TeamForm attacking = new TeamForm(10, 1.5, 2.6, 2.2);
        List<TicketPlan> plans = new StatisticianStrategy().decide(List.of(match(1, 0.45, attacking, attacking)), BALANCE);

        assertTrue(!plans.isEmpty());
        plans.forEach(plan -> plan.picks().forEach(p -> assertTrue(
                p.selection().getMarket() != Selection.Market.RESULT, "Bet on " + p.selection())));
    }

    @Test
    void theAnalystWaitsForEnoughMatches() {
        TeamForm early = new TeamForm(3, 3.0, 3.0, 0.3);
        assertTrue(new AnalystStrategy().decide(List.of(match(1, 0.30, early, AVERAGE)), BALANCE).isEmpty());
    }

    @Test
    void theAnalystBacksATeamTheMarketUnderrates() {
        // The table says home is far better, the odds give it only 30%
        TeamForm strong = new TeamForm(15, 2.6, 2.5, 0.6);
        TeamForm weak = new TeamForm(15, 0.6, 0.7, 2.3);
        List<TicketPlan> plans = new AnalystStrategy().decide(List.of(match(1, 0.30, strong, weak)), BALANCE);

        assertEquals(1, plans.size());
        assertEquals(Selection.HOME, plans.get(0).picks().get(0).selection());
        assertTrue(plans.get(0).stake().compareTo(new BigDecimal("50.00")) <= 0);   // never more than 5%
    }

    @Test
    void stakesAreRoundedDownAndNeverBelowOneCredit() {
        assertEquals(new BigDecimal("33.33"), Stakes.share(new BigDecimal("666.66"), 0.05));
        assertEquals(null, Stakes.share(new BigDecimal("10.00"), 0.05));
    }
}
