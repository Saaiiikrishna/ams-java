import React from 'react';
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import EntityPage from './pages/EntityPage'; // Actual component
import NfcCardsPage from './pages/NfcCardsPage';
import AssignAdminPage from './pages/AssignAdminPage'; // Actual component
import EntityAdminsPage from './pages/EntityAdminsPage';
import SuperAdminsPage from './pages/SuperAdminsPage';
import UnassignedEntitiesPage from './pages/UnassignedEntitiesPage';
import AdminLayout from './components/AdminLayout';
import ProtectedRoute from './components/ProtectedRoute';
import AuthService from './services/AuthService';
import { CustomThemeProvider } from './contexts/ThemeContext';
import './App.css';

function App() {
  return (
    <CustomThemeProvider>
      <Router>
        <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />

        {/* Root Route - Smart redirect based on authentication */}
        <Route path="/" element={<RootRedirect />} />

        {/* Protected Dashboard Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<AdminLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="entities" element={<EntityPage />} />
            <Route path="nfc-cards" element={<NfcCardsPage />} />
            <Route path="assign-admin" element={<AssignAdminPage />} />
            <Route path="entity-admins" element={<EntityAdminsPage />} />
            <Route path="super-admins" element={<SuperAdminsPage />} />
            <Route path="unassigned-entities" element={<UnassignedEntitiesPage />} />
          </Route>
        </Route>

        {/* Catch-all route for 404 handling */}
        <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Router>
    </CustomThemeProvider>
  );
}

// Smart root redirect component
const RootRedirect: React.FC = () => {
  const isAuthenticated = AuthService.isLoggedIn();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Navigate to="/login" replace />;
};

// 404 Not Found component
const NotFoundPage: React.FC = () => {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100vh',
      textAlign: 'center',
      padding: '20px'
    }}>
      <h1 style={{ fontSize: '4rem', margin: '0', color: '#1976d2' }}>404</h1>
      <h2 style={{ margin: '10px 0', color: '#666' }}>Page Not Found</h2>
      <p style={{ margin: '10px 0 20px', color: '#888' }}>
        The page you're looking for doesn't exist.
      </p>
      <a
        href="/"
        style={{
          color: '#1976d2',
          textDecoration: 'none',
          padding: '10px 20px',
          border: '1px solid #1976d2',
          borderRadius: '4px'
        }}
      >
        Go Home
      </a>
    </div>
  );
};

export default App;
