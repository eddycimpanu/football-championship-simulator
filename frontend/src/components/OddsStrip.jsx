// The odds of one match, in three markets. After the match the winning selections light up.
const GROUPS = [
  [['HOME', '1'], ['DRAW', 'X'], ['AWAY', '2']],
  [['OVER_2_5', 'Over 2.5'], ['UNDER_2_5', 'Under 2.5']],
  [['BOTH_SCORE', 'GG'], ['NOT_BOTH_SCORE', 'NG']],
];

function wins(selection, home, away) {
  switch (selection) {
    case 'HOME': return home > away;
    case 'DRAW': return home === away;
    case 'AWAY': return home < away;
    case 'OVER_2_5': return home + away > 2;
    case 'UNDER_2_5': return home + away <= 2;
    case 'BOTH_SCORE': return home > 0 && away > 0;
    default: return home === 0 || away === 0;
  }
}

export default function OddsStrip({ odds, homeGoals = null, awayGoals = null, compact = false }) {
  const played = homeGoals != null;
  const groups = compact ? GROUPS.slice(0, 1) : GROUPS;
  return (
    <div className={`odds${compact ? ' odds--compact' : ''}`}>
      {groups.map((group, g) => (
        <div key={g} className="odds__group">
          {group.map(([key, label]) => {
            const won = played && wins(key, homeGoals, awayGoals);
            const state = !played ? '' : won ? ' odd--won' : ' odd--lost';
            return (
              <span
                key={key}
                className={`odd${state}`}
                title={`${label}: chance ${Math.round(odds.probabilities[key] * 100)}%`}
              >
                <span className="odd__label">{label}</span>
                <span className="odd__value">{odds.odds[key].toFixed(2)}</span>
              </span>
            );
          })}
        </div>
      ))}
    </div>
  );
}
