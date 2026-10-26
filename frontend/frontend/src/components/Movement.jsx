// How many places a team moved since the round before.
export default function Movement({ position, previousPosition }) {
  if (previousPosition == null || previousPosition === position) {
    return <span className="move move--same" aria-label="Same place">–</span>;
  }
  const up = previousPosition > position;
  const places = Math.abs(previousPosition - position);
  return (
    <span
      className={`move ${up ? 'move--up' : 'move--down'}`}
      aria-label={`${up ? 'Up' : 'Down'} ${places} ${places === 1 ? 'place' : 'places'}`}
    >
      <svg width="8" height="6" viewBox="0 0 8 6" aria-hidden="true">
        <path d={up ? 'M4 0L8 6H0z' : 'M4 6L0 0h8z'} fill="currentColor" />
      </svg>
      {places}
    </span>
  );
}
