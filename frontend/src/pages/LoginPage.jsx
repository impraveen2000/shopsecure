import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import Alert from '../components/Alert.jsx';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(form.username, form.password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-card card">
      <h2>Log in</h2>
      <Alert>{error}</Alert>
      <form onSubmit={submit}>
        <label>Username
          <input value={form.username} autoComplete="username" required
                 onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </label>
        <label>Password
          <input type="password" value={form.password} autoComplete="current-password" required
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <button className="btn" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
      </form>
      <div className="hint">
        <strong>Demo accounts</strong><br />
        admin / Admin@123 (ADMIN)<br />
        john / User@1234 (USER)<br />
        <small>Try 5 wrong passwords to see account lockout.</small>
      </div>
      <p>No account? <Link to="/register">Sign up</Link></p>
    </div>
  );
}
