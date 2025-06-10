import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip,
  Divider,
} from '@mui/material';
import {
  Business,
  People,
  Assessment,
  TrendingUp,
  Schedule,
  CheckCircle,
  Warning,
  PersonAdd,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  onClick?: () => void;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color, onClick }) => (
  <Card 
    sx={{ 
      height: '100%', 
      cursor: onClick ? 'pointer' : 'default',
      '&:hover': onClick ? { 
        transform: 'translateY(-2px)', 
        boxShadow: 3,
        transition: 'all 0.2s ease-in-out'
      } : {}
    }}
    onClick={onClick}
  >
    <CardContent>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
          <Typography color="textSecondary" gutterBottom variant="h6">
            {title}
          </Typography>
          <Typography variant="h3" component="div" sx={{ color, fontWeight: 'bold' }}>
            {value}
          </Typography>
        </Box>
        <Box sx={{ color, opacity: 0.8 }}>
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

interface DashboardStats {
  totalEntities: number;
  totalEntityAdmins: number;
  unassignedEntities: number;
  recentActivities: Array<{
    id: number;
    type: string;
    description: string;
    timestamp: string;
    status: 'success' | 'warning' | 'info';
  }>;
}

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats>({
    totalEntities: 0,
    totalEntityAdmins: 0,
    unassignedEntities: 0,
    recentActivities: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch all required data
      const [entitiesResponse, entityAdminsResponse, unassignedResponse] = await Promise.all([
        ApiService.get('/admin/entities'),
        ApiService.get('/admin/entity-admins'),
        ApiService.get('/admin/entities/without-admin'),
      ]);

      const entities = entitiesResponse.data || [];
      const entityAdmins = entityAdminsResponse.data || [];
      const unassignedEntities = unassignedResponse.data || [];

      // Generate recent activities based on actual data
      const activities = [
        ...entityAdmins.slice(-3).map((admin: any, index: number) => ({
          id: index + 1,
          type: 'admin_assigned',
          description: `Admin "${admin.username}" assigned to ${admin.organizationName}`,
          timestamp: admin.createdAt || new Date().toISOString(),
          status: 'success' as const,
        })),
        ...unassignedEntities.slice(0, 2).map((entity: any, index: number) => ({
          id: index + 10,
          type: 'entity_created',
          description: `Entity "${entity.name}" created and needs admin assignment`,
          timestamp: new Date(Date.now() - (index * 3600000)).toISOString(),
          status: 'warning' as const,
        })),
      ].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

      setStats({
        totalEntities: entities.length,
        totalEntityAdmins: entityAdmins.length,
        unassignedEntities: unassignedEntities.length,
        recentActivities: activities.slice(0, 5),
      });
    } catch (err: any) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to load dashboard data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'admin_assigned':
        return <PersonAdd color="success" />;
      case 'entity_created':
        return <Business color="warning" />;
      default:
        return <CheckCircle color="info" />;
    }
  };

  const formatTimeAgo = (timestamp: string) => {
    const now = new Date();
    const past = new Date(timestamp);
    const diffInMs = now.getTime() - past.getTime();
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInDays > 0) return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
    if (diffInHours > 0) return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    return 'Just now';
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Dashboard Overview
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Monitor and manage your attendance system
        </Typography>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Entities"
            value={stats.totalEntities}
            icon={<Business sx={{ fontSize: 40 }} />}
            color="#1976d2"
            onClick={() => navigate('/dashboard/entities')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Entity Admins"
            value={stats.totalEntityAdmins}
            icon={<People sx={{ fontSize: 40 }} />}
            color="#2e7d32"
            onClick={() => navigate('/dashboard/entity-admins')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Unassigned Entities"
            value={stats.unassignedEntities}
            icon={<Assessment sx={{ fontSize: 40 }} />}
            color="#ed6c02"
            onClick={() => navigate('/dashboard/unassigned-entities')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="System Health"
            value={100}
            icon={<TrendingUp sx={{ fontSize: 40 }} />}
            color="#9c27b0"
          />
        </Grid>
      </Grid>

      {/* Recent Activities */}
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Schedule color="primary" />
                Recent Activities
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              {stats.recentActivities.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <CheckCircle sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                  <Typography variant="h6" color="text.secondary">
                    No recent activities
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Activities will appear here as you manage entities and admins
                  </Typography>
                </Box>
              ) : (
                <List>
                  {stats.recentActivities.map((activity, index) => (
                    <React.Fragment key={activity.id}>
                      <ListItem>
                        <ListItemIcon>
                          {getActivityIcon(activity.type)}
                        </ListItemIcon>
                        <ListItemText
                          primary={activity.description}
                          secondary={formatTimeAgo(activity.timestamp)}
                        />
                        <Chip
                          label={activity.status}
                          color={activity.status === 'success' ? 'success' : activity.status === 'warning' ? 'warning' : 'info'}
                          size="small"
                        />
                      </ListItem>
                      {index < stats.recentActivities.length - 1 && <Divider />}
                    </React.Fragment>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Warning color="warning" />
                Quick Actions
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <List>
                <ListItem 
                  onClick={() => navigate('/dashboard/entities')}
                  sx={{ cursor: 'pointer', borderRadius: 1, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon>
                    <Business color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Manage Entities" secondary="Create and edit entities" />
                </ListItem>
                
                <ListItem 
                  onClick={() => navigate('/dashboard/assign-admin')}
                  sx={{ cursor: 'pointer', borderRadius: 1, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon>
                    <PersonAdd color="success" />
                  </ListItemIcon>
                  <ListItemText primary="Assign Admin" secondary="Assign admins to entities" />
                </ListItem>
                
                {stats.unassignedEntities > 0 && (
                  <ListItem 
                    onClick={() => navigate('/dashboard/unassigned-entities')}
                    sx={{ cursor: 'pointer', borderRadius: 1, '&:hover': { bgcolor: 'action.hover' } }}
                  >
                    <ListItemIcon>
                      <Warning color="warning" />
                    </ListItemIcon>
                    <ListItemText 
                      primary="Unassigned Entities" 
                      secondary={`${stats.unassignedEntities} entities need admins`} 
                    />
                  </ListItem>
                )}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
