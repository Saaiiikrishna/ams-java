import React, { useState, useEffect } from 'react';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Box,
  Divider,
  Chip,
  Alert,
  CircularProgress,
  Collapse,
  Badge,
} from '@mui/material';
import {
  Dashboard,
  People,
  Schedule,
  Restaurant,
  ShoppingCart,
  TableRestaurant,
  Analytics,
  Settings,
  ExpandLess,
  ExpandMore,
  Assignment,
  Today,
  History,
  MenuBook,
  LocalDining,
  Receipt,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import ApiService from '../services/ApiService';

interface NavigationItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  path: string;
  permission?: string;
  badge?: number;
  children?: NavigationItem[];
}

interface PermissionAwareNavigationProps {
  drawerWidth: number;
  mobileOpen: boolean;
  onDrawerToggle: () => void;
}

const PermissionAwareNavigation: React.FC<PermissionAwareNavigationProps> = ({
  drawerWidth,
  mobileOpen,
  onDrawerToggle,
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [permissions, setPermissions] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSections, setExpandedSections] = useState<string[]>(['attendance', 'menu']);
  const [badges, setBadges] = useState<{[key: string]: number}>({});
  const [currentTime, setCurrentTime] = useState(new Date());

  // Define navigation structure
  const navigationItems: NavigationItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <Dashboard />,
      path: '/dashboard',
    },
    {
      id: 'attendance',
      label: 'Attendance System',
      icon: <People />,
      path: '/dashboard/attendance',
      permission: 'hasAttendanceAccess',
      children: [
        {
          id: 'members',
          label: 'Members',
          icon: <People />,
          path: '/dashboard/subscribers',
          permission: 'MEMBER_MANAGEMENT',
        },
        {
          id: 'sessions',
          label: 'Sessions',
          icon: <Schedule />,
          path: '/dashboard/sessions',
          permission: 'ATTENDANCE_TRACKING',
        },
        {
          id: 'attendance-today',
          label: "Today's Attendance",
          icon: <Today />,
          path: '/dashboard/attendance/today',
          permission: 'ATTENDANCE_TRACKING',
        },
        {
          id: 'attendance-history',
          label: 'Attendance History',
          icon: <History />,
          path: '/dashboard/attendance/history',
          permission: 'ATTENDANCE_TRACKING',
        },
        {
          id: 'attendance-reports',
          label: 'Reports',
          icon: <Analytics />,
          path: '/dashboard/attendance/reports',
          permission: 'REPORTS_ANALYTICS',
        },
      ],
    },
    {
      id: 'menu',
      label: 'Menu & Ordering',
      icon: <Restaurant />,
      path: '/dashboard/menu',
      permission: 'hasMenuAccess',
      children: [
        {
          id: 'menu-categories',
          label: 'Categories',
          icon: <MenuBook />,
          path: '/dashboard/menu/categories',
          permission: 'MENU_MANAGEMENT',
        },
        {
          id: 'menu-items',
          label: 'Menu Items',
          icon: <LocalDining />,
          path: '/dashboard/menu/items',
          permission: 'MENU_MANAGEMENT',
        },
        {
          id: 'tables',
          label: 'Tables',
          icon: <TableRestaurant />,
          path: '/dashboard/menu/tables',
          permission: 'TABLE_MANAGEMENT',
        },
        {
          id: 'orders',
          label: 'Orders',
          icon: <ShoppingCart />,
          path: '/dashboard/menu/orders',
          permission: 'ORDER_MANAGEMENT',
        },
        {
          id: 'order-history',
          label: 'Order History',
          icon: <Receipt />,
          path: '/dashboard/menu/order-history',
          permission: 'ORDER_MANAGEMENT',
        },
        {
          id: 'menu-reports',
          label: 'Sales Reports',
          icon: <Analytics />,
          path: '/dashboard/menu/reports',
          permission: 'REPORTS_ANALYTICS',
        },
      ],
    },
  ];

  const fetchPermissions = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await ApiService.get('/api/entity/permissions/status');
      setPermissions(response.data);
      
      // Fetch badge counts
      const badgeData: {[key: string]: number} = {};
      
      if (response.data.hasAttendanceAccess) {
        try {
          const [sessionsResponse, membersResponse] = await Promise.all([
            ApiService.get('/api/sessions'),
            ApiService.get('/api/subscribers'),
          ]);
          
          badgeData.sessions = sessionsResponse.data.filter((s: any) => s.isActive).length || 0;
          badgeData.members = membersResponse.data.filter((m: any) => m.isActive).length || 0;
        } catch (err) {
          console.warn('Failed to fetch attendance badges:', err);
        }
      }
      
      if (response.data.hasMenuAccess) {
        try {
          const ordersResponse = await ApiService.get('/api/orders/today');
          badgeData.orders = ordersResponse.data.filter((o: any) => 
            o.status === 'PENDING' || o.status === 'PREPARING'
          ).length || 0;
        } catch (err) {
          console.warn('Failed to fetch menu badges:', err);
        }
      }
      
      setBadges(badgeData);
    } catch (err: any) {
      console.error('Failed to fetch permissions:', err);
      setError('Failed to load navigation permissions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissions();

    // Refresh badges every 30 seconds for real-time order updates
    const interval = setInterval(() => {
      fetchPermissions();
    }, 30 * 1000);

    // Update time every second
    const timeInterval = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => {
      clearInterval(interval);
      clearInterval(timeInterval);
    };
  }, []);

  const hasPermission = (permission?: string): boolean => {
    if (!permission || !permissions) return true;
    
    // Check for direct permission flags
    if (permissions[permission] !== undefined) {
      return permissions[permission];
    }
    
    // Check for specific permissions in permissions object
    if (permissions.permissions && permissions.permissions[permission] !== undefined) {
      return permissions.permissions[permission];
    }
    
    return false;
  };

  const handleSectionToggle = (sectionId: string) => {
    setExpandedSections(prev => 
      prev.includes(sectionId) 
        ? prev.filter(id => id !== sectionId)
        : [...prev, sectionId]
    );
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    if (mobileOpen) {
      onDrawerToggle();
    }
  };

  const renderNavigationItem = (item: NavigationItem, level: number = 0) => {
    const isSelected = location.pathname === item.path;
    const isExpanded = expandedSections.includes(item.id);
    const hasAccess = hasPermission(item.permission);
    
    if (!hasAccess) return null;

    const badge = badges[item.id];

    return (
      <React.Fragment key={item.id}>
        <ListItem disablePadding>
          <ListItemButton
            selected={isSelected}
            onClick={() => {
              if (item.children) {
                handleSectionToggle(item.id);
              } else {
                handleNavigation(item.path);
              }
            }}
            sx={{
              pl: 2 + level * 2,
              borderRadius: 1,
              mx: 1,
              mb: 0.5,
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
            }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>
              {badge && badge > 0 ? (
                <Badge badgeContent={badge} color="error">
                  {item.icon}
                </Badge>
              ) : (
                item.icon
              )}
            </ListItemIcon>
            <ListItemText 
              primary={item.label}
              primaryTypographyProps={{
                fontSize: level > 0 ? '0.875rem' : '1rem',
                fontWeight: level === 0 ? 600 : 400,
              }}
            />
            {item.children && (
              isExpanded ? <ExpandLess /> : <ExpandMore />
            )}
          </ListItemButton>
        </ListItem>
        
        {item.children && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              {item.children.map(child => renderNavigationItem(child, level + 1))}
            </List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ p: 2, background: 'linear-gradient(135deg, #1976d2, #42a5f5)', color: 'white' }}>
        <Typography variant="h6" fontWeight="bold">
          {permissions?.organizationName || 'Dashboard'}
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.9 }}>
          {currentTime.toLocaleString()}
        </Typography>
      </Box>

      <Divider />

      {/* Navigation Content */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress size={24} />
          </Box>
        ) : error ? (
          <Alert severity="error" sx={{ m: 2 }}>
            {error}
          </Alert>
        ) : (
          <List sx={{ pt: 1 }}>
            {navigationItems.map(item => renderNavigationItem(item))}
          </List>
        )}
      </Box>

      {/* Footer */}
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
        {permissions && (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {permissions.hasAttendanceAccess && (
              <Chip 
                label="Attendance" 
                size="small" 
                color="primary" 
                variant="outlined"
              />
            )}
            {permissions.hasMenuAccess && (
              <Chip 
                label="Menu & Orders" 
                size="small" 
                color="secondary" 
                variant="outlined"
              />
            )}
          </Box>
        )}
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Last Login: {new Date().toLocaleDateString()}
        </Typography>
      </Box>
    </Box>
  );

  return (
    <>
      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onDrawerToggle}
        ModalProps={{
          keepMounted: true, // Better open performance on mobile.
        }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* Desktop Drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
        }}
        open
      >
        {drawerContent}
      </Drawer>
    </>
  );
};

export default PermissionAwareNavigation;
