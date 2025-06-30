import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { ShoppingCart, Plus, Minus, MapPin, Users } from 'lucide-react';
import ApiService from '../services/ApiService';

interface MenuItem {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  isActive: boolean;
}

interface Category {
  id: number;
  name: string;
  description: string;
  items: MenuItem[];
}

interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
}

interface TableInfo {
  tableNumber: number;
  capacity: number;
  location: string;
  menuUrl: string;
}

const MenuPage: React.FC = () => {
  const { entityId, tableId } = useParams<{ entityId?: string; tableId?: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const tableNumber = searchParams.get('table');
  const qrCode = searchParams.get('qr');
  
  const [categories, setCategories] = useState<Category[]>([]);
  const [tableInfo, setTableInfo] = useState<TableInfo | null>(null);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCart, setShowCart] = useState(false);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [orderNotes, setOrderNotes] = useState('');
  const [placingOrder, setPlacingOrder] = useState(false);

  useEffect(() => {
    loadMenuData();
    if (qrCode) {
      loadTableInfo();
    }
  }, [entityId, tableId, qrCode]);

  const loadMenuData = async () => {
    try {
      let response;
      if (tableId) {
        // New table-based route
        response = await ApiService.getTableMenu(tableId);
        setCategories(response.menu);
        setTableInfo({
          tableNumber: response.tableNumber,
          capacity: 4, // Default capacity
          location: '',
          menuUrl: ''
        });
      } else if (entityId) {
        // Legacy entity-based route
        const params = tableNumber && qrCode ? { table: parseInt(tableNumber), qr: qrCode } : undefined;
        response = await ApiService.getMenuByEntity(entityId, params);
        setCategories(response.menu || response);
      } else {
        throw new Error('No valid route parameters');
      }
    } catch (err: any) {
      console.error('Failed to load menu:', err);
      setError(err.response?.data?.error || err.message || 'Failed to load menu');
    } finally {
      setLoading(false);
    }
  };

  const loadTableInfo = async () => {
    try {
      const response = await ApiService.getTableByQr(qrCode!);
      setTableInfo({
        tableNumber: response.tableNumber,
        capacity: response.capacity,
        location: response.location,
        menuUrl: response.menuUrl
      });
    } catch (err: any) {
      console.error('Failed to load table info:', err);
      // Set default table info if QR validation fails
      if (tableNumber) {
        setTableInfo({
          tableNumber: parseInt(tableNumber),
          capacity: 4,
          location: '',
          menuUrl: ''
        });
      }
    }
  };

  const addToCart = (item: MenuItem) => {
    setCart(prevCart => {
      const existingItem = prevCart.find(cartItem => cartItem.id === item.id);
      if (existingItem) {
        return prevCart.map(cartItem =>
          cartItem.id === item.id
            ? { ...cartItem, quantity: cartItem.quantity + 1 }
            : cartItem
        );
      } else {
        return [...prevCart, { id: item.id, name: item.name, price: item.price, quantity: 1 }];
      }
    });
  };

  const removeFromCart = (itemId: number) => {
    setCart(prevCart => {
      const existingItem = prevCart.find(cartItem => cartItem.id === itemId);
      if (existingItem && existingItem.quantity > 1) {
        return prevCart.map(cartItem =>
          cartItem.id === itemId
            ? { ...cartItem, quantity: cartItem.quantity - 1 }
            : cartItem
        );
      } else {
        return prevCart.filter(cartItem => cartItem.id !== itemId);
      }
    });
  };

  const getTotalAmount = () => {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
  };

  const getTotalItems = () => {
    return cart.reduce((total, item) => total + item.quantity, 0);
  };

  const placeOrder = async () => {
    // Prevent multiple submissions
    if (placingOrder) {
      console.log('Order already being placed, ignoring duplicate request');
      return;
    }

    if (cart.length === 0) {
      setError('Please add items to your cart');
      return;
    }

    if (!customerName.trim()) {
      setError('Please enter your name');
      return;
    }

    console.log('Starting order placement...');
    setPlacingOrder(true);
    setError(null);

    try {
      const orderData = {
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim() || null,
        notes: orderNotes.trim() || null,
        tableId: tableId ? parseInt(tableId) : null,
        tableNumber: tableNumber ? parseInt(tableNumber) : null,
        items: cart.map(item => ({
          id: item.id,
          qty: item.quantity
        }))
      };

      console.log('Order data prepared:', orderData);
      console.log('Using tableId route:', !!tableId);

      let response;
      if (tableId) {
        // New table-based route
        console.log('Calling createOrder with tableId:', tableId);
        response = await ApiService.createOrder(orderData);
      } else {
        // Legacy entity-based route
        console.log('Calling legacy createOrderLegacy with entityId:', entityId);
        const legacyOrderData = {
          ...orderData,
          orderItems: cart.map(item => ({
            itemId: item.id,
            quantity: item.quantity,
            price: item.price
          }))
        };
        const params = tableNumber && qrCode ? { table: parseInt(tableNumber), qr: qrCode } : undefined;
        console.log('Legacy order data:', legacyOrderData);
        console.log('Legacy params:', params);
        response = await ApiService.createOrderLegacy(entityId!, legacyOrderData, params);
      }

      console.log('Order response received:', response);

      // Navigate to order confirmation
      const orderNumber = response.orderNumber || response.data?.orderNumber;
      console.log('Navigating to confirmation with order number:', orderNumber);
      navigate(`/order/confirmation/${orderNumber}`);
    } catch (err: any) {
      console.error('Order placement failed:', err);
      setError(err.response?.data?.error || 'Failed to place order');
    } finally {
      console.log('Order placement completed, resetting placingOrder flag');
      setPlacingOrder(false);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
      </div>
    );
  }

  if (error && !categories.length) {
    return (
      <div className="container p-6">
        <div className="alert alert-error">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white shadow-sm">
        <div className="container p-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold">Menu</h1>
              {tableInfo && (
                <div className="flex items-center gap-4 text-sm text-gray-600 mt-1">
                  <span className="flex items-center gap-1">
                    <MapPin size={16} />
                    Table {tableInfo.tableNumber}
                  </span>
                  <span className="flex items-center gap-1">
                    <Users size={16} />
                    {tableInfo.capacity} seats
                  </span>
                  {tableInfo.location && (
                    <span>{tableInfo.location}</span>
                  )}
                </div>
              )}
            </div>
            <button
              onClick={() => setShowCart(true)}
              className="btn btn-primary relative"
            >
              <ShoppingCart size={20} />
              Cart
              {getTotalItems() > 0 && (
                <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-6 h-6 flex items-center justify-center">
                  {getTotalItems()}
                </span>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Menu Content */}
      <div className="container p-4">
        {error && !categories.length && (
          <div className="alert alert-error mb-4">
            {error}
          </div>
        )}

        {categories.map(category => (
          <div key={category.id} className="mb-8">
            <div className="mb-4">
              <h2 className="text-xl font-semibold">{category.name}</h2>
              {category.description && (
                <p className="text-gray-600 text-sm">{category.description}</p>
              )}
            </div>
            
            <div className="grid grid-cols-1 gap-4">
              {category.items.filter(item => item.isActive).map(item => (
                <div key={item.id} className="card">
                  <div className="card-body">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <h3 className="font-semibold mb-2">{item.name}</h3>
                        {item.description && (
                          <p className="text-gray-600 text-sm mb-2">{item.description}</p>
                        )}
                        <p className="text-lg font-bold text-green-600">
                          ₹{item.price.toFixed(2)}
                        </p>
                      </div>
                      {item.imageUrl && (
                        <img
                          src={item.imageUrl}
                          alt={item.name}
                          className="w-20 h-20 object-cover rounded ml-4"
                        />
                      )}
                    </div>
                    <div className="flex justify-between items-center mt-4">
                      <div className="flex items-center gap-2">
                        {cart.find(cartItem => cartItem.id === item.id) ? (
                          <>
                            <button
                              onClick={() => removeFromCart(item.id)}
                              className="btn btn-outline"
                              style={{ padding: '0.5rem' }}
                            >
                              <Minus size={16} />
                            </button>
                            <span className="mx-2 font-semibold">
                              {cart.find(cartItem => cartItem.id === item.id)?.quantity || 0}
                            </span>
                            <button
                              onClick={() => addToCart(item)}
                              className="btn btn-primary"
                              style={{ padding: '0.5rem' }}
                            >
                              <Plus size={16} />
                            </button>
                          </>
                        ) : (
                          <button
                            onClick={() => addToCart(item)}
                            className="btn btn-primary"
                          >
                            <Plus size={16} />
                            Add to Cart
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Cart Modal */}
      {showCart && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-end">
          <div className="bg-white w-full max-h-[80vh] overflow-y-auto rounded-t-lg">
            <div className="p-4 border-b">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-semibold">Your Order</h2>
                <button
                  onClick={() => setShowCart(false)}
                  className="btn btn-outline"
                >
                  Close
                </button>
              </div>
            </div>
            
            <div className="p-4">
              {cart.length === 0 ? (
                <p className="text-center text-gray-500 py-8">Your cart is empty</p>
              ) : (
                <>
                  {cart.map(item => (
                    <div key={item.id} className="flex justify-between items-center py-3 border-b">
                      <div>
                        <h4 className="font-semibold">{item.name}</h4>
                        <p className="text-gray-600">₹{item.price.toFixed(2)} each</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => removeFromCart(item.id)}
                          className="btn btn-outline"
                          style={{ padding: '0.25rem 0.5rem' }}
                        >
                          <Minus size={14} />
                        </button>
                        <span className="mx-2 font-semibold">{item.quantity}</span>
                        <button
                          onClick={() => addToCart({ id: item.id, name: item.name, price: item.price, description: '', isActive: true })}
                          className="btn btn-primary"
                          style={{ padding: '0.25rem 0.5rem' }}
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                    </div>
                  ))}
                  
                  <div className="mt-4 pt-4 border-t">
                    <div className="flex justify-between items-center text-lg font-bold mb-4">
                      <span>Total:</span>
                      <span>₹{getTotalAmount().toFixed(2)}</span>
                    </div>
                    
                    <div className="space-y-3">
                      <div className="form-group">
                        <label className="form-label">Name *</label>
                        <input
                          type="text"
                          className="form-input"
                          value={customerName}
                          onChange={(e) => setCustomerName(e.target.value)}
                          placeholder="Enter your name"
                          required
                        />
                      </div>
                      
                      <div className="form-group">
                        <label className="form-label">Phone (optional)</label>
                        <input
                          type="tel"
                          className="form-input"
                          value={customerPhone}
                          onChange={(e) => setCustomerPhone(e.target.value)}
                          placeholder="Enter your phone number"
                        />
                      </div>
                      
                      <div className="form-group">
                        <label className="form-label">Special Instructions (optional)</label>
                        <textarea
                          className="form-input"
                          value={orderNotes}
                          onChange={(e) => setOrderNotes(e.target.value)}
                          placeholder="Any special requests or dietary requirements"
                          rows={3}
                        />
                      </div>
                    </div>
                    
                    <button
                      onClick={placeOrder}
                      disabled={placingOrder || cart.length === 0}
                      className="btn btn-success w-full mt-4"
                    >
                      {placingOrder ? 'Placing Order...' : `Place Order - ₹${getTotalAmount().toFixed(2)}`}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MenuPage;
