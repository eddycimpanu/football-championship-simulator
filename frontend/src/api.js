// Every call to the Spring Boot backend goes through this file.

async function request(method, path) {
  const response = await fetch(`/api${path}`, { method });
  if (!response.ok) {
    throw new Error(`The server answered ${response.status} for ${path}`);
  }
  return response.json();
}

export const api = {
  table: () => request('GET', '/table'),
  season: () => request('GET', '/season'),
  round: (number) => request('GET', `/rounds/${number}`),
  playNextRound: () => request('POST', '/rounds/next'),
  match: (id) => request('GET', `/matches/${id}`),
};
