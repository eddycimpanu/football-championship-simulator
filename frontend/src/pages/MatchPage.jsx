import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import TeamBadge from '../components/TeamBadge.jsx';
import EventIcon from '../components/EventIcon.jsx';

const PERIOD_EVENTS = ['KICK_OFF', 'HALF_TIME', 'SECOND_HALF', 'FULL_TIME'];
const PERIOD_LABELS = {
  KICK_OFF: 'Kick-off',
  HALF_TIME: 'Half-time',
  SECOND_HALF: 'Second half',
  FULL_TIME: 'Full time',
};

function minuteText(event) {
  return event.addedTime > 0 ? `${event.minute}+${event.addedTime}'` : `${event.minute}'`;
}

// "GOAL! Lionel Messi (Barcelona), assist: Xavi [0-3]" → text "Lionel Messi, assist: Xavi", score "0-3"
function splitDescription(event, match) {
  const score = event.description.match(/\[(\d+-\d+)\]\s*$/)?.[1] ?? null;
  const text = event.description
    .replace(/\s*\[\d+-\d+\]\s*$/, '')
    .replace(/^GOAL!\s*/, '')
    .replaceAll(` (${match.homeTeam})`, '')
    .replaceAll(` (${match.awayTeam})`, '')
    // "YELLOW CARD: Hagi" → "Hagi"; the icon already says what happened
    .replace(/^[A-Z][A-Z -]+:\s*/, '');
  return { text, score };
}

// "Lionel Messi 31' 40' 70'" for each scorer of one team
function scorersOf(events, side) {
  const byPlayer = new Map();
  events
    .filter((e) => (e.type === 'GOAL' || e.type === 'OWN_GOAL') && e.side === side)
    .forEach((e) => {
      const name = e.type === 'OWN_GOAL' ? `${e.player} (own goal)` : e.player;
      byPlayer.set(name, [...(byPlayer.get(name) ?? []), minuteText(e)]);
    });
  return [...byPlayer.entries()];
}

function halfTimeScore(events) {
  const halfTime = events.find((e) => e.type === 'HALF_TIME');
  return halfTime?.description.match(/\[(\d+-\d+)\]/)?.[1] ?? null;
}

