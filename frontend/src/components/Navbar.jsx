import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

export default function Navbar() {
  const { user, logout, hasRole } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <Link to="/" className="brand">🔐 ShopSecure</Link>
      <nav>
        <NavLink to="/" end>Products</NavLink>
        {user && <NavLink to="/cart">Cart{count > 0 && <span className="badge">{count}</span>}</NavLink>}
        {user && <NavLink to="/orders">My Orders</NavLink>}
        {user && <NavLink to="/notifications">Notifications</NavLink>}
        {hasRole('ADMIN') && (
          <span className="admin-links">
            <NavLink to="/admin/products">Manage Products</NavLink>
            <NavLink to="/admin/orders">All Orders</NavLink>
            <NavLink to="/admin/users">Users</NavLink>
          </span>
        )}
      </nav>
      <div className="nav-right">
        {user ? (
          <>
            <NavLink to="/profile" className="user-chip">
              {user.username} {user.roles.map((r) => <span key={r} className={`role role-${r}`}>{r}</span>)}
            </NavLink>
            <button className="btn btn-light" onClick={handleLogout}>Logout</button>
          </>
        ) : (
          <>
            <NavLink to="/login">Login</NavLink>
            <NavLink to="/register" className="btn btn-light">Sign up</NavLink>
          </>
        )}
      </div>
    </header>
  );
}
