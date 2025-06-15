import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  Alert,
  CircularProgress,
  Button,
  Divider,
  Avatar,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Restaurant as RestaurantIcon,
  Category as CategoryIcon,
  Visibility as PreviewIcon,
  Share as ShareIcon,
  QrCode as QrCodeIcon,
  AttachMoney as MoneyIcon
} from '@mui/icons-material';
import ApiService from '../../services/ApiService';

interface Item {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  isActive: boolean;
  isAvailable: boolean;
}

interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
  isActive: boolean;
  items: Item[];
}

interface MenuPreviewProps {
  refreshTrigger: number;
}

const MenuPreview: React.FC<MenuPreviewProps> = ({ refreshTrigger }) => {
  const [menu, setMenu] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedCategory, setExpandedCategory] = useState<string | false>(false);

  useEffect(() => {
    fetchMenu();
  }, [refreshTrigger]);

  const fetchMenu = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<Category[]>('/api/menu/categories/with-items');
      setMenu(response.data || []);
      
      // Auto-expand first category if available
      if (response.data && response.data.length > 0) {
        setExpandedCategory(`category-${response.data[0].id}`);
      }
    } catch (err: any) {
      console.error('Failed to fetch menu:', err);
      setError(err.response?.data?.error || 'Failed to fetch menu');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCategoryChange = (panel: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
    setExpandedCategory(isExpanded ? panel : false);
  };

  const getTotalItems = () => {
    return menu.reduce((total, category) => total + (category.items?.length || 0), 0);
  };

  const getAvailableItems = () => {
    return menu.reduce((total, category) => 
      total + (category.items?.filter(item => item.isActive && item.isAvailable).length || 0), 0
    );
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h6" fontWeight="bold" gutterBottom>
          Menu Preview
        </Typography>
        <Typography variant="body2" color="text.secondary">
          This is how your menu will appear to customers
        </Typography>
      </Box>

      {/* Menu Stats */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <CategoryIcon sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {menu.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Categories
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <RestaurantIcon sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {getTotalItems()}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Items
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <PreviewIcon sx={{ fontSize: 40, color: 'info.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {getAvailableItems()}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Available Items
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Menu Actions */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <Button
          variant="outlined"
          startIcon={<ShareIcon />}
          onClick={() => {
            // TODO: Implement share functionality
            alert('Share functionality will be implemented');
          }}
        >
          Share Menu
        </Button>
        <Button
          variant="outlined"
          startIcon={<QrCodeIcon />}
          onClick={() => {
            // TODO: Navigate to table management
            alert('Navigate to Table Management');
          }}
        >
          Generate QR Codes
        </Button>
      </Box>

      {/* Menu Content */}
      {menu.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <RestaurantIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No Menu Available
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Create categories and add menu items to see your menu preview
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Box>
          {menu.map((category) => (
            <Accordion
              key={category.id}
              expanded={expandedCategory === `category-${category.id}`}
              onChange={handleCategoryChange(`category-${category.id}`)}
              sx={{ mb: 2 }}
            >
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls={`category-${category.id}-content`}
                id={`category-${category.id}-header`}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                  <Avatar sx={{ bgcolor: 'primary.main' }}>
                    <CategoryIcon />
                  </Avatar>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="h6" fontWeight="bold">
                      {category.name}
                    </Typography>
                    {category.description && (
                      <Typography variant="body2" color="text.secondary">
                        {category.description}
                      </Typography>
                    )}
                  </Box>
                  <Chip
                    label={`${category.items?.length || 0} items`}
                    size="small"
                    color="primary"
                    variant="outlined"
                  />
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                {category.items && category.items.length > 0 ? (
                  <Grid container spacing={2}>
                    {category.items
                      .filter(item => item.isActive && item.isAvailable)
                      .map((item) => (
                        <Grid item xs={12} sm={6} md={4} key={item.id}>
                          <Card variant="outlined" sx={{ height: '100%' }}>
                            <CardContent>
                              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                                <Typography variant="h6" fontWeight="bold">
                                  {item.name}
                                </Typography>
                                <Typography variant="h6" color="primary" fontWeight="bold">
                                  ${item.price.toFixed(2)}
                                </Typography>
                              </Box>
                              
                              {item.description && (
                                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                                  {item.description}
                                </Typography>
                              )}

                              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <Chip
                                  size="small"
                                  label="Available"
                                  color="success"
                                  variant="outlined"
                                />
                                <Tooltip title="Add to Cart">
                                  <IconButton size="small" color="primary">
                                    <RestaurantIcon />
                                  </IconButton>
                                </Tooltip>
                              </Box>
                            </CardContent>
                          </Card>
                        </Grid>
                      ))}
                  </Grid>
                ) : (
                  <Box sx={{ textAlign: 'center', py: 4 }}>
                    <RestaurantIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                    <Typography variant="body1" color="text.secondary">
                      No items available in this category
                    </Typography>
                  </Box>
                )}
              </AccordionDetails>
            </Accordion>
          ))}
        </Box>
      )}

      {/* Menu Footer */}
      {menu.length > 0 && (
        <Card sx={{ mt: 3, bgcolor: 'grey.50' }}>
          <CardContent sx={{ textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              This is a preview of your customer-facing menu. 
              Only active and available items are shown.
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default MenuPreview;
