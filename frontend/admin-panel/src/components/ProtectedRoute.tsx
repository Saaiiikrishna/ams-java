import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import AuthService from '../services/AuthService';

interface ProtectedRouteProps {
  // No specific props needed for this simple version
  // children?: React.ReactNode; // Not used if Outlet is used directly in App.tsx routing
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = () => {
  if (!AuthService.isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }

  // If logged in, render the child routes
  return <Outlet />;
};

export default ProtectedRoute;
