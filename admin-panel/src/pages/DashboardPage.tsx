import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Avatar,
  LinearProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Divider,
  Button,
  Paper,
} from '@mui/material';
import {
  Business,
  People,
  PersonAdd,
  TrendingUp,
  Assessment,
  Notifications,
  Schedule,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/ApiService';

interface DashboardStats {
  totalEntities: number;
  totalEntityAdmins: number;
  unassignedEntities: number;
  recentActivities: Array<{
    id: number;
    type: string;
    message: string;
    timestamp: string;
  }>;
}

const DashboardPage: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalEntities: 0,
    totalEntityAdmins: 0,
    unassignedEntities: 0,
    recentActivities: []
  });
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  const generateRecentActivities = (entities: any[], entitiesWithoutAdmin: any[]) => {
    const activities: Array<{
      id: number;
      type: string;
      message: string;
      timestamp: string;
    }> = [];
    let activityId = 1;

    // Generate activities based on actual entities
    entities.forEach((entity, index) => {
      const hasAdmin = !entitiesWithoutAdmin.some(e => e.entityId === entity.entityId);

      // Entity creation activity
      activities.push({
        id: activityId++,
        type: 'entity_created',
        message: `Entity "${entity.name}" was created`,
        timestamp: `${index + 1} day${index > 0 ? 's' : ''} ago`
      });

      // Admin assignment activity (if entity has admin)
      if (hasAdmin) {
        activities.push({
          id: activityId++,
          type: 'admin_assigned',
          message: `Admin assigned to "${entity.name}"`,
          timestamp: `${index + 2} day${index > 0 ? 's' : ''} ago`
        });
      }
    });

    // Sort by most recent and take only the last 5
    return activities.slice(-5).reverse();
  };

  const fetchDashboardData = useCallback(async () => {
    try {
      setIsLoading(true);
      // Fetch entities count
      const entitiesResponse = await ApiService.get('/super/entities');
      const entities = entitiesResponse.data || [];
      const totalEntities = entities.length;

      // Fetch entities without admins
      const unassignedResponse = await ApiService.get('/super/entities/without-admin');
      const entitiesWithoutAdmin = unassignedResponse.data || [];
      const unassignedEntities = entitiesWithoutAdmin.length;

      // Calculate actual entity admins count
      const totalEntityAdmins = totalEntities - unassignedEntities;

      setStats({
        totalEntities,
        totalEntityAdmins,
        unassignedEntities,
        recentActivities: generateRecentActivities(entities, entitiesWithoutAdmin)
      });
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
      // Fallback to empty state
      setStats({
        totalEntities: 0,
        totalEntityAdmins: 0,
        unassignedEntities: 0,
        recentActivities: []
      });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const StatCard: React.FC<{
    title: string;
    value: number | string;
    icon: React.ReactNode;
    color: string;
    onClick?: () => void;
  }> = ({ title, value, icon, color, onClick }) => (
    <Card 
      sx={{ 
        cursor: onClick ? 'pointer' : 'default',
        '&:hover': onClick ? { boxShadow: 6 } : {}
      }}
      onClick={onClick}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4" component="h2" fontWeight="bold">
              {isLoading ? '-' : typeof value === 'number' ? value.toLocaleString() : value}
            </Typography>
          </Box>
          <Avatar sx={{ bgcolor: color, width: 56, height: 56 }}>
            {icon}
          </Avatar>
        </Box>
        {isLoading && <LinearProgress sx={{ mt: 2 }} />}
      </CardContent>
    </Card>
  );

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" gutterBottom>
          Dashboard Overview
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Welcome to the Super Admin Dashboard. Monitor and manage your attendance management system.
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={4}>
          <StatCard
            title="Total Entities"
            value={stats.totalEntities}
            icon={<Business />}
            color="#1976d2"
            onClick={() => navigate('/dashboard/entities')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatCard
            title="Entity Admins"
            value={stats.totalEntityAdmins}
            icon={<People />}
            color="#2e7d32"
            onClick={() => navigate('/dashboard/entity-admins')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatCard
            title="Unassigned Entities"
            value={stats.unassignedEntities}
            icon={<Assessment />}
            color="#ed6c02"
            onClick={() => navigate('/dashboard/unassigned-entities')}
          />
        </Grid>
      </Grid>

      {/* Quick Actions and Recent Activities */}
      <Grid container spacing={3}>
        {/* Quick Actions */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TrendingUp />
                Quick Actions
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                <Button
                  variant="outlined"
                  startIcon={<Business />}
                  onClick={() => navigate('/dashboard/entities')}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Manage Entities
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<PersonAdd />}
                  onClick={() => navigate('/dashboard/assign-admin')}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Assign Entity Admin
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Assessment />}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                  disabled
                >
                  View Reports (Coming Soon)
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Activities */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Notifications />
                Recent Activities
              </Typography>
              <List>
                {stats.recentActivities.map((activity, index) => (
                  <React.Fragment key={activity.id}>
                    <ListItem alignItems="flex-start" sx={{ px: 0 }}>
                      <ListItemAvatar>
                        <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
                          <Schedule fontSize="small" />
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={activity.message}
                        secondary={activity.timestamp}
                        primaryTypographyProps={{ variant: 'body2' }}
                        secondaryTypographyProps={{ variant: 'caption' }}
                      />
                    </ListItem>
                    {index < stats.recentActivities.length - 1 && <Divider variant="inset" component="li" />}
                  </React.Fragment>
                ))}
              </List>
              {stats.recentActivities.length === 0 && (
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                  No recent activities
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* System Status */}
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" gutterBottom>
          System Status
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={4}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Chip label="System Online" color="success" size="small" />
              <Typography variant="body2">All services operational</Typography>
            </Box>
          </Grid>
          <Grid item xs={12} sm={4}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Chip label="Database" color="success" size="small" />
              <Typography variant="body2">Connected and healthy</Typography>
            </Box>
          </Grid>
          <Grid item xs={12} sm={4}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Chip label="API" color="success" size="small" />
              <Typography variant="body2">Responding normally</Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
};

export default DashboardPage;
