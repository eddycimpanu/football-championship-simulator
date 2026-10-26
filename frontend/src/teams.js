// Club colours for the badges and the stripe beside each team.
// bg = main colour, ring = second colour, ink = the colour of the letters on the badge.
const COLOURS = {
  BAY: { bg: '#DC052D', ring: '#FFFFFF', ink: '#FFFFFF' },
  RMA: { bg: '#F4F4F4', ring: '#FEBE10', ink: '#1E2F6E' },
  MIL: { bg: '#E3000F', ring: '#111111', ink: '#FFFFFF' },
  MCI: { bg: '#6CABDD', ring: '#1C2C5B', ink: '#1C2C5B' },
  MUN: { bg: '#DA291C', ring: '#FBE122', ink: '#FFFFFF' },
  JUV: { bg: '#F4F4F4', ring: '#111111', ink: '#111111' },
  CEL: { bg: '#018749', ring: '#FFFFFF', ink: '#FFFFFF' },
  BAR: { bg: '#A50044', ring: '#004D98', ink: '#FFFFFF' },
  LIV: { bg: '#C8102E', ring: '#00B2A9', ink: '#FFFFFF' },
  BVB: { bg: '#FDE100', ring: '#111111', ink: '#111111' },
  BEN: { bg: '#E30613', ring: '#FFFFFF', ink: '#FFFFFF' },
  POR: { bg: '#0047AB', ring: '#FFFFFF', ink: '#FFFFFF' },
  ATM: { bg: '#CB3524', ring: '#272E61', ink: '#FFFFFF' },
  PSG: { bg: '#004170', ring: '#DA291C', ink: '#FFFFFF' },
  CHE: { bg: '#034694', ring: '#FFFFFF', ink: '#FFFFFF' },
  OLM: { bg: '#2FAEE0', ring: '#FFFFFF', ink: '#0B2A3D' },
  AJA: { bg: '#F4F4F4', ring: '#D2122E', ink: '#D2122E' },
  ARS: { bg: '#EF0107', ring: '#FFFFFF', ink: '#FFFFFF' },
  INT: { bg: '#0068A8', ring: '#111111', ink: '#FFFFFF' },
  STE: { bg: '#E30613', ring: '#00309B', ink: '#FFFFFF' },
};

const FALLBACK = { bg: '#2A3B33', ring: '#6B8A7A', ink: '#EEF5F0' };

export function teamColours(shortName) {
  return COLOURS[shortName] ?? FALLBACK;
}

// The colour that stands out on the dark page: white clubs use their second colour
export function teamAccent(shortName) {
  const { bg, ring } = teamColours(shortName);
  return bg === '#F4F4F4' ? ring : bg;
}
