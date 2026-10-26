import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import { splitName } from '../format.js';
import TeamBadge from '../components/TeamBadge.jsx';
import ServerDown from '../components/ServerDown.jsx';

// Where each player of the 4-3-3 stands on the pitch (in %, the attack goes up).
// The report lists them as GK, LB, CB, CB, RB, then three midfielders and three forwards.
const SPOTS = [
  [50, 89],
  [13, 68], [37, 72], [63, 72], [87, 68],
  [24, 45], [50, 49], [76, 45],
  [20, 20], [50, 15], [80, 20],
];

function Pitch({ players }) {
  return (
    <div className="pitch" role="img" aria-label="Team of the season on the pitch">
      <svg className="pitch__lines" viewBox="0 0 100 140" preserveAspectRatio="none" aria-hidden="true">
        <rect x="3" y="3" width="94" height="134" rx="1" />
        <line x1="3" y1="70" x2="97" y2="70" />
        <circle cx="50" cy="70" r="11" />
        <rect x="24" y="3" width="52" height="20" />
        <rect x="38" y="3" width="24" height="7" />
        <rect x="24" y="117" width="52" height="20" />
        <rect x="38" y="130" width="24" height="7" />
      </svg>
      {players.map((p, i) => {
        const [left, top] = SPOTS.at(i) ?? [50, 50];
        return (
          <Link
            key={p.playerId}
            className="token"
            to={`/players/${p.playerId}`}
            style={{ left: `${left}%`, top: `${top}%`, '--club': teamAccent(p.teamShort) }}
          >
            <span className="token__rating">{p.averageRating.toFixed(1)}</span>
            <span className="token__name">{splitName(p.player).last}</span>
            <span className="token__team">
              <TeamBadge shortName={p.teamShort} size={16} />
              {p.position}
            </span>
          </Link>
        );
      })}
    </div>
  );
}

function Board({ title, rows, value, unit, note }) {
  return (
    <section className="panel leaders">
      <h2 className="panel__title">{title}</h2>
      {rows.length === 0 && <p className="feed__empty">Not enough matches yet.</p>}
      <ol className="leaders__list">
        {rows.map((row, i) => (
          <li key={row.playerId} className={`leader${i === 0 ? ' leader--first' : ''}`}>
            <span className="leader__rank">{i + 1}</span>
            <TeamBadge shortName={row.teamShort} size={26} />
            <span className="leader__who">
              <Link className="leader__name" to={`/players/${row.playerId}`}>{row.player}</Link>
              <span className="leader__team">{row.team}</span>
            </span>
            <span className="leader__value">
              {value(row)}
              {unit && <span className="leader__unit">{unit}</span>}
            </span>
          </li>
        ))}
      </ol>
      {note && <p className="chart-note">{note}</p>}
    </section>
  );
}

