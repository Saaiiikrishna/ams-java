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
  Fab,
  Tooltip
} from '@mui/material';
import {
  Restaurant,
  Add as AddIcon,
  Category as CategoryIcon,
  MenuBook as MenuIcon
} from '@mui/icons-material';
import CategoryManagement from '../components/menu/CategoryManagement';
import ItemManagement from '../components/menu/ItemManagement';
import MenuPreview from '../components/menu/MenuPreview';
import ApiService from '../services/ApiService';

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
      id={`menu-tabpanel-${index}`}
      aria-labelledby={`menu-tab-${index}`}
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

const MenuManagementPage: React.FC = () => {
  const [currentTab, setCurrentTab] = useState(0);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    checkMenuPermission();
  }, []);

  const checkMenuPermission = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get('/api/entity/permissions/check/menu-access');
      setHasPermission(response.data.hasMenuAccess);
    } catch (err: any) {
      console.error('Failed to check menu permission:', err);
      setError('Failed to check permissions');
      setHasPermission(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
  };

  const handleRefresh = () => {
    setRefreshTrigger(prev => prev + 1);
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
            Menu Management Access Restricted
          </Typography>
          <Typography variant="body1">
            You don't have permission to access the Menu Management system. 
            Please contact your super administrator to request access to menu and ordering features.
          </Typography>
        </Alert>
        
        <Card sx={{ mt: 3 }}>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Restaurant sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h5" color="text.secondary" gutterBottom>
              Menu Management Unavailable
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Contact your administrator to enable menu and ordering features for your organization.
            </Typography>
            <Button 
              variant="outlined" 
              onClick={checkMenuPermission}
              startIcon={<Restaurant />}
            >
              Check Permissions Again
            </Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Error Loading Menu Management
          </Typography>
          <Typography variant="body1">
            {error}
          </Typography>
        </Alert>
        <Button variant="contained" onClick={checkMenuPermission}>
          Retry
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Menu Management
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage your restaurant menu, categories, and items
          </Typography>
        </Box>
        <Button
          variant="outlined"
          onClick={handleRefresh}
          startIcon={<Restaurant />}
        >
          Refresh
        </Button>
      </Box>

      {/* Tabs */}
      <Card sx={{ mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={currentTab} onChange={handleTabChange} aria-label="menu management tabs">
            <Tab 
              label="Categories" 
              icon={<CategoryIcon />} 
              iconPosition="start"
              id="menu-tab-0"
              aria-controls="menu-tabpanel-0"
            />
            <Tab 
              label="Menu Items" 
              icon={<MenuIcon />} 
              iconPosition="start"
              id="menu-tab-1"
              aria-controls="menu-tabpanel-1"
            />
            <Tab 
              label="Menu Preview" 
              icon={<Restaurant />} 
              iconPosition="start"
              id="menu-tab-2"
              aria-controls="menu-tabpanel-2"
            />
          </Tabs>
        </Box>

        <TabPanel value={currentTab} index={0}>
          <CategoryManagement refreshTrigger={refreshTrigger} />
        </TabPanel>

        <TabPanel value={currentTab} index={1}>
          <ItemManagement refreshTrigger={refreshTrigger} />
        </TabPanel>

        <TabPanel value={currentTab} index={2}>
          <MenuPreview refreshTrigger={refreshTrigger} />
        </TabPanel>
      </Card>

      {/* Quick Actions FAB */}
      <Box sx={{ position: 'fixed', bottom: 24, right: 24 }}>
        <Tooltip title="Quick Add">
          <Fab 
            color="primary" 
            aria-label="add"
            onClick={() => {
              // Quick add based on current tab
              if (currentTab === 0) {
                // Trigger add category
              } else if (currentTab === 1) {
                // Trigger add item
              }
            }}
          >
            <AddIcon />
          </Fab>
        </Tooltip>
      </Box>
    </Box>
  );
};

export default MenuManagementPage;
