# Football Championship Simulator

A Java console application that simulates a full season of an all-star football league: 20 legendary clubs, 500 players, 38 rounds. Every match is played phase by phase by a probabilistic engine, where each decision depends on the players' attributes, the strength of both teams and luck.

**Tech stack:** Java 21+, Maven, PostgreSQL, plain JDBC with hand-written SQL.

## Features

- Match engine built as a **state machine**: each match is a chain of phases (attack, cross, foul, shot...) following a [decision tree](docs/match-simulation-tree-decision.md).
- **Roulette-wheel decisions**: better players get a bigger slice of the wheel, but nothing is ever certain.
- Stoppage time, fatigue, cards and suspensions, injuries and up to 5 substitutions chosen by the coach.
- Full season with a double round-robin schedule, referee rotation and a league table with head-to-head tie-breaks.
- Team and player statistics, match ratings and man of the match.
- Reproducible seasons: the same seed always gives the same season.

## How the engine works

Every decision is a roulette wheel. Each outcome gets a base weight, the players involved change it with a skill factor `(attacker / defender)^2`, and a random number between 0 and 1 picks the outcome. Team against team, the attack rating (average OVR of the midfielders and forwards) meets the defence rating with an exponent of 9, so stronger sides create clearly more chances.

The weights were calibrated with `CalibrationRunner`, which plays many seasons in memory. Over 30 seasons (11,400 matches) the engine averages 2.9 goals, 26 shots, 4 yellow cards and 0.16 red cards per match, 77% of goals have an assist, and the champion finishes on about 78 points against 21 for the last team, close to real top-flight football.

## OOP highlights

Inheritance (`Person` → `Player`, `Coach`, `Referee`), State pattern (one class per state of the decision tree), Strategy (OVR calculators per position), DAO pattern for the database, immutable records as value objects and generics (`RouletteWheel<T>`, `Repository<T, ID>`).

## Getting started

1. Create a PostgreSQL database `football_cs` with a user `fcs_user` that owns it.
2. Run `src/main/resources/db/schema.sql`, then `seed.sql`.
3. Copy `src/main/resources/db.properties.example` to `db.properties` and fill in your password.
4. Run `org.example.Main` from IntelliJ.

## Sample output

```
=== Barcelona 2-2 Real Madrid (KEY events, referee Bjorn Kuipers) ===
31'     GOAL! Zinedine Zidane (Real Madrid), assist: Luka Modrić [0-1]
45+3'   OWN GOAL by Zinedine Zidane, goal for Barcelona [1-1]
50'     GOAL! Lionel Messi (Barcelona), assist: Sergio Busquets [2-1]
57'     GOAL! Sergio Ramos (Real Madrid) with a header, assist: Dani Carvajal [2-2]
61'     SUBSTITUTION (Real Madrid): Marcelo on, Roberto Carlos off (card risk)
90+5'   FULL-TIME [2-2]
Man of the match: Sergio Ramos (Real Madrid), rating 7.6
```

## About the data

The league is fictional, with legends from different eras. Player attributes are inspired by FIFA / EA FC ratings; they are approximations, not official EA Sports data.
