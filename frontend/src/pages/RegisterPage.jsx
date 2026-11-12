import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import Alert from '../components/Alert.jsx';

// Mirrors the backend rules for instant feedback. The BACKEND validation is the one that counts.
const rules = [
  { test: (p) => p.length >= 8, label: 'At least 8 characters' },
  { test: (p) => /[a-z]/.test(p), label: 'A lowercase letter' },
  { test: (p) => /[A-Z]/.test(p), label: 'An uppercase letter' },
  { test: (p) => /\d/.test(p), label: 'A digit' },
  { test: (p) => /[^a-zA-Z\d]/.test(p), label: 'A special character' },
];

export default function RegisterPage() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await register(form);
      await login(form.username, form.password);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-card card">
      <h2>Create account</h2>
      <Alert>{error}</Alert>
      <form onSubmit={submit}>
        <label>Username
          <input value={form.username} required minLength={3} maxLength={50} pattern="[a-zA-Z0-9_]+"
                 onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </label>
        <label>Email
          <input type="email" value={form.email} required
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </label>
        <label>Password
          <input type="password" value={form.password} required autoComplete="new-password"
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <ul className="rules">
          {rules.map((r) => (
            <li key={r.label} className={r.test(form.password) ? 'ok' : ''}>{r.label}</li>
          ))}
        </ul>
        <button className="btn" disabled={busy}>{busy ? 'Creating…' : 'Sign up'}</button>
      </form>
      <p>Already registered? <Link to="/login">Log in</Link></p>
    </div>
  );
}
