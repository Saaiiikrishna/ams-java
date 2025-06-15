import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
  Chip,
  Avatar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
  Badge,
} from '@mui/material';
import {
  Restaurant,
  ShoppingCart,
  TableRestaurant,
  TrendingUp,
  LocalDining,
  Receipt,
  Refresh,
  Visibility,
  MenuBook,
  AttachMoney,
  Schedule,
  CheckCircle,
  AccessTime,
  Cancel,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface MenuOrderingData {
  totalCategories: number;
  totalItems: number;
  totalTables: number;
  todayOrders: number;
  totalRevenue: number;
  activeOrders: number;
  recentOrders: Order[];
  popularItems: MenuItem[];
  tableStatus: RestaurantTable[];
}

interface Order {
  id: number;
  orderNumber: string;
  customerName?: string;
  tableNumber?: string;
  items: OrderItem[];
  totalAmount: number;
  status: 'PENDING' | 'PREPARING' | 'READY' | 'SERVED' | 'CANCELLED';
  orderTime: string;
  estimatedTime?: string;
}

interface OrderItem {
  id: number;
  itemName: string;
  quantity: number;
  price: number;
}

interface MenuItem {
  id: number;
  name: string;
  category: string;
  price: number;
  isAvailable: boolean;
  orderCount?: number;
  image?: string;
}

interface RestaurantTable {
  id: number;
  tableNumber: string;
  capacity: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'MAINTENANCE';
  currentOrder?: number;
}

const MenuOrderingOverview: React.FC = () => {
  const [menuData, setMenuData] = useState<MenuOrderingData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [orderDetailsOpen, setOrderDetailsOpen] = useState(false);

  const fetchMenuOrderingData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [categoriesResponse, itemsResponse, tablesResponse, ordersResponse] = await Promise.all([
        ApiService.get('/api/menu/categories'),
        ApiService.get('/api/menu/items'),
        ApiService.get('/api/tables'),
        ApiService.get('/api/orders/today'),
      ]);

      const categories = categoriesResponse.data || [];
      const items = itemsResponse.data || [];
      const tables = tablesResponse.data || [];
      const orders = ordersResponse.data || [];

      const data: MenuOrderingData = {
        totalCategories: categories.length,
        totalItems: items.length,
        totalTables: tables.length,
        todayOrders: orders.length,
        totalRevenue: orders.reduce((sum: number, order: any) => sum + (order.totalAmount || 0), 0),
        activeOrders: orders.filter((o: any) => o.status === 'PENDING' || o.status === 'PREPARING').length,
        recentOrders: orders.slice(0, 10).map((order: any) => ({
          id: order.id,
          orderNumber: order.orderNumber || `ORD-${order.id}`,
          customerName: order.customerName,
          tableNumber: order.table?.tableNumber,
          items: order.orderItems?.map((item: any) => ({
            id: item.id,
            itemName: item.item?.name || 'Unknown Item',
            quantity: item.quantity,
            price: item.price,
          })) || [],
          totalAmount: order.totalAmount || 0,
          status: order.status || 'PENDING',
          orderTime: new Date(order.createdAt || Date.now()).toLocaleTimeString(),
          estimatedTime: order.estimatedTime,
        })),
        popularItems: items.slice(0, 5).map((item: any) => ({
          id: item.id,
          name: item.name,
          category: item.category?.name || 'Uncategorized',
          price: item.price || 0,
          isAvailable: item.isAvailable !== false,
          orderCount: Math.floor(Math.random() * 50) + 1, // Mock data for now
          image: item.image,
        })),
        tableStatus: tables.map((table: any) => ({
          id: table.id,
          tableNumber: table.tableNumber,
          capacity: table.capacity || 4,
          status: table.status || 'AVAILABLE',
          currentOrder: table.currentOrder,
        })),
      };

      setMenuData(data);
    } catch (err: any) {
      console.error('Failed to fetch menu/ordering data:', err);
      setError('Failed to load menu and ordering data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMenuOrderingData();

    // Auto-refresh every 30 seconds for real-time order updates
    const interval = setInterval(fetchMenuOrderingData, 30 * 1000);
    return () => clearInterval(interval);
  }, []);

  const handleOrderDetails = (order: Order) => {
    setSelectedOrder(order);
    setOrderDetailsOpen(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'PREPARING': return 'info';
      case 'READY': return 'success';
      case 'SERVED': return 'default';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  const getTableStatusColor = (status: string) => {
    switch (status) {
      case 'AVAILABLE': return 'success';
      case 'OCCUPIED': return 'error';
      case 'RESERVED': return 'warning';
      case 'MAINTENANCE': return 'default';
      default: return 'default';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  if (!menuData) {
    return (
      <Alert severity="warning" sx={{ m: 2 }}>
        No menu and ordering data available.
      </Alert>
    );
  }

  const { totalCategories, totalItems, totalTables, todayOrders, totalRevenue, activeOrders, recentOrders, popularItems, tableStatus } = menuData;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Menu & Ordering System
        </Typography>
        <Button
          variant="outlined"
          startIcon={<Refresh />}
          onClick={fetchMenuOrderingData}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #7b1fa2, #9c27b0)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalCategories}
                  </Typography>
                  <Typography variant="h6">
                    Categories
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <MenuBook />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #1976d2, #42a5f5)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalItems}
                  </Typography>
                  <Typography variant="h6">
                    Menu Items
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <LocalDining />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #0288d1, #03a9f4)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalTables}
                  </Typography>
                  <Typography variant="h6">
                    Tables
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <TableRestaurant />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #ed6c02, #ff9800)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {todayOrders}
                  </Typography>
                  <Typography variant="h6">
                    Today's Orders
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <ShoppingCart />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #d32f2f, #f44336)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {activeOrders}
                  </Typography>
                  <Typography variant="h6">
                    Active Orders
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <AccessTime />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={2}>
          <Card sx={{ background: 'linear-gradient(135deg, #2e7d32, #4caf50)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    ₹{totalRevenue.toLocaleString()}
                  </Typography>
                  <Typography variant="h6">
                    Revenue
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 48, height: 48 }}>
                  <AttachMoney />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Orders and Table Status */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Receipt color="primary" />
                Recent Orders
                {activeOrders > 0 && (
                  <Badge badgeContent={activeOrders} color="error">
                    <Chip label="Active" size="small" color="warning" />
                  </Badge>
                )}
              </Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Order #</TableCell>
                      <TableCell>Table</TableCell>
                      <TableCell>Items</TableCell>
                      <TableCell>Amount</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Time</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recentOrders.map((order) => (
                      <TableRow key={order.id}>
                        <TableCell>{order.orderNumber}</TableCell>
                        <TableCell>{order.tableNumber || '-'}</TableCell>
                        <TableCell>{order.items.length} items</TableCell>
                        <TableCell>₹{order.totalAmount.toLocaleString()}</TableCell>
                        <TableCell>
                          <Chip
                            label={order.status}
                            size="small"
                            color={getStatusColor(order.status) as any}
                          />
                        </TableCell>
                        <TableCell>{order.orderTime}</TableCell>
                        <TableCell>
                          <Tooltip title="View Details">
                            <IconButton onClick={() => handleOrderDetails(order)}>
                              <Visibility />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TableRestaurant color="primary" />
                Table Status
              </Typography>
              <List dense>
                {tableStatus.slice(0, 8).map((table) => (
                  <ListItem key={table.id}>
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: `${getTableStatusColor(table.status)}.main` }}>
                        {table.tableNumber}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={`Table ${table.tableNumber}`}
                      secondary={
                        <Box>
                          <Typography variant="caption" display="block">
                            Capacity: {table.capacity} people
                          </Typography>
                          <Chip
                            label={table.status}
                            size="small"
                            color={getTableStatusColor(table.status) as any}
                            sx={{ mt: 0.5 }}
                          />
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Popular Items */}
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TrendingUp color="primary" />
                Popular Menu Items
              </Typography>
              <Grid container spacing={2}>
                {popularItems.map((item) => (
                  <Grid item xs={12} sm={6} md={2.4} key={item.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                          {item.name}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                          {item.category}
                        </Typography>
                        <Typography variant="h6" color="primary" gutterBottom>
                          ₹{item.price}
                        </Typography>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Chip
                            label={item.isAvailable ? 'Available' : 'Unavailable'}
                            size="small"
                            color={item.isAvailable ? 'success' : 'error'}
                          />
                          <Typography variant="caption" color="text.secondary">
                            {item.orderCount} orders
                          </Typography>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Order Details Dialog */}
      <Dialog open={orderDetailsOpen} onClose={() => setOrderDetailsOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Order Details</DialogTitle>
        <DialogContent>
          {selectedOrder && (
            <Box>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Order Number:</Typography>
                  <Typography variant="body2">{selectedOrder.orderNumber}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Table:</Typography>
                  <Typography variant="body2">{selectedOrder.tableNumber || 'N/A'}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Order Time:</Typography>
                  <Typography variant="body2">{selectedOrder.orderTime}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Status:</Typography>
                  <Chip
                    label={selectedOrder.status}
                    size="small"
                    color={getStatusColor(selectedOrder.status) as any}
                  />
                </Grid>
              </Grid>
              
              <Divider sx={{ my: 2 }} />
              
              <Typography variant="h6" gutterBottom>Order Items</Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Item</TableCell>
                      <TableCell align="right">Quantity</TableCell>
                      <TableCell align="right">Price</TableCell>
                      <TableCell align="right">Total</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedOrder.items.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.itemName}</TableCell>
                        <TableCell align="right">{item.quantity}</TableCell>
                        <TableCell align="right">₹{item.price}</TableCell>
                        <TableCell align="right">₹{item.quantity * item.price}</TableCell>
                      </TableRow>
                    ))}
                    <TableRow>
                      <TableCell colSpan={3} sx={{ fontWeight: 'bold' }}>Total Amount:</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                        ₹{selectedOrder.totalAmount}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOrderDetailsOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MenuOrderingOverview;
