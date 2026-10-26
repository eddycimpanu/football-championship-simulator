import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from './api.js';

// Your balance, your bets and the bet slip, shared by every page.
const BettingContext = createContext(null);

const money = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const formatMoney = (value) => money.format(Number(value ?? 0));

export function selectionText(selection, homeTeam, awayTeam) {
  return {
    HOME: `${homeTeam} to win`,
    DRAW: 'Draw',
    AWAY: `${awayTeam} to win`,
    OVER_2_5: 'Over 2.5 goals',
    UNDER_2_5: 'Under 2.5 goals',
    BOTH_SCORE: 'Both teams score',
    NOT_BOTH_SCORE: 'Not both teams score',
  }[selection];
}

// The same rule as the server's Selection.wins
export function selectionWins(selection, home, away) {
  switch (selection) {
    case 'HOME': return home > away;
    case 'DRAW': return home === away;
    case 'AWAY': return home < away;
    case 'OVER_2_5': return home + away > 2;
    case 'UNDER_2_5': return home + away <= 2;
    case 'BOTH_SCORE': return home > 0 && away > 0;
    default: return home === 0 || away === 0;
  }
}

export const SHORT_LABELS = {
  HOME: '1', DRAW: 'X', AWAY: '2', OVER_2_5: 'Over 2.5', UNDER_2_5: 'Under 2.5', BOTH_SCORE: 'GG', NOT_BOTH_SCORE: 'NG',
};

// The odds of the selections multiplied, rounded like the server does
export function ticketOdds(odds) {
  return Math.round(odds.reduce((product, o) => product * Number(o), 1) * 100) / 100;
}

export function BettingProvider({ children }) {
  const [me, setMe] = useState(null);
  const [slip, setSlip] = useState([]);   // [{ matchId, selection, odds, homeTeam, awayTeam, round }]
  const [stake, setStake] = useState('10'); // one stake for the whole ticket

  const refresh = useCallback(() => api.bettingMe().then(setMe).catch(() => setMe(null)), []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // One selection per match: clicking the same odds again removes it, other odds of the match replace it
  const toggle = useCallback((pick) => {
    setSlip((items) => {
      const same = items.find((i) => i.matchId === pick.matchId);
      if (same && same.selection === pick.selection) return items.filter((i) => i.matchId !== pick.matchId);
      if (same) return items.map((i) => (i.matchId === pick.matchId ? pick : i));
      return [...items, pick];
    });
  }, []);

  const value = useMemo(
    () => ({
      me,
      refresh,
      slip,
      toggle,
      stake,
      setStake,
      remove: (matchId) => setSlip((items) => items.filter((i) => i.matchId !== matchId)),
      clearRound: (round) => setSlip((items) => items.filter((i) => i.round !== round)),
      isPicked: (matchId, selection) => slip.some((i) => i.matchId === matchId && i.selection === selection),
    }),
    [me, refresh, slip, toggle, stake],
  );

  return <BettingContext.Provider value={value}>{children}</BettingContext.Provider>;
}

export function useBetting() {
  return useContext(BettingContext);
}
