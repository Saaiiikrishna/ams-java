import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import LoginPage from './pages/LoginPage';
import EntityPage from './pages/EntityPage';
import AssignAdminPage from './pages/AssignAdminPage';
import DashboardPage from './pages/DashboardPage';
import EntityAdminsPage from './pages/EntityAdminsPage';
import UnassignedEntitiesPage from './pages/UnassignedEntitiesPage';
import AdminLayout from './components/AdminLayout';
import ProtectedRoute from './components/ProtectedRoute';
import './App.css';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});


function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* Protected Admin Routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<AdminLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="entities" element={<EntityPage />} />
              <Route path="assign-admin" element={<AssignAdminPage />} />
              <Route path="entity-admins" element={<EntityAdminsPage />} />
              <Route path="unassigned-entities" element={<UnassignedEntitiesPage />} />
            </Route>
          </Route>

          {/* Legacy admin routes redirect */}
          <Route path="/admin/*" element={<Navigate to="/dashboard" replace />} />

          {/* Redirect root to login */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* Catch-all for undefined routes */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
