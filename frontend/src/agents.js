// How each bettor looks on the page (the server sends the agent's key, name and description).
// The dash pattern is a second clue next to the colour, so the chart lines can be told apart without colour.
const STYLES = {
  HUMAN: { colour: '#FFC23D', letter: 'Y', dash: '' },
  FAVOURITE: { colour: '#4DA3FF', letter: 'F', dash: '' },
  DREAMER: { colour: '#C77DFF', letter: 'D', dash: '7 5' },
  ANALYST: { colour: '#34D27F', letter: 'A', dash: '' },
  STATISTICIAN: { colour: '#FF8A3D', letter: 'S', dash: '2 4' },
  STEADY: { colour: '#9FB5A8', letter: 'H', dash: '10 4 2 4' },
};

export function agentStyle(key) {
  return STYLES[key] ?? { colour: '#8FA89B', letter: '?', dash: '' };
}

// You are HUMAN; an agent is known by its strategy key.
export function bettorKey(bettor) {
  return bettor.kind === 'HUMAN' ? 'HUMAN' : bettor.strategy;
}
