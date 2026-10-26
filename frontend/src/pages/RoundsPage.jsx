import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import TeamBadge from '../components/TeamBadge.jsx';
import LiveRound from '../components/LiveRound.jsx';

// /rounds opens the next round to play (or the last one when the season is over)
export function CurrentRoundRedirect() {
  const [target, setTarget] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .season()
      .then((season) => setTarget(season.nextRound ?? season.totalRounds))
      .catch(setError);
  }, []);

  if (error) return <ServerDown />;
  if (target == null) return <div className="loading">Loading the season…</div>;
  return <Navigate to={`/rounds/${target}`} replace />;
}

function ServerDown() {
  return (
    <div className="notice" role="alert">
      <h1 className="page-title">The server is not answering</h1>
      <p>Start WebApplication in IntelliJ, then reload this page.</p>
    </div>
  );
}

function RoundPicker({ season, selected }) {
  const selectedRef = useRef(null);

  useEffect(() => {
    selectedRef.current?.scrollIntoView({ block: 'nearest', inline: 'center', behavior: 'smooth' });
  }, [selected]);

  return (
    <nav className="picker" aria-label="Rounds">
      {Array.from({ length: season.totalRounds }, (_, i) => {
        const number = i + 1;
        const state =
          number <= season.roundsPlayed ? 'played' : number === season.nextRound ? 'next' : 'later';
        const isSelected = number === selected;
        return (
          <Link
            key={number}
            ref={isSelected ? selectedRef : null}
            to={`/rounds/${number}`}
            className={`picker__chip picker__chip--${state}${isSelected ? ' picker__chip--selected' : ''}`}
            aria-current={isSelected ? 'page' : undefined}
            aria-label={`Round ${number}`}
          >
            {number}
          </Link>
        );
      })}
    </nav>
  );
}

function outcomeClass(match, side) {
  if (match.homeGoals == null || match.homeGoals === match.awayGoals) return '';
  const homeWon = match.homeGoals > match.awayGoals;
  return (side === 'home') === homeWon ? 'team--won' : 'team--lost';
}

function MatchRow({ match, index, justPlayed }) {
  const played = match.homeGoals != null;
  const Row = played ? Link : 'div';
  const rowProps = played ? { to: `/matches/${match.id}`, 'aria-label': `${match.homeTeam} ${match.homeGoals}-${match.awayGoals} ${match.awayTeam}, open the match` } : {};
  return (
    <li style={{ '--i': index }}>
      <Row className={`fixture${played ? ' fixture--link' : ''}`} {...rowProps}>
      <span className={`fixture__team fixture__team--home ${outcomeClass(match, 'home')}`}>
        <span className="fixture__name">{match.homeTeam}</span>
        <TeamBadge shortName={match.homeShort} size={32} />
      </span>

      <span className={`score${played ? '' : ' score--pending'}${justPlayed ? ' score--reveal' : ''}`}>
        {played ? (
          <>
            <span>{match.homeGoals}</span>
            <span className="score__dash" aria-hidden="true">–</span>
            <span>{match.awayGoals}</span>
          </>
        ) : (
          'vs'
        )}
      </span>

      <span className={`fixture__team fixture__team--away ${outcomeClass(match, 'away')}`}>
        <TeamBadge shortName={match.awayShort} size={32} />
        <span className="fixture__name">{match.awayTeam}</span>
      </span>

      <span className="fixture__referee">{match.referee}</span>
      </Row>
    </li>
  );
}

function BallIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
      <circle cx="10" cy="10" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <path d="M10 5.5l3.4 2.5-1.3 4H7.9L6.6 8z" fill="currentColor" />
      <path d="M10 5.5V1.8M13.4 8l3.6-1.2M12.1 12l2.2 3.1M7.9 12l-2.2 3.1M6.6 8L3 6.8" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  );
}

