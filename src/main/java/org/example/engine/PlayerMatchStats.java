package org.example.engine;

import org.example.config.EngineConfig;
import org.example.model.FieldRole;
import org.example.model.Player;
import org.example.model.Team;

// One player's numbers in one match. Only the engine (this package) can change them;
// everyone else can only read them.
public class PlayerMatchStats {

    private static final int NOT_SET = -1;
    private static final int FULL_MATCH = 90;

    private final Player player;
    private final Team team;
    private final boolean starter;

    private int minuteOn;
    private int minuteOff = NOT_SET;

    private int goals;
    private int assists;
    private int ownGoals;
    private int shots;
    private int shotsOnTarget;
    private int keyPasses;
    private int tackles;
    private int interceptions;
    private int foulsCommitted;
    private int foulsSuffered;
    private int offsides;
    private int yellowCards;
    private int redCards;
    private int saves;
    private int goalsConceded;
    private boolean cleanSheet;
    private double rating;

    PlayerMatchStats(Player player, Team team, boolean starter, int minuteOn) {
        this.player = player;
        this.team = team;
        this.starter = starter;
        this.minuteOn = minuteOn;
    }

    // ----- Changed only by the engine -----

    void leave(int minute) {
        if (minuteOff == NOT_SET) {
            minuteOff = Math.max(minuteOn, minute);
        }
    }

    void addGoal() { goals++; }
    void addAssist() { assists++; }
    void addOwnGoal() { ownGoals++; }
    void addShot(boolean onTarget) { shots++; if (onTarget) shotsOnTarget++; }
    void addKeyPass() { keyPasses++; }
    void addTackle() { tackles++; }
    void addInterception() { interceptions++; }
    void addFoulCommitted() { foulsCommitted++; }
    void addFoulSuffered() { foulsSuffered++; }
    void addOffside() { offsides++; }
    void addYellowCard() { yellowCards++; }
    void addRedCard() { redCards++; }
    void addSave() { saves++; }
    void addGoalConceded() { goalsConceded++; }

    // Called once at the final whistle.
    void finish(int goalsFor, int goalsAgainst) {
        leave(FULL_MATCH);
        FieldRole role = player.getFieldRole();
        cleanSheet = goalsAgainst == 0 && getMinutesPlayed() >= EngineConfig.MIN_MINUTES_FOR_CLEAN_SHEET
                && (role == FieldRole.GOALKEEPER || role == FieldRole.DEFENDER);
        rating = calculateRating(goalsFor, goalsAgainst);
    }

    private double calculateRating(int goalsFor, int goalsAgainst) {
        if (getMinutesPlayed() < EngineConfig.MIN_MINUTES_FOR_RATING) {
            return 0;
        }
        double value = EngineConfig.RATING_BASE
                + goals * EngineConfig.RATING_GOAL
                + assists * EngineConfig.RATING_ASSIST
                + ownGoals * EngineConfig.RATING_OWN_GOAL
                + shotsOnTarget * EngineConfig.RATING_SHOT_ON_TARGET
                + keyPasses * EngineConfig.RATING_KEY_PASS
                + tackles * EngineConfig.RATING_TACKLE
                + interceptions * EngineConfig.RATING_INTERCEPTION
                + foulsSuffered * EngineConfig.RATING_FOUL_SUFFERED
                + foulsCommitted * EngineConfig.RATING_FOUL_COMMITTED
                + saves * EngineConfig.RATING_SAVE
                + yellowCards * EngineConfig.RATING_YELLOW
                + redCards * EngineConfig.RATING_RED;

        if (player.getFieldRole() == FieldRole.GOALKEEPER) {
            value += cleanSheet ? EngineConfig.RATING_CLEAN_SHEET_GOALKEEPER : 0;
            value += goalsConceded * EngineConfig.RATING_GOAL_CONCEDED_GOALKEEPER;
        } else if (player.getFieldRole() == FieldRole.DEFENDER) {
            value += cleanSheet ? EngineConfig.RATING_CLEAN_SHEET_DEFENDER : 0;
            value += goalsAgainst * EngineConfig.RATING_GOAL_CONCEDED_DEFENDER;
        }

        if (goalsFor > goalsAgainst) {
            value += EngineConfig.RATING_WIN;
        } else if (goalsFor < goalsAgainst) {
            value += EngineConfig.RATING_LOSS;
        }

        double clamped = Math.max(EngineConfig.RATING_MIN, Math.min(EngineConfig.RATING_MAX, value));
        return Math.round(clamped * 10) / 10.0;
    }

    // ----- Read by everyone -----

    public int getMinutesPlayed() {
        int off = minuteOff == NOT_SET ? FULL_MATCH : minuteOff;
        return off - minuteOn;
    }

    public boolean isRated() { return rating > 0; }
    public Player getPlayer() { return player; }
    public Team getTeam() { return team; }
    public boolean isStarter() { return starter; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public int getOwnGoals() { return ownGoals; }
    public int getShots() { return shots; }
    public int getShotsOnTarget() { return shotsOnTarget; }
    public int getKeyPasses() { return keyPasses; }
    public int getTackles() { return tackles; }
    public int getInterceptions() { return interceptions; }
    public int getFoulsCommitted() { return foulsCommitted; }
    public int getFoulsSuffered() { return foulsSuffered; }
    public int getOffsides() { return offsides; }
    public int getYellowCards() { return yellowCards; }
    public int getRedCards() { return redCards; }
    public int getSaves() { return saves; }
    public int getGoalsConceded() { return goalsConceded; }
    public boolean hasCleanSheet() { return cleanSheet; }
    public double getRating() { return rating; }

    @Override
    public String toString() {
        return player.getFullName() + " " + (isRated() ? rating : "-");
    }
}
