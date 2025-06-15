import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle, Clock, AlertCircle, RefreshCw, MapPin, Phone, User } from 'lucide-react';
import ApiService from '../services/ApiService';

interface OrderItem {
  id: number;
  itemName: string;
  quantity: number;
  price: number;
}

interface Order {
  id: number;
  orderNumber: string;
  customerName: string;
  customerPhone?: string;
  tableNumber?: number;
  status: string;
  totalAmount: number;
  orderItems: OrderItem[];
  notes?: string;
  orderTime: string;
  estimatedTime?: string;
}

const OrderStatusPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadOrderStatus();
    
    // Auto-refresh every 30 seconds if order is not completed
    const interval = setInterval(() => {
      if (order && !['completed', 'cancelled'].includes(order.status.toLowerCase())) {
        refreshOrderStatus();
      }
    }, 30000);

    return () => clearInterval(interval);
  }, [orderNumber, order?.status]);

  const loadOrderStatus = async () => {
    try {
      const response = await ApiService.getOrderStatus(orderNumber!);
      setOrder(response);
      setError(null);
    } catch (err: any) {
      console.error('Failed to load order status:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load order status');
    } finally {
      setLoading(false);
    }
  };

  const refreshOrderStatus = async () => {
    setRefreshing(true);
    try {
      const response = await ApiService.getOrderStatus(orderNumber!);
      setOrder(response);
      setError(null);
    } catch (err: any) {
      console.error('Failed to refresh order status:', err);
    } finally {
      setRefreshing(false);
    }
  };

  const getStatusInfo = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return {
          color: 'text-yellow-600',
          bgColor: 'bg-yellow-50',
          icon: <Clock size={20} />,
          message: 'Your order is being reviewed',
          description: 'We\'ve received your order and are preparing to start cooking.'
        };
      case 'confirmed':
        return {
          color: 'text-blue-600',
          bgColor: 'bg-blue-50',
          icon: <CheckCircle size={20} />,
          message: 'Order confirmed',
          description: 'Your order has been confirmed and sent to the kitchen.'
        };
      case 'preparing':
        return {
          color: 'text-orange-600',
          bgColor: 'bg-orange-50',
          icon: <Clock size={20} />,
          message: 'Being prepared',
          description: 'Our chefs are currently preparing your delicious meal.'
        };
      case 'ready':
        return {
          color: 'text-green-600',
          bgColor: 'bg-green-50',
          icon: <CheckCircle size={20} />,
          message: 'Ready for pickup/serving',
          description: 'Your order is ready! We\'ll serve it to your table shortly.'
        };
      case 'completed':
        return {
          color: 'text-green-700',
          bgColor: 'bg-green-50',
          icon: <CheckCircle size={20} />,
          message: 'Order completed',
          description: 'Your order has been served. Enjoy your meal!'
        };
      case 'cancelled':
        return {
          color: 'text-red-600',
          bgColor: 'bg-red-50',
          icon: <AlertCircle size={20} />,
          message: 'Order cancelled',
          description: 'This order has been cancelled. Please contact staff if you have questions.'
        };
      default:
        return {
          color: 'text-gray-600',
          bgColor: 'bg-gray-50',
          icon: <Clock size={20} />,
          message: 'Processing',
          description: 'Your order is being processed.'
        };
    }
  };

  const getProgressPercentage = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return 20;
      case 'confirmed':
        return 40;
      case 'preparing':
        return 70;
      case 'ready':
        return 90;
      case 'completed':
        return 100;
      case 'cancelled':
        return 0;
      default:
        return 0;
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="container p-6">
        <div className="alert alert-error">
          {error || 'Order not found'}
        </div>
        <Link to="/" className="btn btn-primary mt-4">
          Back to Menu
        </Link>
      </div>
    );
  }

  const statusInfo = getStatusInfo(order.status);
  const progressPercentage = getProgressPercentage(order.status);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container p-6">
        <div className="max-w-2xl mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold mb-2">Order Status</h1>
            <p className="text-gray-600">Track your order progress</p>
          </div>

          {/* Status Card */}
          <div className="card mb-6">
            <div className="card-header">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-xl font-semibold">Order #{order.orderNumber}</h2>
                  <p className="text-gray-600 text-sm">
                    Placed on {new Date(order.orderTime).toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={refreshOrderStatus}
                  disabled={refreshing}
                  className="btn btn-outline"
                >
                  <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
                  Refresh
                </button>
              </div>
            </div>

            <div className="card-body">
              {/* Progress Bar */}
              <div className="mb-6">
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-blue-600 h-2 rounded-full transition-all duration-500"
                    style={{ width: `${progressPercentage}%` }}
                  ></div>
                </div>
              </div>

              {/* Current Status */}
              <div className={`p-4 rounded-lg ${statusInfo.bgColor} mb-6`}>
                <div className={`flex items-center gap-3 ${statusInfo.color}`}>
                  {statusInfo.icon}
                  <div>
                    <h3 className="font-semibold text-lg">{statusInfo.message}</h3>
                    <p className="text-sm opacity-80">{statusInfo.description}</p>
                  </div>
                </div>
              </div>

              {/* Customer Info */}
              <div className="mb-6">
                <h3 className="font-semibold mb-3">Customer Information</h3>
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <User size={16} className="text-gray-500" />
                    <span>{order.customerName}</span>
                  </div>
                  {order.customerPhone && (
                    <div className="flex items-center gap-2">
                      <Phone size={16} className="text-gray-500" />
                      <span>{order.customerPhone}</span>
                    </div>
                  )}
                  {order.tableNumber && (
                    <div className="flex items-center gap-2">
                      <MapPin size={16} className="text-gray-500" />
                      <span>Table {order.tableNumber}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Order Items */}
              <div className="mb-6">
                <h3 className="font-semibold mb-3">Order Items</h3>
                <div className="space-y-2">
                  {order.orderItems.map((item, index) => (
                    <div key={index} className="flex justify-between items-center py-2 border-b border-gray-100">
                      <div>
                        <span className="font-medium">{item.itemName}</span>
                        <span className="text-gray-600 ml-2">x{item.quantity}</span>
                      </div>
                      <span className="font-semibold">
                        ₹{(item.price * item.quantity).toFixed(2)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Special Instructions */}
              {order.notes && (
                <div className="mb-6">
                  <h3 className="font-semibold mb-2">Special Instructions</h3>
                  <p className="text-gray-600 bg-gray-50 p-3 rounded">{order.notes}</p>
                </div>
              )}

              {/* Total */}
              <div className="border-t pt-4">
                <div className="flex justify-between items-center text-xl font-bold">
                  <span>Total:</span>
                  <span className="text-green-600">₹{order.totalAmount.toFixed(2)}</span>
                </div>
              </div>

              {/* Estimated Time */}
              {order.estimatedTime && order.status.toLowerCase() !== 'completed' && (
                <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                  <div className="flex items-center gap-2 text-blue-700">
                    <Clock size={16} />
                    <span className="font-semibold">Estimated time: {order.estimatedTime}</span>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-4 justify-center">
            <Link to="/" className="btn btn-primary">
              Back to Menu
            </Link>
            {order.status.toLowerCase() === 'completed' && (
              <Link to="/" className="btn btn-success">
                Order Another
              </Link>
            )}
          </div>

          {/* Auto-refresh notice */}
          {!['completed', 'cancelled'].includes(order.status.toLowerCase()) && (
            <div className="mt-6 text-center text-sm text-gray-500">
              <p>This page automatically refreshes every 30 seconds</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OrderStatusPage;
