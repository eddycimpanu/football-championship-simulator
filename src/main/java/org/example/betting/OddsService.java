package org.example.betting;

import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gives the odds of a round. They are calculated once, when the round is the next one to play
// (the players' fitness, bans and injuries are known then), saved, and never changed afterwards.
public class OddsService {

    private final OddsCalculator calculator;
    private final OddsStore store;

    public OddsService(OddsCalculator calculator, OddsStore store) {
        this.calculator = calculator;
        this.store = store;
    }

    // The saved odds of the round; for the next round the missing ones are calculated first
    public Map<Integer, MatchOdds> oddsFor(Season season, Round round) {
        List<Integer> ids = round.getFixtures().stream().map(Fixture::getId).toList();
        Map<Integer, MatchOdds> odds = new HashMap<>(store.findByMatchIds(ids));

        if (isNextRound(season, round)) {
            List<Fixture> missing = round.getFixtures().stream()
                    .filter(fixture -> !fixture.isPlayed() && !odds.containsKey(fixture.getId()))
                    .toList();
            // The ten matches are independent, so they are simulated at the same time on all processor cores
            List<MatchOdds> calculated = missing.parallelStream()
                    .map(fixture -> calculator.calculate(fixture.getId(), fixture.getHomeTeam(),
                            fixture.getAwayTeam(), fixture.getReferee(), seedFor(season, fixture)))
                    .toList();
            if (!calculated.isEmpty()) {
                store.saveAll(calculated);
                calculated.forEach(o -> odds.put(o.matchId(), o));
            }
        }
        return odds;
    }

    public boolean isNextRound(Season season, Round round) {
        return !season.isFinished() && season.getCurrentRoundNumber() == round.getNumber();
    }

    // Reproducible: the same season and match always give the same odds
    private static long seedFor(Season season, Fixture fixture) {
        return season.getSeed() * 31 + fixture.getId();
    }
}
