import { teamColours } from '../teams.js';

// A round badge in the club's colours with its three letters (not the real crest).
export default function TeamBadge({ shortName, size = 28 }) {
  const { bg, ring, ink } = teamColours(shortName);
  return (
    <span
      className="badge"
      style={{
        width: size,
        height: size,
        background: bg,
        color: ink,
        boxShadow: `inset 0 0 0 ${Math.max(2, size / 14)}px ${ring}`,
        fontSize: size * 0.32,
      }}
      aria-hidden="true"
    >
      {shortName}
    </span>
  );
}
