import { NavLink } from 'react-router-dom';

const LINKS = [
  { to: '/table', label: 'League table', ready: true },
  { to: '/rounds', label: 'Rounds', ready: true },
  { to: '/teams', label: 'Clubs', ready: true },
  { to: '/players', label: 'Players', ready: true },
  { to: '/reports', label: 'Season reports', ready: true },
  { to: '/betting', label: 'Betting' },
];

function PitchMark() {
  return (
    <svg width="34" height="24" viewBox="0 0 34 24" aria-hidden="true" className="brand__mark">
      <rect x="1" y="1" width="32" height="22" rx="3" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <line x1="17" y1="1" x2="17" y2="23" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="17" cy="12" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <rect x="1" y="7" width="4" height="10" fill="none" stroke="currentColor" strokeWidth="1.5" />
      <rect x="29" y="7" width="4" height="10" fill="none" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">
        <PitchMark />
        <div>
          <div className="brand__name">All-Star League</div>
          <div className="brand__sub">Championship simulator</div>
        </div>
      </div>
      <nav className="nav" aria-label="Main">
        {LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => `nav__link${isActive ? ' nav__link--active' : ''}`}
          >
            <span>{link.label}</span>
            {!link.ready && <span className="nav__soon">soon</span>}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
