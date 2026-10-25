package org.example.engine;

import org.example.engine.random.RandomProvider;
import org.example.engine.random.RouletteWheel;
import org.example.model.Attribute;
import org.example.model.Player;

import java.util.Comparator;

public final class PlayerPicker {

    private PlayerPicker() {
        // utility class, no instances
    }

    // Picks a player on the pitch with the roulette wheel: role weight x attribute (fatigue included).
    public static Player pick(MatchTeam team, PlayerAction action, RandomProvider random) {
        return pick(team, action, null, random);
    }

    public static Player pick(MatchTeam team, PlayerAction action, Player excluded, RandomProvider random) {
        RouletteWheel<Player> wheel = new RouletteWheel<>();
        for (Player player : team.getOnPitch()) {
            if (player != excluded) {
                wheel.addOption(player, action.weightFor(player.getFieldRole(), team.attributesOf(player)));
            }
        }
        return wheel.spin(random);
    }

    // The outfield player with the highest attribute right now (free-kick and penalty taker).
    public static Player best(MatchTeam team, Attribute attribute) {
        return team.getOutfieldPlayers().stream()
                .max(Comparator.comparingInt(player -> team.attributesOf(player).get(attribute)))
                .orElseThrow(() -> new IllegalStateException(team.getName() + " has no outfield players"));
    }
}
