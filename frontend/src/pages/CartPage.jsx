import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext.jsx';
import { orderApi } from '../api/services.js';
import { errorMessage } from '../api/client.js';
import Alert from '../components/Alert.jsx';
import { formatPrice } from './ProductsPage.jsx';

export default function CartPage() {
  const cart = useCart();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const placeOrder = async () => {
    setBusy(true);
    setError('');
    try {
      // We send ONLY ids and quantities. Even if someone edits the cart prices in localStorage,
      // the order-service gets the real price from product-service.
      await orderApi.place(cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })));
      cart.clear();
      navigate('/orders');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (cart.items.length === 0) {
    return <div className="card center">Your cart is empty. <Link to="/">Browse products</Link></div>;
  }

  return (
    <div className="card">
      <h1>Cart</h1>
      <Alert>{error}</Alert>
      <table>
        <thead><tr><th>Product</th><th>Price</th><th>Qty</th><th>Subtotal</th><th /></tr></thead>
        <tbody>
          {cart.items.map((i) => (
            <tr key={i.productId}>
              <td>{i.name}</td>
              <td>{formatPrice(i.price)}</td>
              <td>
                <input type="number" min="1" max="100" value={i.quantity} className="qty"
                       onChange={(e) => cart.update(i.productId, Math.max(1, Math.min(100, Number(e.target.value))))} />
              </td>
              <td>{formatPrice(i.price * i.quantity)}</td>
              <td><button className="btn btn-danger btn-sm" onClick={() => cart.remove(i.productId)}>Remove</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="cart-foot">
        <span>Estimated total: <strong>{formatPrice(cart.total)}</strong></span>
        <button className="btn" onClick={placeOrder} disabled={busy}>{busy ? 'Placing order…' : 'Place order'}</button>
      </div>
    </div>
  );
}
