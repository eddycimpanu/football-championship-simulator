package org.example.league;

import org.example.config.EngineConfig;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.List;
import java.util.Optional;

// Plays a season round by round and saves everything as it goes.
public class SeasonService {

    private final Season season;
    private final SeasonStore store;

    private SeasonService(Season season, SeasonStore store) {
        this.season = season;
        this.store = store;
    }

    // Creates the season, its schedule and referees, and saves them before the first match.
    public static SeasonService startNewSeason(String name, List<Team> teams, List<Referee> referees,
                                               long seed, SeasonStore store) {
        Season season = Season.create(name, teams, referees, seed);
        store.saveNewSeason(season);
        return new SeasonService(season, store);
    }

    // Continues the last season saved in the database, exactly where it stopped.
    public static Optional<SeasonService> continueLatestSeason(List<Team> teams, List<Referee> referees,
                                                                SeasonStore store) {
        return store.loadLatestSeason(teams, referees).map(season -> new SeasonService(season, store));
    }

    // Plays the next round: 10 matches, each saved on its own; then the players recover.
    // If the program was closed in the middle of a round, only the matches still missing are played.
    public Round playNextRound() {
        Round round = season.getNextRound()
                .orElseThrow(() -> new IllegalStateException(season.getName() + " is already finished"));

        for (Fixture fixture : round.getFixtures()) {
            if (fixture.isPlayed()) {
                continue;
            }
            fixture.play(season.getRandom());
            store.saveMatch(season, fixture);
        }

        recoverBetweenRounds();
        store.saveRoundProgress(season, season.getTeams());
        return round;
    }

    public void playRemainingRounds() {
        while (!season.isFinished()) {
            playNextRound();
        }
    }

    private void recoverBetweenRounds() {
        for (Team team : season.getTeams()) {
            for (Player player : team.getSquad()) {
                player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND);
            }
        }
    }

    public Season getSeason() {
        return season;
    }

    public LeagueTable getTable() {
        return season.getTable();
    }
}
