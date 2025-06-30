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
  console.log('🔗 Entity Dashboard: API Base URL configured:', API_BASE_URL);
  console.log('🔗 Entity Dashboard: Full backend URL will be:', API_BASE_URL || window.location.origin);
}).catch(error => {
  console.error('❌ Entity Dashboard: Failed to get API base URL:', error);
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
      console.log('🔄 Entity Dashboard: Updated API base URL to:', newBaseUrl);
    }
  } catch (error) {
    console.error('❌ Entity Dashboard: Failed to update API base URL:', error);
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
