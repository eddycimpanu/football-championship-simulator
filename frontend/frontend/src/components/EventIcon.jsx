// A small picture for each kind of match event.
export default function EventIcon({ type }) {
  const box = { width: 18, height: 18, viewBox: '0 0 18 18', 'aria-hidden': true, className: 'event-icon' };
  switch (type) {
    case 'GOAL':
    case 'OWN_GOAL':
      return (
        <svg {...box} className={`event-icon${type === 'OWN_GOAL' ? ' event-icon--own' : ''}`}>
          <circle cx="9" cy="9" r="7.5" fill="currentColor" />
          <path d="M9 5.2l2.9 2.1-1.1 3.4H7.2L6.1 7.3z" fill="var(--bg)" />
        </svg>
      );
    case 'YELLOW_CARD':
    case 'COACH_YELLOW_CARD':
      return <svg {...box}><rect x="5" y="2.5" width="8.5" height="13" rx="1.5" fill="#FFD233" /></svg>;
    case 'RED_CARD':
    case 'COACH_RED_CARD':
      return <svg {...box}><rect x="5" y="2.5" width="8.5" height="13" rx="1.5" fill="#FF4D52" /></svg>;
    case 'SECOND_YELLOW':
      return (
        <svg {...box}>
          <rect x="3" y="2" width="8" height="12" rx="1.5" fill="#FFD233" />
          <rect x="7" y="4.5" width="8" height="12" rx="1.5" fill="#FF4D52" />
        </svg>
      );
    case 'SUBSTITUTION':
      return (
        <svg {...box}>
          <path d="M5 13V4M2.5 6.5L5 4l2.5 2.5" fill="none" stroke="var(--win)" strokeWidth="1.8" />
          <path d="M13 5v9M10.5 11.5L13 14l2.5-2.5" fill="none" stroke="var(--loss)" strokeWidth="1.8" />
        </svg>
      );
    case 'INJURY':
      return (
        <svg {...box}>
          <rect x="2.5" y="2.5" width="13" height="13" rx="3" fill="#FF4D52" />
          <path d="M9 5.5v7M5.5 9h7" stroke="#fff" strokeWidth="2" />
        </svg>
      );
    case 'CORNER':
    case 'OFFSIDE':
      return (
        <svg {...box}>
          <path d="M5 16V2" stroke="currentColor" strokeWidth="1.5" />
          <path d="M5.5 2.5h8l-2 3 2 3h-8z" fill={type === 'OFFSIDE' ? 'var(--amber)' : 'currentColor'} />
        </svg>
      );
    case 'SAVE':
      return (
        <svg {...box}>
          <path d="M9 2l6 2.5v4c0 3.6-2.6 6.4-6 7.5-3.4-1.1-6-3.9-6-7.5v-4z" fill="none" stroke="currentColor" strokeWidth="1.5" />
        </svg>
      );
    case 'FOUL':
      return (
        <svg {...box}>
          <path d="M5 5l8 8M13 5l-8 8" stroke="currentColor" strokeWidth="1.6" />
        </svg>
      );
    default:
      return (
        <svg {...box}>
          <circle cx="9" cy="9" r="3" fill="currentColor" />
        </svg>
      );
  }
}
