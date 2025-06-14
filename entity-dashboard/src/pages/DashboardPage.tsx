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
  ListItemAvatar,
  ListItemText,
  Button,
  ListItemButton,
  Tooltip,
  IconButton,
  Divider,
} from '@mui/material';
import {
  People,
  AccessTime,
  Assessment,
  TrendingUp,
  Schedule,
  Today,
  CheckCircle,
  ArrowForward,
  Group,
  Nfc,
  QrCode,
  Bluetooth,
  Wifi,
  PhoneAndroid,
  Add,
  PlayArrow,
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
  allowedCheckInMethods?: string[];
}

interface ActiveSession {
  id: number;
  name: string;
  description?: string;
  startTime: string;
  allowedCheckInMethods: string[];
  attendeeCount: number;
}

interface RecentAttendance {
  id: number;
  subscriber: string;
  session: string;
  time: string;
  type: 'Check-in' | 'Check-out';
  method: string;
  checkOutTime?: string;
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
  const [activeSessions, setActiveSessions] = useState<ActiveSession[]>([]);
  const [recentAttendance, setRecentAttendance] = useState<RecentAttendance[]>([]);
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
          allowedCheckInMethods: session.allowedCheckInMethods || [],
        }));

      setRecentSessions(recent);

      // Set active sessions with attendance count
      const activeSessionsData = activeSessions.map((session: any) => ({
        id: session.id,
        name: session.name,
        description: session.description,
        startTime: session.startTime,
        allowedCheckInMethods: session.allowedCheckInMethods || [],
        attendeeCount: 0, // Will be updated with real data
      }));

      setActiveSessions(activeSessionsData);

      // Fetch recent attendance data
      try {
        const attendanceResponse = await ApiService.get('/api/attendance/recent?limit=10');
        const attendanceData = attendanceResponse.data || [];
        setRecentAttendance(attendanceData);
      } catch (err) {
        console.warn('Failed to fetch recent attendance:', err);
      }
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



  const getTimeAgo = (timeString: string) => {
    const now = new Date();
    const sessionTime = new Date(timeString);
    const diffInMs = now.getTime() - sessionTime.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes} min ago`;
    if (diffInHours < 24) return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
  };

  const handleSessionClick = (sessionId: number) => {
    navigate(`/dashboard/sessions/${sessionId}`);
  };

  const getCheckInMethodIcon = (method: string) => {
    switch (method) {
      case 'NFC': return <Nfc />;
      case 'QR': return <QrCode />;
      case 'BLUETOOTH': return <Bluetooth />;
      case 'WIFI': return <Wifi />;
      case 'MOBILE_NFC': return <PhoneAndroid />;
      default: return <Nfc />;
    }
  };

  const getCheckInMethodLabel = (method: string) => {
    switch (method) {
      case 'NFC': return 'NFC Card';
      case 'QR': return 'QR Code';
      case 'BLUETOOTH': return 'Bluetooth';
      case 'WIFI': return 'WiFi';
      case 'MOBILE_NFC': return 'Mobile NFC';
      default: return method;
    }
  };

  const handleQuickAction = (action: string) => {
    switch (action) {
      case 'nfc':
        // Handle NFC check-in
        break;
      case 'qr':
        navigate('/dashboard/sessions');
        break;
      case 'wifi':
        // Handle WiFi check-in
        break;
      case 'bluetooth':
        // Handle Bluetooth check-in
        break;
      case 'mobile_nfc':
        // Handle Mobile NFC check-in
        break;
      case 'create_session':
        navigate('/dashboard/sessions');
        break;
      default:
        break;
    }
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

      {/* Quick Actions */}
      <Paper sx={{ p: 3, mb: 4 }}>
        <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PlayArrow color="primary" />
          Quick Actions
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Start attendance tracking with your preferred method
        </Typography>

        <Grid container spacing={2}>
          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'primary.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('nfc')}
            >
              <Avatar sx={{ bgcolor: 'primary.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <Nfc />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                NFC Scan
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Tap NFC card
              </Typography>
            </Card>
          </Grid>

          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'success.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('qr')}
            >
              <Avatar sx={{ bgcolor: 'success.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <QrCode />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                QR Code
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Show QR code
              </Typography>
            </Card>
          </Grid>

          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'info.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('wifi')}
            >
              <Avatar sx={{ bgcolor: 'info.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <Wifi />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                WiFi Check-in
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Network based
              </Typography>
            </Card>
          </Grid>

          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'warning.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('bluetooth')}
            >
              <Avatar sx={{ bgcolor: 'warning.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <Bluetooth />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                Bluetooth
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Proximity based
              </Typography>
            </Card>
          </Grid>

          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'secondary.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('mobile_nfc')}
            >
              <Avatar sx={{ bgcolor: 'secondary.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <PhoneAndroid />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                Mobile NFC
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Phone based
              </Typography>
            </Card>
          </Grid>

          <Grid item xs={6} sm={4} md={2}>
            <Card
              sx={{
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)',
                  boxShadow: 4,
                  borderColor: 'error.main'
                },
                border: '1px solid',
                borderColor: 'divider',
                textAlign: 'center',
                p: 2
              }}
              onClick={() => handleQuickAction('create_session')}
            >
              <Avatar sx={{ bgcolor: 'error.main', mx: 'auto', mb: 1, width: 48, height: 48 }}>
                <Add />
              </Avatar>
              <Typography variant="subtitle2" fontWeight="bold">
                New Session
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Start session
              </Typography>
            </Card>
          </Grid>
        </Grid>
      </Paper>

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

      {/* Active Sessions */}
      {activeSessions.length > 0 && (
        <Paper sx={{ p: 3, mb: 4 }}>
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Schedule color="warning" />
            Active Sessions
          </Typography>
          <Grid container spacing={2}>
            {activeSessions.map((session) => (
              <Grid item xs={12} sm={6} md={4} key={session.id}>
                <Card
                  sx={{
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    '&:hover': {
                      transform: 'translateY(-2px)',
                      boxShadow: 3
                    },
                    border: '1px solid',
                    borderColor: 'warning.main',
                    borderRadius: 2
                  }}
                  onClick={() => handleSessionClick(session.id)}
                >
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <Avatar sx={{ bgcolor: 'warning.main', mr: 2 }}>
                        <Schedule />
                      </Avatar>
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          {session.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Started {getTimeAgo(session.startTime)}
                        </Typography>
                      </Box>
                      <Chip label="ACTIVE" color="warning" size="small" />
                    </Box>

                    {session.description && (
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        {session.description}
                      </Typography>
                    )}

                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
                      {session.allowedCheckInMethods.slice(0, 3).map((method) => (
                        <Chip
                          key={method}
                          icon={getCheckInMethodIcon(method)}
                          label={getCheckInMethodLabel(method)}
                          size="small"
                          variant="outlined"
                        />
                      ))}
                      {session.allowedCheckInMethods.length > 3 && (
                        <Chip
                          label={`+${session.allowedCheckInMethods.length - 3} more`}
                          size="small"
                          variant="outlined"
                        />
                      )}
                    </Box>

                    <Typography variant="body2" color="text.secondary">
                      <Group fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                      {session.attendeeCount} attendees
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Paper>
      )}

      {/* Recent Sessions and Recent Attendance */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Sessions
            </Typography>
            {recentSessions.length === 0 ? (
              <Typography color="textSecondary">
                No sessions found
              </Typography>
            ) : (
              <List sx={{ p: 0 }}>
                {recentSessions.map((session, index) => (
                  <React.Fragment key={session.id}>
                    <ListItemButton
                      onClick={() => handleSessionClick(session.id)}
                      sx={{
                        borderRadius: 2,
                        mb: 1,
                        border: '1px solid',
                        borderColor: 'divider',
                        '&:hover': {
                          borderColor: 'primary.main',
                          backgroundColor: 'primary.50',
                        },
                        transition: 'all 0.2s ease-in-out',
                      }}
                    >
                      <ListItemAvatar>
                        <Avatar
                          sx={{
                            bgcolor: session.status === 'active' ? 'warning.main' : 'success.main',
                            width: 48,
                            height: 48,
                          }}
                        >
                          {session.status === 'active' ? <Schedule /> : <Group />}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="subtitle1" fontWeight="bold">
                              {session.name}
                            </Typography>
                            <Chip
                              icon={session.status === 'active' ? <Schedule /> : <CheckCircle />}
                              label={session.status === 'active' ? 'Active' : 'Completed'}
                              color={session.status === 'active' ? 'warning' : 'success'}
                              size="small"
                              variant="outlined"
                            />
                          </Box>
                        }
                        secondary={
                          <Box sx={{ mt: 0.5 }}>
                            <Typography variant="body2" color="textSecondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <Schedule fontSize="small" />
                              Started {getTimeAgo(session.startTime)} • {formatTime(session.startTime)}
                            </Typography>
                            {session.endTime && (
                              <Typography variant="body2" color="textSecondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
                                <CheckCircle fontSize="small" />
                                Ended at {formatTime(session.endTime)}
                              </Typography>
                            )}
                          </Box>
                        }
                      />
                      <Tooltip title="View session details">
                        <IconButton edge="end" color="primary">
                          <ArrowForward />
                        </IconButton>
                      </Tooltip>
                    </ListItemButton>
                  </React.Fragment>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <TrendingUp color="primary" />
              Recent Attendance
            </Typography>
            {recentAttendance.length === 0 ? (
              <Typography color="textSecondary">
                No recent attendance activity
              </Typography>
            ) : (
              <List sx={{ p: 0 }}>
                {recentAttendance.slice(0, 8).map((attendance, index) => (
                  <React.Fragment key={attendance.id}>
                    <ListItemButton
                      sx={{
                        borderRadius: 1,
                        mb: 0.5,
                        '&:hover': {
                          backgroundColor: 'action.hover',
                        },
                      }}
                    >
                      <ListItemAvatar>
                        <Avatar
                          sx={{
                            bgcolor: attendance.type === 'Check-in' ? 'success.main' : 'warning.main',
                            width: 40,
                            height: 40,
                          }}
                        >
                          {attendance.type === 'Check-in' ?
                            getCheckInMethodIcon(attendance.method) :
                            <CheckCircle />
                          }
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="subtitle2" fontWeight="bold">
                              {attendance.subscriber}
                            </Typography>
                            <Chip
                              label={attendance.type}
                              color={attendance.type === 'Check-in' ? 'success' : 'warning'}
                              size="small"
                              variant="outlined"
                            />
                          </Box>
                        }
                        secondary={
                          <Box>
                            <Typography variant="body2" color="textSecondary">
                              {attendance.session} • {getTimeAgo(attendance.time)}
                            </Typography>
                            <Typography variant="caption" color="textSecondary">
                              via {getCheckInMethodLabel(attendance.method)}
                            </Typography>
                          </Box>
                        }
                      />
                    </ListItemButton>
                    {index < recentAttendance.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
              </List>
            )}

            <Box sx={{ mt: 2, textAlign: 'center' }}>
              <Button
                variant="outlined"
                startIcon={<Assessment />}
                onClick={() => navigate('/dashboard/reports')}
                size="small"
              >
                View All Reports
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
