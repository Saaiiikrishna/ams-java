import React from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';

const EntityLayout: React.FC = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
  };

  return (
    <div>
      <nav style={{ backgroundColor: '#e9ecef', padding: '10px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #ced4da' }}>
        <div>
          <h3 style={{ margin: 0, marginRight: '20px', display: 'inline-block' }}>Entity Dashboard</h3>
          <Link to="/dashboard/subscribers" style={{ marginRight: '15px', textDecoration: 'none', color: '#007bff' }}>Subscribers</Link>
          <Link to="/dashboard/sessions" style={{ marginRight: '15px', textDecoration: 'none', color: '#007bff' }}>Sessions</Link>
          <Link to="/dashboard/reports" style={{ textDecoration: 'none', color: '#007bff' }}>Reports</Link>
        </div>
        <button
          onClick={handleLogout}
          style={{
            padding: '8px 12px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '3px',
            cursor: 'pointer'
          }}
        >
          Logout
        </button>
      </nav>
      <main style={{ padding: '20px' }}>
        <Outlet /> {/* Child routes (SubscriberPage, SessionPage, etc.) will render here */}
      </main>
    </div>
  );
};

export default EntityLayout;
