import { useEffect, useMemo, useRef, useState } from 'react';
import TeamBadge from './TeamBadge.jsx';
import EventIcon from './EventIcon.jsx';
import { SHORT_LABELS, formatMoney, selectionWins } from '../betting.jsx';
import AgentAvatar from './AgentAvatar.jsx';

// myBets = your tickets on this round, each with its selections

// Replays a round that has just been simulated: the ten matches run at the same time,
// on one shared clock, and every key event appears at its minute.
//
// The clock counts "match minutes" on one line: the first half (with the longest stoppage time
// of the ten matches), then the second half. At 1x the whole round lasts about 30 seconds.

const SECONDS_PER_ROUND = 30;
const HALF_TIME_PAUSE_MS = 1400;
const SHOWN_TYPES = ['GOAL', 'OWN_GOAL', 'YELLOW_CARD', 'SECOND_YELLOW', 'RED_CARD', 'SUBSTITUTION', 'INJURY'];
const FEED_TYPES = ['GOAL', 'OWN_GOAL', 'SECOND_YELLOW', 'RED_CARD'];

function surname(name) {
  if (!name) return '';
  const parts = name.split(' ');
  return parts.length > 1 ? parts.slice(1).join(' ') : name;
}

function minuteText(event) {
  return event.addedTime > 0 ? `${event.minute}+${event.addedTime}'` : `${event.minute}'`;
}

// Turns every match into a list of events placed on the shared clock
function buildTimeline(details) {
  const firstHalfStoppage = Math.max(
    0,
    ...details.flatMap((d) => d.events.filter((e) => e.type === 'HALF_TIME').map((e) => e.addedTime)),
  );
  const secondHalfStoppage = Math.max(
    0,
    ...details.flatMap((d) => d.events.filter((e) => e.type === 'FULL_TIME').map((e) => e.addedTime)),
  );
  const halfTimeAt = 45 + firstHalfStoppage;
  const end = halfTimeAt + 45 + secondHalfStoppage;

  const at = (event, secondHalf) =>
    secondHalf ? halfTimeAt + (event.minute - 45) + event.addedTime : event.minute + event.addedTime;

  const matches = details.map((detail) => {
    let secondHalf = false;
    const placed = [];
    for (const event of detail.events) {
      if (event.type === 'SECOND_HALF') secondHalf = true;
      placed.push({ ...event, t: at(event, secondHalf && event.type !== 'HALF_TIME') });
    }
    const halfTime = placed.find((e) => e.type === 'HALF_TIME');
    const fullTime = placed.find((e) => e.type === 'FULL_TIME');
    return {
      match: detail.match,
      events: placed.filter((e) => SHOWN_TYPES.includes(e.type)),
      halfTimeT: halfTime ? halfTime.t : 45,
      fullTimeT: fullTime ? fullTime.t : end,
    };
  });
  return { matches, halfTimeAt, end };
}

function clockText(v, halfTimeAt, pausedAtHalfTime, finished) {
  if (finished) return 'FT';
  if (pausedAtHalfTime) return 'HT';
  if (v <= 45) return `${Math.max(1, Math.floor(v))}'`;
  if (v < halfTimeAt) return `45+${Math.ceil(v - 45)}'`;
  const minute = 45 + (v - halfTimeAt);
  if (minute <= 90) return `${Math.floor(minute)}'`;
  return `90+${Math.ceil(minute - 90)}'`;
}

function matchState(m, v, halfTimeAt) {
  const happened = m.events.filter((e) => e.t <= v);
  const goals = happened.filter((e) => e.type === 'GOAL' || e.type === 'OWN_GOAL');
  const home = goals.filter((e) => e.side === 'home').length;
  const away = goals.filter((e) => e.side === 'away').length;
  let status = 'live';
  if (v >= m.fullTimeT) status = 'ft';
  else if (v >= m.halfTimeT && v < halfTimeAt) status = 'ht';
  return { home, away, status, last: happened.at(-1) ?? null, goals: goals.length };
}

function groupByAgent(agentTickets) {
  const groups = [];
  agentTickets.forEach(({ key, name, ticket }) => {
    const group = groups.find((g) => g.key === key);
    if (group) group.tickets.push(ticket);
    else groups.push({ key, name, tickets: [ticket] });
  });
  return groups;
}

