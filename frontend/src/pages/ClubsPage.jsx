import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import { ordinal } from '../format.js';
import TeamBadge from '../components/TeamBadge.jsx';
import ServerDown from '../components/ServerDown.jsx';

export default function ClubsPage() {
  const [teams, setTeams] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.teams().then(setTeams).catch(setError);
  }, []);

  if (error) return <ServerDown />;
  if (!teams) return <div className="loading">Loading the clubs…</div>;

  return (
    <>
      <header className="page-head">
        <h1 className="page-title">Clubs</h1>
        <p className="page-lead">Twenty clubs, each with the legends who made it famous.</p>
      </header>
      <ul className="clubs">
        {teams.map((t) => (
          <li key={t.id}>
            <Link className="club-card" to={`/teams/${t.id}`} style={{ '--club': teamAccent(t.shortName) }}>
              <TeamBadge shortName={t.shortName} size={56} />
              <span className="club-card__name">{t.name}</span>
              <span className="club-card__ground">{t.stadium}, {t.city}</span>
              <span className="club-card__foot">
                <span>{t.coach}, {t.formation}</span>
                <span className="club-card__place">
                  {ordinal(t.position)}
                  <span className="club-card__pts">{t.points} pts</span>
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
