import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

/**
 * Client-side route guard. It improves UX (no broken admin pages for normal users),
 * but it is NOT security: a user can always call the API directly. The backend enforces access.
 */
export default function ProtectedRoute({ roles }) {
  const { user, loading, hasRole } = useAuth();
  const location = useLocation();

  if (loading) return <div className="center muted">Checking session…</div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.some(hasRole)) return <Navigate to="/unauthorized" replace />;
  return <Outlet />;
}
