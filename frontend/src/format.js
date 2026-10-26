// Small helpers for showing numbers and names.

export function ordinal(n) {
  const rest = n % 100;
  if (rest >= 11 && rest <= 13) return `${n}th`;
  return `${n}${{ 1: 'st', 2: 'nd', 3: 'rd' }[n % 10] ?? 'th'}`;
}

export function signed(n) {
  return n > 0 ? `+${n}` : String(n);
}

export const POSITION_NAMES = {
  GK: 'Goalkeeper', CB: 'Centre-back', LB: 'Left-back', RB: 'Right-back', LWB: 'Left wing-back',
  RWB: 'Right wing-back', CDM: 'Defensive midfielder', CM: 'Central midfielder', CAM: 'Attacking midfielder',
  LM: 'Left midfielder', RM: 'Right midfielder', LW: 'Left winger', RW: 'Right winger', CF: 'Centre-forward',
  ST: 'Striker',
};

export const STATUS_NAMES = { AVAILABLE: 'Available', SUSPENDED: 'Suspended', INJURED: 'Injured' };

// "Lionel Messi" → { first: "Lionel", last: "Messi" }; one-word names stay as they are
export function splitName(name) {
  const parts = name.split(' ');
  if (parts.length === 1) return { first: '', last: name };
  return { first: parts[0], last: parts.slice(1).join(' ') };
}
