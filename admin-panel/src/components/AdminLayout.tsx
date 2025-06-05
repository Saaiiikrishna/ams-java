import React from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';

const AdminLayout: React.FC = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
  };

  return (
    <div>
      <nav style={{ backgroundColor: '#f0f0f0', padding: '10px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Link to="/admin/entities" style={{ marginRight: '15px', textDecoration: 'none', color: '#333' }}>Entity Management</Link>
          <Link to="/admin/assign-admin" style={{ textDecoration: 'none', color: '#333' }}>Assign Entity Admin</Link>
        </div>
        <button onClick={handleLogout} style={{ padding: '8px 12px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}>
          Logout
        </button>
      </nav>
      <main style={{ padding: '20px' }}>
        {/* The Outlet here will render the specific admin page (EntityPage, AssignAdminPage) */}
        <Outlet />
      </main>
    </div>
  );
};

export default AdminLayout;
