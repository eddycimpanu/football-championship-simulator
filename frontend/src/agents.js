// How each AI agent looks on the page (the server sends its key, name and description).
const STYLES = {
  FAVOURITE: { colour: '#4DA3FF', letter: 'F' },
  DREAMER: { colour: '#C77DFF', letter: 'D' },
  ANALYST: { colour: '#34D27F', letter: 'A' },
  STATISTICIAN: { colour: '#FF8A3D', letter: 'S' },
  STEADY: { colour: '#9FB5A8', letter: 'H' },
};

export function agentStyle(key) {
  return STYLES[key] ?? { colour: '#8FA89B', letter: '?' };
}