export default function LiveRound({ details, onFinish, myBets = [], onBetsSettled, agentTickets = [] }) {
  const timeline = useMemo(() => buildTimeline(details), [details]);
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const [v, setV] = useState(reduced ? timeline.end : 0);
  const [speed, setSpeed] = useState(1);
  const [paused, setPaused] = useState(false);
  const [flashing, setFlashing] = useState({});
  const goalsSeen = useRef({});
  const speedRef = useRef(speed);
  speedRef.current = speed;

  const finished = v >= timeline.end;
  const vRef = useRef(v);
  const halfTimeDone = useRef(false);

  // The clock: requestAnimationFrame, with a short pause at half-time
  useEffect(() => {
    if (finished) return undefined;
    let frame;
    let last = performance.now();
    let pauseUntil = 0;
    const minutesPerMs = timeline.end / (SECONDS_PER_ROUND * 1000);

    const tick = (now) => {
      const elapsed = now - last;
      last = now;
      if (now < pauseUntil) {
        frame = requestAnimationFrame(tick);
        return;
      }
      setPaused(false);
      const current = vRef.current;
      let next = Math.min(timeline.end, current + elapsed * minutesPerMs * speedRef.current);
      if (!halfTimeDone.current && current < timeline.halfTimeAt && next >= timeline.halfTimeAt) {
        halfTimeDone.current = true;
        next = timeline.halfTimeAt;
        pauseUntil = now + HALF_TIME_PAUSE_MS / speedRef.current;
        setPaused(true);
      }
      vRef.current = next;
      setV(next);
      if (next < timeline.end) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [timeline, finished]);

  function skipToEnd() {
    vRef.current = timeline.end;
    setPaused(false);
    setV(timeline.end);
  }

  const states = timeline.matches.map((m) => matchState(m, v, timeline.halfTimeAt));

  // A goal makes its row flash for a moment
  useEffect(() => {
    states.forEach((state, i) => {
      const id = timeline.matches[i].match.id;
      if (state.goals > (goalsSeen.current[id] ?? 0)) {
        goalsSeen.current[id] = state.goals;
        if (!finished) {
          setFlashing((f) => ({ ...f, [id]: Date.now() }));
          setTimeout(() => setFlashing((f) => ({ ...f, [id]: 0 })), 1800);
        }
      }
    });
  });

  useEffect(() => {
    if (!finished) return;
    onFinish();
    if (myBets.length > 0 && onBetsSettled) {
      // How your tickets ended, from the final scores (the server has already paid them)
      let won = 0;
      let returned = 0;
      let right = 0;
      let selections = 0;
      myBets.forEach((ticket) => {
        const results = ticket.selections.map((sel) => isWinning(sel));
        right += results.filter(Boolean).length;
        selections += results.length;
        if (results.every(Boolean)) {
          won++;
          returned += Number(ticket.stake) * Number(ticket.totalOdds);
        }
      });
      const staked = myBets.reduce((sum, t) => sum + Number(t.stake), 0);
      onBetsSettled({ tickets: myBets.length, won, staked, returned, right, selections });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [finished]);

  // Is this selection winning at the current score?
  function isWinning(selection) {
    const i = timeline.matches.findIndex((m) => m.match.id === selection.matchId);
    const s = states[i];
    return s ? selectionWins(selection.selection, s.home, s.away) : false;
  }
  const mySelections = myBets.flatMap((t) => t.selections.map((sel) => ({ ...sel, ticketId: t.id })));

  const feed = timeline.matches
    .flatMap((m) =>
      m.events.filter((e) => FEED_TYPES.includes(e.type) && e.t <= v).map((e) => ({ ...e, match: m.match })),
    )
    .sort((a, b) => b.t - a.t)
    .slice(0, 8);

  const progress = Math.min(100, (v / timeline.end) * 100);

  return (
    <div className="live">
      <div className="live-bar">
        <div className="live-clock" aria-live="off">
          <span className={`live-dot${finished ? ' live-dot--off' : ''}`} />
          <span className="live-clock__time">{clockText(v, timeline.halfTimeAt, paused, finished)}</span>
        </div>
        <div className="live-progress" aria-hidden="true">
          <span style={{ width: `${progress}%` }} />
        </div>
        <div className="live-controls">
          <div className="segmented" role="group" aria-label="Speed">
            <button type="button" aria-pressed={speed === 1} onClick={() => setSpeed(1)}>1x</button>
            <button type="button" aria-pressed={speed === 3} onClick={() => setSpeed(3)}>3x</button>
          </div>
          <button type="button" className="ghost-button" onClick={skipToEnd} disabled={finished}>
            Skip to the end
          </button>
        </div>
      </div>

      <div className="live-grid">
        <ol className="fixtures">
          {timeline.matches.map((m, i) => {
            const s = states[i];
            const { match } = m;
            const leader = s.home === s.away ? null : s.home > s.away ? 'home' : 'away';
            return (
              <li key={match.id}>
                <div className={`fixture fixture--live${flashing[match.id] ? ' fixture--goal' : ''}`}>
                  <span className={`fixture__team fixture__team--home${leader === 'away' ? ' team--lost' : ''}`}>
                    <span className="fixture__name">{match.homeTeam}</span>
                    <TeamBadge shortName={match.homeShort} size={32} />
                  </span>
                  <span className={`score score--live${flashing[match.id] ? ' score--bump' : ''}`}>
                    <span>{s.home}</span>
                    <span className="score__dash" aria-hidden="true">–</span>
                    <span>{s.away}</span>
                  </span>
                  <span className={`fixture__team fixture__team--away${leader === 'home' ? ' team--lost' : ''}`}>
                    <TeamBadge shortName={match.awayShort} size={32} />
                    <span className="fixture__name">{match.awayTeam}</span>
                  </span>
                  {mySelections.some((b) => b.matchId === match.id) && (
                    <span className="live-bets">
                      {mySelections.filter((b) => b.matchId === match.id).map((b) => {
                        const winning = selectionWins(b.selection, s.home, s.away);
                        return (
                          <span key={`${b.ticketId}-${b.id}`}
                            className={`live-bet ${winning ? 'live-bet--winning' : 'live-bet--losing'}`}
                            title={`On your ticket at ${Number(b.odds).toFixed(2)}`}>
                            Your pick: {SHORT_LABELS[b.selection]}
                          </span>
                        );
                      })}
                    </span>
                  )}
                  <span className="fixture__latest">
                    <span className={`match-status match-status--${s.status}`}>
                      {s.status === 'ft' ? 'FT' : s.status === 'ht' ? 'HT' : clockText(v, timeline.halfTimeAt, paused, false)}
                    </span>
                    {s.last && (
                      <span className="latest-event">
                        <EventIcon type={s.last.type} />
                        <span>{minuteText(s.last)} {surname(s.last.player)}</span>
                      </span>
                    )}
                  </span>
                </div>
              </li>
            );
          })}
        </ol>

        <div className="live-side">
        {myBets.length > 0 && (
          <section className="panel live-tickets" aria-label="Your tickets">
            <h2 className="panel__title">{myBets.length === 1 ? 'Your ticket' : 'Your tickets'}</h2>
            {myBets.map((ticket) => {
              const right = ticket.selections.filter((sel) => isWinning(sel)).length;
              const all = right === ticket.selections.length;
              return (
                <div key={ticket.id} className={`live-ticket ${all ? 'live-ticket--up' : 'live-ticket--down'}`}>
                  <div className="live-ticket__count">
                    {right} of {ticket.selections.length}
                    <span>{finished ? (all ? 'Won' : 'Lost') : 'winning right now'}</span>
                  </div>
                  <div className="live-ticket__money">
                    {formatMoney(ticket.stake)} at {Number(ticket.totalOdds).toFixed(2)}
                    <strong>{all ? `Pays ${formatMoney(Number(ticket.stake) * Number(ticket.totalOdds))}` : 'Not paying yet'}</strong>
                  </div>
                </div>
              );
            })}
          </section>
        )}
        {agentTickets.length > 0 && (
          <section className="panel live-agents" aria-label="The agents">
            <h2 className="panel__title">The agents</h2>
            <ul className="live-agents__list">
              {groupByAgent(agentTickets).map(({ key, name, tickets }) => {
                // One ticket: how many of its selections are winning. Several: how many tickets are paying.
                const paying = tickets.filter((t) => t.selections.every((sel) => isWinning(sel))).length;
                const single = tickets.length === 1 ? tickets[0] : null;
                const right = single ? single.selections.filter((sel) => isWinning(sel)).length : paying;
                const total = single ? single.selections.length : tickets.length;
                return (
                  <li key={key} className={`live-agent ${paying > 0 ? 'live-agent--up' : 'live-agent--down'}`}>
                    <AgentAvatar agentKey={key} size={26} />
                    <span className="live-agent__name">{name}</span>
                    <span className="live-agent__count">
                      {right}/{total}
                      {!single && <span className="live-agent__unit"> tickets</span>}
                    </span>
                  </li>
                );
              })}
            </ul>
          </section>
        )}
        <aside className="panel feed" aria-label="Goals and red cards">
          <h2 className="panel__title">Goals and red cards</h2>
          {feed.length === 0 && <p className="feed__empty">Nothing yet. The first goal will show up here.</p>}
          <ol className="feed__list">
            {feed.map((e) => (
              <li key={`${e.match.id}-${e.t}-${e.type}-${e.player}`} className="feed__item">
                <EventIcon type={e.type} />
                <div>
                  <div className="feed__who">
                    {minuteText(e)} {e.player}
                    {e.type === 'OWN_GOAL' && ' (own goal)'}
                  </div>
                  <div className="feed__match">
                    {e.match.homeTeam} – {e.match.awayTeam}
                  </div>
                </div>
              </li>
            ))}
          </ol>
        </aside>
        </div>
      </div>
    </div>
  );
}
