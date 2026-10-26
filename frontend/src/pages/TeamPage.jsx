import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { teamAccent } from '../teams.js';
import { ordinal, signed, STATUS_NAMES } from '../format.js';
import TeamBadge from '../components/TeamBadge.jsx';
import FormStrip from '../components/FormStrip.jsx';
import Rating from '../components/Rating.jsx';
import ServerDown from '../components/ServerDown.jsx';

const ROLES = [
  ['GOALKEEPER', 'Goalkeepers'],
  ['DEFENDER', 'Defenders'],
  ['MIDFIELDER', 'Midfielders'],
  ['FORWARD', 'Forwards'],
];

function StatusMark({ status }) {
  if (status === 'AVAILABLE') return null;
  return (
    <span className={`status-mark status-mark--${status.toLowerCase()}`} title={STATUS_NAMES[status]}>
      {status === 'INJURED' ? '+' : ''}
      <span className="sr-only">{STATUS_NAMES[status]}</span>
    </span>
  );
}

function Squad({ squad }) {
  return (
    <section className="panel squad">
      <h2 className="panel__title">Squad</h2>
      <table className="ratings__table squad__table">
        <thead>
          <tr>
            <th scope="col" title="Shirt number">#</th>
            <th scope="col" className="ratings__player">Player</th>
            <th scope="col" className="hide-sm">Age</th>
            <th scope="col">OVR</th>
            <th scope="col" title="Appearances">Apps</th>
            <th scope="col" title="Goals">G</th>
            <th scope="col" title="Assists">A</th>
            <th scope="col" className="hide-sm">Rating</th>
          </tr>
        </thead>
        {ROLES.map(([role, title]) => (
          <tbody key={role}>
            <tr className="squad__group">
              <th colSpan={8} scope="rowgroup">{title}</th>
            </tr>
            {squad.filter((p) => p.fieldRole === role).map((p) => (
              <tr key={p.id} className={p.appearances === 0 ? 'squad__unused' : ''}>
                <td className="squad__shirt">{p.shirtNumber}</td>
                <th scope="row" className="ratings__player">
                  <span className="ratings__pos">{p.position}</span>
                  <Link className="ratings__name club-link" to={`/players/${p.id}`}>{p.name}</Link>
                  <StatusMark status={p.status} />
                </th>
                <td className="hide-sm">{p.age}</td>
                <td><span className="ovr ovr--small">{p.overall}</span></td>
                <td>{p.appearances}</td>
                <td>{p.goals || ''}</td>
                <td>{p.assists || ''}</td>
                <td className="hide-sm"><Rating value={p.averageRating} digits={2} /></td>
              </tr>
            ))}
          </tbody>
        ))}
      </table>
    </section>
  );
}

function Fixtures({ fixtures }) {
  return (
    <section className="panel">
      <h2 className="panel__title">Results and fixtures</h2>
      <ol className="team-fixtures">
        {fixtures.map((f) => {
          const inner = (
            <>
              <span className="team-fixture__round">{f.round}</span>
              <span className="team-fixture__where">{f.home ? 'H' : 'A'}</span>
              <TeamBadge shortName={f.opponentShort} size={24} />
              <span className="team-fixture__opponent">{f.opponent}</span>
              {f.result ? (
                <span className={`result-chip result-chip--${f.result}`}>
                  {f.goalsFor}–{f.goalsAgainst}
                </span>
              ) : (
                <span className="result-chip result-chip--later">vs</span>
              )}
            </>
          );
          return (
            <li key={f.matchId}>
              {f.result ? (
                <Link className="team-fixture team-fixture--link" to={`/matches/${f.matchId}`}>{inner}</Link>
              ) : (
                <div className="team-fixture">{inner}</div>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
}

export default function TeamPage() {
  const { id } = useParams();
  const [team, setTeam] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setTeam(null);
    api.team(id).then(setTeam).catch(setError);
  }, [id]);

  if (error) return <ServerDown title="This club could not be loaded" />;
  if (!team) return <div className="loading">Loading the club…</div>;

  const st = team.standing;
  return (
    <>
      <section className="club-hero" style={{ '--club': teamAccent(team.shortName) }}>
        <TeamBadge shortName={team.shortName} size={104} />
        <div className="club-hero__main">
          <h1 className="club-hero__name">{team.name}</h1>
          <p className="club-hero__ground">{team.stadium}, {team.city}</p>
          <p className="club-hero__coach">
            Coach {team.coach}, playing {team.formation}
          </p>
        </div>
        <div className="club-hero__standing">
          <span className="club-hero__place">{ordinal(st.position)}</span>
          <span className="club-hero__pts">{st.points} points</span>
          {st.form.length > 0 && <FormStrip form={st.form} />}
        </div>
      </section>

      <div className="tiles tiles--five">
        <div className="tile"><span className="tile__value">{st.played}</span><span className="tile__label">Played</span></div>
        <div className="tile"><span className="tile__value tile__value--win">{st.won}</span><span className="tile__label">Won</span></div>
        <div className="tile"><span className="tile__value">{st.drawn}</span><span className="tile__label">Drawn</span></div>
        <div className="tile"><span className="tile__value tile__value--loss">{st.lost}</span><span className="tile__label">Lost</span></div>
        <div className="tile">
          <span className="tile__value">{st.goalsFor}–{st.goalsAgainst}</span>
          <span className="tile__label">Goals, {signed(st.goalDifference)}</span>
        </div>
      </div>

      <div className="team-grid">
        <Squad squad={team.squad} />
        <Fixtures fixtures={team.fixtures} />
      </div>
    </>
  );
}
