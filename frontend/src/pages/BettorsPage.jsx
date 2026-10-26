import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { agentStyle, bettorKey } from '../agents.js';
import { formatMoney } from '../betting.jsx';
import AgentAvatar from '../components/AgentAvatar.jsx';
import BalanceChart from '../components/BalanceChart.jsx';
import SeasonFinale from '../components/SeasonFinale.jsx';
import ServerDown from '../components/ServerDown.jsx';
import { TicketCard } from './BettingPage.jsx';

const percent = (value) => (value == null ? '–' : `${Math.round(value * 100)}%`);
const signedPercent = (value) => (value == null ? '–' : `${value >= 0 ? '+' : ''}${(value * 100).toFixed(1)}%`);
const signedMoney = (value) => `${Number(value) >= 0 ? '+' : ''}${formatMoney(value)}`;
const upDown = (value) => (value == null || Number(value) === 0 ? '' : Number(value) > 0 ? 'up' : 'down');

const PLACES = ['1st', '2nd', '3rd'];

function Podium({ bettors }) {
  // Second, first, third: the winner stands in the middle
  const top = bettors.slice(0, 3);
  const order = [top[1], top[0], top[2]].filter(Boolean);
  return (
    <div className="podium">
      {order.map((s) => {
        const place = top.indexOf(s);
        const key = bettorKey(s.bettor);
        return (
          <div
            key={key}
            className={`podium__step podium__step--${place + 1}${key === 'HUMAN' ? ' podium__step--you' : ''}`}
            style={{ '--agent': agentStyle(key).colour }}
          >
            <AgentAvatar agentKey={key} size={place === 0 ? 64 : 50} />
            <span className="podium__name">{s.bettor.name}</span>
            <span className="podium__balance">{formatMoney(s.bettor.balance)}</span>
            <span className={`podium__profit ${upDown(s.profit)}`}>{signedMoney(s.profit)}</span>
            <span className="podium__place">{PLACES[place]}</span>
          </div>
        );
      })}
    </div>
  );
}

function History({ bettorId }) {
  const [tickets, setTickets] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.bettorTickets(bettorId).then(setTickets).catch(setError);
  }, [bettorId]);

  if (error) return <p className="feed__empty">{error.message}</p>;
  if (!tickets) return <p className="feed__empty">Loading the tickets…</p>;
  if (tickets.length === 0) return <p className="feed__empty">No tickets yet.</p>;
  return (
    <ul className="tickets standing__tickets">
      {tickets.map((ticket) => <TicketCard key={ticket.id} ticket={ticket} />)}
    </ul>
  );
}

function StandingRow({ stats, rank, open, onToggle, highlighted, onHighlight }) {
  const key = bettorKey(stats.bettor);
  const you = key === 'HUMAN';
  const settled = stats.won + stats.lost;
  return (
    <li
      className={`standing${you ? ' standing--you' : ''}${highlighted ? ' standing--lit' : ''}${open ? ' standing--open' : ''}`}
      style={{ '--agent': agentStyle(key).colour }}
      onMouseEnter={() => onHighlight(key)}
      onMouseLeave={() => onHighlight(null)}
    >
      <button type="button" className="standing__row" onClick={onToggle} aria-expanded={open}>
        <span className="standing__rank">{rank}</span>
        <AgentAvatar agentKey={key} size={40} />
        <span className="standing__who">
          <span className="standing__name">
            {stats.bettor.name}
            {you && <span className="standing__tag">You</span>}
          </span>
          <span className="standing__about">{stats.description}</span>
        </span>
        <span className="standing__stat standing__stat--balance">
          <span className="standing__value">{formatMoney(stats.bettor.balance)}</span>
          <span className={`standing__sub ${upDown(stats.profit)}`}>{signedMoney(stats.profit)}</span>
        </span>
        <span className="standing__stat">
          <span className="standing__value">{percent(stats.winRate)}</span>
          <span className="standing__sub">{stats.won} of {settled} won</span>
        </span>
        <span className="standing__stat">
          <span className={`standing__value ${upDown(stats.returnOnStakes)}`}>{signedPercent(stats.returnOnStakes)}</span>
          <span className="standing__sub">return on stakes</span>
        </span>
        <span className="standing__stat standing__stat--best">
          <span className="standing__value">{stats.biggestWin == null ? '–' : formatMoney(stats.biggestWin)}</span>
          <span className="standing__sub">
            {stats.biggestWin == null ? 'no win yet' : `best win, odds ${Number(stats.biggestWinOdds).toFixed(2)}`}
          </span>
        </span>
        <span className="standing__chevron" aria-hidden="true" />
      </button>
      {open && (
        <div className="standing__history">
          <div className="standing__money">
            <span>Staked <strong>{formatMoney(stats.staked)}</strong></span>
            <span>Returned <strong>{formatMoney(stats.returned)}</strong></span>
            <span>Won <strong>{stats.won} of {settled}</strong> tickets</span>
            <span>Return on stakes <strong>{signedPercent(stats.returnOnStakes)}</strong></span>
            {stats.open > 0 && <span>Waiting <strong>{stats.open}</strong> {stats.open === 1 ? 'ticket' : 'tickets'}</span>}
          </div>
          <History bettorId={stats.bettor.id} />
        </div>
      )}
    </li>
  );
}

