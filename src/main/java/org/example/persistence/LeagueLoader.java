package org.example.persistence;

import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.List;

public class LeagueLoader {

    private final TeamDao teamDao;
    private final PlayerDao playerDao;
    private final RefereeDao refereeDao;

    public LeagueLoader(ConnectionProvider connectionProvider) {
        this.teamDao = new TeamDao(connectionProvider);
        this.playerDao = new PlayerDao(connectionProvider);
        this.refereeDao = new RefereeDao(connectionProvider);
    }

    public List<Team> loadTeams() {
        List<Team> teams = teamDao.findAll();
        for (Team team : teams) {
            for (Player player : playerDao.findByTeamId(team.getId())) {
                team.addPlayer(player);
            }
        }
        return teams;
    }

    public List<Referee> loadReferees() {
        return refereeDao.findAll();
    }
}