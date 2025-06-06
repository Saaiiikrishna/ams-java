import React, { useState, FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom'; // Added useLocation
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const location = useLocation(); // To get redirect path

  // Redirect to dashboard if already logged in, unless already on login page
  // This basic check can be enhanced or moved to a higher order component.
  if (AuthService.isLoggedIn() && location.pathname === '/login') {
    navigate('/dashboard/subscribers');
  }


  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    try {
      // Assuming Entity Admins also use the /admin/authenticate endpoint
      const response = await ApiService.post('/admin/authenticate', {
        username,
        password,
      });

      if (response.data && response.data.jwt) {
        const token = response.data.jwt;
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const authorities: string[] = Array.isArray(payload.authorities)
            ? payload.authorities.map((a: any) => a.authority)
            : [];
          if (!authorities.includes('ROLE_ENTITY_ADMIN')) {
            setError('Access denied: not an entity admin.');
            return;
          }
        } catch (e) {
          console.error('Failed to decode token', e);
          setError('Login failed: invalid token.');
          return;
        }
        AuthService.login(token);
        navigate('/dashboard/subscribers');
      } else {
        setError('Login failed: No token received.');
      }
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(`Login failed: ${err.response.data.message}`);
      } else if (err.response && err.response.status === 401) {
        setError('Login failed: Invalid username or password.');
      } else {
        setError('Login failed: An unexpected error occurred.');
        console.error(err);
      }
    }
  };

  return (
    <div style={{ maxWidth: '400px', margin: '50px auto', padding: '20px', border: '1px solid #ccc', borderRadius: '5px' }}>
      <h2>Entity Admin Login</h2>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label htmlFor="username" style={{ display: 'block', marginBottom: '5px' }}>Username:</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ marginBottom: '15px' }}>
          <label htmlFor="password" style={{ display: 'block', marginBottom: '5px' }}>Password:</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
          />
        </div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit" style={{ padding: '10px 15px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}>
          Login
        </button>
      </form>
    </div>
  );
};

export default LoginPage;
