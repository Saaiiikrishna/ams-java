import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Menu,
  MenuItem,
  Avatar,
  Button,
  Container,
  Paper,
  ListItemIcon,
  Divider,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  AccountCircle,
  Logout,
  Settings,
  Dashboard,
  ExitToApp,
  Person,
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';

interface UserInfo {
  username: string;
  organizationName: string;
  hasAttendanceAccess: boolean;
  hasMenuAccess: boolean;
}

const SimpleEntityLayout: React.FC = () => {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentTime, setCurrentTime] = useState(new Date());

  // Update time every second
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Fetch user info
  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await ApiService.get('/api/entity/permissions/status');
        setUserInfo({
          username: response.data.entityAdminName || response.data.adminName || 'Admin',
          organizationName: response.data.organizationName || 'Organization',
          hasAttendanceAccess: response.data.hasAttendanceAccess || false,
          hasMenuAccess: response.data.hasMenuAccess || false,
        });
      } catch (error) {
        console.error('Failed to fetch user info:', error);
        setUserInfo({
          username: 'Admin',
          organizationName: 'Organization',
          hasAttendanceAccess: false,
          hasMenuAccess: false,
        });
      } finally {
        setLoading(false);
      }
    };

    fetchUserInfo();
  }, []);

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
    handleClose();
  };

  const handleDashboard = () => {
    navigate('/dashboard');
    handleClose();
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Top Navigation Bar */}
      <AppBar 
        position="static" 
        sx={{ 
          background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
          boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
        }}
      >
        <Toolbar>
          {/* Logo/Title */}
          <Dashboard sx={{ mr: 2 }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
            {userInfo?.organizationName || 'Entity Dashboard'}
          </Typography>

          {/* Current Time */}
          <Typography variant="body2" sx={{ mr: 3, opacity: 0.9 }}>
            {currentTime.toLocaleString()}
          </Typography>

          {/* User Menu */}
          <Button
            color="inherit"
            onClick={handleMenu}
            startIcon={<AccountCircle />}
            sx={{ 
              textTransform: 'none',
              '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' }
            }}
          >
            {userInfo?.username || 'Admin'}
          </Button>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
            PaperProps={{
              sx: {
                mt: 1,
                minWidth: 200,
                boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
              }
            }}
          >
            <MenuItem onClick={handleDashboard}>
              <ListItemIcon>
                <Dashboard fontSize="small" />
              </ListItemIcon>
              Dashboard
            </MenuItem>
            <MenuItem onClick={handleClose}>
              <ListItemIcon>
                <Person fontSize="small" />
              </ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem onClick={handleClose}>
              <ListItemIcon>
                <Settings fontSize="small" />
              </ListItemIcon>
              Settings
            </MenuItem>
            <Divider />
            <MenuItem 
              onClick={handleLogout} 
              sx={{ 
                color: 'error.main',
                '&:hover': { backgroundColor: 'error.light', color: 'white' }
              }}
            >
              <ListItemIcon>
                <ExitToApp fontSize="small" sx={{ color: 'error.main' }} />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Welcome Section */}
      <Paper 
        sx={{ 
          m: 3, 
          p: 3, 
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          color: 'white',
          borderRadius: 2,
        }}
      >
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          Welcome {userInfo?.username}!
        </Typography>
        <Typography variant="h6" sx={{ opacity: 0.9, mb: 2 }}>
          {userInfo?.organizationName}
        </Typography>
        
        {/* Quick Access Navigation */}
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
          {userInfo?.hasAttendanceAccess && (
            <>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/attendance')}
              >
                Attendance System
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/subscribers')}
              >
                Members
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/sessions')}
              >
                Sessions
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/scheduled-sessions')}
              >
                Scheduled Sessions
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/cards')}
              >
                NFC Cards
              </Button>
            </>
          )}
          {userInfo?.hasMenuAccess && (
            <>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/menu')}
              >
                Menu & Ordering
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/menu/categories')}
              >
                Categories
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/menu/items')}
              >
                Menu Items
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/menu/tables')}
              >
                Tables
              </Button>
              <Button
                variant="outlined"
                size="small"
                sx={{ color: 'white', borderColor: 'white' }}
                onClick={() => navigate('/dashboard/menu/orders')}
              >
                Orders
              </Button>
            </>
          )}
          {!userInfo?.hasAttendanceAccess && !userInfo?.hasMenuAccess && (
            <Alert severity="info" sx={{ mt: 2 }}>
              No system access granted. Please contact your administrator.
            </Alert>
          )}
        </Box>
      </Paper>

      {/* Main Content */}
      <Container maxWidth="xl" sx={{ mt: 2, mb: 4 }}>
        <Outlet />
      </Container>

      {/* Footer */}
      <Box 
        component="footer" 
        sx={{ 
          mt: 'auto', 
          py: 2, 
          px: 3, 
          backgroundColor: 'grey.100',
          borderTop: 1,
          borderColor: 'divider'
        }}
      >
        <Typography variant="body2" color="text.secondary" align="center">
          © 2025 {userInfo?.organizationName}. Last Login: {new Date().toLocaleDateString()}
        </Typography>
      </Box>
    </Box>
  );
};

export default SimpleEntityLayout;
