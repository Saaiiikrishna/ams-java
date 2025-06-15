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
  ListItemText
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  Category as CategoryIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  DragIndicator as DragIcon
} from '@mui/icons-material';
import ApiService from '../../services/ApiService';

interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
  displayOrder: number;
  isActive: boolean;
  items?: any[];
  createdAt: string;
  updatedAt: string;
}

interface CategoryManagementProps {
  refreshTrigger: number;
}

const CategoryManagement: React.FC<CategoryManagementProps> = ({ refreshTrigger }) => {
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
    imageUrl: '',
    displayOrder: 0,
    isActive: true
  });

  // Menu states
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);

  useEffect(() => {
    fetchCategories();
  }, [refreshTrigger]);

  const fetchCategories = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<Category[]>('/api/menu/categories');
      setCategories(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch categories:', err);
      setError(err.response?.data?.error || 'Failed to fetch categories');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenDialog = (category?: Category) => {
    if (category) {
      setIsEditing(true);
      setEditingId(category.id);
      setFormData({
        name: category.name,
        description: category.description,
        imageUrl: category.imageUrl || '',
        displayOrder: category.displayOrder,
        isActive: category.isActive
      });
    } else {
      setIsEditing(false);
      setEditingId(null);
      setFormData({
        name: '',
        description: '',
        imageUrl: '',
        displayOrder: categories.length,
        isActive: true
      });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setIsEditing(false);
    setEditingId(null);
    setFormData({
      name: '',
      description: '',
      imageUrl: '',
      displayOrder: 0,
      isActive: true
    });
  };

  const handleSubmit = async () => {
    setError(null);
    setSuccessMessage(null);

    try {
      if (isEditing && editingId) {
        await ApiService.put(`/api/menu/categories/${editingId}`, formData);
        setSuccessMessage('Category updated successfully!');
      } else {
        await ApiService.post('/api/menu/categories', formData);
        setSuccessMessage('Category created successfully!');
      }
      
      handleCloseDialog();
      fetchCategories();
    } catch (err: any) {
      console.error('Failed to save category:', err);
      setError(err.response?.data?.error || 'Failed to save category');
    }
  };

  const handleDelete = async (categoryId: number) => {
    if (!window.confirm('Are you sure you want to delete this category? This action cannot be undone.')) {
      return;
    }

    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.delete(`/api/menu/categories/${categoryId}`);
      setSuccessMessage('Category deleted successfully!');
      fetchCategories();
    } catch (err: any) {
      console.error('Failed to delete category:', err);
      setError(err.response?.data?.error || 'Failed to delete category');
    }
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, category: Category) => {
    setAnchorEl(event.currentTarget);
    setSelectedCategory(category);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedCategory(null);
  };

  const handleInputChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

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
          Categories ({categories.length})
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Add Category
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

      {/* Categories Grid */}
      {categories.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <CategoryIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No Categories Found
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Get started by creating your first menu category
            </Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenDialog()}
            >
              Create First Category
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {categories.map((category) => (
            <Grid item xs={12} sm={6} md={4} key={category.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
                        <CategoryIcon fontSize="small" />
                      </Avatar>
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {category.name}
                        </Typography>
                        <Chip
                          size="small"
                          label={category.isActive ? 'Active' : 'Inactive'}
                          color={category.isActive ? 'success' : 'default'}
                          icon={category.isActive ? <VisibilityIcon /> : <VisibilityOffIcon />}
                        />
                      </Box>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={(e) => handleMenuClick(e, category)}
                    >
                      <MoreVertIcon />
                    </IconButton>
                  </Box>

                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {category.description || 'No description provided'}
                  </Typography>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="caption" color="text.secondary">
                      Order: {category.displayOrder}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {category.items?.length || 0} items
                    </Typography>
                  </Box>
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
          handleOpenDialog(selectedCategory!);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit Category</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => {
          handleDelete(selectedCategory!.id);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Delete Category</ListItemText>
        </MenuItem>
      </Menu>

      {/* Add/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {isEditing ? 'Edit Category' : 'Add New Category'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Category Name"
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
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained"
            disabled={!formData.name.trim()}
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CategoryManagement;
