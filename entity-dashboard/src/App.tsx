import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import EntityLayout from './components/EntityLayout';
import ProtectedRoute from './components/ProtectedRoute';
import './App.css';
import SubscriberPage from './pages/SubscriberPage';
import SessionPage from './pages/SessionPage';
import ReportPage from './pages/ReportPage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<EntityLayout />}>
            <Route path="subscribers" element={<SubscriberPage />} />
            <Route path="sessions" element={<SessionPage />} />
            <Route path="reports" element={<ReportPage />} />
            <Route index element={<Navigate to="subscribers" replace />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} /> {/* Catch-all */}
      </Routes>
    </Router>
  );
}

export default App;
