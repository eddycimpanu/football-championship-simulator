package org.example.console;

import org.example.league.LeagueTable;
import org.example.league.StandingRow;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.SeasonReportDao;
import org.example.persistence.SeasonReportDao.HatTrick;
import org.example.persistence.SeasonReportDao.MatchRecord;
import org.example.persistence.SeasonReportDao.PlayerTotal;
import org.example.persistence.SeasonReportDao.TeamTotal;

import java.util.List;

// Prints the season reports. Players must have played a share of the possible minutes to be ranked
// on ratings, so a substitute with one great match cannot top the list.
public class SeasonReportPrinter {

    private static final int LIST_SIZE = 10;
    private static final double MIN_SHARE_FOR_LISTS = 0.4;    // 40% of the minutes his team played
    private static final double MIN_SHARE_FOR_AWARDS = 0.6;   // MVP, team of the season, best goalkeeper
    private static final int MINUTES_PER_MATCH = 90;

    private final SeasonReportDao reportDao;
    private final PlayerStatsDao playerStatsDao;

    public SeasonReportPrinter(SeasonReportDao reportDao, PlayerStatsDao playerStatsDao) {
        this.reportDao = reportDao;
        this.playerStatsDao = playerStatsDao;
    }

    // ----- Short summary at the end of the season -----

    public void printSummary(int seasonId, String seasonName, LeagueTable table, int roundsPlayed) {
        int awardMinutes = minMinutes(roundsPlayed, MIN_SHARE_FOR_AWARDS);
        List<StandingRow> standings = table.getStandings();

        System.out.println();
        System.out.println("=== " + seasonName + ": end of season ===");
        System.out.println("Champion:        " + standings.get(0).getTeam().getName() + ", " + standings.get(0).getPoints() + " points");
        System.out.println("Runner-up:       " + standings.get(1).getTeam().getName() + ", " + standings.get(1).getPoints() + " points");
        System.out.println("Relegated:       " + standings.subList(standings.size() - 3, standings.size()).stream()
                .map(row -> row.getTeam().getName()).toList());
        playerStatsDao.findTopScorers(seasonId, 1).stream().findFirst().ifPresent(scorer ->
                System.out.println("Top scorer:      " + scorer.player() + " (" + scorer.team() + "), " + scorer.goals() + " goals"));
        reportDao.findTopAssists(seasonId, 1).stream().findFirst().ifPresent(player ->
                System.out.println("Most assists:    " + player.player() + " (" + player.team() + "), " + player.assists() + " assists"));
        reportDao.findGoalkeepers(seasonId, awardMinutes, 1).stream().findFirst().ifPresent(keeper ->
                System.out.println("Best goalkeeper: " + keeper.player() + " (" + keeper.team() + "), " + keeper.cleanSheets() + " clean sheets"));
        reportDao.findBestRatings(seasonId, awardMinutes, 1).stream().findFirst().ifPresent(mvp ->
                System.out.printf("MVP:             %s (%s), average rating %.2f, %d goals, %d assists%n",
                        mvp.player(), mvp.team(), mvp.averageRating(), mvp.goals(), mvp.assists()));
        printTeamOfTheSeason(seasonId, roundsPlayed);
    }

    // ----- Reports -----

    public void printTopAssists(int seasonId) {
        List<PlayerTotal> players = reportDao.findTopAssists(seasonId, LIST_SIZE);
        System.out.println("=== Most assists ===");
        System.out.printf("  %3s  %-26s %-20s %7s %5s %4s%n", "#", "Player", "Team", "Assists", "Goals", "Apps");
        for (int i = 0; i < players.size(); i++) {
            PlayerTotal p = players.get(i);
            System.out.printf("  %3d  %-26s %-20s %7d %5d %4d%n", i + 1, p.player(), p.team(), p.assists(), p.goals(), p.appearances());
        }
    }

    public void printDiscipline(int seasonId) {
        List<PlayerTotal> players = reportDao.findMostCarded(seasonId, LIST_SIZE);
        System.out.println("=== Discipline (a red card counts as three yellows) ===");
        System.out.printf("  %3s  %-26s %-20s %-4s %6s %4s %4s%n", "#", "Player", "Team", "Pos", "Yellow", "Red", "Apps");
        for (int i = 0; i < players.size(); i++) {
            PlayerTotal p = players.get(i);
            System.out.printf("  %3d  %-26s %-20s %-4s %6d %4d %4d%n", i + 1, p.player(), p.team(), p.position(),
                    p.yellowCards(), p.redCards(), p.appearances());
        }
    }

