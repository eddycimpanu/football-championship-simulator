package org.example.engine;

import org.example.model.Attribute;
import org.example.model.FieldRole;
import org.example.model.PlayerAttributes;

public enum PlayerAction {
    // Weight on the roulette wheel = role weight x attribute ^ sharpness.
    // A higher sharpness gives the best players a bigger share (playmakers make most assists).
    //          attribute that matters     GK  DEF  MID  FWD  sharpness
    SHOOT(Attribute.SHOOTING,              0,   1,   3,   5,   1),
    ASSIST(Attribute.PASSING,              0,   1,   4,   3,   3),
    CROSS(Attribute.PASSING,               0,   2,   3,   3,   1),
    HEADER(Attribute.HEADING,              0,   2,   1,   4,   1),
    DEFEND(Attribute.DEFENDING,            0,   5,   3,   1,   1),
    COMMIT_FOUL(Attribute.AGGRESSION,      0,   5,   3,   1,   1),
    WIN_FOUL(Attribute.DRIBBLING,          0,   1,   3,   4,   1),
    RUN_IN_BEHIND(Attribute.PACE,          0,   0,   1,   5,   1),
    CLEAR_INTO_OWN_NET(Attribute.HEADING,  0,   5,   2,   1,   1);

    private final Attribute attribute;
    private final double goalkeeper;
    private final double defender;
    private final double midfielder;
    private final double forward;
    private final double sharpness;

    PlayerAction(Attribute attribute, double goalkeeper, double defender, double midfielder, double forward,
                 double sharpness) {
        this.attribute = attribute;
        this.goalkeeper = goalkeeper;
        this.defender = defender;
        this.midfielder = midfielder;
        this.forward = forward;
        this.sharpness = sharpness;
    }

    // Role weight x the attribute that matters for this action (raised to the sharpness)
    public double weightFor(FieldRole fieldRole, PlayerAttributes attributes) {
        return roleWeight(fieldRole) * Math.pow(attributes.get(attribute), sharpness);
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
