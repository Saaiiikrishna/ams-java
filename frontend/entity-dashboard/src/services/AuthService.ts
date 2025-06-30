import logger from './LoggingService';

const TOKEN_KEY = 'authToken';

const AuthService = {
  login: (token: string): void => {
    localStorage.setItem(TOKEN_KEY, token);
    logger.authEvent('Entity admin logged in');
  },

  logout: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    logger.authEvent('Entity admin logged out');
  },

  getToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY);
  },

  isLoggedIn: (): boolean => {
    const token = localStorage.getItem(TOKEN_KEY);
    // Future: Add token expiration check here if tokens have an expiry claim
    return !!token;
  },

  getAuthHeader: (): { Authorization?: string } => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  },
};

export default AuthService;
