import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import AuthService from '../services/AuthService';

const ProtectedRoute: React.FC = () => {
  const location = useLocation();

  if (!AuthService.isLoggedIn()) {
    // Redirect them to the /login page, but save the current location they were
    // trying to go to so we can send them there after login.
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If logged in, render the child routes via Outlet
  return <Outlet />;
};

export default ProtectedRoute;
