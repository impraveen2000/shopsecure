import { formatPrice } from '../pages/ProductsPage.jsx';

export default function OrderList({ orders, onCancel, showUser = false }) {
  if (orders.length === 0) return <div className="card center muted">No orders yet.</div>;
  return (
    <div className="orders">
      {orders.map((o) => (
        <div key={o.id} className="card order">
          <div className="order-head">
            <strong>Order #{o.id}</strong>
            {showUser && <span className="muted">by {o.username}</span>}
            <span className={`status status-${o.status}`}>{o.status}</span>
            <span className="muted">{new Date(o.createdAt).toLocaleString()}</span>
          </div>
          <ul>
            {o.items.map((i) => (
              <li key={i.productId}>{i.quantity} × {i.productName} — {formatPrice(i.subtotal)}</li>
            ))}
          </ul>
          <div className="order-foot">
            <span>Total: <strong>{formatPrice(o.total)}</strong></span>
            {o.status === 'CONFIRMED' && onCancel && (
              <button className="btn btn-danger btn-sm" onClick={() => onCancel(o.id)}>Cancel</button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
