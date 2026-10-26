import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import TeamBadge from '../components/TeamBadge.jsx';
import ServerDown from '../components/ServerDown.jsx';

function SearchIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true" className="search__icon">
      <circle cx="9.5" cy="9.5" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
      <path d="M15 15l5 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function Leaders({ title, unit, rows }) {
  return (
    <section className="panel leaders">
      <h2 className="panel__title">{title}</h2>
      {rows.length === 0 && <p className="feed__empty">No numbers yet. Play a round first.</p>}
      <ol className="leaders__list">
        {rows.map((row, i) => (
          <li key={row.playerId} className={`leader${i === 0 ? ' leader--first' : ''}`}>
            <span className="leader__rank">{i + 1}</span>
            <TeamBadge shortName={row.teamShort} size={28} />
            <span className="leader__who">
              <Link className="leader__name" to={`/players/${row.playerId}`}>{row.player}</Link>
              <span className="leader__team">{row.team}</span>
            </span>
            <span className="leader__value">
              {row.value}
              <span className="leader__unit">{unit}</span>
            </span>
          </li>
        ))}
      </ol>
    </section>
  );
}

export default function PlayersPage() {
  const [text, setText] = useState('');
  const [results, setResults] = useState(null);
  const [reports, setReports] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.reports().then(setReports).catch(setError);
  }, []);

  // Search while typing, a moment after the last key
  useEffect(() => {
    if (text.trim().length < 2) {
      setResults(null);
      return undefined;
    }
    const timer = setTimeout(() => {
      api.searchPlayers(text.trim()).then(setResults).catch(setError);
    }, 180);
    return () => clearTimeout(timer);
  }, [text]);

  if (error) return <ServerDown />;

  return (
    <>
      <header className="page-head">
        <h1 className="page-title">Players</h1>
        <p className="page-lead">500 legends from twenty clubs. Type a name to open a player.</p>
      </header>

      <label className="search">
        <SearchIcon />
        <span className="sr-only">Search for a player</span>
        <input
          type="search"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Hagi, Zidane, Messi…"
          autoFocus
        />
      </label>

      {results && (
        <section className="results" aria-live="polite">
          {results.length === 0 ? (
            <p className="feed__empty">No player matches “{text.trim()}”.</p>
          ) : (
            <ul className="results__list">
              {results.map((p) => (
                <li key={p.id}>
                  <Link className="result" to={`/players/${p.id}`}>
                    <TeamBadge shortName={p.teamShort} size={34} />
                    <span className="result__who">
                      <span className="result__name">{p.name}</span>
                      <span className="result__meta">{p.team}, {p.nationality}</span>
                    </span>
                    <span className="pos-chip">{p.position}</span>
                    <span className="ovr">{p.overall}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {!results && reports && (
        <>
          <h2 className="section-title">Season leaders after round {reports.roundsPlayed}</h2>
          <div className="leaders-grid">
            <Leaders title="Goals" unit="" rows={reports.topScorers.slice(0, 5).map((r) => ({ ...r, value: r.goals }))} />
            <Leaders title="Assists" unit="" rows={reports.topAssists.slice(0, 5).map((r) => ({ ...r, value: r.assists }))} />
            <Leaders
              title="Average rating"
              unit=""
              rows={reports.bestRatings.slice(0, 5).map((r) => ({ ...r, value: r.averageRating.toFixed(2) }))}
            />
          </div>
        </>
      )}
    </>
  );
}
