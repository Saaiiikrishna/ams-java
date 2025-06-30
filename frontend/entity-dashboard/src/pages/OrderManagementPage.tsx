import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  Tabs,
  Tab,
  Alert,
  CircularProgress,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import {
  ShoppingCart,
  Refresh as RefreshIcon,
  MoreVert as MoreVertIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Cancel as CancelIcon,
  CheckCircle as CompleteIcon,
  Schedule as PendingIcon,
  Restaurant as RestaurantIcon
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface OrderItem {
  id: number;
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
  subtotal: number;
  specialInstructions?: string;
}

interface Order {
  id: number;
  orderNumber: string;
  tableNumber?: number;
  customerName?: string;
  customerPhone?: string;
  status: 'PENDING' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'COMPLETED' | 'CANCELLED';
  totalAmount: number;
  orderItems: OrderItem[];
  notes?: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`order-tabpanel-${index}`}
      aria-labelledby={`order-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const OrderManagementPage: React.FC = () => {
  const [currentTab, setCurrentTab] = useState(0);
  const [orders, setOrders] = useState<Order[]>([]);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Menu states
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  // Dialog states
  const [orderDetailOpen, setOrderDetailOpen] = useState(false);
  const [viewingOrder, setViewingOrder] = useState<Order | null>(null);

  useEffect(() => {
    checkOrderPermission();
  }, []);

  useEffect(() => {
    if (hasPermission) {
      fetchOrders();

      // Set up real-time polling for orders (every 10 seconds for immediate updates)
      const interval = setInterval(() => {
        fetchOrders();
      }, 10000);

      return () => clearInterval(interval);
    }
  }, [hasPermission, currentTab]);

  const checkOrderPermission = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get('/api/entity/permissions/check/ORDER_MANAGEMENT');
      setHasPermission(response.data.hasPermission);
    } catch (err: any) {
      console.error('Failed to check order permission:', err);
      setError('Failed to check permissions');
      setHasPermission(false);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchOrders = async () => {
    setError(null);
    try {
      let endpoint = '/api/orders';
      
      switch (currentTab) {
        case 1:
          endpoint = '/api/orders/pending';
          break;
        case 2:
          endpoint = '/api/orders/today';
          break;
        default:
          endpoint = '/api/orders';
      }

      const response = await ApiService.get<Order[]>(endpoint);
      setOrders(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch orders:', err);
      setError(err.response?.data?.error || 'Failed to fetch orders');
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, order: Order) => {
    setAnchorEl(event.currentTarget);
    setSelectedOrder(order);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedOrder(null);
  };

  const handleViewOrder = (order: Order) => {
    setViewingOrder(order);
    setOrderDetailOpen(true);
    handleMenuClose();
  };

  const handleUpdateOrderStatus = async (orderId: number, newStatus: string) => {
    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.put(`/api/orders/${orderId}/status`, { status: newStatus });
      setSuccessMessage(`Order status updated to ${newStatus}`);
      fetchOrders();
    } catch (err: any) {
      console.error('Failed to update order status:', err);
      setError(err.response?.data?.error || 'Failed to update order status');
    }
    handleMenuClose();
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'warning';
      case 'CONFIRMED':
        return 'info';
      case 'PREPARING':
        return 'primary';
      case 'READY':
        return 'success';
      case 'COMPLETED':
        return 'success';
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <PendingIcon />;
      case 'CONFIRMED':
      case 'PREPARING':
        return <RestaurantIcon />;
      case 'READY':
      case 'COMPLETED':
        return <CompleteIcon />;
      case 'CANCELLED':
        return <CancelIcon />;
      default:
        return <PendingIcon />;
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
        <Typography variant="h6" sx={{ ml: 2 }}>
          Checking permissions...
        </Typography>
      </Box>
    );
  }

  if (!hasPermission) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning" sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Order Management Access Restricted
          </Typography>
          <Typography variant="body1">
            You don't have permission to access the Order Management system. 
            Please contact your super administrator to request access.
          </Typography>
        </Alert>
        
        <Card sx={{ mt: 3 }}>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <ShoppingCart sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h5" color="text.secondary" gutterBottom>
              Order Management Unavailable
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Contact your administrator to enable order management features.
            </Typography>
            <Button 
              variant="outlined" 
              onClick={checkOrderPermission}
              startIcon={<ShoppingCart />}
            >
              Check Permissions Again
            </Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Order Management
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage customer orders and track order status
          </Typography>
        </Box>
        <Button
          variant="outlined"
          onClick={fetchOrders}
          startIcon={<RefreshIcon />}
        >
          Refresh
        </Button>
      </Box>

      {/* Messages */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {/* Order Stats */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <PendingIcon sx={{ fontSize: 40, color: 'warning.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {orders.filter(o => o.status === 'PENDING').length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Pending
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <RestaurantIcon sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {orders.filter(o => ['CONFIRMED', 'PREPARING'].includes(o.status)).length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                In Progress
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <CompleteIcon sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {orders.filter(o => o.status === 'READY').length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Ready
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <ShoppingCart sx={{ fontSize: 40, color: 'info.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {orders.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Orders
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={currentTab} onChange={handleTabChange} aria-label="order management tabs">
            <Tab label="All Orders" />
            <Tab label="Pending Orders" />
            <Tab label="Today's Orders" />
          </Tabs>
        </Box>

        <TabPanel value={currentTab} index={0}>
          <OrderTable 
            orders={orders} 
            onMenuClick={handleMenuClick}
            onViewOrder={handleViewOrder}
          />
        </TabPanel>

        <TabPanel value={currentTab} index={1}>
          <OrderTable 
            orders={orders.filter(o => ['PENDING', 'CONFIRMED', 'PREPARING', 'READY'].includes(o.status))} 
            onMenuClick={handleMenuClick}
            onViewOrder={handleViewOrder}
          />
        </TabPanel>

        <TabPanel value={currentTab} index={2}>
          <OrderTable 
            orders={orders} 
            onMenuClick={handleMenuClick}
            onViewOrder={handleViewOrder}
          />
        </TabPanel>
      </Card>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => handleViewOrder(selectedOrder!)}>
          <ListItemIcon>
            <ViewIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>View Details</ListItemText>
        </MenuItem>
        {selectedOrder && selectedOrder.status === 'PENDING' && (
          <MenuItem onClick={() => handleUpdateOrderStatus(selectedOrder.id, 'CONFIRMED')}>
            <ListItemIcon>
              <CompleteIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Confirm Order</ListItemText>
          </MenuItem>
        )}
        {selectedOrder && selectedOrder.status === 'CONFIRMED' && (
          <MenuItem onClick={() => handleUpdateOrderStatus(selectedOrder.id, 'PREPARING')}>
            <ListItemIcon>
              <RestaurantIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Start Preparing</ListItemText>
          </MenuItem>
        )}
        {selectedOrder && selectedOrder.status === 'PREPARING' && (
          <MenuItem onClick={() => handleUpdateOrderStatus(selectedOrder.id, 'READY')}>
            <ListItemIcon>
              <CompleteIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Mark Ready</ListItemText>
          </MenuItem>
        )}
        {selectedOrder && selectedOrder.status === 'READY' && (
          <MenuItem onClick={() => handleUpdateOrderStatus(selectedOrder.id, 'COMPLETED')}>
            <ListItemIcon>
              <CompleteIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Complete Order</ListItemText>
          </MenuItem>
        )}
        {selectedOrder && !['COMPLETED', 'CANCELLED'].includes(selectedOrder.status) && (
          <MenuItem onClick={() => handleUpdateOrderStatus(selectedOrder.id, 'CANCELLED')}>
            <ListItemIcon>
              <CancelIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Cancel Order</ListItemText>
          </MenuItem>
        )}
      </Menu>

      {/* Order Detail Dialog */}
      <Dialog open={orderDetailOpen} onClose={() => setOrderDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          Order Details - {viewingOrder?.orderNumber}
        </DialogTitle>
        <DialogContent>
          {viewingOrder && (
            <Box>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Customer</Typography>
                  <Typography variant="body1">
                    {viewingOrder.customerName || 'Walk-in Customer'}
                  </Typography>
                  {viewingOrder.customerPhone && (
                    <Typography variant="body2" color="text.secondary">
                      {viewingOrder.customerPhone}
                    </Typography>
                  )}
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Table</Typography>
                  <Typography variant="body1">
                    {viewingOrder.tableNumber ? `Table ${viewingOrder.tableNumber}` : 'Takeaway'}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Status</Typography>
                  <Chip
                    label={viewingOrder.status}
                    color={getStatusColor(viewingOrder.status) as any}
                    icon={getStatusIcon(viewingOrder.status)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="text.secondary">Total Amount</Typography>
                  <Typography variant="h6" color="primary">
                    ${viewingOrder.totalAmount.toFixed(2)}
                  </Typography>
                </Grid>
              </Grid>

              <Typography variant="h6" gutterBottom>Order Items</Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Item</TableCell>
                      <TableCell align="center">Qty</TableCell>
                      <TableCell align="right">Price</TableCell>
                      <TableCell align="right">Subtotal</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {viewingOrder.orderItems.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>
                          <Typography variant="body2" fontWeight="bold">
                            {item.itemName}
                          </Typography>
                          {item.specialInstructions && (
                            <Typography variant="caption" color="text.secondary">
                              Note: {item.specialInstructions}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell align="center">{item.quantity}</TableCell>
                        <TableCell align="right">${item.price.toFixed(2)}</TableCell>
                        <TableCell align="right">${item.subtotal.toFixed(2)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {viewingOrder.notes && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" color="text.secondary">Notes</Typography>
                  <Typography variant="body2">{viewingOrder.notes}</Typography>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOrderDetailOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

// Order Table Component
interface OrderTableProps {
  orders: Order[];
  onMenuClick: (event: React.MouseEvent<HTMLElement>, order: Order) => void;
  onViewOrder: (order: Order) => void;
}

const OrderTable: React.FC<OrderTableProps> = ({ orders, onMenuClick, onViewOrder }) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'CONFIRMED': return 'info';
      case 'PREPARING': return 'primary';
      case 'READY': return 'success';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  if (orders.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <ShoppingCart sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary" gutterBottom>
          No Orders Found
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Orders will appear here when customers place them
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Order #</TableCell>
            <TableCell>Customer</TableCell>
            <TableCell>Table</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Total</TableCell>
            <TableCell>Time</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {orders.map((order) => (
            <TableRow key={order.id} hover>
              <TableCell>
                <Typography variant="body2" fontWeight="bold">
                  {order.orderNumber}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2">
                  {order.customerName || 'Walk-in'}
                </Typography>
                {order.customerPhone && (
                  <Typography variant="caption" color="text.secondary" display="block">
                    {order.customerPhone}
                  </Typography>
                )}
              </TableCell>
              <TableCell>
                {order.tableNumber ? `Table ${order.tableNumber}` : 'Takeaway'}
              </TableCell>
              <TableCell>
                <Chip
                  label={order.status}
                  color={getStatusColor(order.status) as any}
                  size="small"
                />
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight="bold">
                  ₹{order.totalAmount.toFixed(2)}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2">
                  {new Date(order.createdAt).toLocaleTimeString()}
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block">
                  {new Date(order.createdAt).toLocaleDateString()}
                </Typography>
              </TableCell>
              <TableCell align="center">
                <IconButton
                  size="small"
                  onClick={(e) => onMenuClick(e, order)}
                >
                  <MoreVertIcon />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default OrderManagementPage;
