import { useState } from 'react';
import { api } from '../api.js';
import { formatMoney, selectionText, ticketOdds, useBetting } from '../betting.jsx';

const QUICK_STAKES = [10, 25, 50, 100];

// The bet slip: one ticket, one stake. The odds of the selections are multiplied,
// and the ticket only pays if every selection wins.
export default function BetSlip({ round, openTickets }) {
  const { me, slip, stake, setStake, remove, clearRound, refresh } = useBetting();
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState(null);
  const [placed, setPlaced] = useState(false);

  const items = slip.filter((item) => item.round === round);
  const balance = Number(me?.bettor.balance ?? 0);
  const totalOdds = ticketOdds(items.map((i) => i.odds));
  const amount = Number(stake) || 0;
  const tooMuch = amount > balance;
  const invalid = !(amount >= 1);

  async function placeTicket() {
    setPlacing(true);
    setError(null);
    try {
      await api.placeTicket(amount.toFixed(2), items.map((i) => ({ matchId: i.matchId, selection: i.selection })));
      clearRound(round);
      setPlaced(true);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setPlacing(false);
    }
  }

  return (
    <aside className="slip" aria-label="Bet slip">
      <div className="slip__head">
        <h2 className="panel__title">Ticket</h2>
        <span className="slip__balance">{formatMoney(balance)} credits</span>
      </div>

      {items.length === 0 && (
        <p className="slip__empty">
          {placed
            ? 'Ticket placed. Simulate the round to see how it does.'
            : 'Click any odds to add a selection. One selection per match.'}
        </p>
      )}

      <ul className="slip__items">
        {items.map((item) => (
          <li key={item.matchId} className="slip-item">
            <div className="slip-item__top">
              <div>
                <div className="slip-item__pick">{selectionText(item.selection, item.homeTeam, item.awayTeam)}</div>
                <div className="slip-item__match">{item.homeTeam} – {item.awayTeam}</div>
              </div>
              <span className="slip-item__odds">{Number(item.odds).toFixed(2)}</span>
              <button type="button" className="slip-item__remove" onClick={() => remove(item.matchId)}
                aria-label="Remove this selection">×</button>
            </div>
          </li>
        ))}
      </ul>

      {items.length > 0 && (
        <div className="slip__foot">
          <div className="slip__totals">
            <span>{items.length === 1 ? 'Odds' : `Total odds (${items.length} selections)`}</span>
            <strong className="slip__odds">{totalOdds.toFixed(2)}</strong>
          </div>
          <div className="slip-item__stake">
            <label className="stake">
              <span className="sr-only">Stake</span>
              <input type="number" min="1" step="1" inputMode="decimal" value={stake}
                onChange={(e) => setStake(e.target.value)} />
              <span className="stake__unit">credits</span>
            </label>
            <div className="quick-stakes">
              {QUICK_STAKES.map((q) => (
                <button key={q} type="button" onClick={() => setStake(String(q))}>{q}</button>
              ))}
            </div>
          </div>
          <div className="slip__totals slip__totals--win">
            <span>Wins</span>
            <strong className="slip__win">{formatMoney(amount * totalOdds)}</strong>
          </div>
          {items.length > 1 && <p className="slip__rule">Every selection must win. One wrong and the ticket is lost.</p>}
          {tooMuch && <p className="slip-item__error">The stake is more than your {formatMoney(balance)} credits.</p>}
          {error && <p className="slip-item__error" role="alert">{error}</p>}
          <button type="button" className="play-button slip__place" onClick={placeTicket}
            disabled={placing || tooMuch || invalid}>
            {placing ? 'Placing…' : `Place ticket, ${formatMoney(amount)}`}
          </button>
        </div>
      )}

      {openTickets.length > 0 && (
        <div className="slip__open">
          <h3 className="attr-group__title">Your tickets on this round</h3>
          <ul>
            {openTickets.map((t) => (
              <li key={t.id}>
                <span className="slip-open__pick">
                  {t.selections.length === 1
                    ? selectionText(t.selections[0].selection, t.selections[0].homeTeam, t.selections[0].awayTeam)
                    : `${t.selections.length} selections`}
                </span>
                <span className="slip-open__numbers">
                  {formatMoney(t.stake)} at {Number(t.totalOdds).toFixed(2)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </aside>
  );
}
