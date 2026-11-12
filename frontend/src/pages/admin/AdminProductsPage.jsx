import { useCallback, useEffect, useState } from 'react';
import { productApi } from '../../api/services.js';
import { errorMessage } from '../../api/client.js';
import Alert from '../../components/Alert.jsx';
import { formatPrice } from '../ProductsPage.jsx';

const empty = { name: '', description: '', price: '', stock: 0, category: '', imageUrl: '' };

export default function AdminProductsPage() {
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    productApi.list({ size: 100 }).then((r) => setProducts(r.data.content)).catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(load, [load]);

  const save = async (e) => {
    e.preventDefault();
    setError('');
    const payload = { ...form, price: Number(form.price), stock: Number(form.stock), imageUrl: form.imageUrl || null };
    try {
      if (editingId) await productApi.update(editingId, payload);
      else await productApi.create(payload);
      setMessage(editingId ? 'Product updated' : 'Product created');
      setForm(empty);
      setEditingId(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const edit = (p) => {
    setEditingId(p.id);
    setForm({ name: p.name, description: p.description || '', price: p.price, stock: p.stock,
              category: p.category || '', imageUrl: p.imageUrl || '' });
  };

  const remove = async (p) => {
    if (!window.confirm(`Delete "${p.name}"?`)) return;
    try {
      await productApi.remove(p.id);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div>
      <h1>Manage Products <span className="role role-ADMIN">ADMIN</span></h1>
      <Alert>{error}</Alert>
      <Alert type="success">{message}</Alert>

      <form className="card form-grid" onSubmit={save}>
        <h3>{editingId ? `Edit product #${editingId}` : 'New product'}</h3>
        <input placeholder="Name" value={form.name} required onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
        <input placeholder="Price" type="number" step="0.01" min="0.01" value={form.price} required
               onChange={(e) => setForm({ ...form, price: e.target.value })} />
        <input placeholder="Stock" type="number" min="0" value={form.stock} required
               onChange={(e) => setForm({ ...form, stock: e.target.value })} />
        <input placeholder="Image URL (https://…)" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
        <textarea placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <div>
          <button className="btn">{editingId ? 'Update' : 'Create'}</button>
          {editingId && <button type="button" className="btn btn-light" onClick={() => { setEditingId(null); setForm(empty); }}>Cancel</button>}
        </div>
      </form>

      <div className="card">
        <table>
          <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Created by</th><th /></tr></thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td><td>{p.name}</td><td>{p.category}</td><td>{formatPrice(p.price)}</td>
                <td>{p.stock}</td><td className="muted">{p.createdBy}</td>
                <td className="actions">
                  <button className="btn btn-light btn-sm" onClick={() => edit(p)}>Edit</button>
                  <button className="btn btn-danger btn-sm" onClick={() => remove(p)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
