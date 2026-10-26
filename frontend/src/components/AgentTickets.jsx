import { formatMoney, pickLabel } from '../betting.jsx';
import AgentAvatar from './AgentAvatar.jsx';

// What the agents bet on a round. Before the round the tickets are open; after it they show won or lost.
export default function AgentTickets({ tickets }) {
  if (tickets.length === 0) return null;
  // One block per agent, in the order the server sent them
  const byAgent = [];
  tickets.forEach((t) => {
    const block = byAgent.find((b) => b.key === t.key);
    if (block) block.tickets.push(t.ticket);
    else byAgent.push({ key: t.key, name: t.name, tickets: [t.ticket] });
  });

  return (
    <section className="panel agents-panel">
      <h2 className="panel__title">What the agents bet</h2>
      <ul className="agent-blocks">
        {byAgent.map((agent) => (
          <li key={agent.key} className="agent-block">
            <AgentAvatar agentKey={agent.key} />
            <div className="agent-block__body">
              <span className="agent-block__name">{agent.name}</span>
              {agent.tickets.map((ticket) => (
                <div key={ticket.id} className={`agent-ticket agent-ticket--${ticket.status.toLowerCase()}`}>
                  <span className="agent-ticket__picks">
                    {ticket.selections.map((s) => (
                      <span key={s.id} className={`agent-pick agent-pick--${s.status.toLowerCase()}`}>
                        {pickLabel(s)}
                      </span>
                    ))}
                  </span>
                  <span className="agent-ticket__money">
                    {formatMoney(ticket.stake)} at {Number(ticket.totalOdds).toFixed(2)}
                    {ticket.status === 'WON' && <strong className="agent-ticket__won"> Won {formatMoney(ticket.payout)}</strong>}
                    {ticket.status === 'LOST' && <strong className="agent-ticket__lost"> Lost</strong>}
                  </span>
                </div>
              ))}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