function Scoreboard({ data }) {
  const { match, events } = data;
  const halfTime = halfTimeScore(events);
  const homeScorers = scorersOf(events, 'home');
  const awayScorers = scorersOf(events, 'away');

  return (
    <section
      className="scoreboard"
      style={{ '--home': teamAccent(match.homeShort), '--away': teamAccent(match.awayShort) }}
    >
      <div className="scoreboard__team scoreboard__team--home">
        <TeamBadge shortName={match.homeShort} size={76} />
        <h2 className="scoreboard__name">{match.homeTeam}</h2>
        <ul className="scorers">
          {homeScorers.map(([name, minutes]) => (
            <li key={name}>{name} {minutes.join(' ')}</li>
          ))}
        </ul>
      </div>

      <div className="scoreboard__middle">
        <div className="scoreboard__score" aria-label={`${match.homeGoals} to ${match.awayGoals}`}>
          <span>{match.homeGoals}</span>
          <span className="scoreboard__dash">–</span>
          <span>{match.awayGoals}</span>
        </div>
        <div className="scoreboard__status">Full time</div>
        {halfTime && <div className="scoreboard__half">Half-time {halfTime.replace('-', '–')}</div>}
      </div>

      <div className="scoreboard__team scoreboard__team--away">
        <TeamBadge shortName={match.awayShort} size={76} />
        <h2 className="scoreboard__name">{match.awayTeam}</h2>
        <ul className="scorers">
          {awayScorers.map(([name, minutes]) => (
            <li key={name}>{name} {minutes.join(' ')}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function Timeline({ events, match }) {
  return (
    <ol className="timeline">
      {events.map((event, i) => {
        if (PERIOD_EVENTS.includes(event.type)) {
          const { score } = splitDescription(event, match);
          return (
            <li key={i} className="timeline__period">
              <span>
                {PERIOD_LABELS[event.type]}
                {score && (event.type === 'HALF_TIME' || event.type === 'FULL_TIME') && <strong> {score.replace('-', '–')}</strong>}
              </span>
            </li>
          );
        }
        const { text, score } = splitDescription(event, match);
        const isGoal = event.type === 'GOAL' || event.type === 'OWN_GOAL';
        const content = (
          <div className={`moment${isGoal ? ' moment--goal' : ''}${event.key ? '' : ' moment--minor'}`}>
            <EventIcon type={event.type} />
            <span className="moment__text">{text}</span>
            {isGoal && score && <span className="moment__score">{score.replace('-', '–')}</span>}
          </div>
        );
        return (
          <li key={i} className="timeline__row">
            <div className="timeline__side timeline__side--home">{event.side === 'home' && content}</div>
            <div className="timeline__minute">{minuteText(event)}</div>
            <div className="timeline__side timeline__side--away">{event.side !== 'home' && content}</div>
          </li>
        );
      })}
    </ol>
  );
}

const STATS = [
  ['Possession', 'possessionPercent', '%'],
  ['Shots', 'shots'],
  ['Shots on target', 'shotsOnTarget'],
  ['Corners', 'corners'],
  ['Fouls', 'fouls'],
  ['Offsides', 'offsides'],
  ['Saves', 'saves'],
  ['Yellow cards', 'yellowCards'],
  ['Red cards', 'redCards'],
];

function StatsPanel({ home, away, match }) {
  return (
    <section
      className="panel stats"
      style={{ '--home': teamAccent(match.homeShort), '--away': teamAccent(match.awayShort) }}
    >
      <h2 className="panel__title">Match stats</h2>
      {STATS.map(([label, key, unit = '']) => {
        const h = home[key];
        const a = away[key];
        const total = h + a;
        const homeShare = total === 0 ? 0 : (h / total) * 100;
        const awayShare = total === 0 ? 0 : (a / total) * 100;
        return (
          <div key={key} className="stat">
            <div className="stat__numbers">
              <span className={h > a ? 'stat__lead' : ''}>{h}{unit}</span>
              <span className="stat__label">{label}</span>
              <span className={a > h ? 'stat__lead' : ''}>{a}{unit}</span>
            </div>
            <div className="stat__bars" aria-hidden="true">
              <span className="stat__bar stat__bar--home"><span style={{ width: `${homeShare}%` }} /></span>
              <span className="stat__bar stat__bar--away"><span style={{ width: `${awayShare}%` }} /></span>
            </div>
          </div>
        );
      })}
    </section>
  );
}

function ratingClass(rating) {
  if (rating == null) return 'rating--none';
  if (rating >= 8) return 'rating--great';
  if (rating >= 7) return 'rating--good';
  if (rating >= 6) return 'rating--ok';
  return 'rating--poor';
}

function PlayerRatings({ team, shortName, players, manOfTheMatch }) {
  return (
    <section className="panel ratings">
      <h2 className="panel__title ratings__title">
        <TeamBadge shortName={shortName} size={24} />
        {team}
      </h2>
      <table className="ratings__table">
        <thead>
          <tr>
            <th scope="col" className="ratings__player">Player</th>
            <th scope="col" title="Minutes">Min</th>
            <th scope="col" title="Goals">G</th>
            <th scope="col" title="Assists">A</th>
            <th scope="col">Rating</th>
          </tr>
        </thead>
        <tbody>
          {players.map((p) => (
            <tr key={p.player} className={p.starter ? '' : 'ratings__sub'}>
              <th scope="row" className="ratings__player">
                <span className="ratings__pos">{p.position}</span>
                <span className="ratings__name">{p.player}</span>
                {p.player === manOfTheMatch && (
                  <span className="motm" title="Man of the match" aria-label="Man of the match">★</span>
                )}
                {p.yellowCards > 0 && <span className="mini-card mini-card--yellow" title="Yellow card" />}
                {p.redCards > 0 && <span className="mini-card mini-card--red" title="Red card" />}
              </th>
              <td>{p.minutes}</td>
              <td>{p.goals || ''}</td>
              <td>{p.assists || ''}</td>
              <td>
                <span className={`rating ${ratingClass(p.rating)}`}>
                  {p.rating == null ? '–' : p.rating.toFixed(1)}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default function MatchPage() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [showAll, setShowAll] = useState(false);

  useEffect(() => {
    setData(null);
    api.match(id).then(setData).catch(setError);
  }, [id]);

  const events = useMemo(
    () => (data ? data.events.filter((e) => showAll || e.key) : []),
    [data, showAll],
  );

  if (error) {
    return (
      <div className="notice" role="alert">
        <h1 className="page-title">This match could not be loaded</h1>
        <p>Check that WebApplication is running, then reload the page.</p>
      </div>
    );
  }
  if (!data) return <div className="loading">Loading the match…</div>;

  const { match } = data;
  if (!match.played) {
    return (
      <div className="notice">
        <h1 className="page-title">{match.homeTeam} vs {match.awayTeam}</h1>
        <p>
          This match is in round {match.round} and has not been played yet.{' '}
          <Link className="text-link" to={`/rounds/${match.round}`}>Go to round {match.round}</Link>
        </p>
      </div>
    );
  }

  return (
    <>
      <p className="crumbs">
        <Link className="text-link text-link--quiet" to={`/rounds/${match.round}`}>Round {match.round}</Link>
        <span>Referee {match.referee}</span>
      </p>

      <Scoreboard data={data} />

      <div className="match-grid">
        <section className="panel">
          <div className="panel__head">
            <h2 className="panel__title">Match events</h2>
            <div className="segmented" role="group" aria-label="Which events to show">
              <button type="button" aria-pressed={!showAll} onClick={() => setShowAll(false)}>Key events</button>
              <button type="button" aria-pressed={showAll} onClick={() => setShowAll(true)}>All events</button>
            </div>
          </div>
          <Timeline events={events} match={match} />
        </section>

        <StatsPanel home={data.homeStats} away={data.awayStats} match={match} />
      </div>

      <div className="ratings-grid">
        <PlayerRatings team={match.homeTeam} shortName={match.homeShort} players={data.homePlayers} manOfTheMatch={data.manOfTheMatch} />
        <PlayerRatings team={match.awayTeam} shortName={match.awayShort} players={data.awayPlayers} manOfTheMatch={data.manOfTheMatch} />
      </div>
    </>
  );
}
