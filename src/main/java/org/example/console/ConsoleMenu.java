package org.example.console;

import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.league.StandingRow;
import org.example.model.FieldRole;
import org.example.model.Player;
import org.example.model.Team;
import org.example.persistence.MatchReportDao;
import org.example.persistence.PlayerStatsDao;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public class ConsoleMenu {

    private static final int TOP_SCORERS_SHOWN = 15;

    private SeasonService service;
    private final Supplier<SeasonService> newSeason;
    private final PlayerStatsDao playerStatsDao;
    private final MatchReportDao matchReportDao;
    private final ConsoleInput input;
    private final LeagueTablePrinter tablePrinter = new LeagueTablePrinter();
    private final SavedMatchPrinter matchPrinter;

    // newSeason creates and saves a fresh season (with fresh players) when the user asks for one
    public ConsoleMenu(SeasonService service, Supplier<SeasonService> newSeason, PlayerStatsDao playerStatsDao,
                       MatchReportDao matchReportDao, ConsoleInput input) {
        this.service = service;
        this.newSeason = newSeason;
        this.playerStatsDao = playerStatsDao;
        this.matchReportDao = matchReportDao;
        this.matchPrinter = new SavedMatchPrinter(matchReportDao);
        this.input = input;
    }

    public void run() {
        while (true) {
            printMenu();
            int choice = input.readInt("Choose an option:", 0, 7);
            System.out.println();
            switch (choice) {
                case 1 -> simulateNextRound();
                case 2 -> showTable();
                case 3 -> findMatch();
                case 4 -> showPlayerStatistics();
                case 5 -> showTopScorers();
                case 6 -> showTeamResults();
                case 7 -> startNewSeason();
                case 0 -> {
                    System.out.println("Goodbye! The season is saved in the database (id " + season().getId() + ").");
                    return;
                }
                default -> throw new IllegalStateException("Unknown option " + choice);
            }
        }
    }

    private void printMenu() {
        Season season = season();
        String progress = season.isFinished()
                ? "season finished"
                : "next: round " + season.getCurrentRoundNumber() + " of " + season.getNumberOfRounds();
        System.out.println();
        System.out.println("=== " + season.getName() + " | " + progress + " ===");
        System.out.println("1. Simulate next round");
        System.out.println("2. Show the league table");
        System.out.println("3. Find a match");
        System.out.println("4. Player statistics");
        System.out.println("5. Top scorers");
        System.out.println("6. Team results and fixtures");
        System.out.println("7. Start a new season");
        System.out.println("0. Exit");
    }

    // ----- 1. Simulate next round -----

    private void simulateNextRound() {
        if (season().isFinished()) {
            System.out.println("The season is over. Choose 7 to start a new one.");
            printChampion();
            return;
        }
        Round round = service.playNextRound();
        System.out.println("=== " + round + " results ===");
        for (Fixture fixture : round.getFixtures()) {
            System.out.printf("  %20s %d-%d %-20s%n", fixture.getHomeTeam().getName(),
                    fixture.getScore().home(), fixture.getScore().away(), fixture.getAwayTeam().getName());
        }
        if (season().isFinished()) {
            System.out.println();
            System.out.println("That was the last round!");
            printChampion();
        }
        input.waitForEnter();
    }

    private void printChampion() {
        StandingRow champion = service.getTable().getLeader();
        System.out.println("Champion: " + champion.getTeam().getName() + " with " + champion.getPoints() + " points");
    }

    // ----- 2. League table -----

    private void showTable() {
        String title = season().getName() + " after round " + lastPlayedRound();
        tablePrinter.print(title, service.getTable());
        input.waitForEnter();
    }

    // ----- 3. Find a match -----

    // Type the two teams (part of the name is enough); the report is read from the database,
    // so it works for every match, also the ones played before the program was restarted.
    private void findMatch() {
        System.out.println("Type the names of the two teams (0 to go back).");
        Optional<Team> homeTeam = readTeam("Home team:");
        if (homeTeam.isEmpty()) {
            return;
        }
        Optional<Team> awayTeam = readTeam("Away team:");
        if (awayTeam.isEmpty()) {
            return;
        }
        Team home = homeTeam.get();
        Team away = awayTeam.get();
        if (home == away) {
            System.out.println("A team cannot play against itself.");
            return;
        }

        Optional<MatchReportDao.MatchInfo> found = matchReportDao.findMatch(season().getId(), home.getId(), away.getId());
        if (found.isEmpty()) {
            System.out.println("No match " + home.getName() + " vs " + away.getName() + " in this season.");
            return;
        }
        MatchReportDao.MatchInfo match = found.get();
        if (!match.played()) {
            System.out.println(home.getName() + " vs " + away.getName() + " is in round " + match.round()
                    + " and has not been played yet.");
            return;
        }

        System.out.println();
        System.out.println("Round " + match.round() + ": " + match.homeTeam() + " " + match.homeGoals() + "-"
                + match.awayGoals() + " " + match.awayTeam());
        boolean detailed = input.readYesNo("Show the detailed report (all events)? n = key events only");

        System.out.println();
        matchPrinter.printEvents(match, !detailed);
        matchPrinter.printStats(match);
        if (input.readYesNo("Show the player ratings?")) {
            matchPrinter.printPlayerStats(match);
        }
        input.waitForEnter();
    }

    // Accepts part of the name, without accents ("bayern", "steaua", "munchen") or the short name ("RMA").
    // Asks again when nothing matches and lets the user choose when several teams match ("milan").
    // Empty when the user types 0 to go back.
    private Optional<Team> readTeam(String prompt) {
        while (true) {
            String search = normalize(input.readText(prompt));
            if (search.equals("0")) {
                return Optional.empty();
            }
            List<Team> matches = season().getTeams().stream()
                    .filter(team -> normalize(team.getName()).contains(search)
                            || normalize(team.getShortName()).equals(search))
                    .sorted(Comparator.comparing(Team::getName))
                    .toList();
            Optional<Team> exact = matches.stream().filter(team -> normalize(team.getName()).equals(search)).findFirst();
            if (exact.isPresent()) {
                return exact;
            }
            if (matches.size() == 1) {
                return Optional.of(matches.get(0));
            }
            if (matches.isEmpty()) {
                System.out.println("No team matches \"" + search + "\". Try again.");
                continue;
            }
            for (int i = 0; i < matches.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + matches.get(i).getName());
            }
            return Optional.of(matches.get(input.readInt("Which one (1-" + matches.size() + "):", 1, matches.size()) - 1));
        }
    }

    // ----- 4. Player statistics -----

    private void showPlayerStatistics() {
        String search = normalize(input.readText("Player name (or part of it):"));
        List<Player> found = season().getTeams().stream()
                .flatMap(team -> team.getSquad().stream())
                .filter(player -> normalize(player.getFullName()).contains(search))
                .toList();
        if (found.isEmpty()) {
            System.out.println("No player found.");
            return;
        }
        Player player = found.get(0);
        if (found.size() > 1) {
            for (int i = 0; i < found.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + found.get(i).getFullName() + " (" + teamOf(found.get(i)).getName() + ")");
            }
            player = found.get(input.readInt("Which one (1-" + found.size() + "):", 1, found.size()) - 1);
        }
        printPlayerSeason(player);
        input.waitForEnter();
    }

    private void printPlayerSeason(Player player) {
        PlayerStatsDao.SeasonTotals totals = playerStatsDao.findSeasonTotals(season().getId(), player.getId());

        System.out.println();
        System.out.println("=== " + player.getFullName() + " (" + teamOf(player).getName() + ", "
                + player.getPosition() + ", " + player.getOverall() + " OVR) ===");
        System.out.println("Status: " + player.getStatus() + ", condition " + player.getCondition()
                + ", yellow cards towards a ban: " + player.getYellowCardsAccumulated());
        if (totals.appearances() == 0) {
            System.out.println("Has not played yet this season.");
            return;
        }
        System.out.printf("Appearances %d (%d starts), %d minutes%n", totals.appearances(), totals.starts(), totals.minutes());
        System.out.printf("Goals %d, assists %d, shots %d (%d on target), key passes %d%n",
                totals.goals(), totals.assists(), totals.shots(), totals.shotsOnTarget(), totals.keyPasses());
        System.out.printf("Tackles %d, interceptions %d, yellow cards %d, red cards %d%n",
                totals.tackles(), totals.interceptions(), totals.yellowCards(), totals.redCards());
        if (player.getFieldRole() == FieldRole.GOALKEEPER) {
            System.out.printf("Saves %d, clean sheets %d%n", totals.saves(), totals.cleanSheets());
        }
        System.out.println("Average rating: " + (totals.averageRating() == null ? "-" : totals.averageRating()));

        System.out.println();
        System.out.printf("  %5s  %-24s %5s %4s %2s %2s %6s%n", "Round", "Opponent", "Score", "Min", "G", "A", "Rating");
        for (PlayerStatsDao.MatchLine line : playerStatsDao.findMatchLines(season().getId(), player.getId())) {
            System.out.printf("  %5d  %-24s %5s %4d %2d %2d %6s%n", line.round(),
                    (line.home() ? "vs " : "at ") + line.opponent(), line.score(), line.minutes(),
                    line.goals(), line.assists(), line.rating() == null ? "-" : String.format("%.1f", line.rating()));
        }
    }

    // ----- 5. Top scorers -----

    private void showTopScorers() {
        List<PlayerStatsDao.ScorerLine> scorers = playerStatsDao.findTopScorers(season().getId(), TOP_SCORERS_SHOWN);
        if (scorers.isEmpty()) {
            System.out.println("No goals scored yet.");
            return;
        }
        System.out.println("=== Top scorers after round " + lastPlayedRound() + " ===");
        System.out.printf("  %3s  %-26s %-20s %5s %7s %7s%n", "Pos", "Player", "Team", "Goals", "Assists", "Minutes");
        for (int i = 0; i < scorers.size(); i++) {
            PlayerStatsDao.ScorerLine line = scorers.get(i);
            System.out.printf("  %3d  %-26s %-20s %5d %7d %7d%n",
                    i + 1, line.player(), line.team(), line.goals(), line.assists(), line.minutes());
        }
        input.waitForEnter();
    }

    // ----- 6. Team results and fixtures -----

    private void showTeamResults() {
        List<Team> teams = season().getTeams().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();
        for (int i = 0; i < teams.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, teams.get(i).getName());
        }
        Team team = teams.get(input.readInt("Team (1-" + teams.size() + "):", 1, teams.size()) - 1);

        System.out.println();
        System.out.println("=== " + team.getName() + ": position " + service.getTable().positionOf(team) + " ===");
        for (Fixture fixture : season().getFixturesOf(team)) {
            String where = fixture.getHomeTeam() == team ? "vs" : "at";
            String opponent = fixture.opponentOf(team).getName();
            if (fixture.isPlayed()) {
                int scored = fixture.goalsFor(team);
                int conceded = fixture.goalsAgainst(team);
                String outcome = scored > conceded ? "W" : scored == conceded ? "D" : "L";
                // The team's own goals first, like on club websites
                System.out.printf("  Round %2d  %s %-20s %d-%d  %s%n", fixture.getRound(), where, opponent,
                        scored, conceded, outcome);
            } else {
                System.out.printf("  Round %2d  %s %-20s  -%n", fixture.getRound(), where, opponent);
            }
        }
        input.waitForEnter();
    }

    // ----- 7. New season -----

    private void startNewSeason() {
        if (!season().isFinished()
                && !input.readYesNo(season().getName() + " is not finished. Start a new season anyway?")) {
            return;
        }
        service = newSeason.get();
        System.out.println("New season created (id " + season().getId() + "). The old one stays saved in the database.");
    }

    // ----- Helpers -----

    private Season season() {
        return service.getSeason();
    }

    private int lastPlayedRound() {
        return season().isFinished() ? season().getNumberOfRounds() : season().getCurrentRoundNumber() - 1;
    }

    private Team teamOf(Player player) {
        return season().getTeams().stream()
                .filter(team -> team.getSquad().contains(player))
                .findFirst()
                .orElseThrow();
    }

    // "Lăcătuș" and "lacatus" match: no accents, lower case
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
