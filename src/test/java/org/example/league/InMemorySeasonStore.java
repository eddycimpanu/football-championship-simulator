package org.example.league;

import org.example.engine.Score;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// A fake database for the tests: SeasonService only knows the SeasonStore interface,
// so the tests can give it this class instead of PostgreSQL.
// Public because the web tests use it too. Like the database, every new season gets the next id,
// and match ids keep counting across seasons.
public class InMemorySeasonStore implements SeasonStore {

    private int seasonId;
    private int nextMatchId = 1;
    private String name;
    private long seed;
    private final Map<Integer, SavedMatch> schedule = new LinkedHashMap<>();
    int matchesSaved;
    int progressSaves;

    @Override
    public void saveNewSeason(Season season) {
        season.assignId(++seasonId);
        name = season.getName();
        seed = season.getSeed();
        schedule.clear();
        for (Round round : season.getRounds()) {
            for (Fixture fixture : round.getFixtures()) {
                int id = nextMatchId++;
                fixture.assignId(id);
                schedule.put(id, new SavedMatch(id, fixture.getRound(), fixture.getHomeTeam().getId(),
                        fixture.getAwayTeam().getId(), fixture.getReferee().getId(), null));
            }
        }
    }

    @Override
    public void saveMatch(Season season, Fixture fixture) {
        SavedMatch saved = schedule.get(fixture.getId());
        schedule.put(saved.id(), withScore(saved, fixture.getScore()));
        matchesSaved++;
    }

    @Override
    public void saveRoundProgress(Season season, List<Team> teams) {
        progressSaves++;
    }

    @Override
    public Optional<Season> loadLatestSeason(List<Team> teams, List<Referee> referees) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.of(Season.restore(seasonId, name, seed, teams, referees, new ArrayList<>(schedule.values())));
    }

    // Pretends the program stopped before these matches were saved
    void forgetResult(int matchId) {
        schedule.put(matchId, withScore(schedule.get(matchId), null));
    }

    List<SavedMatch> getSchedule() {
        return List.copyOf(schedule.values());
    }

    private static SavedMatch withScore(SavedMatch match, Score score) {
        return new SavedMatch(match.id(), match.round(), match.homeTeamId(), match.awayTeamId(), match.refereeId(), score);
    }
}
