import axios from 'axios';

// API base URLs
const ORDER_SERVICE_URL = 'http://localhost:8090/api';
const ANALYTICS_SERVICE_URL = 'http://localhost:8091';

// Create axios instances
const orderApi = axios.create({
  baseURL: ORDER_SERVICE_URL,
  auth: {
    username: 'admin',
    password: 'admin123'
  }
});

const analyticsApi = axios.create({
  baseURL: ANALYTICS_SERVICE_URL
});

// Order Service APIs
export const orderService = {
  // Get all orders
  getOrders: (page = 0, size = 10) => 
    orderApi.get(`/orders?page=${page}&size=${size}`),
  
  // Get order by ID
  getOrder: (id) => 
    orderApi.get(`/orders/${id}`),
  
  // Create new order
  createOrder: (orderData) => 
    orderApi.post('/orders', orderData),
  
  // Update order status
  updateOrderStatus: (id, status) => 
    orderApi.put(`/orders/${id}/status?status=${status}`),
  
  // Cancel order
  cancelOrder: (id) => 
    orderApi.put(`/orders/${id}/cancel`),
  
  // Get products
  getProducts: () => 
    orderApi.get('/products'),
  
  // Get users
  getUsers: () => 
    orderApi.get('/users'),
  
  // Analytics endpoints from order service
  getRevenue: () => 
    orderApi.get('/orders/analytics/revenue'),
  
  getOrderCount: (status) => 
    orderApi.get(`/orders/analytics/count/${status}`)
};

// Analytics Service APIs
export const analyticsService = {
  // Get order metrics
  getOrderMetrics: () => 
    analyticsApi.get('/metrics/orders'),
  
  // Get real-time stats
  getRealtimeStats: () => 
    analyticsApi.get('/metrics/realtime'),
  
  // Get revenue and order trends
  getTrends: (interval = 'hour', window = interval === 'minute' ? 60 : 24) =>
    analyticsApi.get(`/metrics/trends?interval=${interval}&window=${window}`),
  
  // Get top products
  getTopProducts: (limit = 10) => 
    analyticsApi.get(`/metrics/products/top?limit=${limit}`),
  
  // Health check
  healthCheck: () => 
    analyticsApi.get('/health')
};

// WebSocket connection for real-time updates
export class RealtimeConnection {
  constructor(onMessage, onError) {
    this.ws = null;
    this.onMessage = onMessage;
    this.onError = onError;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    try {
      this.ws = new WebSocket('ws://localhost:8091/ws/realtime');
      
      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
      };
      
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          this.onMessage(data);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      };
      
      this.ws.onclose = () => {
        console.log('WebSocket disconnected');
        this.attemptReconnect();
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        if (this.onError) {
          this.onError(error);
        }
      };
    } catch (error) {
      console.error('Error creating WebSocket connection:', error);
      this.attemptReconnect();
    }
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => this.connect(), 5000);
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

export default { orderService, analyticsService, RealtimeConnection };
