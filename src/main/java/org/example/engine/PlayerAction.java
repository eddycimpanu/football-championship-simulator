package org.example.engine;

import org.example.model.Attribute;
import org.example.model.FieldRole;
import org.example.model.Player;

public enum PlayerAction {
    //          attribute that matters     GK  DEF  MID  FWD
    SHOOT(Attribute.SHOOTING,              0,   1,   3,   5),
    ASSIST(Attribute.PASSING,              0,   1,   4,   3),
    CROSS(Attribute.PASSING,               0,   2,   3,   3),
    HEADER(Attribute.HEADING,              0,   2,   1,   4),
    DEFEND(Attribute.DEFENDING,            0,   5,   3,   1),
    COMMIT_FOUL(Attribute.AGGRESSION,      0,   5,   3,   1),
    WIN_FOUL(Attribute.DRIBBLING,          0,   1,   3,   4),
    RUN_IN_BEHIND(Attribute.PACE,          0,   0,   1,   5),
    CLEAR_INTO_OWN_NET(Attribute.HEADING,  0,   5,   2,   1);

    private final Attribute attribute;
    private final double goalkeeper;
    private final double defender;
    private final double midfielder;
    private final double forward;

    PlayerAction(Attribute attribute, double goalkeeper, double defender, double midfielder, double forward) {
        this.attribute = attribute;
        this.goalkeeper = goalkeeper;
        this.defender = defender;
        this.midfielder = midfielder;
        this.forward = forward;
    }

    public double weightFor(Player player) {
        double roleWeight = switch (player.getFieldRole()) {
            case GOALKEEPER -> goalkeeper;
            case DEFENDER -> defender;
            case MIDFIELDER -> midfielder;
            case FORWARD -> forward;
        };
        return roleWeight * player.getAttributes().get(attribute);
    }

    public double roleWeight(FieldRole fieldRole) {
        return switch (fieldRole) {
            case GOALKEEPER -> goalkeeper;
            case DEFENDER -> defender;
            case MIDFIELDER -> midfielder;
            case FORWARD -> forward;
        };
    }
}
