import { useCallback, useEffect, useState } from 'react';
import { orderApi } from '../api/services.js';
import { errorMessage } from '../api/client.js';
import Alert from '../components/Alert.jsx';
import OrderList from '../components/OrderList.jsx';

export default function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    orderApi.mine().then((r) => setOrders(r.data)).catch((err) => setError(errorMessage(err)));
  }, []);

  useEffect(load, [load]);

  const cancel = async (id) => {
    try {
      await orderApi.cancel(id);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div>
      <h1>My Orders</h1>
      <Alert>{error}</Alert>
      <OrderList orders={orders} onCancel={cancel} />
    </div>
  );
}
