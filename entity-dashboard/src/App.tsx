import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import LoginPage from './pages/LoginPage';
import SimpleEntityLayout from './components/SimpleEntityLayout';
import ProtectedRoute from './components/ProtectedRoute';
import DashboardPage from './pages/NewDashboardPage';
import SubscriberPage from './pages/SubscriberPage';
import CardsPage from './pages/CardsPage';
import SessionPage from './pages/SessionPage';
import SessionDetailsPage from './pages/SessionDetailsPage';
import ScheduledSessionsPage from './pages/ScheduledSessionsPage';
import ScheduledSessionDetailsPage from './pages/ScheduledSessionDetailsPage';
import ReportPage from './pages/ReportPage';
import MenuManagementPage from './pages/MenuManagementPage';
import OrderManagementPage from './pages/OrderManagementPage';
import TableManagementPage from './pages/TableManagementPage';
import AttendancePage from './pages/AttendancePage';
import MenuOrderingPage from './pages/MenuOrderingPage';
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
            <Route path="/dashboard" element={<SimpleEntityLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="overview" element={<DashboardPage />} />
              <Route path="subscribers" element={<SubscriberPage />} />
              <Route path="cards" element={<CardsPage />} />
              <Route path="sessions" element={<SessionPage />} />
              <Route path="sessions/:sessionId" element={<SessionDetailsPage />} />
              <Route path="scheduled-sessions" element={<ScheduledSessionsPage />} />
              <Route path="scheduled-sessions/:id" element={<ScheduledSessionDetailsPage />} />
              <Route path="reports" element={<ReportPage />} />
              <Route path="attendance" element={<AttendancePage />} />
              <Route path="menu" element={<MenuOrderingPage />} />
              <Route path="menu/categories" element={<MenuManagementPage />} />
              <Route path="menu/items" element={<MenuManagementPage />} />
              <Route path="menu/tables" element={<TableManagementPage />} />
              <Route path="menu/orders" element={<OrderManagementPage />} />
              <Route path="menu/order-history" element={<OrderManagementPage />} />
              <Route path="menu/reports" element={<ReportPage />} />
              <Route path="attendance/today" element={<AttendancePage />} />
              <Route path="attendance/history" element={<ReportPage />} />
              <Route path="attendance/reports" element={<ReportPage />} />
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
