import axios from 'axios';

/*
 * TOKEN STRATEGY (the secure SPA pattern)
 * - Access token (15 min JWT): kept ONLY in this module's memory. Not in localStorage,
 *   because any XSS script could read localStorage and steal it.
 * - Refresh token (7 days): HttpOnly cookie set by auth-service. JavaScript can't read it at all;
 *   the browser sends it automatically, but only to /api/auth/* on the same site.
 * - On page reload memory is empty, so we call /api/auth/refresh to get a new access token.
 */
let accessToken = null;
let onSessionExpired = () => {};

export const setAccessToken = (token) => { accessToken = token; };
export const getAccessToken = () => accessToken;
export const setSessionExpiredHandler = (fn) => { onSessionExpired = fn; };

const api = axios.create({ baseURL: '/api', withCredentials: true });

// Attach "Authorization: Bearer <token>" to every request
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Single-flight refresh: if 5 requests fail with 401 at once, only ONE refresh call is made.
// This matters because refresh tokens ROTATE: reusing an old one is treated as theft.
let refreshPromise = null;
export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = axios
      .post('/api/auth/refresh', null, { withCredentials: true })
      .then((res) => {
        setAccessToken(res.data.accessToken);
        return res.data;
      })
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

const NO_RETRY = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];

// When the access token has expired (401), refresh it once and replay the original request
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    if (status === 401 && original && !original._retry && !NO_RETRY.includes(original.url)) {
      original._retry = true;
      try {
        await refreshAccessToken();
        original.headers.Authorization = `Bearer ${accessToken}`;
        return api(original);
      } catch (refreshError) {
        setAccessToken(null);
        onSessionExpired();
      }
    }
    return Promise.reject(error);
  }
);

/** Turn backend ProblemDetail errors into a readable message. */
export function errorMessage(err) {
  const data = err?.response?.data;
  if (err?.response?.status === 429) return 'Too many requests. Please slow down and try again.';
  if (data?.errors) return Object.entries(data.errors).map(([f, m]) => `${f}: ${m}`).join(' | ');
  return data?.detail || data?.message || err?.message || 'Something went wrong';
}

export default api;
