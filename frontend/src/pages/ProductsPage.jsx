import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productApi } from '../api/services.js';
import { errorMessage } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import Alert from '../components/Alert.jsx';

export const formatPrice = (v) => `₹${Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

export default function ProductsPage() {
  const { user } = useAuth();
  const cart = useCart();
  const navigate = useNavigate();
  const [data, setData] = useState({ content: [], totalPages: 0, page: 0 });
  const [categories, setCategories] = useState([]);
  const [filters, setFilters] = useState({ q: '', category: '', sort: 'id', page: 0 });
  const [error, setError] = useState('');
  const [added, setAdded] = useState('');

  useEffect(() => {
    productApi.categories().then((r) => setCategories(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    const params = { ...filters, size: 8 };
    if (!params.q) delete params.q;
    if (!params.category) delete params.category;
    productApi.list(params)
      .then((r) => { setData(r.data); setError(''); })
      .catch((err) => setError(errorMessage(err)));
  }, [filters]);

  const addToCart = (p) => {
    if (!user) return navigate('/login', { state: { from: '/' } });
    cart.add(p);
    setAdded(`Added "${p.name}" to cart`);
    setTimeout(() => setAdded(''), 2000);
  };

  return (
    <div>
      <div className="page-head">
        <h1>Products</h1>
        <div className="filters">
          <input placeholder="Search…" value={filters.q}
                 onChange={(e) => setFilters({ ...filters, q: e.target.value, page: 0 })} />
          <select value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value, page: 0 })}>
            <option value="">All categories</option>
            {categories.map((c) => <option key={c}>{c}</option>)}
          </select>
          <select value={filters.sort} onChange={(e) => setFilters({ ...filters, sort: e.target.value })}>
            <option value="id">Newest</option>
            <option value="name">Name</option>
            <option value="price">Price</option>
          </select>
        </div>
      </div>
      <Alert>{error}</Alert>
      <Alert type="success">{added}</Alert>

      <div className="grid">
        {data.content.map((p) => (
          <div key={p.id} className="card product">
            <div className="product-img">{p.imageUrl ? <img src={p.imageUrl} alt="" /> : '📦'}</div>
            <span className="tag">{p.category}</span>
            <h3>{p.name}</h3>
            <p className="muted">{p.description}</p>
            <div className="product-foot">
              <strong>{formatPrice(p.price)}</strong>
              <span className={p.stock > 0 ? 'muted' : 'danger'}>{p.stock > 0 ? `${p.stock} in stock` : 'Sold out'}</span>
            </div>
            <button className="btn" disabled={p.stock === 0} onClick={() => addToCart(p)}>Add to cart</button>
          </div>
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="pager">
          <button className="btn btn-light" disabled={data.page === 0}
                  onClick={() => setFilters({ ...filters, page: data.page - 1 })}>‹ Prev</button>
          <span>Page {data.page + 1} of {data.totalPages}</span>
          <button className="btn btn-light" disabled={data.page + 1 >= data.totalPages}
                  onClick={() => setFilters({ ...filters, page: data.page + 1 })}>Next ›</button>
        </div>
      )}
    </div>
  );
}
