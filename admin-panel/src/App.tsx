import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import EntityPage from './pages/EntityPage'; // Actual component
import AssignAdminPage from './pages/AssignAdminPage'; // Actual component
import AdminLayout from './components/AdminLayout';
import ProtectedRoute from './components/ProtectedRoute';
import './App.css';


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Protected Admin Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/admin" element={<AdminLayout />}>
            <Route path="entities" element={<EntityPage />} />
            <Route path="assign-admin" element={<AssignAdminPage />} />
            <Route index element={<Navigate to="entities" replace />} />
          </Route>
        </Route>

        {/* Redirect root to login or admin based on auth (optional, or handle in ProtectedRoute) */}
        {/* For simplicity, let's redirect to /login, ProtectedRoute will handle redirect to /admin/* if already logged in from there. */}
        <Route path="/" element={<Navigate to="/login" replace />} />

        {/* Catch-all for undefined routes (optional) */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
