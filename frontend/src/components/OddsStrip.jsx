import { selectionWins as wins } from '../betting.jsx';

// The odds of one match, in three markets. After the match the winning selections light up.
const GROUPS = [
  [['HOME', '1'], ['DRAW', 'X'], ['AWAY', '2']],
  [['OVER_2_5', 'Over 2.5'], ['UNDER_2_5', 'Under 2.5']],
  [['BOTH_SCORE', 'GG'], ['NOT_BOTH_SCORE', 'NG']],
];

export default function OddsStrip({ odds, homeGoals = null, awayGoals = null, compact = false, onPick, isPicked }) {
  const played = homeGoals != null;
  const groups = compact ? GROUPS.slice(0, 1) : GROUPS;
  return (
    <div className={`odds${compact ? ' odds--compact' : ''}`}>
      {groups.map((group, g) => (
        <div key={g} className="odds__group">
          {group.map(([key, label]) => {
            const won = played && wins(key, homeGoals, awayGoals);
            const state = !played ? '' : won ? ' odd--won' : ' odd--lost';
            const title = `${label}: chance ${Math.round(odds.probabilities[key] * 100)}%`;
            const content = (
              <>
                <span className="odd__label">{label}</span>
                <span className="odd__value">{odds.odds[key].toFixed(2)}</span>
              </>
            );
            if (onPick && !played) {
              const picked = isPicked?.(key);
              return (
                <button key={key} type="button" className={`odd odd--pick${picked ? ' odd--picked' : ''}`}
                  title={title} aria-pressed={picked} onClick={() => onPick(key, odds.odds[key])}>
                  {content}
                </button>
              );
            }
            return <span key={key} className={`odd${state}`} title={title}>{content}</span>;
          })}
        </div>
      ))}
    </div>
  );
}
