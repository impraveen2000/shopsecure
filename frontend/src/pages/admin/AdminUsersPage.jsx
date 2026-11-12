import { useCallback, useEffect, useState } from 'react';
import { adminUserApi } from '../../api/services.js';
import { errorMessage } from '../../api/client.js';
import Alert from '../../components/Alert.jsx';

const ALL_ROLES = ['USER', 'ADMIN'];

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    adminUserApi.list().then((r) => setUsers(r.data)).catch((err) => setError(errorMessage(err)));
  }, []);

  useEffect(load, [load]);

  const run = async (fn) => {
    setError('');
    try {
      await fn();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const toggleRole = (u, role) => {
    const roles = u.roles.includes(role) ? u.roles.filter((r) => r !== role) : [...u.roles, role];
    run(() => adminUserApi.setRoles(u.id, roles));
  };

  return (
    <div>
      <h1>Users <span className="role role-ADMIN">ADMIN</span></h1>
      <p className="muted">
        Changing roles or disabling a user revokes their refresh tokens. Their current access token
        stays valid until it expires (max 15 min): that's the trade-off of stateless JWTs.
      </p>
      <Alert>{error}</Alert>
      <div className="card">
        <table>
          <thead><tr><th>ID</th><th>Username</th><th>Email</th><th>Roles</th><th>Status</th><th /></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>
                  {ALL_ROLES.map((r) => (
                    <label key={r} className="inline">
                      <input type="checkbox" checked={u.roles.includes(r)} onChange={() => toggleRole(u, r)} /> {r}
                    </label>
                  ))}
                </td>
                <td>
                  {u.locked ? <span className="status status-CANCELLED">LOCKED</span>
                    : u.enabled ? <span className="status status-CONFIRMED">ACTIVE</span>
                    : <span className="status status-CANCELLED">DISABLED</span>}
                </td>
                <td className="actions">
                  {u.locked && <button className="btn btn-light btn-sm" onClick={() => run(() => adminUserApi.unlock(u.id))}>Unlock</button>}
                  <button className={`btn btn-sm ${u.enabled ? 'btn-danger' : ''}`}
                          onClick={() => run(() => adminUserApi.setEnabled(u.id, !u.enabled))}>
                    {u.enabled ? 'Disable' : 'Enable'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
