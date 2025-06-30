import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle, Clock, MapPin, Phone, User } from 'lucide-react';
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

const OrderConfirmationPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadOrderDetails();
  }, [orderNumber]);

  const loadOrderDetails = async () => {
    try {
      const response = await ApiService.getOrderStatus(orderNumber!);
      setOrder(response);
    } catch (err: any) {
      console.error('Failed to load order details:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load order details');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'text-yellow-600';
      case 'confirmed':
        return 'text-blue-600';
      case 'preparing':
        return 'text-orange-600';
      case 'ready':
        return 'text-green-600';
      case 'completed':
        return 'text-green-700';
      case 'cancelled':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'pending':
      case 'confirmed':
      case 'preparing':
        return <Clock size={20} />;
      case 'ready':
      case 'completed':
        return <CheckCircle size={20} />;
      default:
        return <Clock size={20} />;
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

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container p-6">
        <div className="max-w-2xl mx-auto">
          {/* Success Header */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
              <CheckCircle size={32} className="text-green-600" />
            </div>
            <h1 className="text-3xl font-bold text-green-600 mb-2">Order Confirmed!</h1>
            <p className="text-gray-600">Thank you for your order. We'll prepare it shortly.</p>
          </div>

          {/* Order Details Card */}
          <div className="card mb-6">
            <div className="card-header">
              <div className="flex justify-between items-start">
                <div>
                  <h2 className="text-xl font-semibold">Order #{order.orderNumber}</h2>
                  <p className="text-gray-600 text-sm">
                    Placed on {new Date(order.orderTime).toLocaleString()}
                  </p>
                </div>
                <div className={`flex items-center gap-2 ${getStatusColor(order.status)}`}>
                  {getStatusIcon(order.status)}
                  <span className="font-semibold capitalize">{order.status}</span>
                </div>
              </div>
            </div>

            <div className="card-body">
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
                <div className="space-y-3">
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
              {order.estimatedTime && (
                <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                  <div className="flex items-center gap-2 text-blue-700">
                    <Clock size={16} />
                    <span className="font-semibold">Estimated preparation time: {order.estimatedTime}</span>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-4 justify-center">
            <Link
              to={`/order/status/${order.orderNumber}`}
              className="btn btn-primary"
            >
              Track Order Status
            </Link>
            <Link
              to="/"
              className="btn btn-outline"
            >
              Back to Menu
            </Link>
          </div>

          {/* Additional Info */}
          <div className="mt-8 p-4 bg-yellow-50 rounded-lg">
            <h3 className="font-semibold text-yellow-800 mb-2">What's Next?</h3>
            <ul className="text-yellow-700 text-sm space-y-1">
              <li>• Your order has been sent to the kitchen</li>
              <li>• You'll receive updates as your order progresses</li>
              <li>• We'll notify you when your order is ready</li>
              {order.tableNumber && (
                <li>• Your order will be served to Table {order.tableNumber}</li>
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmationPage;
