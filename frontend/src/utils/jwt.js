/**
 * Decodes (NOT verifies) a JWT payload, for display only.
 * The frontend must never make security decisions from this: anyone can edit a token in the browser.
 * Only the backend, which checks the signature, can trust the claims.
 */
export function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(payload).split('').map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function decodeJwtHeader(token) {
  if (!token) return null;
  try {
    return JSON.parse(atob(token.split('.')[0].replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}