    public void printGoalkeepers(int seasonId, int roundsPlayed) {
        List<PlayerTotal> keepers = reportDao.findGoalkeepers(seasonId, minMinutes(roundsPlayed, MIN_SHARE_FOR_LISTS), LIST_SIZE);
        System.out.println("=== Goalkeepers ===");
        System.out.printf("  %3s  %-26s %-20s %4s %6s %8s %5s %6s%n", "#", "Keeper", "Team", "Apps", "Clean", "Conceded", "Saves", "Rating");
        for (int i = 0; i < keepers.size(); i++) {
            PlayerTotal k = keepers.get(i);
            System.out.printf("  %3d  %-26s %-20s %4d %6d %8d %5d %6s%n", i + 1, k.player(), k.team(), k.appearances(),
                    k.cleanSheets(), k.goalsConceded(), k.saves(), rating(k));
        }
    }

    public void printBestRatings(int seasonId, int roundsPlayed) {
        int minMinutes = minMinutes(roundsPlayed, MIN_SHARE_FOR_LISTS);
        List<PlayerTotal> players = reportDao.findBestRatings(seasonId, minMinutes, LIST_SIZE);
        System.out.println("=== Best average rating (at least " + minMinutes + " minutes) ===");
        System.out.printf("  %3s  %-26s %-20s %-4s %4s %5s %7s %6s%n", "#", "Player", "Team", "Pos", "Apps", "Goals", "Assists", "Rating");
        for (int i = 0; i < players.size(); i++) {
            PlayerTotal p = players.get(i);
            System.out.printf("  %3d  %-26s %-20s %-4s %4d %5d %7d %6s%n", i + 1, p.player(), p.team(), p.position(),
                    p.appearances(), p.goals(), p.assists(), rating(p));
        }
    }

    public void printTeamOfTheSeason(int seasonId, int roundsPlayed) {
        int minMinutes = minMinutes(roundsPlayed, MIN_SHARE_FOR_AWARDS);
        List<PlayerTotal> team = reportDao.findTeamOfTheSeason(seasonId, minMinutes);
        System.out.println();
        System.out.println("=== Team of the season (4-3-3, best average rating, at least " + minMinutes + " minutes) ===");
        String role = "";
        for (PlayerTotal p : team) {
            if (!p.fieldRole().equals(role)) {
                role = p.fieldRole();
                System.out.println("  " + role);
            }
            System.out.printf("    %-26s %-20s %-4s %6s  %2d G %2d A%n", p.player(), p.team(), p.position(),
                    rating(p), p.goals(), p.assists());
        }
        reportDao.findBestRatings(seasonId, minMinutes, 1).stream().findFirst().ifPresent(mvp ->
                System.out.println("  MVP: " + mvp.player() + " (" + mvp.team() + ")"));
    }

    public void printTeamStatistics(int seasonId) {
        List<TeamTotal> teams = reportDao.findTeamTotals(seasonId);
        System.out.println("=== Team statistics ===");
        System.out.printf("  %-20s %3s %4s %4s %3s %7s %6s %5s %9s %4s %3s%n",
                "Team", "P", "GF", "GA", "CS", "Shots/g", "Acc %", "Pos %", "Corners/g", "Y", "R");
        for (TeamTotal t : teams) {
            System.out.printf("  %-20s %3d %4d %4d %3d %7.1f %6d %5d %9.1f %4d %3d%n", t.team(), t.played(),
                    t.goalsFor(), t.goalsAgainst(), t.cleanSheets(), t.shotsPerMatch(), t.shotAccuracy(),
                    t.possession(), t.cornersPerMatch(), t.yellowCards(), t.redCards());
        }
        System.out.println("  CS = clean sheets, Acc = shots on target, Pos = average possession");
    }

    public void printRecords(int seasonId) {
        System.out.println("=== Season records ===");
        System.out.println("Biggest wins:");
        reportDao.findBiggestWins(seasonId, 3).forEach(SeasonReportPrinter::printMatch);
        System.out.println("Most goals in a match:");
        reportDao.findHighestScoring(seasonId, 3).forEach(SeasonReportPrinter::printMatch);
        List<HatTrick> hatTricks = reportDao.findHatTricks(seasonId);
        System.out.println("Hat-tricks: " + (hatTricks.isEmpty() ? "none yet" : hatTricks.size()));
        for (HatTrick h : hatTricks) {
            System.out.printf("  Round %2d  %-26s %d goals for %s against %s%n", h.round(), h.player(), h.goals(), h.team(), h.opponent());
        }
    }

    // ----- Helpers -----

    private static void printMatch(MatchRecord m) {
        System.out.printf("  Round %2d  %s %d-%d %s%n", m.round(), m.homeTeam(), m.homeGoals(), m.awayGoals(), m.awayTeam());
    }

    private static int minMinutes(int roundsPlayed, double share) {
        return (int) Math.ceil(roundsPlayed * MINUTES_PER_MATCH * share);
    }

    private static String rating(PlayerTotal player) {
        return player.averageRating() == null ? "-" : String.format("%.2f", player.averageRating());
    }
}
