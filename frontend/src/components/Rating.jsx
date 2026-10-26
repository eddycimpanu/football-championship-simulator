// The colour of a match rating: green for great games, red for poor ones.
export function ratingClass(rating) {
  if (rating == null) return 'rating--none';
  if (rating >= 8) return 'rating--great';
  if (rating >= 7) return 'rating--good';
  if (rating >= 6) return 'rating--ok';
  return 'rating--poor';
}

export default function Rating({ value, digits = 1 }) {
  return <span className={`rating ${ratingClass(value)}`}>{value == null ? '–' : value.toFixed(digits)}</span>;
}
