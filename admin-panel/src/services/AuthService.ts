const TOKEN_KEY = 'authToken';
const REFRESH_TOKEN_KEY = 'refreshToken'; // New key

const AuthService = {
  // login method is replaced by storeTokens for clarity or can be updated
  // login: (token: string): void => { // Old login
  //   localStorage.setItem(TOKEN_KEY, token);
  // },

  storeTokens: (accessToken: string, refreshToken: string): void => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  logout: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY); // Clear refresh token as well
    // window.location.href = '/login'; // Handled by UI components reacting to auth state
  },

  getToken: (): string | null => { // This is for the access token
    return localStorage.getItem(TOKEN_KEY);
  },

  getRefreshToken: (): string | null => { // New method
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  updateAccessToken: (newAccessToken: string): void => { // New method
    localStorage.setItem(TOKEN_KEY, newAccessToken);
  },

  isLoggedIn: (): boolean => {
    const token = localStorage.getItem(TOKEN_KEY);
    // TODO: Add token expiration check here
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