export default function BettorsPage() {
  const [data, setData] = useState(null);
  const [mvp, setMvp] = useState(null);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(null);
  const [highlight, setHighlight] = useState(null);

  useEffect(() => {
    api.bettors().then((d) => {
      setData(d);
      if (d.finished) api.reports().then((r) => setMvp(r.mvp)).catch(() => {});
    }).catch(setError);
  }, []);

  if (error) return <ServerDown />;
  if (!data) return <div className="loading">Loading the bettors…</div>;

  const { bettors, roundsPlayed } = data;
  const youAt = bettors.findIndex((s) => s.bettor.kind === 'HUMAN') + 1;
  const startingBalance = Number(bettors[0].bettor.startingBalance);

  return (
    <>
      <header className="page-head">
        <h1 className="page-title">Bettors</h1>
        <p className="page-lead">
          You against five betting agents. Everyone started the season with {formatMoney(startingBalance)} credits
          {roundsPlayed > 0
            ? <>; after round {roundsPlayed} you are <strong className="lead-strong">{PLACES[youAt - 1] ?? `${youAt}th`} of {bettors.length}</strong>.</>
            : '.'}
        </p>
      </header>

      {data.finished && (
        <SeasonFinale season={data.season} champion={data.champion} mvp={mvp} bestBettor={bettors[0].bettor} />
      )}

      <Podium bettors={bettors} />

      <section className="panel">
        <div className="panel__head">
          <h2 className="panel__title">Credits round by round</h2>
          <span className="panel__note">The dashed line is the {formatMoney(startingBalance)} everyone started with</span>
        </div>
        {roundsPlayed === 0 ? (
          <p className="feed__empty">
            The lines start after the first round. <Link className="text-link" to="/rounds">Bet on round 1</Link>
          </p>
        ) : (
          <BalanceChart
            bettors={bettors}
            roundsPlayed={roundsPlayed}
            startingBalance={startingBalance}
            highlight={highlight}
            onHighlight={setHighlight}
          />
        )}
      </section>

      <section className="panel">
        <div className="panel__head">
          <h2 className="panel__title">The table</h2>
          <span className="panel__note">Open a bettor to see every ticket</span>
        </div>
        <ol className="standings">
          {bettors.map((s, i) => {
            const key = bettorKey(s.bettor);
            return (
              <StandingRow
                key={key}
                stats={s}
                rank={i + 1}
                open={open === key}
                onToggle={() => setOpen(open === key ? null : key)}
                highlighted={highlight === key}
                onHighlight={setHighlight}
              />
            );
          })}
        </ol>
      </section>
    </>
  );
}
