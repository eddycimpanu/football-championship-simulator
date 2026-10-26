import { Link } from 'react-router-dom';
import { bettorKey } from '../agents.js';
import { formatMoney } from '../betting.jsx';
import { teamAccent } from '../teams.js';
import AgentAvatar from './AgentAvatar.jsx';
import TeamBadge from './TeamBadge.jsx';

// The end of the season in three cards: the champion, the best player and the best bettor.
export default function SeasonFinale({ season, champion, mvp, bestBettor }) {
  if (!champion) return null;
  const profit = bestBettor ? Number(bestBettor.balance) - Number(bestBettor.startingBalance) : 0;
  return (
    <section className="finale" aria-label="Season winners">
      <div className="finale__head">
        <span className="finale__eyebrow">Full time</span>
        <h2 className="finale__title">{season} is over</h2>
      </div>
      <div className="finale__cards">
        <Link className="finale-card" to={`/teams/${champion.teamId}`} style={{ '--club': teamAccent(champion.teamShort) }}>
          <span className="finale-card__label">Champion</span>
          <TeamBadge shortName={champion.teamShort} size={58} />
          <span className="finale-card__name">{champion.team}</span>
          <span className="finale-card__fact">{champion.points} points</span>
        </Link>
        {mvp && (
          <Link className="finale-card" to={`/players/${mvp.playerId}`} style={{ '--club': teamAccent(mvp.teamShort) }}>
            <span className="finale-card__label">Most valuable player</span>
            <TeamBadge shortName={mvp.teamShort} size={58} />
            <span className="finale-card__name">{mvp.player}</span>
            <span className="finale-card__fact">{mvp.averageRating.toFixed(2)} average rating</span>
          </Link>
        )}
        {bestBettor && (
          <Link className="finale-card finale-card--bettor" to="/bettors">
            <span className="finale-card__label">Best bettor</span>
            <AgentAvatar agentKey={bettorKey(bestBettor)} size={58} />
            <span className="finale-card__name">{bestBettor.name}</span>
            <span className="finale-card__fact">
              {formatMoney(bestBettor.balance)} credits ({profit >= 0 ? '+' : ''}{formatMoney(profit)})
            </span>
          </Link>
        )}
      </div>
    </section>
  );
}
