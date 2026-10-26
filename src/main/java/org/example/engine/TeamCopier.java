package org.example.engine;

import org.example.model.Player;
import org.example.model.Team;

// Makes a copy of a team that a practice simulation can change freely: the same players,
// attributes, fitness, cards and injuries, but new objects, so the real squad is never touched.
public final class TeamCopier {

    private TeamCopier() {
    }

    public static Team copy(Team team) {
        // The coach and the attributes never change during a match, so they can be shared
        Team copy = new Team(team.getId(), team.getName(), team.getShortName(), team.getCity(), team.getStadium(),
                team.getCoach());
        for (Player player : team.getSquad()) {
            Player twin = new Player(player.getId(), player.getFirstName(), player.getLastName(),
                    player.getNationality(), player.getAge(), player.getShirtNumber(), player.getPosition(),
                    player.getPreferredFoot(), player.getAttributes());
            twin.restoreState(player.getCondition(), player.getYellowCardsAccumulated(),
                    player.getMatchesSuspended(), player.getMatchesInjured());
            copy.addPlayer(twin);
        }
        return copy;
    }
}
