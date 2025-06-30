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
  LinearProgress,
  IconButton,
  Tooltip,
  Paper,
  Divider,
} from '@mui/material';
import {
  People,
  Schedule,
  TrendingUp,
  Restaurant,
  ShoppingCart,
  TableRestaurant,
  CheckCircle,
  Warning,
  Info,
  Refresh,
  Analytics,
  Today,
  AccessTime,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface DashboardData {
  attendance: {
    totalMembers: number;
    activeMembers: number;
    todayAttendance: number;
    activeSessions: number;
    attendanceRate: number;
  };
  menu: {
    totalCategories: number;
    totalItems: number;
    totalTables: number;
    todayOrders: number;
    totalRevenue: number;
    activeOrders: number;
  };
  permissions: {
    hasAttendance: boolean;
    hasMenuOrdering: boolean;
    hasMemberManagement: boolean;
    hasOrderManagement: boolean;
    hasReports: boolean;
  };
  organization: {
    name: string;
    entityId: string;
    memberCount: number;
    adminName: string;
  };
}

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info';
  trend?: {
    value: number;
    isPositive: boolean;
  };
  loading?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  color,
  trend,
  loading = false,
}) => {
  return (
    <Card 
      sx={{ 
        height: '100%',
        background: `linear-gradient(135deg, ${color === 'primary' ? '#1976d2' : 
                     color === 'success' ? '#2e7d32' : 
                     color === 'warning' ? '#ed6c02' : 
                     color === 'error' ? '#d32f2f' : 
                     color === 'info' ? '#0288d1' : '#9c27b0'}15, transparent)`,
        border: `1px solid ${color === 'primary' ? '#1976d2' : 
                color === 'success' ? '#2e7d32' : 
                color === 'warning' ? '#ed6c02' : 
                color === 'error' ? '#d32f2f' : 
                color === 'info' ? '#0288d1' : '#9c27b0'}30`,
        transition: 'all 0.3s ease',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        },
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Avatar 
            sx={{ 
              bgcolor: `${color}.main`, 
              width: 48, 
              height: 48,
              boxShadow: 2,
            }}
          >
            {icon}
          </Avatar>
          {trend && (
            <Chip
              label={`${trend.isPositive ? '+' : ''}${trend.value}%`}
              color={trend.isPositive ? 'success' : 'error'}
              size="small"
              icon={<TrendingUp />}
            />
          )}
        </Box>
        
        <Typography variant="h4" fontWeight="bold" color="text.primary" gutterBottom>
          {loading ? <CircularProgress size={24} /> : value}
        </Typography>
        
        <Typography variant="h6" color="text.secondary" gutterBottom>
          {title}
        </Typography>
        
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
};

const DashboardOverview: React.FC = () => {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Fetch permissions first
      const permissionsResponse = await ApiService.get('/api/entity/permissions/status');
      const permissions = permissionsResponse.data;

      // Initialize dashboard data
      const data: DashboardData = {
        attendance: {
          totalMembers: 0,
          activeMembers: 0,
          todayAttendance: 0,
          activeSessions: 0,
          attendanceRate: 0,
        },
        menu: {
          totalCategories: 0,
          totalItems: 0,
          totalTables: 0,
          todayOrders: 0,
          totalRevenue: 0,
          activeOrders: 0,
        },
        permissions: {
          hasAttendance: permissions.hasAttendanceAccess || false,
          hasMenuOrdering: permissions.hasMenuAccess || false,
          hasMemberManagement: permissions.permissions?.MEMBER_MANAGEMENT || false,
          hasOrderManagement: permissions.permissions?.ORDER_MANAGEMENT || false,
          hasReports: permissions.permissions?.REPORTS_ANALYTICS || false,
        },
        organization: {
          name: permissions.organizationName || 'Organization',
          entityId: permissions.entityId || '',
          memberCount: 0,
          adminName: permissions.adminName || permissions.entityAdminName || 'Admin',
        },
      };

      // Fetch attendance data if permitted
      if (data.permissions.hasAttendance) {
        try {
          const [membersResponse, sessionsResponse, attendanceResponse] = await Promise.all([
            ApiService.get('/api/subscribers'),
            ApiService.get('/api/sessions'),
            ApiService.get('/api/sessions/today-stats'),
          ]);

          data.attendance.totalMembers = membersResponse.data.length || 0;
          data.attendance.activeMembers = membersResponse.data.filter((m: any) => m.isActive).length || 0;
          data.attendance.activeSessions = sessionsResponse.data.filter((s: any) => s.isActive).length || 0;
          data.attendance.todayAttendance = attendanceResponse.data.totalAttendance || 0;
          data.attendance.attendanceRate = data.attendance.totalMembers > 0 
            ? Math.round((data.attendance.todayAttendance / data.attendance.totalMembers) * 100) 
            : 0;
          data.organization.memberCount = data.attendance.totalMembers;
        } catch (err) {
          console.warn('Failed to fetch attendance data:', err);
        }
      }

      // Fetch menu/ordering data if permitted
      if (data.permissions.hasMenuOrdering) {
        try {
          const [categoriesResponse, itemsResponse, tablesResponse, ordersResponse] = await Promise.all([
            ApiService.get('/api/categories'),
            ApiService.get('/api/items'),
            ApiService.get('/api/tables'),
            ApiService.get('/api/orders/today'),
          ]);

          data.menu.totalCategories = categoriesResponse.data.length || 0;
          data.menu.totalItems = itemsResponse.data.length || 0;
          data.menu.totalTables = tablesResponse.data.length || 0;
          data.menu.todayOrders = ordersResponse.data.length || 0;
          data.menu.activeOrders = ordersResponse.data.filter((o: any) => o.status === 'PENDING' || o.status === 'PREPARING').length || 0;
          data.menu.totalRevenue = ordersResponse.data.reduce((sum: number, order: any) => sum + (order.totalAmount || 0), 0);
        } catch (err) {
          console.warn('Failed to fetch menu/ordering data:', err);
        }
      }

      setDashboardData(data);
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
    
    // Auto-refresh every 5 minutes
    const interval = setInterval(fetchDashboardData, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    fetchDashboardData();
  };

  if (loading && !dashboardData) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error && !dashboardData) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  if (!dashboardData) {
    return (
      <Alert severity="warning" sx={{ m: 2 }}>
        No dashboard data available.
      </Alert>
    );
  }

  const { attendance, menu, permissions, organization } = dashboardData;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Paper sx={{ p: 3, mb: 3, background: 'linear-gradient(135deg, #1976d2, #42a5f5)' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h4" fontWeight="bold" color="white" gutterBottom>
              Welcome {organization.adminName}
            </Typography>
            <Typography variant="h6" color="white" sx={{ opacity: 0.9 }}>
              {organization.name}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
              {permissions.hasAttendance && (
                <Chip label="Attendance System" color="secondary" variant="outlined" sx={{ color: 'white', borderColor: 'white' }} />
              )}
              {permissions.hasMenuOrdering && (
                <Chip label="Menu & Ordering" color="secondary" variant="outlined" sx={{ color: 'white', borderColor: 'white' }} />
              )}
            </Box>
          </Box>
          <Box sx={{ textAlign: 'right', color: 'white' }}>
            <Typography variant="body2" sx={{ opacity: 0.8 }}>
              Last Updated: {lastUpdated.toLocaleTimeString()}
            </Typography>
            <Tooltip title="Refresh Data">
              <IconButton onClick={handleRefresh} sx={{ color: 'white', mt: 1 }}>
                <Refresh />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
      </Paper>

      {error && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Grid */}
      <Grid container spacing={3}>
        {/* Attendance System Stats */}
        {permissions.hasAttendance && (
          <>
            <Grid item xs={12}>
              <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <People color="primary" />
                Attendance Management System
              </Typography>
              <Divider sx={{ mb: 2 }} />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Total Members"
                value={attendance.totalMembers}
                subtitle="Registered members"
                icon={<People />}
                color="primary"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Members"
                value={attendance.activeMembers}
                subtitle="Currently active"
                icon={<CheckCircle />}
                color="success"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Attendance"
                value={attendance.todayAttendance}
                subtitle="Members present today"
                icon={<Today />}
                color="info"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Sessions"
                value={attendance.activeSessions}
                subtitle="Currently running"
                icon={<Schedule />}
                color="warning"
                loading={loading}
              />
            </Grid>

            {/* Attendance Rate Progress */}
            <Grid item xs={12} md={6}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Analytics color="primary" />
                    Attendance Rate
                  </Typography>
                  <Box sx={{ mt: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2" color="text.secondary">
                        Today's Attendance
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {attendance.attendanceRate}%
                      </Typography>
                    </Box>
                    <LinearProgress 
                      variant="determinate" 
                      value={attendance.attendanceRate} 
                      sx={{ height: 8, borderRadius: 4 }}
                      color={attendance.attendanceRate >= 80 ? 'success' : attendance.attendanceRate >= 60 ? 'warning' : 'error'}
                    />
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                      {attendance.todayAttendance} out of {attendance.totalMembers} members present
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </>
        )}

        {/* Menu & Ordering System Stats */}
        {permissions.hasMenuOrdering && (
          <>
            <Grid item xs={12}>
              <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: permissions.hasAttendance ? 4 : 0 }}>
                <Restaurant color="secondary" />
                Menu & Ordering System
              </Typography>
              <Divider sx={{ mb: 2 }} />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Menu Categories"
                value={menu.totalCategories}
                subtitle="Available categories"
                icon={<Restaurant />}
                color="secondary"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Menu Items"
                value={menu.totalItems}
                subtitle="Total items"
                icon={<Restaurant />}
                color="primary"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Restaurant Tables"
                value={menu.totalTables}
                subtitle="Available tables"
                icon={<TableRestaurant />}
                color="info"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Orders"
                value={menu.todayOrders}
                subtitle="Orders placed today"
                icon={<ShoppingCart />}
                color="success"
                loading={loading}
              />
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Active Orders"
                value={menu.activeOrders}
                subtitle="Pending/Preparing"
                icon={<AccessTime />}
                color="warning"
                loading={loading}
              />
            </Grid>
            
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Today's Revenue"
                value={`₹${menu.totalRevenue.toLocaleString()}`}
                subtitle="Total earnings"
                icon={<TrendingUp />}
                color="success"
                loading={loading}
              />
            </Grid>
          </>
        )}

        {/* No Permissions Message */}
        {!permissions.hasAttendance && !permissions.hasMenuOrdering && (
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

export default DashboardOverview;
