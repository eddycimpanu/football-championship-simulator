package org.example.model;

import org.example.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachTest {

    @Test
    void picksElevenPlayersWithExactlyOneGoalkeeper() {
        Team team = TestData.team("Lions", 80);
        Lineup lineup = team.getCoach().selectStartingEleven(team);

        assertEquals(11, lineup.startingEleven().size());
        assertEquals(1, lineup.startingEleven().stream().filter(p -> p.getFieldRole() == FieldRole.GOALKEEPER).count());
    }

    @Test
    void theBenchAlwaysHasAReserveGoalkeeper() {
        Team team = TestData.team("Lions", 80);
        Lineup lineup = team.getCoach().selectStartingEleven(team);

        assertTrue(lineup.bench().stream().anyMatch(p -> p.getFieldRole() == FieldRole.GOALKEEPER));
        assertTrue(lineup.bench().stream().noneMatch(lineup.startingEleven()::contains));
    }

    @Test
    void suspendedAndInjuredPlayersDoNotPlay() {
        Team team = TestData.team("Lions", 80);
        Player suspended = team.getSquad().get(3);
        Player injured = team.getSquad().get(17);
        suspended.suspend(1);
        injured.injure(1);

        Lineup lineup = team.getCoach().selectStartingEleven(team);

        assertFalse(lineup.startingEleven().contains(suspended));
        assertFalse(lineup.startingEleven().contains(injured));
        assertFalse(lineup.bench().contains(suspended));
    }

    @Test
    void aRestedPlayerStartsBeforeAnEquallyGoodTiredOne() {
        Team team = TestData.team("Lions", 80);
        // The two goalkeepers are equally good: the tired one goes to the bench
        Player firstKeeper = team.getSquad().get(0);
        Player secondKeeper = team.getSquad().get(1);
        firstKeeper.reduceCondition(40);

        Lineup lineup = team.getCoach().selectStartingEleven(team);

        assertTrue(lineup.startingEleven().contains(secondKeeper));
        assertTrue(lineup.bench().contains(firstKeeper));
    }

    @Test
    void theBetterPlayerWinsThePlace() {
        Team team = TestData.team("Lions", 75);
        Player star = TestData.player(DetailedPosition.ST, 92);
        team.addPlayer(star);

        assertTrue(team.getCoach().selectStartingEleven(team).startingEleven().contains(star));
    }
}
