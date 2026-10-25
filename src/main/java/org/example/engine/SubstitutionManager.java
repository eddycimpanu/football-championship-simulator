package org.example.engine;

import org.example.config.EngineConfig;
import org.example.engine.event.SubstitutionEvent;
import org.example.model.FieldRole;
import org.example.model.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// The coach's substitutions during a match.
public final class SubstitutionManager {

    private SubstitutionManager() {
        // utility class, no instances
    }

    // Forced change: the injured player is replaced by the best player of the same role on the bench.
    // Allowed even when the coach was sent off. With no changes left, the team plays with one man less.
    public static void afterInjury(MatchContext context, MatchTeam team, Player injured) {
        if (!team.canSubstitute()) {
            return;
        }
        Optional<Player> replacement = bestOnBench(team, injured.getFieldRole());
        if (replacement.isEmpty() && injured.getFieldRole() != FieldRole.GOALKEEPER) {
            replacement = bestOutfieldOnBench(team);
        }
        replacement.ifPresent(in -> substitute(context, team, injured, in, SubstitutionEvent.Reason.INJURY));
    }

    // The goalkeeper was sent off: an outfield player comes off so the reserve goalkeeper can go in.
    public static void afterGoalkeeperSentOff(MatchContext context, MatchTeam team) {
        if (!team.canSubstitute()) {
            return;
        }
        Optional<Player> reserveKeeper = bestOnBench(team, FieldRole.GOALKEEPER);
        Optional<Player> sacrificed = weakestOnPitch(team, FieldRole.FORWARD)
                .or(() -> weakestOnPitch(team, FieldRole.MIDFIELDER));
        if (reserveKeeper.isPresent() && sacrificed.isPresent()) {
            substitute(context, team, sacrificed.get(), reserveKeeper.get(), SubstitutionEvent.Reason.GOALKEEPER_SENT_OFF);
        }
    }

    // Planned changes at half-time, 60', 70' and 80'. A coach who was sent off cannot make them.
    public static void considerChanges(MatchContext context, MatchTeam team) {
        if (team.isCoachSentOff()) {
            return;
        }
        int changes = 0;

        // 1. Tired players, the most tired first, if the bench has someone not much worse
        List<Player> tired = team.getOutfieldPlayers().stream()
                .filter(p -> team.getEnergy(p) < EngineConfig.TIRED_ENERGY)
                .sorted(Comparator.comparingDouble(team::getEnergy))
                .toList();
        for (Player player : tired) {
            if (changes >= EngineConfig.MAX_CHANGES_PER_WINDOW || !canMakePlannedChange(context, team)) {
                break;
            }
            Optional<Player> replacement = bestOnBench(team, player.getFieldRole())
                    .filter(in -> in.getOverall() >= player.getOverall() - EngineConfig.FATIGUE_SUBSTITUTION_MARGIN);
            if (replacement.isPresent()) {
                substitute(context, team, player, replacement.get(), SubstitutionEvent.Reason.FATIGUE);
                changes++;
            }
        }

        // 2. An aggressive player on a yellow card, before he gets a second one
        if (changes < EngineConfig.MAX_CHANGES_PER_WINDOW && canMakePlannedChange(context, team)
                && context.minute().minute() >= EngineConfig.SUBSTITUTION_MINUTES[0]) {
            Optional<Player> atRisk = team.getOutfieldPlayers().stream()
                    .filter(team::hasYellowCard)
                    .filter(p -> p.getAttributes().aggression() >= EngineConfig.CARD_RISK_AGGRESSION)
                    .findFirst();
            if (atRisk.isPresent()) {
                Optional<Player> replacement = bestOnBench(team, atRisk.get().getFieldRole());
                if (replacement.isPresent()) {
                    substitute(context, team, atRisk.get(), replacement.get(), SubstitutionEvent.Reason.CARD_RISK);
                    changes++;
                }
            }
        }

        // 3. Tactics: losing late -> a forward on; winning by one goal at the end -> a defender on
        if (changes < EngineConfig.MAX_CHANGES_PER_WINDOW && canMakePlannedChange(context, team)
                && context.minute().minute() >= EngineConfig.TACTICAL_FROM_MINUTE) {
            int goalDifference = goalDifference(context, team);
            if (goalDifference < 0) {
                Optional<Player> out = weakestOnPitch(team, FieldRole.DEFENDER).or(() -> weakestOnPitch(team, FieldRole.MIDFIELDER));
                Optional<Player> in = bestOnBench(team, FieldRole.FORWARD);
                if (out.isPresent() && in.isPresent()) {
                    substitute(context, team, out.get(), in.get(), SubstitutionEvent.Reason.TACTICAL);
                }
            } else if (goalDifference == 1 && context.minute().minute() >= EngineConfig.KEEP_ONE_SUBSTITUTION_UNTIL) {
                Optional<Player> out = weakestOnPitch(team, FieldRole.FORWARD);
                Optional<Player> in = bestOnBench(team, FieldRole.DEFENDER);
                if (out.isPresent() && in.isPresent()) {
                    substitute(context, team, out.get(), in.get(), SubstitutionEvent.Reason.TACTICAL);
                }
            }
        }
    }

    // Until 80' the coach keeps one change in reserve, in case of an injury.
    private static boolean canMakePlannedChange(MatchContext context, MatchTeam team) {
        return team.canSubstitute()
                && (team.substitutionsLeft() > 1 || context.minute().minute() >= EngineConfig.KEEP_ONE_SUBSTITUTION_UNTIL);
    }

    private static int goalDifference(MatchContext context, MatchTeam team) {
        Score score = context.score();
        return team == context.getHome() ? score.home() - score.away() : score.away() - score.home();
    }

    private static Optional<Player> bestOnBench(MatchTeam team, FieldRole role) {
        return team.getBench().stream()
                .filter(p -> p.getFieldRole() == role)
                .max(Comparator.comparingInt(Player::getOverall));
    }

    private static Optional<Player> bestOutfieldOnBench(MatchTeam team) {
        return team.getBench().stream()
                .filter(p -> p.getFieldRole() != FieldRole.GOALKEEPER)
                .max(Comparator.comparingInt(Player::getOverall));
    }

    private static Optional<Player> weakestOnPitch(MatchTeam team, FieldRole role) {
        return team.getOnPitch().stream()
                .filter(p -> p.getFieldRole() == role)
                .min(Comparator.comparingInt(Player::getOverall));
    }

    private static void substitute(MatchContext context, MatchTeam team, Player out, Player in,
                                   SubstitutionEvent.Reason reason) {
        team.substitute(out, in);
        context.playerLeaves(out);
        context.playerEnters(in);
        context.clock().recordSubstitution();
        context.addEvent(new SubstitutionEvent(context.minute(), team.getTeam(), out, in, reason));
    }
}
