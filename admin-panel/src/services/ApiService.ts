import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AuthService from './AuthService';
import logger from './LoggingService';
import DynamicApiService from './DynamicApiService';

// Dynamic API service instance
const dynamicApiService = DynamicApiService.getInstance();

// Initialize with dynamic base URL
let API_BASE_URL = '';

// Function to get current API base URL
const getApiBaseUrl = async (): Promise<string> => {
  return await dynamicApiService.getApiBaseUrl();
};

// Initialize the base URL
getApiBaseUrl().then(url => {
  API_BASE_URL = url;
  console.log('🔗 Admin Panel: API Base URL configured:', API_BASE_URL);
  console.log('🔗 Admin Panel: Full backend URL will be:', API_BASE_URL || window.location.origin);
}).catch(error => {
  console.error('❌ Admin Panel: Failed to get API base URL:', error);
  API_BASE_URL = `http://${window.location.hostname}:8080`; // Fallback
});

// Create axios instance with initial base URL
const ApiService: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Function to update the base URL dynamically
const updateApiBaseUrl = async (): Promise<void> => {
  try {
    const newBaseUrl = await getApiBaseUrl();
    if (newBaseUrl !== ApiService.defaults.baseURL) {
      ApiService.defaults.baseURL = newBaseUrl;
      console.log('🔄 Admin Panel: Updated API base URL to:', newBaseUrl);
    }
  } catch (error) {
    console.error('❌ Admin Panel: Failed to update API base URL:', error);
  }
};

// Request interceptor to add the auth token and ensure dynamic URL
ApiService.interceptors.request.use(
  async (config: InternalAxiosRequestConfig): Promise<InternalAxiosRequestConfig> => {
    // Update base URL if needed
    await updateApiBaseUrl();

    // Add auth token
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
  async (error) => {
    // Log API error
    logger.apiError(
      error.config?.method?.toUpperCase() || 'UNKNOWN',
      error.config?.url || '',
      error
    );

    const originalRequest = error.config;

    // Handle network errors (server unreachable)
    if (!error.response && error.code === 'NETWORK_ERROR') {
      console.warn('🌐 Admin Panel: Network error detected, triggering server rediscovery...');
      try {
        const newServerUrl = await dynamicApiService.forceRediscovery();
        if (newServerUrl && !originalRequest._networkRetry) {
          originalRequest._networkRetry = true;
          console.log('🔄 Admin Panel: Retrying request with new server:', newServerUrl);
          return ApiService(originalRequest);
        }
      } catch (rediscoveryError) {
        console.error('❌ Admin Panel: Server rediscovery failed:', rediscoveryError);
      }
    }

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
          const refreshResponse = await refreshAxiosInstance.post('/super/auth/refresh-token', { refreshToken });

          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = refreshResponse.data;
          AuthService.storeTokens(newAccessToken, newRefreshToken); // Store new tokens

          // Update the header of the original request
          ApiService.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`; // Update default for subsequent requests
          originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`; // Update current request

          processQueue(null, newAccessToken); // Process queued requests with new token
          return ApiService(originalRequest); // Retry original request

        } catch (refreshError: any) {
          processQueue(refreshError, null);
          logger.authEvent('Token refresh failed - logging out');
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
        logger.authEvent('No refresh token available - logging out');
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
