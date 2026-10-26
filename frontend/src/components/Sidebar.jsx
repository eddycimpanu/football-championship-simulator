import { Link, NavLink } from 'react-router-dom';
import { formatMoney, useBetting } from '../betting.jsx';

const LINKS = [
  { to: '/table', label: 'League table', ready: true },
  { to: '/rounds', label: 'Rounds', ready: true },
  { to: '/teams', label: 'Clubs', ready: true },
  { to: '/players', label: 'Players', ready: true },
  { to: '/reports', label: 'Season reports', ready: true },
  { to: '/betting', label: 'My bets', ready: true },
  { to: '/bettors', label: 'Bettors', ready: true },
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

function Wallet() {
  const { me } = useBetting();
  if (!me) return null;
  const profit = Number(me.bettor.balance) - Number(me.bettor.startingBalance);
  return (
    <Link className="wallet" to="/betting">
      <span className="wallet__label">Your credits</span>
      <span className="wallet__balance">{formatMoney(me.bettor.balance)}</span>
      <span className={`wallet__profit ${profit >= 0 ? 'wallet__profit--up' : 'wallet__profit--down'}`}>
        {profit >= 0 ? '+' : ''}{formatMoney(profit)} this season
      </span>
    </Link>
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
      <Wallet />
    </aside>
  );
}
