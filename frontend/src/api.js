// Every call to the Spring Boot backend goes through this file.

async function request(method, path, body) {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    // The server explains broken rules, e.g. "Not enough credits for a stake of 50.00"
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.message ?? `The server answered ${response.status} for ${path}`);
  }
  return response.json();
}

export const api = {
  table: () => request('GET', '/table'),
  season: () => request('GET', '/season'),
  round: (number) => request('GET', `/rounds/${number}`),
  roundOdds: (number) => request('GET', `/rounds/${number}/odds`),
  playNextRound: () => request('POST', '/rounds/next'),
  resetSeason: () => request('POST', '/season/reset'),
  match: (id) => request('GET', `/matches/${id}`),
  searchPlayers: (text) => request('GET', `/players?search=${encodeURIComponent(text)}`),
  player: (id) => request('GET', `/players/${id}`),
  teams: () => request('GET', '/teams'),
  team: (id) => request('GET', `/teams/${id}`),
  reports: () => request('GET', '/reports'),
  bettingMe: () => request('GET', '/betting/me'),
  placeTicket: (stake, selections) => request('POST', '/tickets', { stake, selections }),
  agents: () => request('GET', '/agents'),
  agentTickets: (round) => request('GET', `/rounds/${round}/agent-tickets`),
  bettors: () => request('GET', '/bettors'),
  bettorTickets: (id) => request('GET', `/bettors/${id}/tickets`),
};
