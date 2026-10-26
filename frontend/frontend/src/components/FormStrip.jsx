const LABELS = { W: 'Win', D: 'Draw', L: 'Loss' };

// The last five results, oldest on the left.
export default function FormStrip({ form }) {
  return (
    <span className="form" aria-label={`Last results: ${form.map((r) => LABELS[r]).join(', ')}`}>
      {form.map((result, i) => (
        <span key={i} className={`form__dot form__dot--${result}`} title={LABELS[result]}>
          {result}
        </span>
      ))}
    </span>
  );
}
