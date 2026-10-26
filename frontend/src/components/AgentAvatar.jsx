import { agentStyle } from '../agents.js';

export default function AgentAvatar({ agentKey, size = 32 }) {
  const { colour, letter } = agentStyle(agentKey);
  return (
    <span
      className="agent-avatar"
      style={{ width: size, height: size, fontSize: size * 0.46, '--agent': colour }}
      aria-hidden="true"
    >
      {letter}
    </span>
  );
}
