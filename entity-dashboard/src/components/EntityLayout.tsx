import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Box,
  IconButton,
  useTheme,
  Avatar,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard,
  People,
  Schedule,
  Assessment,
  Logout,
  Person,
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';

const DRAWER_WIDTH = 280;

const EntityLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [entityName, setEntityName] = useState('Sample Organization');
  const [adminName, setAdminName] = useState('Entity Admin');

  useEffect(() => {
    fetchUserAndEntityInfo();
  }, []);

  const fetchUserAndEntityInfo = async () => {
    try {
      // Get current user info from token
      const token = localStorage.getItem('token');
      let tokenUsername = 'Entity Admin';

      if (token) {
        try {
          // Decode JWT token to get username (basic implementation)
          const payload = JSON.parse(atob(token.split('.')[1]));
          tokenUsername = payload.sub || 'Entity Admin';
          setAdminName(tokenUsername);
        } catch (e) {
          console.warn('Failed to decode token:', e);
          setAdminName('Entity Admin');
        }
      } else {
        setAdminName('Entity Admin');
      }

      // Try to get entity info from API
      try {
        console.log('Fetching entity info from /api/entity/info...');
        const response = await ApiService.get('/api/entity/info');
        console.log('Entity info response:', response.data);

        if (response.data && response.data.name) {
          const entityName = response.data.name;
          const adminName = response.data.adminName || tokenUsername;

          console.log('Setting entity name:', entityName, 'admin name:', adminName);
          setEntityName(entityName);
          setAdminName(adminName);
          document.title = entityName;
        } else {
          console.warn('No entity data in response, using fallback');
          setEntityName('Sample Organization');
          document.title = 'Sample Organization';
        }
      } catch (error) {
        console.error('Failed to fetch entity info:', error);
        // Try to extract entity name from current URL or use fallback
        const currentPath = window.location.pathname;
        if (currentPath.includes('entity')) {
          setEntityName('Entity Dashboard');
          document.title = 'Entity Dashboard';
        } else {
          setEntityName('Sample Organization');
          document.title = 'Sample Organization';
        }
      }
    } catch (error) {
      console.error('Failed to fetch user/entity info:', error);
      setAdminName('Entity Admin');
      setEntityName('Sample Organization');
    }
  };

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
  };

  const menuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/entity/dashboard' },
    { text: 'Subscribers', icon: <People />, path: '/entity/subscribers' },
    { text: 'Sessions', icon: <Schedule />, path: '/entity/sessions' },
    { text: 'Reports', icon: <Assessment />, path: '/entity/reports' },
  ];

  const drawer = (
    <Box>
      {/* Header */}
      <Box
        sx={{
          p: 3,
          background: 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
          color: 'white',
          textAlign: 'center',
        }}
      >
        <Avatar
          sx={{
            width: 64,
            height: 64,
            mx: 'auto',
            mb: 2,
            bgcolor: 'rgba(255,255,255,0.2)',
          }}
        >
          <Person sx={{ fontSize: 32 }} />
        </Avatar>
        <Typography variant="h6" fontWeight="bold">
          {adminName}
        </Typography>
        <Typography variant="body2" sx={{ opacity: 0.9 }}>
          {entityName}
        </Typography>
      </Box>

      {/* Navigation */}
      <List sx={{ px: 2, py: 1 }}>
        {menuItems.map((item) => (
          <ListItem
            key={item.text}
            onClick={() => navigate(item.path)}
            sx={{
              borderRadius: 2,
              mb: 0.5,
              cursor: 'pointer',
              backgroundColor: location.pathname === item.path ? 'primary.main' : 'transparent',
              color: location.pathname === item.path ? 'white' : 'inherit',
              '&:hover': {
                backgroundColor: location.pathname === item.path ? 'primary.dark' : 'action.hover',
              },
            }}
          >
            <ListItemIcon
              sx={{
                color: location.pathname === item.path ? 'white' : 'primary.main',
                minWidth: 40,
              }}
            >
              {item.icon}
            </ListItemIcon>
            <ListItemText
              primary={item.text}
              primaryTypographyProps={{
                fontWeight: location.pathname === item.path ? 'bold' : 'normal',
              }}
            />
          </ListItem>
        ))}
      </List>

      {/* Logout */}
      <Box sx={{ position: 'absolute', bottom: 16, left: 16, right: 16 }}>
        <ListItem
          onClick={handleLogout}
          sx={{
            borderRadius: 2,
            cursor: 'pointer',
            backgroundColor: 'error.main',
            color: 'white',
            '&:hover': {
              backgroundColor: 'error.dark',
            },
          }}
        >
          <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
            <Logout />
          </ListItemIcon>
          <ListItemText primary="Logout" />
        </ListItem>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      {/* App Bar */}
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { sm: `${DRAWER_WIDTH}px` },
          background: 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div">
            {entityName} - Attendance System
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Drawer */}
      <Box
        component="nav"
        sx={{ width: { sm: DRAWER_WIDTH }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
          backgroundColor: '#f5f5f5',
          minHeight: '100vh',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
};

export default EntityLayout;
