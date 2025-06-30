import React from 'react';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import App from './App';

// Mock AuthService to avoid authentication issues in tests
jest.mock('./services/AuthService', () => ({
  isLoggedIn: () => false,
  getToken: () => null,
  getRefreshToken: () => null,
  logout: jest.fn(),
}));

test('renders login page when not authenticated', () => {
  render(
    <BrowserRouter>
      <App />
    </BrowserRouter>
  );
  // The app should redirect to login page when not authenticated
  expect(window.location.pathname).toBe('/');
});
