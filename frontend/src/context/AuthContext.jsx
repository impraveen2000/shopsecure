import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/services.js';
import { refreshAccessToken, setAccessToken, setSessionExpiredHandler } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // On first load, try to restore the session using the HttpOnly refresh cookie
  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null));
    refreshAccessToken()
      .then((data) => setUser(data.user))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (username, password) => {
    const { data } = await authApi.login(username, password);
    setAccessToken(data.accessToken);
    setUser(data.user);
    return data.user;
  }, []);

  const register = useCallback((form) => authApi.register(form), []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout(); // revokes the refresh token server-side and clears the cookie
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const hasRole = useCallback((role) => !!user?.roles?.includes(role), [user]);

  const value = useMemo(
    () => ({ user, loading, login, logout, register, hasRole }),
    [user, loading, login, logout, register, hasRole]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