export default function RoundsPage() {
  const number = Number(useParams().number);
  const navigate = useNavigate();
  const [season, setSeason] = useState(null);
  const [round, setRound] = useState(null);
  const [error, setError] = useState(null);
  const [playing, setPlaying] = useState(false);
  const [playError, setPlayError] = useState(null);
  const [justPlayed, setJustPlayed] = useState(false);
  const [live, setLive] = useState(null);   // the ten match reports while the round plays on screen
  const [replayed, setReplayed] = useState(false);

  useEffect(() => {
    setRound(null);
    setLive(null);
    setReplayed(false);
    setJustPlayed(false);
    setPlayError(null);
    Promise.all([api.season(), api.round(number)])
      .then(([seasonData, roundData]) => {
        setSeason(seasonData);
        setRound(roundData);
      })
      .catch(setError);
  }, [number]);

  async function simulate() {
    setPlaying(true);
    setPlayError(null);
    try {
      const played = await api.playNextRound();
      // The round is already decided and saved; now it is replayed on screen
      const details = await Promise.all(played.matches.map((m) => api.match(m.id)));
      setRound(played);
      setLive(details);
      setReplayed(true);
      setSeason(await api.season());
    } catch {
      setPlayError('The round could not be simulated. Check that WebApplication is running and try again.');
    } finally {
      setPlaying(false);
    }
  }

  const finishLive = useCallback(() => setJustPlayed(true), []);

  if (error) return <ServerDown />;
  if (!season || !round) return <div className="loading">Loading round {number}…</div>;

  const liveNow = live && !justPlayed;
  const status = liveNow ? 'Live' : round.played ? 'Played' : round.next ? 'Next up' : 'Still to come';
  const hasPrevious = number > 1;
  const hasNext = number < season.totalRounds;

  return (
    <>
      <header className="round-head">
        <div>
          <p className="round-head__status">
            <span className={`status-dot status-dot--${liveNow ? 'live' : round.played ? 'played' : round.next ? 'next' : 'later'}`} />
            {status}
          </p>
          <h1 className="round-head__title">Round {number}</h1>
        </div>
        <div className="round-head__arrows">
          <button
            type="button"
            className="arrow"
            onClick={() => navigate(`/rounds/${number - 1}`)}
            disabled={!hasPrevious}
            aria-label="Previous round"
          >
            <svg width="10" height="16" viewBox="0 0 10 16" aria-hidden="true"><path d="M8 2L2 8l6 6" fill="none" stroke="currentColor" strokeWidth="2" /></svg>
          </button>
          <button
            type="button"
            className="arrow"
            onClick={() => navigate(`/rounds/${number + 1}`)}
            disabled={!hasNext}
            aria-label="Next round"
          >
            <svg width="10" height="16" viewBox="0 0 10 16" aria-hidden="true"><path d="M2 2l6 6-6 6" fill="none" stroke="currentColor" strokeWidth="2" /></svg>
          </button>
        </div>
      </header>

      <RoundPicker season={season} selected={number} />

      <div className="round-action">
        {round.next && !live && (
          <button type="button" className="play-button" onClick={simulate} disabled={playing}>
            <BallIcon />
            {playing ? `Playing round ${number}…` : `Simulate round ${number}`}
          </button>
        )}
        {live && justPlayed && (
          <button type="button" className="ghost-button" onClick={() => setLive(null)}>
            Show the results list
          </button>
        )}
        {round.played && justPlayed && season.nextRound && (
          <>
            <Link className="text-link" to={`/rounds/${season.nextRound}`}>
              Go to round {season.nextRound}
            </Link>
            <Link className="text-link text-link--quiet" to="/table">
              See the updated table
            </Link>
          </>
        )}
        {round.played && !justPlayed && !live && <p className="round-action__note">All 10 matches played.</p>}
        {!round.played && !round.next && (
          <p className="round-action__note">
            Not played yet. Next up is round {season.nextRound}.{' '}
            <Link className="text-link" to={`/rounds/${season.nextRound}`}>Go to round {season.nextRound}</Link>
          </p>
        )}
        {round.played && justPlayed && !season.nextRound && (
          <Link className="text-link" to="/table">That was the last round. See the final table</Link>
        )}
        {playError && <p className="round-action__error" role="alert">{playError}</p>}
      </div>

      {live ? (
        <LiveRound details={live} onFinish={finishLive} />
      ) : (
        <ol className="fixtures">
          {round.matches.map((match, i) => (
            <MatchRow key={match.id} match={match} index={i} justPlayed={justPlayed && !replayed} />
          ))}
        </ol>
      )}
    </>
  );
}
