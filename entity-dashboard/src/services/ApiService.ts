import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AuthService from './AuthService';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'; // Backend API URL

const ApiService: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add the auth token to headers
ApiService.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => { // Corrected type
    const token = AuthService.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor (optional, for global error handling or token refresh logic)
ApiService.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    // Example: Handle 401 Unauthorized globally (e.g., logout user)
    if (error.response && error.response.status === 401) {
      AuthService.logout();
      // Redirect to login page, perhaps using a history object if outside React component
      // window.location.href = '/login'; // Simple redirect
      console.error("Unauthorized access - logging out.");
    }
    return Promise.reject(error);
  }
);

export default ApiService;
