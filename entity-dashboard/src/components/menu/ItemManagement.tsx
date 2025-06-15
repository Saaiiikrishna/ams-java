import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Alert,
  CircularProgress,
  Chip,
  Avatar,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  FormControl,
  InputLabel,
  Select,
  InputAdornment,
  Switch,
  FormControlLabel
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  Restaurant as RestaurantIcon,
  AttachMoney as MoneyIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  CheckCircle as AvailableIcon,
  Cancel as UnavailableIcon
} from '@mui/icons-material';
import ApiService from '../../services/ApiService';

interface Category {
  id: number;
  name: string;
}

interface Item {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  displayOrder: number;
  isActive: boolean;
  isAvailable: boolean;
  categoryId: number;
  categoryName: string;
  createdAt: string;
  updatedAt: string;
}

interface ItemManagementProps {
  refreshTrigger: number;
}

const ItemManagement: React.FC<ItemManagementProps> = ({ refreshTrigger }) => {
  const [items, setItems] = useState<Item[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Form states
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: 0,
    imageUrl: '',
    displayOrder: 0,
    isActive: true,
    isAvailable: true,
    categoryId: 0
  });

  // Menu states
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);

  // Filter states
  const [filterCategory, setFilterCategory] = useState<number>(0);

  useEffect(() => {
    fetchData();
  }, [refreshTrigger]);

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [itemsResponse, categoriesResponse] = await Promise.all([
        ApiService.get<Item[]>('/api/menu/items'),
        ApiService.get<Category[]>('/api/menu/categories')
      ]);
      
      setItems(itemsResponse.data || []);
      setCategories(categoriesResponse.data || []);
    } catch (err: any) {
      console.error('Failed to fetch data:', err);
      setError(err.response?.data?.error || 'Failed to fetch data');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenDialog = (item?: Item) => {
    if (item) {
      setIsEditing(true);
      setEditingId(item.id);
      setFormData({
        name: item.name,
        description: item.description,
        price: item.price,
        imageUrl: item.imageUrl || '',
        displayOrder: item.displayOrder,
        isActive: item.isActive,
        isAvailable: item.isAvailable,
        categoryId: item.categoryId
      });
    } else {
      setIsEditing(false);
      setEditingId(null);
      setFormData({
        name: '',
        description: '',
        price: 0,
        imageUrl: '',
        displayOrder: items.length,
        isActive: true,
        isAvailable: true,
        categoryId: categories.length > 0 ? categories[0].id : 0
      });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setIsEditing(false);
    setEditingId(null);
  };

  const handleSubmit = async () => {
    setError(null);
    setSuccessMessage(null);

    if (!formData.name.trim() || formData.price <= 0 || formData.categoryId === 0) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      if (isEditing && editingId) {
        await ApiService.put(`/api/menu/items/${editingId}`, formData);
        setSuccessMessage('Item updated successfully!');
      } else {
        await ApiService.post('/api/menu/items', formData);
        setSuccessMessage('Item created successfully!');
      }
      
      handleCloseDialog();
      fetchData();
    } catch (err: any) {
      console.error('Failed to save item:', err);
      setError(err.response?.data?.error || 'Failed to save item');
    }
  };

  const handleDelete = async (itemId: number) => {
    if (!window.confirm('Are you sure you want to delete this item? This action cannot be undone.')) {
      return;
    }

    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.delete(`/api/menu/items/${itemId}`);
      setSuccessMessage('Item deleted successfully!');
      fetchData();
    } catch (err: any) {
      console.error('Failed to delete item:', err);
      setError(err.response?.data?.error || 'Failed to delete item');
    }
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, item: Item) => {
    setAnchorEl(event.currentTarget);
    setSelectedItem(item);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedItem(null);
  };

  const handleInputChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const filteredItems = filterCategory === 0 
    ? items 
    : items.filter(item => item.categoryId === filterCategory);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6" fontWeight="bold">
          Menu Items ({filteredItems.length})
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Filter by Category</InputLabel>
            <Select
              value={filterCategory}
              label="Filter by Category"
              onChange={(e) => setFilterCategory(Number(e.target.value))}
            >
              <MenuItem value={0}>All Categories</MenuItem>
              {categories.map((category) => (
                <MenuItem key={category.id} value={category.id}>
                  {category.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
            disabled={categories.length === 0}
          >
            Add Item
          </Button>
        </Box>
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

      {/* No Categories Warning */}
      {categories.length === 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          You need to create at least one category before adding menu items.
        </Alert>
      )}

      {/* Items Grid */}
      {filteredItems.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <RestaurantIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom>
              {filterCategory === 0 ? 'No Menu Items Found' : 'No Items in Selected Category'}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {categories.length === 0 
                ? 'Create categories first, then add menu items'
                : 'Get started by adding your first menu item'
              }
            </Typography>
            {categories.length > 0 && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => handleOpenDialog()}
              >
                Add First Item
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {filteredItems.map((item) => (
            <Grid item xs={12} sm={6} md={4} key={item.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
                        <RestaurantIcon fontSize="small" />
                      </Avatar>
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {item.name}
                        </Typography>
                        <Typography variant="body2" color="primary" fontWeight="bold">
                          ${item.price.toFixed(2)}
                        </Typography>
                      </Box>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={(e) => handleMenuClick(e, item)}
                    >
                      <MoreVertIcon />
                    </IconButton>
                  </Box>

                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {item.description || 'No description provided'}
                  </Typography>

                  <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                    <Chip
                      size="small"
                      label={item.categoryName}
                      variant="outlined"
                    />
                    <Chip
                      size="small"
                      label={item.isActive ? 'Active' : 'Inactive'}
                      color={item.isActive ? 'success' : 'default'}
                      icon={item.isActive ? <VisibilityIcon /> : <VisibilityOffIcon />}
                    />
                    <Chip
                      size="small"
                      label={item.isAvailable ? 'Available' : 'Unavailable'}
                      color={item.isAvailable ? 'success' : 'error'}
                      icon={item.isAvailable ? <AvailableIcon /> : <UnavailableIcon />}
                    />
                  </Box>

                  <Typography variant="caption" color="text.secondary">
                    Order: {item.displayOrder}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => {
          handleOpenDialog(selectedItem!);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit Item</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => {
          handleDelete(selectedItem!.id);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Delete Item</ListItemText>
        </MenuItem>
      </Menu>

      {/* Add/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {isEditing ? 'Edit Menu Item' : 'Add New Menu Item'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Item Name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Description"
              value={formData.description}
              onChange={(e) => handleInputChange('description', e.target.value)}
              margin="normal"
              multiline
              rows={3}
            />
            <TextField
              fullWidth
              label="Price"
              type="number"
              value={formData.price}
              onChange={(e) => handleInputChange('price', parseFloat(e.target.value) || 0)}
              margin="normal"
              required
              InputProps={{
                startAdornment: <InputAdornment position="start">₹</InputAdornment>,
              }}
            />
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Category</InputLabel>
              <Select
                value={formData.categoryId}
                label="Category"
                onChange={(e) => handleInputChange('categoryId', Number(e.target.value))}
              >
                {categories.map((category) => (
                  <MenuItem key={category.id} value={category.id}>
                    {category.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth
              label="Image URL (Optional)"
              value={formData.imageUrl}
              onChange={(e) => handleInputChange('imageUrl', e.target.value)}
              margin="normal"
            />
            <TextField
              fullWidth
              label="Display Order"
              type="number"
              value={formData.displayOrder}
              onChange={(e) => handleInputChange('displayOrder', parseInt(e.target.value) || 0)}
              margin="normal"
            />
            <Box sx={{ mt: 2 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) => handleInputChange('isActive', e.target.checked)}
                  />
                }
                label="Active"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isAvailable}
                    onChange={(e) => handleInputChange('isAvailable', e.target.checked)}
                  />
                }
                label="Available for Ordering"
              />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained"
            disabled={!formData.name.trim() || formData.price <= 0 || formData.categoryId === 0}
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ItemManagement;
