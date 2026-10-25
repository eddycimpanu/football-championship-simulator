package org.example.engine;

import org.example.config.EngineConfig;
import org.example.engine.event.CardEvent;
import org.example.engine.event.CoachCardEvent;
import org.example.engine.random.RouletteWheel;
import org.example.model.FieldRole;
import org.example.model.Player;

// The referee's decisions after a foul: card for the player, and sometimes for a coach who protests.
public final class CardDecider {

    private enum Decision { NO_CARD, YELLOW, RED }

    private CardDecider() {
        // utility class, no instances
    }

    // Strict referees and aggressive players mean more cards; a foul that gives a penalty is punished harder.
    public static void afterFoul(MatchContext context, Player fouler, boolean seriousFoul) {
        MatchTeam team = context.teamOf(fouler);
        if (!team.isOnPitch(fouler)) {
            return;
        }
        double strictness = context.refereeStrictness() / EngineConfig.AVERAGE_REFEREE_STRICTNESS;
        double aggression = fouler.getAttributes().aggression() / EngineConfig.REFERENCE_ATTRIBUTE;
        double yellowFactor = seriousFoul ? EngineConfig.SERIOUS_FOUL_YELLOW_FACTOR : 1;
        double redFactor = seriousFoul ? EngineConfig.SERIOUS_FOUL_RED_FACTOR : 1;
        if (team.hasYellowCard(fouler)) {
            yellowFactor *= EngineConfig.BOOKED_PLAYER_YELLOW_FACTOR;
        }

        Decision decision = new RouletteWheel<Decision>()
                .addOption(Decision.NO_CARD, EngineConfig.CARD_NONE)
                .addOption(Decision.YELLOW, EngineConfig.CARD_YELLOW * strictness * aggression * yellowFactor)
                .addOption(Decision.RED, EngineConfig.CARD_RED * strictness * aggression * redFactor)
                .spin(context.random());

        switch (decision) {
            case NO_CARD -> { }
            case YELLOW -> {
                boolean secondYellow = team.giveYellowCard(fouler);
                show(context, team, fouler, secondYellow ? CardEvent.CardType.SECOND_YELLOW : CardEvent.CardType.YELLOW);
            }
            case RED -> {
                team.sendOff(fouler);
                show(context, team, fouler, CardEvent.CardType.RED);
            }
        }
    }

    // The coach of the team that was penalised protests; rarely the referee shows him a card.
    public static void coachProtest(MatchContext context, MatchTeam team) {
        if (team.isCoachSentOff()) {
            return;
        }
        double strictness = context.refereeStrictness() / EngineConfig.AVERAGE_REFEREE_STRICTNESS;

        Decision decision = new RouletteWheel<Decision>()
                .addOption(Decision.NO_CARD, EngineConfig.COACH_NO_CARD)
                .addOption(Decision.YELLOW, EngineConfig.COACH_YELLOW * strictness)
                .addOption(Decision.RED, EngineConfig.COACH_RED * strictness)
                .spin(context.random());

        CardEvent.CardType cardType = switch (decision) {
            case NO_CARD -> null;
            case YELLOW -> team.giveCoachYellowCard() ? CardEvent.CardType.SECOND_YELLOW : CardEvent.CardType.YELLOW;
            case RED -> {
                team.sendOffCoach();
                yield CardEvent.CardType.RED;
            }
        };
        if (cardType != null) {
            context.clock().recordCard();
            context.addEvent(new CoachCardEvent(context.minute(), team.getTeam(), team.getTeam().getCoach(), cardType));
        }
    }

    private static void show(MatchContext context, MatchTeam team, Player player, CardEvent.CardType cardType) {
        context.clock().recordCard();
        context.addEvent(new CardEvent(context.minute(), team.getTeam(), player, cardType));

        // A second yellow counts as a yellow and a red in the statistics
        PlayerMatchStats stats = context.statsOf(player);
        if (cardType != CardEvent.CardType.RED) {
            stats.addYellowCard();
        }
        if (cardType.isSendingOff()) {
            stats.addRedCard();
            context.playerLeaves(player);
        }
        if (cardType.isSendingOff() && player.getFieldRole() == FieldRole.GOALKEEPER) {
            SubstitutionManager.afterGoalkeeperSentOff(context, team);
        }
    }
}
