package org.example.league;

import org.example.model.Referee;
import org.example.model.Team;

import java.util.List;

// Where a new season gets its clubs, players and referees. LeagueLoader reads them from the database;
// the tests give made-up ones instead.
public interface LeagueSource {

    // Fresh objects every time: a new season starts with rested players and no cards
    List<Team> loadTeams();

    List<Referee> loadReferees();
}
