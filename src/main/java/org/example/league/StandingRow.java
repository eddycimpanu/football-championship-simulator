package org.example.league;

import org.example.engine.MatchResult;
import org.example.model.Team;

import java.util.ArrayDeque;
import java.util.Deque;

// One line of the league table. Only the league table (this package) can add results.
public class StandingRow {

    private static final int FORM_LENGTH = 5;

    private final Team team;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private final Deque<Character> form = new ArrayDeque<>();

    StandingRow(Team team) {
        this.team = team;
    }

    void addResult(int scored, int conceded) {
        goalsFor += scored;
        goalsAgainst += conceded;
        char letter;
        if (scored > conceded) {
            won++;
            letter = 'W';
        } else if (scored == conceded) {
            drawn++;
            letter = 'D';
        } else {
            lost++;
            letter = 'L';
        }
        form.addLast(letter);
        if (form.size() > FORM_LENGTH) {
            form.removeFirst();
        }
    }

    public int getPoints() {
        return won * MatchResult.POINTS_FOR_WIN + drawn * MatchResult.POINTS_FOR_DRAW;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    public int getPlayed() {
        return won + drawn + lost;
    }

    // The last five results, oldest first, e.g. "WWDLW"
    public String getForm() {
        StringBuilder text = new StringBuilder();
        form.forEach(text::append);
        return text.toString();
    }

    public Team getTeam() { return team; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }

    @Override
    public String toString() {
        return team.getName() + " " + getPoints() + " pts";
    }
}
