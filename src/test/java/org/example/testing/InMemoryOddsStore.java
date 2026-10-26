package org.example.testing;

import org.example.betting.MatchOdds;
import org.example.betting.OddsStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The odds table (match_odds) kept in a map, for tests that need no database.
public class InMemoryOddsStore implements OddsStore {

    private final Map<Integer, MatchOdds> odds = new HashMap<>();
    private int saves;

    @Override
    public synchronized Map<Integer, MatchOdds> findByMatchIds(Collection<Integer> matchIds) {
        Map<Integer, MatchOdds> found = new HashMap<>();
        for (int id : matchIds) {
            if (odds.containsKey(id)) {
                found.put(id, odds.get(id));
            }
        }
        return found;
    }

    @Override
    public synchronized void saveAll(List<MatchOdds> list) {
        list.forEach(o -> odds.put(o.matchId(), o));
        saves++;
    }

    // How many times odds were saved: the odds of a round are calculated once, then read back
    public synchronized int getSaves() {
        return saves;
    }
}
