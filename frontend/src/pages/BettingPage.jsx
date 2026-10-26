import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { formatMoney, selectionText, useBetting } from '../betting.jsx';
import TeamBadge from '../components/TeamBadge.jsx';

function SelectionRow({ sel }) {
  const played = sel.homeGoals != null;
  const status = sel.status.toLowerCase();
  const content = (
    <>
      <span className={`sel-dot sel-dot--${status}`} aria-label={sel.status.toLowerCase()} />
      <span className="bet-row__match">
        <TeamBadge shortName={sel.homeShort} size={20} />
        <span className="bet-row__teams">
          {sel.homeTeam} {played ? `${sel.homeGoals}–${sel.awayGoals}` : 'vs'} {sel.awayTeam}
        </span>
        <TeamBadge shortName={sel.awayShort} size={20} />
      </span>
      <span className="bet-row__pick">{selectionText(sel.selection, sel.homeTeam, sel.awayTeam)}</span>
      <span className="bet-row__odds">{Number(sel.odds).toFixed(2)}</span>
    </>
  );
  return (
    <li>
      {played ? (
        <Link className="sel-row" to={`/matches/${sel.matchId}`}>{content}</Link>
      ) : (
        <div className="sel-row">{content}</div>
      )}
    </li>
  );
}

function TicketCard({ ticket }) {
  const status = ticket.status.toLowerCase();
  const right = ticket.selections.filter((s) => s.status === 'WON').length;
  return (
    <li className={`ticket ticket--${status}`}>
      <div className="ticket__head">
        <span className="ticket__round">Round {ticket.round}</span>
        <span className="ticket__kind">
          {ticket.selections.length === 1 ? 'Single' : `${ticket.selections.length} selections`}
          {ticket.status === 'LOST' && ticket.selections.length > 1 && `, ${right} right`}
        </span>
        <span className="ticket__numbers">
          {formatMoney(ticket.stake)} at <strong>{Number(ticket.totalOdds).toFixed(2)}</strong>
        </span>
        <span className={`bet-status bet-status--${status}`}>
          {ticket.status === 'OPEN' && `To win ${formatMoney(Number(ticket.stake) * Number(ticket.totalOdds))}`}
          {ticket.status === 'WON' && `Won ${formatMoney(ticket.payout)}`}
          {ticket.status === 'LOST' && 'Lost'}
        </span>
      </div>
      <ul className="ticket__selections">
        {ticket.selections.map((sel) => <SelectionRow key={sel.id} sel={sel} />)}
      </ul>
    </li>
  );
}

function TicketList({ title, tickets, empty }) {
  return (
    <section className="panel">
      <h2 className="panel__title">{title}</h2>
      {tickets.length === 0 ? (
        <p className="feed__empty">{empty}</p>
      ) : (
        <ul className="tickets">
          {tickets.map((ticket) => <TicketCard key={ticket.id} ticket={ticket} />)}
        </ul>
      )}
    </section>
  );
}

export default function BettingPage() {
  const { me, refresh } = useBetting();

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (!me) return <div className="loading">Loading your tickets…</div>;

  const { bettor, summary } = me;
  const profit = Number(summary.profit);
  const staked = Number(summary.staked);
  const roi = staked > 0 ? ((Number(summary.returned) - staked) / staked) * 100 : null;

  return (
    <>
      <section className="wallet-hero">
        <div>
          <span className="board__label">Your credits</span>
          <div className="wallet-hero__balance">{formatMoney(bettor.balance)}</div>
          <span className={`wallet-hero__profit ${profit >= 0 ? 'wallet__profit--up' : 'wallet__profit--down'}`}>
            {profit >= 0 ? '+' : ''}{formatMoney(profit)} since the start of the season ({formatMoney(bettor.startingBalance)})
          </span>
        </div>
        <Link className="play-button" to="/rounds">Bet on the next round</Link>
      </section>

      <div className="tiles tiles--five">
        <div className="tile"><span className="tile__value tile__value--win">{summary.won}</span><span className="tile__label">Tickets won</span></div>
        <div className="tile"><span className="tile__value tile__value--loss">{summary.lost}</span><span className="tile__label">Tickets lost</span></div>
        <div className="tile"><span className="tile__value">{summary.open}</span><span className="tile__label">Waiting</span></div>
        <div className="tile"><span className="tile__value">{formatMoney(summary.staked)}</span><span className="tile__label">Staked on settled tickets</span></div>
        <div className="tile">
          <span className={`tile__value ${roi == null ? '' : roi >= 0 ? 'tile__value--win' : 'tile__value--loss'}`}>
            {roi == null ? '–' : `${roi >= 0 ? '+' : ''}${roi.toFixed(1)}%`}
          </span>
          <span className="tile__label">Return on stakes</span>
        </div>
      </div>

      <TicketList title="Waiting for the matches" tickets={me.open} empty="No open tickets. Pick some odds in the next round." />
      <TicketList title="Settled" tickets={me.settled} empty="Your settled tickets will show up here." />
    </>
  );
}
