import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import LoginPage from './pages/LoginPage';
import EntityLayout from './components/EntityLayout';
import ProtectedRoute from './components/ProtectedRoute';
import DashboardPage from './pages/DashboardPage';
import SubscriberPage from './pages/SubscriberPage';
import CardsPage from './pages/CardsPage';
import SessionPage from './pages/SessionPage';
import SessionDetailsPage from './pages/SessionDetailsPage';
import ReportPage from './pages/ReportPage';
import theme from './theme/theme';
import './App.css';

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<EntityLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="overview" element={<DashboardPage />} />
              <Route path="subscribers" element={<SubscriberPage />} />
              <Route path="cards" element={<CardsPage />} />
              <Route path="sessions" element={<SessionPage />} />
              <Route path="sessions/:sessionId" element={<SessionDetailsPage />} />
              <Route path="reports" element={<ReportPage />} />
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} /> {/* Catch-all */}
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
