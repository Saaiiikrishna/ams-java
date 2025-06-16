import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AuthService from './AuthService';
import logger from './LoggingService';

// EMERGENCY FIX: Force direct connection to backend
const getApiBaseUrl = (): string => {
  // ALWAYS use the current window location when served from backend
  const currentHost = window.location.hostname;
  const currentPort = window.location.port;

  console.log('🔗 Current location:', window.location.href);
  console.log('🔗 Host:', currentHost, 'Port:', currentPort);

  // If we're on the backend server (port 8080), use same origin
  if (currentPort === '8080') {
    console.log('✅ Using same-origin API calls (relative URLs)');
    return ''; // Use relative URLs - this should work!
  }

  // Fallback: try multiple backend URLs
  const possibleUrls = [
    `http://${currentHost}:8080`,
    'http://192.168.31.4:8080',
    'http://restaurant.local:8080',
    'http://localhost:8080'
  ];

  const backendUrl = possibleUrls[0];
  console.log('🔗 Using external API calls:', backendUrl);
  return backendUrl;
};

const API_BASE_URL = getApiBaseUrl();

console.log('🔗 API Base URL configured:', API_BASE_URL);
console.log('🔗 Full backend URL will be:', API_BASE_URL || window.location.origin);

const ApiService: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add the auth token to headers
ApiService.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = AuthService.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Log API request
    logger.apiRequest(config.method?.toUpperCase() || 'UNKNOWN', config.url || '', config.data);

    return config;
  },
  (error) => {
    logger.error('API Request Error', 'API', error);
    return Promise.reject(error);
  }
);

// Response interceptor for logging and error handling
ApiService.interceptors.response.use(
  (response: AxiosResponse) => {
    // Log successful API response
    logger.apiResponse(
      response.config.method?.toUpperCase() || 'UNKNOWN',
      response.config.url || '',
      response.status,
      response.data
    );
    return response;
  },
  (error) => {
    // Log API error
    logger.apiError(
      error.config?.method?.toUpperCase() || 'UNKNOWN',
      error.config?.url || '',
      error
    );

    // Handle 401 Unauthorized globally
    if (error.response && error.response.status === 401) {
      logger.authEvent('Unauthorized access - logging out');
      AuthService.logout();
      // Redirect to login page
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default ApiService;
