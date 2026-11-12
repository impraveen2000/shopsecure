import { useEffect, useState } from 'react';
import { notificationApi } from '../api/services.js';
import { errorMessage } from '../api/client.js';
import Alert from '../components/Alert.jsx';

export default function NotificationsPage() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    notificationApi.mine().then((r) => setItems(r.data)).catch((err) => setError(errorMessage(err)));
  }, []);

  return (
    <div>
      <h1>Notifications</h1>
      <p className="muted">These were created by notification-service from Kafka events (welcome email, order confirmations).</p>
      <Alert>{error}</Alert>
      {items.length === 0 && !error && <div className="card center muted">Nothing yet. Place an order!</div>}
      {items.map((n) => (
        <div key={n.id} className="card notification">
          <strong>{n.subject}</strong>
          <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
          <p>{n.message}</p>
        </div>
      ))}
    </div>
  );
}
