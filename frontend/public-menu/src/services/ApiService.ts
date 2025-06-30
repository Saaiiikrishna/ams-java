import axios, { AxiosInstance } from 'axios';

// API Configuration
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://restaurant.local:8080';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000, // 30 second timeout
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for logging
    this.api.interceptors.request.use(
      (config) => {
        console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('❌ API Request Error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor for logging
    this.api.interceptors.response.use(
      (response) => {
        console.log(`✅ API Response: ${response.status} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('❌ API Response Error:', error.response?.status, error.response?.data);
        return Promise.reject(error);
      }
    );
  }

  // Menu API methods
  async getTableMenu(tableId: string) {
    const response = await this.api.get(`/api/public/tables/${tableId}/menu`);
    return response.data;
  }

  async getMenuByEntity(entityId: string, params?: { table?: number; qr?: string }) {
    const response = await this.api.get(`/api/public/menu/${entityId}`, { params });
    return response.data;
  }

  async getTableByQr(qrCode: string) {
    const response = await this.api.get(`/api/public/menu/table/qr/${qrCode}`);
    return response.data;
  }

  // Order API methods
  async createOrder(orderData: any) {
    const requestId = Math.random().toString(36).substr(2, 9);
    console.log(`🚀 Creating order with request ID: ${requestId}`, orderData);
    const response = await this.api.post('/api/public/orders', orderData);
    console.log(`✅ Order created successfully with request ID: ${requestId}`, response.data);
    return response.data;
  }

  async createOrderLegacy(entityId: string, orderData: any, params?: { table?: number; qr?: string }) {
    const requestId = Math.random().toString(36).substr(2, 9);
    console.log(`🚀 Creating legacy order with request ID: ${requestId}`, { entityId, orderData, params });
    const response = await this.api.post(`/api/public/menu/${entityId}/order`, orderData, { params });
    console.log(`✅ Legacy order created successfully with request ID: ${requestId}`, response.data);
    return response.data;
  }

  async getOrderStatus(orderNumber: string) {
    const response = await this.api.get(`/api/public/menu/order/${orderNumber}`);
    return response.data;
  }

  // Health check
  async healthCheck() {
    try {
      const response = await this.api.get('/subscriber/health');
      return response.data;
    } catch (error) {
      console.error('Health check failed:', error);
      throw error;
    }
  }
}

export default new ApiService();
