import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import { POSITION_NAMES, STATUS_NAMES, splitName } from '../format.js';
import TeamBadge from '../components/TeamBadge.jsx';
import Rating from '../components/Rating.jsx';
import ServerDown from '../components/ServerDown.jsx';

const GROUPS = [
  ['Attacking', ['pace', 'shooting', 'passing', 'dribbling']],
  ['Defending', ['defending', 'physical', 'heading']],
  ['Goalkeeping', ['reflexes', 'handling']],
  ['Engine', ['stamina', 'aggression']],
];

function level(value) {
  if (value >= 88) return 'elite';
  if (value >= 78) return 'strong';
  if (value >= 62) return 'fair';
  return 'weak';
}

function Attributes({ attributes, isGoalkeeper }) {
  // Goalkeeping first for keepers; outfield players never use it, so it is left out for them
  const groups = isGoalkeeper ? [GROUPS[2], GROUPS[0], GROUPS[1], GROUPS[3]] : [GROUPS[0], GROUPS[1], GROUPS[3]];
  return (
    <section className="panel">
      <h2 className="panel__title">Attributes</h2>
      <div className="attr-groups">
        {groups.map(([group, keys]) => (
          <div key={group} className="attr-group">
            <h3 className="attr-group__title">{group}</h3>
            {keys.map((key) => (
              <div key={key} className="attr">
                <span className="attr__name">{key[0].toUpperCase() + key.slice(1)}</span>
                <span className="attr__bar" aria-hidden="true">
                  <span className={`attr__fill attr__fill--${level(attributes[key])}`} style={{ width: `${attributes[key]}%` }} />
                </span>
                <span className={`attr__value attr__value--${level(attributes[key])}`}>{attributes[key]}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </section>
  );
}

// A small line chart of the match ratings, round by round
function RatingChart({ matches }) {
  const rated = matches.filter((m) => m.rating != null);
  if (rated.length < 2) return null;
  const width = 520;
  const height = 230;
  const pad = { left: 28, right: 10, top: 12, bottom: 28 };
  const maxRound = Math.max(...matches.map((m) => m.round));
  const x = (round) => pad.left + ((round - 1) / Math.max(1, maxRound - 1)) * (width - pad.left - pad.right);
  const y = (rating) => pad.top + ((10 - rating) / 5) * (height - pad.top - pad.bottom); // scale 5 to 10
  const clamp = (r) => Math.max(5, Math.min(10, r));
  const path = rated.map((m, i) => `${i === 0 ? 'M' : 'L'}${x(m.round)} ${y(clamp(m.rating))}`).join(' ');
  const average = rated.reduce((sum, m) => sum + m.rating, 0) / rated.length;

  return (
    <section className="panel">
      <h2 className="panel__title">Rating by match</h2>
      <svg viewBox={`0 0 ${width} ${height}`} className="rating-chart" role="img"
        aria-label={`Match ratings, average ${average.toFixed(2)}`}>
        {[6, 7, 8, 9, 10].map((g) => (
          <g key={g}>
            <line x1={pad.left} x2={width - pad.right} y1={y(g)} y2={y(g)} className="rating-chart__grid" />
            <text x={pad.left - 8} y={y(g) + 4} textAnchor="end" className="rating-chart__label">{g}</text>
          </g>
        ))}
        <line x1={pad.left} x2={width - pad.right} y1={y(average)} y2={y(average)} className="rating-chart__avg" />
        <path d={path} className="rating-chart__line" />
        {rated.map((m) => (
          <circle key={m.round} cx={x(m.round)} cy={y(clamp(m.rating))} r={m.goals > 0 ? 5 : 3.5}
            className={m.goals > 0 ? 'rating-chart__dot rating-chart__dot--goal' : 'rating-chart__dot'}>
            <title>Round {m.round}: {m.rating.toFixed(1)}{m.goals > 0 ? `, ${m.goals} goal${m.goals > 1 ? 's' : ''}` : ''}</title>
          </circle>
        ))}
        <text x={pad.left} y={height - 6} className="rating-chart__label">Round 1</text>
        <text x={width - pad.right} y={height - 6} textAnchor="end" className="rating-chart__label">Round {maxRound}</text>
      </svg>
      <p className="chart-note">
        Dashed line: his average of {average.toFixed(2)}. Bigger dots are matches where he scored.
      </p>
    </section>
  );
}

export default function PlayerPage() {
  const { id } = useParams();
  const [p, setPlayer] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setPlayer(null);
    api.player(id).then(setPlayer).catch(setError);
  }, [id]);

  if (error) return <ServerDown title="This player could not be loaded" />;
  if (!p) return <div className="loading">Loading the player…</div>;

  const { first, last } = splitName(p.name);
  const s = p.season;
  const isGoalkeeper = p.fieldRole === 'GOALKEEPER';
  const tiles = isGoalkeeper
    ? [['Appearances', s.appearances], ['Clean sheets', s.cleanSheets], ['Saves', s.saves], ['Minutes', s.minutes]]
    : [['Appearances', s.appearances], ['Goals', s.goals], ['Assists', s.assists], ['Minutes', s.minutes]];

  return (
    <>
      <section className="profile" style={{ '--club': teamAccent(p.teamShort) }}>
        <span className="profile__shirt" aria-hidden="true">{p.shirtNumber}</span>
        <div className="profile__ovr">
          <span className="profile__ovr-value">{p.overall}</span>
          <span className="profile__ovr-pos">{p.position}</span>
        </div>
        <div className="profile__main">
          {first && <span className="profile__first">{first}</span>}
          <h1 className="profile__last">{last}</h1>
          <Link className="profile__team" to={`/teams/${p.teamId}`}>
            <TeamBadge shortName={p.teamShort} size={26} />
            {p.team}
          </Link>
          <dl className="profile__facts">
            <div><dt>Position</dt><dd>{POSITION_NAMES[p.position]}</dd></div>
            <div><dt>Nationality</dt><dd>{p.nationality}</dd></div>
            <div><dt>Age</dt><dd>{p.age}</dd></div>
            <div><dt>Shirt</dt><dd>{p.shirtNumber}</dd></div>
          </dl>
        </div>
        <div className="profile__status">
          <span className={`status-pill status-pill--${p.status.toLowerCase()}`}>{STATUS_NAMES[p.status]}</span>
          <div className="fitness">
            <div className="fitness__label">
              <span>Fitness</span>
              <span>{p.condition}%</span>
            </div>
            <span className="fitness__bar"><span style={{ width: `${p.condition}%` }} /></span>
          </div>
          {p.yellowCardsTowardsBan > 0 && (
            <p className="profile__note">{p.yellowCardsTowardsBan} of 5 yellow cards towards a ban</p>
          )}
        </div>
      </section>

      <div className="tiles">
        {tiles.map(([label, value]) => (
          <div key={label} className="tile">
            <span className="tile__value">{value}</span>
            <span className="tile__label">{label}</span>
          </div>
        ))}
        <div className="tile">
          <span className="tile__value tile__value--rating">{s.averageRating == null ? '–' : s.averageRating.toFixed(2)}</span>
          <span className="tile__label">Average rating</span>
        </div>
        <div className="tile">
          <span className="tile__value">
            <span className="mini-card mini-card--yellow mini-card--big" /> {s.yellowCards}
            <span className="mini-card mini-card--red mini-card--big" /> {s.redCards}
          </span>
          <span className="tile__label">Cards</span>
        </div>
      </div>

      <div className="player-grid">
        <Attributes attributes={p.attributes} isGoalkeeper={isGoalkeeper} />
        <RatingChart matches={p.matches} />
      </div>

      <section className="panel matchlog">
        <h2 className="panel__title">Match by match</h2>
        {p.matches.length === 0 ? (
          <p className="feed__empty">He has not played yet this season.</p>
        ) : (
          <div className="table-scroll">
          <table className="ratings__table">
            <thead>
              <tr>
                <th scope="col">Round</th>
                <th scope="col" className="ratings__player">Opponent</th>
                <th scope="col">Score</th>
                <th scope="col" title="Minutes" className="hide-sm">Min</th>
                <th scope="col" title="Goals">G</th>
                <th scope="col" title="Assists">A</th>
                <th scope="col">Rating</th>
              </tr>
            </thead>
            <tbody>
              {p.matches.map((m) => (
                <tr key={m.matchId}>
                  <td>{m.round}</td>
                  <th scope="row" className="ratings__player">
                    <span className="ratings__pos">{m.home ? 'vs' : 'at'}</span>
                    <TeamBadge shortName={m.opponentShort} size={22} />
                    <Link className="club-link" to={`/matches/${m.matchId}`}>{m.opponent}</Link>
                  </th>
                  <td>{m.score.replace('-', '–')}</td>
                  <td className="hide-sm">{m.minutes}</td>
                  <td>{m.goals || ''}</td>
                  <td>{m.assists || ''}</td>
                  <td><Rating value={m.rating} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        )}
      </section>
    </>
  );
}
