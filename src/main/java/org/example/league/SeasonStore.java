package org.example.league;

import org.example.model.Referee;
import org.example.model.Team;

import java.util.List;
import java.util.Optional;

// Where a season is saved. The league only knows this interface; the database code implements it.
public interface SeasonStore {

    // A new season with its whole schedule (all 380 matches, still to be played)
    void saveNewSeason(Season season);

    // One played match: score, events and statistics, all or nothing
    void saveMatch(Season season, Fixture fixture);

    // After a round: the season's progress and every player's fitness, bans and injuries
    void saveRoundProgress(Season season, List<Team> teams);

    // The most recent season, with its schedule, scores and the players' state; empty if none is saved
    Optional<Season> loadLatestSeason(List<Team> teams, List<Referee> referees);
}
