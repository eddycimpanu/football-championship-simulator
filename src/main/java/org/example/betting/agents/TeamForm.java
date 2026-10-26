package org.example.betting.agents;

// What the agents know about a team so far this season (from the league table).
public record TeamForm(int played, double pointsPerGame, double goalsForPerGame, double goalsAgainstPerGame) {

    public double goalDifferencePerGame() {
        return goalsForPerGame - goalsAgainstPerGame;
    }
}
