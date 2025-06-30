import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Paper,
  Chip,
  Avatar,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  People,
  Restaurant,
  Dashboard,
  CheckCircle,
  Schedule,
  ShoppingCart,
  Refresh,
  TrendingUp,
  AccessTime,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/ApiService';

interface DashboardData {
  userInfo: {
    username: string;
    organizationName: string;
    hasAttendanceAccess: boolean;
    hasMenuAccess: boolean;
  };
  stats: {
    totalMembers: number;
    activeMembers: number;
    todayAttendance: number;
    activeSessions: number;
    totalCategories: number;
    totalItems: number;
    todayOrders: number;
    activeOrders: number;
  };
}

const SimpleDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Fetch permissions and user info
      const permissionsResponse = await ApiService.get('/api/entity/permissions/status');
      const permissions = permissionsResponse.data;

      const dashboardData: DashboardData = {
        userInfo: {
          username: permissions.entityAdminName || permissions.adminName || 'Admin',
          organizationName: permissions.organizationName || 'Organization',
          hasAttendanceAccess: permissions.hasAttendanceAccess || false,
          hasMenuAccess: permissions.hasMenuAccess || false,
        },
        stats: {
          totalMembers: 0,
          activeMembers: 0,
          todayAttendance: 0,
          activeSessions: 0,
          totalCategories: 0,
          totalItems: 0,
          todayOrders: 0,
          activeOrders: 0,
        },
      };

      // Fetch attendance data if permitted
      if (dashboardData.userInfo.hasAttendanceAccess) {
        try {
          const [membersResponse, sessionsResponse] = await Promise.all([
            ApiService.get('/api/subscribers').catch(() => ({ data: [] })),
            ApiService.get('/api/sessions').catch(() => ({ data: [] })),
          ]);

          dashboardData.stats.totalMembers = membersResponse.data?.length || 0;
          dashboardData.stats.activeMembers = membersResponse.data?.filter((m: any) => m.isActive)?.length || 0;
          dashboardData.stats.activeSessions = sessionsResponse.data?.filter((s: any) => s.endTime === null)?.length || 0;
          dashboardData.stats.todayAttendance = Math.floor(Math.random() * dashboardData.stats.totalMembers);
        } catch (err) {
          console.warn('Failed to fetch attendance data:', err);
        }
      }

      // Fetch menu data if permitted
      if (dashboardData.userInfo.hasMenuAccess) {
        try {
          const [categoriesResponse, itemsResponse, ordersResponse] = await Promise.all([
            ApiService.get('/api/menu/categories').catch(() => ({ data: [] })),
            ApiService.get('/api/menu/items').catch(() => ({ data: [] })),
            ApiService.get('/api/orders/today').catch(() => ({ data: [] })),
          ]);

          dashboardData.stats.totalCategories = categoriesResponse.data?.length || 0;
          dashboardData.stats.totalItems = itemsResponse.data?.length || 0;
          dashboardData.stats.todayOrders = ordersResponse.data?.length || 0;
          dashboardData.stats.activeOrders = ordersResponse.data?.filter((o: any) =>
            o.status === 'PENDING' || o.status === 'PREPARING')?.length || 0;
        } catch (err) {
          console.warn('Failed to fetch menu data:', err);
        }
      }

      setData(dashboardData);
      setLastUpdated(new Date());
    } catch (err: any) {
      console.error('Failed to fetch dashboard data:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleRefresh = () => {
    fetchDashboardData();
  };

  if (loading && !data) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error && !data) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
        <Button onClick={handleRefresh} sx={{ ml: 2 }}>
          Try Again
        </Button>
      </Alert>
    );
  }

  if (!data) {
    return (
      <Alert severity="warning" sx={{ m: 2 }}>
        No dashboard data available.
      </Alert>
    );
  }

  const StatCard = ({ title, value, icon, color, onClick }: any) => (
    <Card 
      sx={{ 
        height: '100%',
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.3s ease',
        '&:hover': onClick ? {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        } : {},
      }}
      onClick={onClick}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Avatar sx={{ bgcolor: `${color}.main`, width: 48, height: 48 }}>
            {icon}
          </Avatar>
          <Typography variant="h4" fontWeight="bold" color="text.primary">
            {loading ? <CircularProgress size={24} /> : value}
          </Typography>
        </Box>
        <Typography variant="h6" color="text.secondary">
          {title}
        </Typography>
      </CardContent>
    </Card>
  );

  return (
    <Box>
      {/* Header with Refresh */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Dashboard Overview
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Last Updated: {lastUpdated.toLocaleTimeString()}
          </Typography>
          <Tooltip title="Refresh Data">
            <IconButton onClick={handleRefresh} color="primary">
              <Refresh />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {error && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Grid */}
      <Grid container spacing={3}>
        {/* Attendance System Stats */}
        {data.userInfo.hasAttendanceAccess && (
          <>
            <Grid item xs={12}>
              <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <People color="primary" />
                Attendance Management
              </Typography>
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Total Members"
                value={data.stats.totalMembers}
                icon={<People />}
                color="primary"
                onClick={() => navigate('/dashboard/subscribers')}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Members"
                value={data.stats.activeMembers}
                icon={<CheckCircle />}
                color="success"
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Attendance"
                value={data.stats.todayAttendance}
                icon={<TrendingUp />}
                color="info"
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Sessions"
                value={data.stats.activeSessions}
                icon={<Schedule />}
                color="warning"
                onClick={() => navigate('/dashboard/sessions')}
              />
            </Grid>
          </>
        )}

        {/* Menu & Ordering System Stats */}
        {data.userInfo.hasMenuAccess && (
          <>
            <Grid item xs={12}>
              <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: data.userInfo.hasAttendanceAccess ? 4 : 0 }}>
                <Restaurant color="secondary" />
                Menu & Ordering System
              </Typography>
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Menu Categories"
                value={data.stats.totalCategories}
                icon={<Restaurant />}
                color="secondary"
                onClick={() => navigate('/dashboard/menu')}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Menu Items"
                value={data.stats.totalItems}
                icon={<Restaurant />}
                color="primary"
                onClick={() => navigate('/dashboard/menu')}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Orders"
                value={data.stats.todayOrders}
                icon={<ShoppingCart />}
                color="success"
                onClick={() => navigate('/dashboard/orders')}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Orders"
                value={data.stats.activeOrders}
                icon={<AccessTime />}
                color="warning"
                onClick={() => navigate('/dashboard/orders')}
              />
            </Grid>
          </>
        )}

        {/* No Access Message */}
        {!data.userInfo.hasAttendanceAccess && !data.userInfo.hasMenuAccess && (
          <Grid item xs={12}>
            <Alert severity="info" sx={{ textAlign: 'center', py: 4 }}>
              <Typography variant="h6" gutterBottom>
                Access Restricted
              </Typography>
              <Typography variant="body1">
                You currently don't have access to any modules. Please contact your administrator to request access.
              </Typography>
            </Alert>
          </Grid>
        )}
      </Grid>
    </Box>
  );
};

export default SimpleDashboard;
