import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import TeamBadge from '../components/TeamBadge.jsx';
import FormStrip from '../components/FormStrip.jsx';
import Movement from '../components/Movement.jsx';

const RELEGATION_PLACES = 3;
const TOP_PLACES = 4;

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

// The leader's points count up once when the page opens
function CountUp({ value }) {
  const [shown, setShown] = useState(prefersReducedMotion() ? value : 0);
  useEffect(() => {
    if (prefersReducedMotion()) {
      setShown(value);
      return undefined;
    }
    let frame;
    const start = performance.now();
    const duration = 900;
    const tick = (now) => {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      setShown(Math.round(value * eased));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value]);
  return <>{shown}</>;
}

// One mark per round: played, next, still to come
function RoundStrip({ played, total }) {
  return (
    <div className="strip" role="img" aria-label={`${played} of ${total} rounds played`}>
      <div className="strip__marks">
        {Array.from({ length: total }, (_, i) => {
          const state = i < played ? 'played' : i === played ? 'next' : 'later';
          return <span key={i} className={`strip__mark strip__mark--${state}`} style={{ '--i': i }} />;
        })}
      </div>
      <div className="strip__ends">
        <span>Round 1</span>
        <span>Round {total}</span>
      </div>
    </div>
  );
}

function LeaderBoard({ data }) {
  const [leader, second] = data.rows;
  const started = data.roundsPlayed > 0;
  const gap = leader.points - second.points;

  return (
    <section className="board" style={{ '--club': started ? teamAccent(leader.shortName) : 'var(--surface-2)' }}>
      <div className="board__season">
        <h1 className="board__title">{data.season}</h1>
        <p className="board__round">
          {started
            ? `After round ${data.roundsPlayed} of ${data.totalRounds}`
            : 'No round played yet. Simulate round 1 to start the season.'}
        </p>
        <RoundStrip played={data.roundsPlayed} total={data.totalRounds} />
      </div>

      {started && (
        <div className="board__leader">
          <TeamBadge shortName={leader.shortName} size={68} />
          <div className="board__leader-text">
            <span className="board__label">Top of the league</span>
            <span className="board__team">{leader.team}</span>
            <span className="board__gap">
              {gap > 0
                ? `${gap} ${gap === 1 ? 'point' : 'points'} clear of ${second.team}`
                : `Level on points with ${second.team}`}
            </span>
          </div>
          <div className="board__points">
            <CountUp value={leader.points} />
            <span className="board__pts">pts</span>
          </div>
        </div>
      )}
    </section>
  );
}

function zoneOf(position, teams) {
  if (position === 1) return 'champion';
  if (position <= TOP_PLACES) return 'top';
  if (position > teams - RELEGATION_PLACES) return 'down';
  return 'none';
}

function signed(number) {
  return number > 0 ? `+${number}` : String(number);
}

function LeagueTable({ rows }) {
  return (
    <div className="table-card">
      <table className="league">
        <caption className="sr-only">League table</caption>
        <thead>
          <tr>
            <th scope="col" className="league__pos">Pos</th>
            <th scope="col" className="league__club">Club</th>
            <th scope="col" title="Played">P</th>
            <th scope="col" title="Won" className="hide-sm">W</th>
            <th scope="col" title="Drawn" className="hide-sm">D</th>
            <th scope="col" title="Lost" className="hide-sm">L</th>
            <th scope="col" title="Goals for" className="hide-md">GF</th>
            <th scope="col" title="Goals against" className="hide-md">GA</th>
            <th scope="col" title="Goal difference">GD</th>
            <th scope="col" title="Points">Pts</th>
            <th scope="col" className="league__form hide-sm">Last 5</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.teamId} className={`zone-${zoneOf(row.position, rows.length)}`}>
              <td className="league__pos">
                <span className="pos">{row.position}</span>
                <Movement position={row.position} previousPosition={row.previousPosition} />
              </td>
              <th scope="row" className="league__club">
                <TeamBadge shortName={row.shortName} size={30} />
                <Link className="club-name club-link" to={`/teams/${row.teamId}`}>{row.team}</Link>
              </th>
              <td>{row.played}</td>
              <td className="hide-sm">{row.won}</td>
              <td className="hide-sm">{row.drawn}</td>
              <td className="hide-sm">{row.lost}</td>
              <td className="hide-md">{row.goalsFor}</td>
              <td className="hide-md">{row.goalsAgainst}</td>
              <td className={row.goalDifference > 0 ? 'gd-plus' : row.goalDifference < 0 ? 'gd-minus' : ''}>
                {signed(row.goalDifference)}
              </td>
              <td className="league__points">{row.points}</td>
              <td className="league__form hide-sm">
                <FormStrip form={row.form} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="legend">
        <span className="legend__item legend__item--champion">Champion</span>
        <span className="legend__item legend__item--top">Top four</span>
        <span className="legend__item legend__item--down">Relegation</span>
      </div>
    </div>
  );
}

export default function TablePage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.table().then(setData).catch(setError);
  }, []);

  if (error) {
    return (
      <div className="notice" role="alert">
        <h1 className="page-title">The server is not answering</h1>
        <p>Start WebApplication in IntelliJ, then reload this page.</p>
      </div>
    );
  }

  if (!data) {
    return <div className="loading" aria-busy="true">Loading the table…</div>;
  }

  return (
    <>
      <LeaderBoard data={data} />
      <LeagueTable rows={data.rows} />
    </>
  );
}
