package org.example.console;

import org.example.engine.MatchMinute;
import org.example.persistence.MatchReportDao;
import org.example.persistence.MatchReportDao.EventLine;
import org.example.persistence.MatchReportDao.MatchInfo;
import org.example.persistence.MatchReportDao.PlayerLine;
import org.example.persistence.MatchReportDao.TeamStatsLine;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

// Prints a match report read from the database, in the same layout as MatchReportPrinter.
public class SavedMatchPrinter {

    private final MatchReportDao reportDao;

    public SavedMatchPrinter(MatchReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public void printEvents(MatchInfo match, boolean keyEventsOnly) {
        String mode = keyEventsOnly ? "KEY" : "ALL";
        String referee = match.referee() == null ? "" : ", referee " + match.referee();
        System.out.println("=== " + title(match) + " (round " + match.round() + ", " + mode + " events" + referee + ") ===");
        for (EventLine event : reportDao.findEvents(match.id(), keyEventsOnly)) {
            MatchMinute minute = new MatchMinute(event.minute(), event.addedTime());
            System.out.printf("%-7s %s%n", minute, event.description());
        }
    }

    public void printStats(MatchInfo match) {
        List<TeamStatsLine> stats = reportDao.findTeamStats(match.id());
        if (stats.size() != 2) {
            return;
        }
        TeamStatsLine home = stats.get(0);
        TeamStatsLine away = stats.get(1);

        System.out.println();
        System.out.printf("%-14s %8s %8s%n", "", home.team(), away.team());
        printStat("Goals", home, away, TeamStatsLine::goals);
        printStat("Shots", home, away, TeamStatsLine::shots);
        printStat("On target", home, away, TeamStatsLine::shotsOnTarget);
        printStat("Corners", home, away, TeamStatsLine::corners);
        printStat("Fouls", home, away, TeamStatsLine::fouls);
        printStat("Offsides", home, away, TeamStatsLine::offsides);
        printStat("Saves", home, away, TeamStatsLine::saves);
        printStat("Yellow cards", home, away, TeamStatsLine::yellowCards);
        printStat("Red cards", home, away, TeamStatsLine::redCards);
        printStat("Possession %", home, away, TeamStatsLine::possessionPercent);
    }

    public void printPlayerStats(MatchInfo match) {
        List<PlayerLine> lines = reportDao.findPlayerLines(match.id());
        printPlayerTable(match.homeTeam(), lines);
        printPlayerTable(match.awayTeam(), lines);

        // Best rating; with equal ratings, more goals and assists win (same rule as MatchResult)
        lines.stream()
                .filter(line -> line.rating() != null)
                .max(Comparator.comparingDouble(PlayerLine::rating)
                        .thenComparingInt(line -> line.goals() + line.assists()))
                .ifPresent(best -> System.out.println("Man of the match: " + best.player()
                        + " (" + best.team() + "), rating " + best.rating()));
    }

    private void printPlayerTable(String team, List<PlayerLine> lines) {
        System.out.println();
        System.out.println("=== " + team + " players ===");
        System.out.printf("  %-26s %-4s %4s %3s %3s %6s %3s %4s %4s %4s %5s %3s %6s%n",
                "Player", "Pos", "Min", "G", "A", "Sh(OT)", "KP", "Tkl", "Int", "Fls", "Cards", "Sv", "Rating");
        for (PlayerLine line : lines) {
            if (!Objects.equals(line.team(), team)) {
                continue;
            }
            String cards = "Y".repeat(line.yellowCards()) + "R".repeat(line.redCards());
            String name = (line.starter() ? "" : "  ") + line.player();
            System.out.printf("  %-26s %-4s %4d %3d %3d %6s %3d %4d %4d %4d %5s %3d %6s%n",
                    name, line.position(), line.minutes(), line.goals(), line.assists(),
                    line.shots() + "(" + line.shotsOnTarget() + ")", line.keyPasses(), line.tackles(),
                    line.interceptions(), line.fouls(), cards.isEmpty() ? "-" : cards, line.saves(),
                    line.rating() == null ? "-" : String.format("%.1f", line.rating()));
        }
    }

    private static String title(MatchInfo match) {
        return match.homeTeam() + " " + match.homeGoals() + "-" + match.awayGoals() + " " + match.awayTeam();
    }

    private void printStat(String name, TeamStatsLine home, TeamStatsLine away, ToIntFunction<TeamStatsLine> value) {
        System.out.printf("%-14s %8d %8d%n", name, value.applyAsInt(home), value.applyAsInt(away));
    }
}
