package org.example.betting.agents;

import java.util.List;

// An AI bettor: a name, a short key (saved as its strategy) and the rules it bets by.
public record Agent(String key, String name, BettingStrategy strategy) {

    public static List<Agent> all() {
        return List.of(
                new Agent("FAVOURITE", "The Favourite", new FavouriteStrategy()),
                new Agent("DREAMER", "The Dreamer", new DreamerStrategy()),
                new Agent("ANALYST", "The Analyst", new AnalystStrategy()),
                new Agent("STATISTICIAN", "The Statistician", new StatisticianStrategy()),
                new Agent("STEADY", "The Steady Hand", new SteadyStrategy()));
    }
}
