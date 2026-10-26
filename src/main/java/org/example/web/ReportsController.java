package org.example.web;

import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.PlayerStatsDao.ScorerLine;
import org.example.persistence.SeasonReportDao;
import org.example.persistence.SeasonReportDao.HatTrick;
import org.example.persistence.SeasonReportDao.MatchRecord;
import org.example.persistence.SeasonReportDao.PlayerTotal;
import org.example.persistence.SeasonReportDao.TeamTotal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// GET /api/reports: every season report in one answer, all calculated in SQL.
// Ratings only count players who played enough: 40% of the possible minutes for the lists,
// 60% for the awards (MVP, team of the season), the same rules as the console.
@RestController
@RequestMapping("/api")
public class ReportsController {

    private static final int LIST_SIZE = 10;
    private static final int RECORDS = 3;
    private static final double MIN_SHARE_FOR_LISTS = 0.4;
    private static final double MIN_SHARE_FOR_AWARDS = 0.6;

    public record ReportsResponse(String season, int roundsPlayed, int listMinMinutes, int awardMinMinutes,
                                  PlayerTotal mvp, List<PlayerTotal> teamOfTheSeason, List<ScorerLine> topScorers,
                                  List<PlayerTotal> topAssists, List<PlayerTotal> bestRatings,
                                  List<PlayerTotal> goalkeepers, List<PlayerTotal> discipline,
                                  List<TeamTotal> teams, List<MatchRecord> biggestWins,
                                  List<MatchRecord> highestScoring, List<HatTrick> hatTricks) {
    }

    private final SeasonService seasonService;
    private final SeasonReportDao reportDao;
    private final PlayerStatsDao statsDao;

    public ReportsController(SeasonService seasonService, SeasonReportDao reportDao, PlayerStatsDao statsDao) {
        this.seasonService = seasonService;
        this.reportDao = reportDao;
        this.statsDao = statsDao;
    }

    @GetMapping("/reports")
    public ReportsResponse reports() {
        Season season;
        int roundsPlayed;
        synchronized (seasonService) {
            season = seasonService.getSeason();
            roundsPlayed = season.getRoundsPlayed();
        }
        int id = season.getId();
        int listMinutes = minMinutes(roundsPlayed, MIN_SHARE_FOR_LISTS);
        int awardMinutes = minMinutes(roundsPlayed, MIN_SHARE_FOR_AWARDS);

        PlayerTotal mvp = roundsPlayed == 0 ? null
                : reportDao.findBestRatings(id, awardMinutes, 1).stream().findFirst().orElse(null);
        return new ReportsResponse(season.getName(), roundsPlayed, listMinutes, awardMinutes, mvp,
                roundsPlayed == 0 ? List.of() : reportDao.findTeamOfTheSeason(id, awardMinutes),
                statsDao.findTopScorers(id, LIST_SIZE),
                reportDao.findTopAssists(id, LIST_SIZE),
                roundsPlayed == 0 ? List.of() : reportDao.findBestRatings(id, listMinutes, LIST_SIZE),
                roundsPlayed == 0 ? List.of() : reportDao.findGoalkeepers(id, listMinutes, LIST_SIZE),
                reportDao.findMostCarded(id, LIST_SIZE),
                reportDao.findTeamTotals(id),
                reportDao.findBiggestWins(id, RECORDS),
                reportDao.findHighestScoring(id, RECORDS),
                reportDao.findHatTricks(id));
    }

    private static int minMinutes(int roundsPlayed, double share) {
        return (int) Math.ceil(roundsPlayed * 90 * share);
    }
}
