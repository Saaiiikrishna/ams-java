import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
  Button,
} from '@mui/material';
import {
  People,
  AccessTime,
  Assessment,
  TrendingUp,
  Person,
  Schedule,
  Today,
  CheckCircle,

} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface DashboardStats {
  totalSubscribers: number;
  totalSessions: number;
  todaySessions: number;
  activeSessions: number;
}

interface RecentSession {
  id: number;
  name: string;
  startTime: string;
  endTime?: string;
  status: 'active' | 'completed';
}

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats>({
    totalSubscribers: 0,
    totalSessions: 0,
    todaySessions: 0,
    activeSessions: 0,
  });
  const [recentSessions, setRecentSessions] = useState<RecentSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Initialize with default values
      let subscribers: any[] = [];
      let sessions: any[] = [];

      // Fetch subscribers with error handling
      try {
        const subscribersResponse = await ApiService.get('/api/subscribers');
        subscribers = subscribersResponse.data || [];
      } catch (err) {
        console.warn('Failed to fetch subscribers:', err);
      }

      // Fetch attendance sessions with error handling
      try {
        const sessionsResponse = await ApiService.get('/api/sessions');
        sessions = sessionsResponse.data || [];
      } catch (err) {
        console.warn('Failed to fetch sessions:', err);
      }

      // No need to fetch attendance logs - we'll use sessions data

      // Calculate stats based on sessions
      const today = new Date().toDateString();
      const todaySessions = sessions.filter((session: any) =>
        session.startTime && new Date(session.startTime).toDateString() === today
      );
      const activeSessions = sessions.filter((session: any) =>
        session.startTime && !session.endTime
      );

      setStats({
        totalSubscribers: subscribers.length,
        totalSessions: sessions.length,
        todaySessions: todaySessions.length,
        activeSessions: activeSessions.length,
      });

      // Set recent sessions (last 5 sessions)
      const recent = sessions
        .sort((a: any, b: any) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())
        .slice(0, 5)
        .map((session: any) => ({
          id: session.id,
          name: session.name,
          startTime: session.startTime,
          endTime: session.endTime,
          status: (session.endTime ? 'completed' : 'active') as 'active' | 'completed',
        }));

      setRecentSessions(recent);
    } catch (err: any) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to load dashboard data. Some features may not be available.');
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (timeString: string) => {
    return new Date(timeString).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const formatDate = (timeString: string) => {
    return new Date(timeString).toLocaleDateString();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Dashboard Overview
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                  <People />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Subscribers
                  </Typography>
                  <Typography variant="h5">
                    {stats.totalSubscribers}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Avatar sx={{ bgcolor: 'success.main', mr: 2 }}>
                  <AccessTime />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Sessions
                  </Typography>
                  <Typography variant="h5">
                    {stats.totalSessions}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Avatar sx={{ bgcolor: 'info.main', mr: 2 }}>
                  <Today />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Today's Sessions
                  </Typography>
                  <Typography variant="h5">
                    {stats.todaySessions}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Avatar sx={{ bgcolor: 'warning.main', mr: 2 }}>
                  <TrendingUp />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Active Sessions
                  </Typography>
                  <Typography variant="h5">
                    {stats.activeSessions}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Sessions */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Sessions
            </Typography>
            {recentSessions.length === 0 ? (
              <Typography color="textSecondary">
                No sessions found
              </Typography>
            ) : (
              <List>
                {recentSessions.map((session, index) => (
                  <React.Fragment key={session.id}>
                    <ListItem>
                      <ListItemAvatar>
                        <Avatar>
                          <Person />
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={session.name}
                        secondary={
                          <Box>
                            <Typography variant="body2" color="textSecondary">
                              Started: {formatDate(session.startTime)} at {formatTime(session.startTime)}
                            </Typography>
                            {session.endTime && (
                              <Typography variant="body2" color="textSecondary">
                                Ended: {formatTime(session.endTime)}
                              </Typography>
                            )}
                          </Box>
                        }
                      />
                      <Chip
                        icon={session.status === 'active' ? <Schedule /> : <CheckCircle />}
                        label={session.status === 'active' ? 'Active' : 'Completed'}
                        color={session.status === 'active' ? 'warning' : 'success'}
                        size="small"
                      />
                    </ListItem>
                    {index < recentSessions.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <Typography variant="body2" color="textSecondary" paragraph>
              Manage your attendance system:
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Button
                variant="outlined"
                startIcon={<People />}
                onClick={() => navigate('/dashboard/subscribers')}
                fullWidth
                sx={{ justifyContent: 'flex-start' }}
              >
                Manage Subscribers
              </Button>
              <Button
                variant="outlined"
                startIcon={<AccessTime />}
                onClick={() => navigate('/dashboard/sessions')}
                fullWidth
                sx={{ justifyContent: 'flex-start' }}
              >
                View Sessions
              </Button>
              <Button
                variant="outlined"
                startIcon={<Assessment />}
                onClick={() => navigate('/dashboard/reports')}
                fullWidth
                sx={{ justifyContent: 'flex-start' }}
              >
                Generate Reports
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
