package org.example.league;

import org.example.TestData;
import org.example.engine.random.RandomProvider;
import org.example.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureGeneratorTest {

    private List<Team> teams;
    private List<Round> rounds;

    @BeforeEach
    void generateSchedule() {
        teams = TestData.league(20, 75);
        rounds = new FixtureGenerator().generate(teams, new RandomProvider(2026));
    }

    @Test
    void twentyTeamsPlayThirtyEightRoundsOfTenMatches() {
        assertEquals(38, rounds.size());
        for (Round round : rounds) {
            assertEquals(10, round.getFixtures().size());
        }
    }

    @Test
    void everyTeamPlaysExactlyOnceInEachRound() {
        for (Round round : rounds) {
            Set<Team> playing = new HashSet<>();
            for (Fixture fixture : round.getFixtures()) {
                assertTrue(playing.add(fixture.getHomeTeam()), round + ": a team plays twice");
                assertTrue(playing.add(fixture.getAwayTeam()), round + ": a team plays twice");
            }
            assertEquals(20, playing.size());
        }
    }

    @Test
    void everyTeamMeetsEveryOtherTeamOnceAtHomeAndOnceAway() {
        Set<String> pairs = new HashSet<>();
        for (Round round : rounds) {
            for (Fixture fixture : round.getFixtures()) {
                assertTrue(pairs.add(fixture.getHomeTeam().getName() + " vs " + fixture.getAwayTeam().getName()),
                        "Played twice at the same ground: " + fixture);
            }
        }
        assertEquals(20 * 19, pairs.size());
    }

    @Test
    void everyTeamPlaysNineteenHomeMatches() {
        Map<Team, Integer> homeMatches = new HashMap<>();
        rounds.forEach(round -> round.getFixtures()
                .forEach(fixture -> homeMatches.merge(fixture.getHomeTeam(), 1, Integer::sum)));
        teams.forEach(team -> assertEquals(19, homeMatches.get(team), team.getName()));
    }

    @Test
    void noTeamPlaysMoreThanThreeHomeOrAwayMatchesInARow() {
        for (Team team : teams) {
            int run = 0;
            Boolean lastAtHome = null;
            for (Round round : rounds) {
                boolean atHome = round.getFixtures().stream()
                        .filter(fixture -> fixture.involves(team))
                        .findFirst().orElseThrow()
                        .getHomeTeam() == team;
                run = Boolean.valueOf(atHome).equals(lastAtHome) ? run + 1 : 1;
                lastAtHome = atHome;
                assertTrue(run <= 3, team.getName() + " plays " + run + " in a row " + (atHome ? "at home" : "away"));
            }
        }
    }

    @Test
    void theSameSeedGivesTheSameSchedule() {
        List<Round> again = new FixtureGenerator().generate(teams, new RandomProvider(2026));
        for (int i = 0; i < rounds.size(); i++) {
            assertEquals(rounds.get(i).getFixtures().toString(), again.get(i).getFixtures().toString());
        }
    }
}
