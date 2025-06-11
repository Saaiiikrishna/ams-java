import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Avatar,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Button,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
} from '@mui/material';
import {
  ArrowBack,
  Schedule,
  CheckCircle,
  Person,
  AccessTime,
  Group,
  Today,
  Timer,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface SessionDetails {
  id: number;
  name: string;
  startTime: string;
  endTime?: string;
  status: 'active' | 'completed';
  attendees: Attendee[];
}

interface Attendee {
  id: number;
  subscriberId: number;
  subscriberName: string;
  checkInTime: string;
  checkOutTime?: string;
  status: 'checked_in' | 'checked_out';
}

const SessionDetailsPage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [session, setSession] = useState<SessionDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (sessionId) {
      fetchSessionDetails(parseInt(sessionId));
    }
  }, [sessionId]);

  const fetchSessionDetails = async (id: number) => {
    try {
      setLoading(true);
      setError(null);

      // Fetch session details
      const sessionResponse = await ApiService.get(`/api/sessions/${id}`);
      const sessionData = sessionResponse.data;

      // Fetch attendance logs for this session
      const attendanceResponse = await ApiService.get(`/api/sessions/${id}/attendance`);
      const attendanceData = attendanceResponse.data || [];

      // Combine session and attendance data
      const sessionDetails: SessionDetails = {
        id: sessionData.id,
        name: sessionData.name,
        startTime: sessionData.startTime,
        endTime: sessionData.endTime,
        status: sessionData.endTime ? 'completed' : 'active',
        attendees: attendanceData.map((log: any) => ({
          id: log.id,
          subscriberId: log.subscriber?.id || 0,
          subscriberName: `${log.subscriber?.firstName || 'Unknown'} ${log.subscriber?.lastName || 'User'}`,
          checkInTime: log.checkInTime,
          checkOutTime: log.checkOutTime,
          status: log.checkOutTime ? 'checked_out' : 'checked_in',
        })),
      };

      setSession(sessionDetails);
    } catch (err: any) {
      console.error('Failed to fetch session details:', err);
      setError('Failed to load session details. Please try again.');
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
    return new Date(timeString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const calculateDuration = (startTime: string, endTime?: string) => {
    const start = new Date(startTime);
    const end = endTime ? new Date(endTime) : new Date();
    const diffInMs = end.getTime() - start.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const hours = Math.floor(diffInMinutes / 60);
    const minutes = diffInMinutes % 60;
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error || !session) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          {error || 'Session not found'}
        </Alert>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/dashboard')}
          variant="outlined"
        >
          Back to Dashboard
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/dashboard')}
          variant="outlined"
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" component="h1" fontWeight="bold">
          Session Details
        </Typography>
      </Box>

      {/* Session Info Card */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Avatar
              sx={{
                bgcolor: session.status === 'active' ? 'warning.main' : 'success.main',
                width: 56,
                height: 56,
                mr: 2,
              }}
            >
              {session.status === 'active' ? <Schedule /> : <CheckCircle />}
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h5" fontWeight="bold">
                {session.name}
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Chip
                  icon={session.status === 'active' ? <Schedule /> : <CheckCircle />}
                  label={session.status === 'active' ? 'Active Session' : 'Completed Session'}
                  color={session.status === 'active' ? 'warning' : 'success'}
                  variant="outlined"
                />
              </Box>
            </Box>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Grid container spacing={3}>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Today color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Date
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {formatDate(session.startTime)}
                  </Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccessTime color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Start Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {formatTime(session.startTime)}
                  </Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Timer color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Duration
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {calculateDuration(session.startTime, session.endTime)}
                  </Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Group color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Total Attendees
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {session.attendees.length}
                  </Typography>
                </Box>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Attendees List */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Attendees ({session.attendees.length})
          </Typography>
          
          {session.attendees.length === 0 ? (
            <Typography color="textSecondary" sx={{ textAlign: 'center', py: 4 }}>
              No attendees for this session yet.
            </Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Attendee</TableCell>
                    <TableCell>Check-in Time</TableCell>
                    <TableCell>Check-out Time</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Duration</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {session.attendees.map((attendee) => (
                    <TableRow key={attendee.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar sx={{ bgcolor: 'primary.main' }}>
                            <Person />
                          </Avatar>
                          <Typography variant="body1" fontWeight="medium">
                            {attendee.subscriberName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatTime(attendee.checkInTime)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {attendee.checkOutTime ? formatTime(attendee.checkOutTime) : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={attendee.status === 'checked_in' ? 'Checked In' : 'Checked Out'}
                          color={attendee.status === 'checked_in' ? 'warning' : 'success'}
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {attendee.checkOutTime 
                            ? calculateDuration(attendee.checkInTime, attendee.checkOutTime)
                            : calculateDuration(attendee.checkInTime)
                          }
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default SessionDetailsPage;