function MatchList({ title, matches }) {
  return (
    <div className="records__block">
      <h3 className="attr-group__title">{title}</h3>
      <ul className="records__list">
        {matches.map((m) => (
          <li key={m.matchId}>
            <Link className="record" to={`/matches/${m.matchId}`}>
              <span className="record__round">R{m.round}</span>
              <span className="record__team record__team--home">{m.homeTeam}</span>
              <span className="result-chip result-chip--D">{m.homeGoals}–{m.awayGoals}</span>
              <span className="record__team">{m.awayTeam}</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function ReportsPage() {
  const [r, setReports] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.reports().then(setReports).catch(setError);
  }, []);

  if (error) return <ServerDown />;
  if (!r) return <div className="loading">Loading the reports…</div>;

  if (r.roundsPlayed === 0) {
    return (
      <div className="notice">
        <h1 className="page-title">Season reports</h1>
        <p>
          The awards appear after the first round.{' '}
          <Link className="text-link" to="/rounds">Simulate round 1</Link>
        </p>
      </div>
    );
  }

  const mvp = r.mvp;
  return (
    <>
      <header className="page-head">
        <h1 className="page-title">Season reports</h1>
        <p className="page-lead">
          {r.season} after round {r.roundsPlayed}. Awards count players with at least {r.awardMinMinutes} minutes.
        </p>
      </header>

      <div className="awards">
        {mvp && (
          <section className="mvp" style={{ '--club': teamAccent(mvp.teamShort) }}>
            <span className="mvp__label">Most valuable player</span>
            <TeamBadge shortName={mvp.teamShort} size={64} />
            <Link className="mvp__name" to={`/players/${mvp.playerId}`}>{mvp.player}</Link>
            <span className="mvp__team">{mvp.team}, {mvp.position}</span>
            <span className="mvp__rating">{mvp.averageRating.toFixed(2)}</span>
            <span className="mvp__rating-label">average rating</span>
            <dl className="mvp__facts">
              <div><dt>Goals</dt><dd>{mvp.goals}</dd></div>
              <div><dt>Assists</dt><dd>{mvp.assists}</dd></div>
              <div><dt>Matches</dt><dd>{mvp.appearances}</dd></div>
            </dl>
          </section>
        )}
        <section className="panel tots">
          <h2 className="panel__title">Team of the season</h2>
          <Pitch players={r.teamOfTheSeason} />
        </section>
      </div>

      <div className="boards">
        <Board title="Top scorers" rows={r.topScorers} value={(x) => x.goals} />
        <Board title="Most assists" rows={r.topAssists} value={(x) => x.assists} />
        <Board
          title="Best average rating"
          rows={r.bestRatings}
          value={(x) => x.averageRating.toFixed(2)}
          note={`At least ${r.listMinMinutes} minutes played.`}
        />
        <Board
          title="Goalkeepers"
          rows={r.goalkeepers}
          value={(x) => x.cleanSheets}
          unit="clean sheets"
          note={`Sorted by clean sheets, then fewest goals conceded.`}
        />
        <Board
          title="Discipline"
          rows={r.discipline}
          value={(x) => (
            <span className="cards-count">
              <span className="mini-card mini-card--yellow mini-card--big" />{x.yellowCards}
              <span className="mini-card mini-card--red mini-card--big" />{x.redCards}
            </span>
          )}
          note="A red card counts as three yellows."
        />
        <section className="panel records">
          <h2 className="panel__title">Records</h2>
          <MatchList title="Biggest wins" matches={r.biggestWins} />
          <MatchList title="Most goals in a match" matches={r.highestScoring} />
          <div className="records__block">
            <h3 className="attr-group__title">Hat-tricks ({r.hatTricks.length})</h3>
            {r.hatTricks.length === 0 && <p className="feed__empty">None yet.</p>}
            <ul className="records__list">
              {r.hatTricks.slice(0, 6).map((h) => (
                <li key={`${h.matchId}-${h.playerId}`}>
                  <Link className="record" to={`/matches/${h.matchId}`}>
                    <span className="record__round">R{h.round}</span>
                    <TeamBadge shortName={h.teamShort} size={20} />
                    <span className="record__team">{h.player}</span>
                    <span className="record__note">{h.goals} against {h.opponent}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      </div>

      <section className="panel team-stats">
        <h2 className="panel__title">Team statistics</h2>
        <div className="table-scroll">
          <table className="ratings__table">
            <thead>
              <tr>
                <th scope="col" className="ratings__player">Club</th>
                <th scope="col" title="Goals for">GF</th>
                <th scope="col" title="Goals against">GA</th>
                <th scope="col" title="Clean sheets">Clean sheets</th>
                <th scope="col">Shots a game</th>
                <th scope="col">On target</th>
                <th scope="col">Possession</th>
                <th scope="col" title="Yellow cards">Yellow</th>
                <th scope="col" title="Red cards">Red</th>
              </tr>
            </thead>
            <tbody>
              {r.teams.map((t) => (
                <tr key={t.teamId}>
                  <th scope="row" className="ratings__player">
                    <TeamBadge shortName={t.teamShort} size={22} />
                    <Link className="ratings__name club-link" to={`/teams/${t.teamId}`}>{t.team}</Link>
                  </th>
                  <td>{t.goalsFor}</td>
                  <td>{t.goalsAgainst}</td>
                  <td>{t.cleanSheets}</td>
                  <td>{t.shotsPerMatch.toFixed(1)}</td>
                  <td>{t.shotAccuracy}%</td>
                  <td>{t.possession}%</td>
                  <td>{t.yellowCards}</td>
                  <td>{t.redCards}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
