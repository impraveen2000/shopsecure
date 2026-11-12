import { Link } from 'react-router-dom';

export default function UnauthorizedPage() {
  return (
    <div className="card center">
      <h1>403 · Not allowed</h1>
      <p>Your account doesn't have the role needed for this page.</p>
      <Link to="/">Back to products</Link>
    </div>
  );
}
