package org.example.engine;

import org.example.engine.random.RandomProvider;
import org.example.engine.random.RouletteWheel;
import org.example.model.Attribute;
import org.example.model.Player;

import java.util.Comparator;
import java.util.List;

public final class PlayerPicker {

    private PlayerPicker() {
        // utility class, no instances
    }

    public static Player pick(List<Player> players, PlayerAction action, RandomProvider random) {
        return pick(players, action, null, random);
    }

    public static Player pick(List<Player> players, PlayerAction action, Player excluded, RandomProvider random) {
        RouletteWheel<Player> wheel = new RouletteWheel<>();
        for (Player player : players) {
            if (player != excluded) {
                wheel.addOption(player, action.weightFor(player));
            }
        }
        return wheel.spin(random);
    }

    public static Player best(List<Player> players, Attribute attribute) {
        return players.stream()
                .max(Comparator.comparingInt(player -> player.getAttributes().get(attribute)))
                .orElseThrow(() -> new IllegalStateException("No players to choose from"));
    }
}
