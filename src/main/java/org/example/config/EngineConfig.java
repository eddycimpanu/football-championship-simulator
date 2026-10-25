package org.example.config;

public final class EngineConfig {

    private EngineConfig() {
        // constants only, no instances
    }

    // ----- Stoppage time -----
    public static final double STOPPAGE_BASE_MINUTES = 1.0;
    public static final double STOPPAGE_PER_EVENT = 0.5;      // per goal, substitution or card
    public static final double STOPPAGE_PER_INJURY = 1.0;
    public static final int MIN_STOPPAGE_FIRST_HALF = 1;
    public static final int MAX_STOPPAGE_FIRST_HALF = 5;
    public static final int MIN_STOPPAGE_SECOND_HALF = 2;
    public static final int MAX_STOPPAGE_SECOND_HALF = 8;

    // ----- General -----
    public static final double HOME_ADVANTAGE = 1.05;         // good results for the home team x 1.05
    public static final double REFERENCE_ATTRIBUTE = 75;      // an "average" attribute in this league

    // ----- Fitness -----
    public static final double FITNESS_WEIGHT_IN_SELECTION = 0.3;  // a player at 70% condition counts as 91% as good
    public static final int MIN_CONDITION_TO_START = 50;          // below this he starts only if nobody else can
    public static final double ENERGY_DROP_PER_MINUTE = 0.30;     // for a player with average STAMINA
    public static final double GOALKEEPER_ENERGY_FACTOR = 0.3;    // a goalkeeper runs much less
    public static final double FATIGUE_EFFECT = 0.15;             // attributes x (1 - 0.15 x missing energy)
    public static final double HALF_TIME_RECOVERY = 4;            // energy back during the break
    public static final double CONDITION_COST_PER_ENERGY = 0.8;   // condition lost after the match per energy used
    public static final int CONDITION_RECOVERY_PER_ROUND = 20;    // condition back between two rounds

    // ----- Discipline -----
    public static final double AVERAGE_REFEREE_STRICTNESS = 68;   // a referee with this strictness is "normal"
    public static final double CARD_NONE = 80;                   // after a foul: no card / yellow / red
    public static final double CARD_YELLOW = 20;
    public static final double CARD_RED = 0.28;
    public static final double SERIOUS_FOUL_YELLOW_FACTOR = 2;   // a foul that gives a penalty
    public static final double SERIOUS_FOUL_RED_FACTOR = 5;
    public static final double BOOKED_PLAYER_YELLOW_FACTOR = 0.15; // a booked player is careful and gets the benefit of the doubt
    public static final double COACH_NO_CARD = 98.8;             // the coach protests after a foul
    public static final double COACH_YELLOW = 1.0;
    public static final double COACH_RED = 0.1;
    public static final double RED_CARD_FACTOR = 0.90;           // a team with 10 men is 10% weaker
    public static final int MIN_PLAYERS_ON_PITCH = 7;            // below this the match is abandoned

    // ----- Durations in seconds (min, max) -----
    public static final int KICKOFF_MIN = 5, KICKOFF_MAX = 10;
    public static final int GOAL_KICK_MIN = 10, GOAL_KICK_MAX = 25;
    public static final int THROW_IN_MIN = 5, THROW_IN_MAX = 15;
    public static final int DISPOSSESSION_MIN = 0, DISPOSSESSION_MAX = 5;
    public static final int FREE_KICK_MIN = 20, FREE_KICK_MAX = 50;
    public static final int CORNER_MIN = 20, CORNER_MAX = 40;
    public static final int ATTACK_MIN = 15, ATTACK_MAX = 60;
    public static final int CROSS_MIN = 3, CROSS_MAX = 5;
    public static final int FOUL_MIN = 5, FOUL_MAX = 15;
    public static final int PENALTY_MIN = 60, PENALTY_MAX = 90;
    public static final int SHOT_MIN = 2, SHOT_MAX = 4;
    public static final int GOAL_CELEBRATION = 60;

    // ----- Attack: how does the attack resolve? (base weights) -----
    public static final double ATTACK_SHOT = 15;
    public static final double ATTACK_DEFENDER_WINS_BALL = 40;
    public static final double ATTACK_FOUL = 24;
    public static final double ATTACK_OFFSIDE = 5;
    public static final double ATTACK_OUT_FOR_THROW_IN = 12;
    public static final double ATTACK_OUT_FOR_CORNER = 8;
    public static final double ATTACK_OUT_FOR_GOAL_KICK = 6;
    public static final double ATTACK_OWN_GOAL = 0.1;
    public static final double ASSIST_CHANCE = 0.7;           // open-play goals that have an assist
    public static final double THROW_IN_TO_OPPONENT = 0.6;    // the attacker usually touched it last

    // ----- Free kick: shoot, cross or short pass? -----
    public static final double FREE_KICK_SHOOT = 25;
    public static final double FREE_KICK_CROSS = 35;
    public static final double FREE_KICK_SHORT_PASS = 40;
    public static final double DEEP_FREE_KICK_CROSS = 10;     // free kick far from goal (after offside)
    public static final double DEEP_FREE_KICK_SHORT_PASS = 90;

    // ----- Corner: cross or short pass? -----
    public static final double CORNER_CROSS = 85;
    public static final double CORNER_SHORT_PASS = 15;

    // ----- Cross: how does the cross resolve? -----
    public static final double CROSS_HEADER = 35;
    public static final double CROSS_DEFENDED = 55;
    public static final double CROSS_FOUL = 9;
    public static final double CROSS_OWN_GOAL = 0.5;

    // ----- Foul: inside or outside the box? (probability of inside) -----
    public static final double PENALTY_CHANCE_OPEN_PLAY = 0.012;
    public static final double PENALTY_CHANCE_FROM_CROSS = 0.10;

    // ----- Save: what happens to the rebound? -----
    public static final double SAVE_REBOUND_TO_ATTACKER = 20;
    public static final double SAVE_PARRIED_BEHIND = 35;
    public static final double SAVE_CLEARED = 45;
}
