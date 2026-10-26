// Every call to the Spring Boot backend goes through this file.

async function get(path) {
  const response = await fetch(`/api${path}`);
  if (!response.ok) {
    throw new Error(`The server answered ${response.status} for ${path}`);
  }
  return response.json();
}

export const api = {
  table: () => get('/table'),
};
