import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { getAccessToken, refreshAccessToken } from '../api/client.js';
import { authApi } from '../api/services.js';
import { decodeJwt, decodeJwtHeader } from '../utils/jwt.js';

/** Learning page: look inside your own access token. */
export default function ProfilePage() {
  const { user } = useAuth();
  const [token, setToken] = useState(getAccessToken());
  const [now, setNow] = useState(Date.now());
  const [me, setMe] = useState(null);

  useEffect(() => {
    const t = setInterval(() => { setNow(Date.now()); setToken(getAccessToken()); }, 1000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    authApi.me().then((r) => setMe(r.data)).catch(() => {});
  }, []);

  const claims = decodeJwt(token);
  const header = decodeJwtHeader(token);
  const secondsLeft = claims ? Math.max(0, Math.round(claims.exp - now / 1000)) : 0;

  return (
    <div className="card">
      <h1>Profile</h1>
      <p><strong>{user.username}</strong> · {user.email} · roles: {user.roles.join(', ')}</p>
      {me && <p className="muted">/api/auth/me confirmed by server (user id {me.id}, created {new Date(me.createdAt).toLocaleDateString()})</p>}

      <h3>Your access token</h3>
      <p className="muted">
        Expires in <strong>{Math.floor(secondsLeft / 60)}m {secondsLeft % 60}s</strong>. When it expires, the next API
        call gets a 401 and the axios interceptor silently refreshes it using the HttpOnly cookie.
      </p>
      <button className="btn btn-light" onClick={() => refreshAccessToken().then(() => setToken(getAccessToken()))}>
        Refresh now (rotates the refresh token)
      </button>
      <div className="jwt-grid">
        <div>
          <h4>Header</h4>
          <pre>{JSON.stringify(header, null, 2)}</pre>
        </div>
        <div>
          <h4>Payload (claims)</h4>
          <pre>{JSON.stringify(claims, null, 2)}</pre>
        </div>
      </div>
      <h4>Raw token</h4>
      <pre className="raw">{token}</pre>
      <p className="muted">
        The payload is only Base64-encoded, not encrypted. Anyone can read it, but nobody can change it
        without breaking the RS256 signature, which every service verifies with the public key from
        <code> /.well-known/jwks.json</code>.
      </p>
    </div>
  );
}
