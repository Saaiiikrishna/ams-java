import React, { useState, useEffect } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
} from '@mui/material';
import {
  Menu as MenuIcon,
  People,
  EventNote,
  Assessment,
  Dashboard,
  Logout,
  AccountCircle,
  Business,
  Nfc,
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';

const drawerWidth = 280;

const EntityLayout: React.FC = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [adminName, setAdminName] = useState('Entity Admin');
  const [entityName, setEntityName] = useState('Attendance Management');
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();

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

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
    handleProfileMenuClose();
  };

  const menuItems = [
    {
      text: 'Dashboard',
      icon: <Dashboard />,
      path: '/dashboard',
    },
    {
      text: 'Subscribers',
      icon: <People />,
      path: '/dashboard/subscribers',
    },
    {
      text: 'NFC Cards',
      icon: <Nfc />,
      path: '/dashboard/cards',
    },
    {
      text: 'Sessions',
      icon: <EventNote />,
      path: '/dashboard/sessions',
    },
    {
      text: 'Reports',
      icon: <Assessment />,
      path: '/dashboard/reports',
    },
  ];

  const drawer = (
    <Box>
      <Box
        sx={{
          p: 3,
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          textAlign: 'center',
        }}
      >
        <Business sx={{ fontSize: 40, mb: 1 }} />
        <Typography variant="h6" fontWeight="bold">
          {adminName}
        </Typography>
        <Typography variant="body2" sx={{ opacity: 0.8 }}>
          {entityName}
        </Typography>
      </Box>

      <Divider />

      <List sx={{ px: 2, py: 1 }}>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              component={Link}
              to={item.path}
              selected={location.pathname === item.path}
              sx={{
                borderRadius: 2,
                '&.Mui-selected': {
                  backgroundColor: 'primary.main',
                  color: 'white',
                  '&:hover': {
                    backgroundColor: 'primary.dark',
                  },
                  '& .MuiListItemIcon-root': {
                    color: 'white',
                  },
                },
                '&:hover': {
                  backgroundColor: 'action.hover',
                },
              }}
            >
              <ListItemIcon
                sx={{
                  color: location.pathname === item.path ? 'white' : 'text.secondary',
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

            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          backgroundColor: 'white',
          color: 'text.primary',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>

          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
            {menuItems.find(item => item.path === location.pathname)?.text || 'Dashboard'}
          </Typography>

          <IconButton
            size="large"
            edge="end"
            aria-label="account of current user"
            aria-controls="profile-menu"
            aria-haspopup="true"
            onClick={handleProfileMenuOpen}
            color="inherit"
          >
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
              <AccountCircle />
            </Avatar>
          </IconButton>

          <Menu
            id="profile-menu"
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleProfileMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <Logout fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          mt: '64px',
          backgroundColor: 'background.default',
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
};

export default EntityLayout;
