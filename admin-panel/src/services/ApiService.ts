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

// Global flag and queue for token refresh logic
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: any) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      // For retrying requests, the original promise should be resolved by retrying the request.
      // This simplified version might not correctly re-chain, actual retry needs to happen.
      // The logic below in the interceptor handles retrying originalRequest directly.
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor for handling token refresh
ApiService.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config;

    // Check if error is 401 and not a retry request
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // If token is already being refreshed, queue the request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
        .then(token => { // This token is the new access token
          originalRequest.headers['Authorization'] = 'Bearer ' + AuthService.getToken(); // Or use token passed from processQueue
          return ApiService(originalRequest); // Retry the original request
        })
        .catch(err => {
          return Promise.reject(err); // If queuing or subsequent retry fails
        });
      }

      originalRequest._retry = true; // Mark as retried
      isRefreshing = true;

      const refreshToken = AuthService.getRefreshToken();
      if (refreshToken) {
        try {
          // Use a separate, clean Axios instance for the refresh token request to avoid circular interceptors
          const refreshAxiosInstance = axios.create({ baseURL: API_BASE_URL });
          const refreshResponse = await refreshAxiosInstance.post('/admin/refresh-token', { refreshToken });

          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = refreshResponse.data;
          AuthService.storeTokens(newAccessToken, newRefreshToken); // Store new tokens

          // Update the header of the original request
          ApiService.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`; // Update default for subsequent requests
          originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`; // Update current request

          processQueue(null, newAccessToken); // Process queued requests with new token
          return ApiService(originalRequest); // Retry original request

        } catch (refreshError: any) {
          processQueue(refreshError, null);
          AuthService.logout();
          // Consider using a router or navigation service for redirection
          window.location.href = '/login';
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      } else {
        // No refresh token available, logout and redirect
        isRefreshing = false;
        AuthService.logout();
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }

    // For other errors, or if it's already a retry, just reject
    return Promise.reject(error);
  }
);

export default ApiService;
