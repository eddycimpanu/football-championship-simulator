import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useBetting } from '../betting.jsx';

function ResetIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M2.5 8a5.5 5.5 0 1 0 1.7-4" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M2.5 1.8v3.4h3.4" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// The "Reset league" button and its confirmation window.
export default function ResetLeague({ onReset }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const { refresh, clearAll } = useBetting();
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => { if (e.key === 'Escape' && !busy) setOpen(false); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, busy]);

  async function reset() {
    setBusy(true);
    setError(null);
    try {
      await api.resetSeason();
      clearAll();
      await refresh();
      setOpen(false);
      navigate('/table');
      onReset?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className="nav__link nav__reset" onClick={() => setOpen(true)}>
        <ResetIcon />
        <span>Reset league</span>
      </button>
      {/* Drawn on <body>, so the window covers the whole page and not only the sidebar */}
      {open && createPortal(
        <div className="modal" role="presentation" onClick={() => !busy && setOpen(false)}>
          <div
            className="modal__card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reset-title"
            onClick={(e) => e.stopPropagation()}
          >
            <span className="modal__icon"><ResetIcon /></span>
            <h2 id="reset-title" className="modal__title">Start the league again?</h2>
            <p className="modal__text">
              A new season starts from round 1 with a new fixture list. Every player is fresh again,
              and you and the agents get back to 1,000 credits.
            </p>
            <p className="modal__note">The old season stays saved in the database.</p>
            {error && <p className="modal__error">{error}</p>}
            <div className="modal__actions">
              <button type="button" className="modal__cancel" onClick={() => setOpen(false)} disabled={busy}>
                Cancel
              </button>
              <button type="button" className="modal__confirm" onClick={reset} disabled={busy} autoFocus>
                {busy ? 'Starting…' : 'Start a new season'}
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </>
  );
}
