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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  People,
  Schedule,
  TrendingUp,
  CheckCircle,
  Cancel,
  AccessTime,
  Refresh,
  Visibility,
  PersonAdd,
  Today,
  CalendarMonth,
  Timer,
  Group,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface AttendanceData {
  totalMembers: number;
  activeMembers: number;
  todayAttendance: number;
  activeSessions: number;
  attendanceRate: number;
  recentAttendance: AttendanceRecord[];
  activeMembersList: Member[];
  todaysSessions: Session[];
}

interface AttendanceRecord {
  id: number;
  memberName: string;
  checkInTime: string;
  checkOutTime?: string;
  duration?: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE';
}

interface Member {
  id: number;
  name: string;
  mobileNumber: string;
  isActive: boolean;
  lastAttendance?: string;
  profilePicture?: string;
}

interface Session {
  id: number;
  name: string;
  startTime: string;
  endTime?: string;
  isActive: boolean;
  attendeeCount: number;
  description?: string;
}

const AttendanceOverview: React.FC = () => {
  const [attendanceData, setAttendanceData] = useState<AttendanceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedSession, setSelectedSession] = useState<Session | null>(null);
  const [sessionDetailsOpen, setSessionDetailsOpen] = useState(false);

  const fetchAttendanceData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [membersResponse, sessionsResponse, attendanceResponse] = await Promise.all([
        ApiService.get('/api/subscribers'),
        ApiService.get('/api/sessions'),
        ApiService.get('/api/attendance/today').catch(() => ({ data: [] })),
      ]);

      const members = membersResponse.data || [];
      const sessions = sessionsResponse.data || [];
      const todayAttendance = attendanceResponse.data || [];

      const data: AttendanceData = {
        totalMembers: members.length,
        activeMembers: members.filter((m: any) => m.isActive).length,
        todayAttendance: todayAttendance.length,
        activeSessions: sessions.filter((s: any) => s.isActive).length,
        attendanceRate: members.length > 0 ? Math.round((todayAttendance.length / members.length) * 100) : 0,
        recentAttendance: todayAttendance.slice(0, 10).map((record: any) => ({
          id: record.id,
          memberName: record.subscriber?.name || 'Unknown',
          checkInTime: new Date(record.checkInTime).toLocaleTimeString(),
          checkOutTime: record.checkOutTime ? new Date(record.checkOutTime).toLocaleTimeString() : undefined,
          duration: record.duration || 'Ongoing',
          status: record.status || 'PRESENT',
        })),
        activeMembersList: members.filter((m: any) => m.isActive).slice(0, 10).map((member: any) => ({
          id: member.id,
          name: member.name,
          mobileNumber: member.mobileNumber,
          isActive: member.isActive,
          lastAttendance: member.lastAttendance,
          profilePicture: member.profilePicture,
        })),
        todaysSessions: sessions.map((session: any) => ({
          id: session.id,
          name: session.name,
          startTime: new Date(session.startTime).toLocaleTimeString(),
          endTime: session.endTime ? new Date(session.endTime).toLocaleTimeString() : undefined,
          isActive: session.isActive,
          attendeeCount: session.attendeeCount || 0,
          description: session.description,
        })),
      };

      setAttendanceData(data);
    } catch (err: any) {
      console.error('Failed to fetch attendance data:', err);
      setError('Failed to load attendance data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAttendanceData();
    
    // Auto-refresh every 2 minutes
    const interval = setInterval(fetchAttendanceData, 2 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  const handleSessionDetails = (session: Session) => {
    setSelectedSession(session);
    setSessionDetailsOpen(true);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  if (!attendanceData) {
    return (
      <Alert severity="warning" sx={{ m: 2 }}>
        No attendance data available.
      </Alert>
    );
  }

  const { totalMembers, activeMembers, todayAttendance, activeSessions, attendanceRate, recentAttendance, activeMembersList, todaysSessions } = attendanceData;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Attendance Management
        </Typography>
        <Button
          variant="outlined"
          startIcon={<Refresh />}
          onClick={fetchAttendanceData}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #1976d2, #42a5f5)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalMembers}
                  </Typography>
                  <Typography variant="h6">
                    Total Members
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 56, height: 56 }}>
                  <People fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #2e7d32, #4caf50)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {activeMembers}
                  </Typography>
                  <Typography variant="h6">
                    Active Members
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 56, height: 56 }}>
                  <CheckCircle fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #ed6c02, #ff9800)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {todayAttendance}
                  </Typography>
                  <Typography variant="h6">
                    Today's Attendance
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 56, height: 56 }}>
                  <Today fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #7b1fa2, #9c27b0)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {activeSessions}
                  </Typography>
                  <Typography variant="h6">
                    Active Sessions
                  </Typography>
                </Box>
                <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 56, height: 56 }}>
                  <Schedule fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Attendance Rate */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TrendingUp color="primary" />
                Attendance Rate
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">
                    Today's Attendance
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {attendanceRate}%
                  </Typography>
                </Box>
                <LinearProgress 
                  variant="determinate" 
                  value={attendanceRate} 
                  sx={{ height: 10, borderRadius: 5 }}
                  color={attendanceRate >= 80 ? 'success' : attendanceRate >= 60 ? 'warning' : 'error'}
                />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  {todayAttendance} out of {totalMembers} members present
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Schedule color="primary" />
                Today's Sessions
              </Typography>
              <List dense>
                {todaysSessions.slice(0, 3).map((session) => (
                  <ListItem
                    key={session.id}
                    secondaryAction={
                      <Tooltip title="View Details">
                        <IconButton onClick={() => handleSessionDetails(session)}>
                          <Visibility />
                        </IconButton>
                      </Tooltip>
                    }
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: session.isActive ? 'success.main' : 'grey.500' }}>
                        <Timer />
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={session.name}
                      secondary={
                        <Box>
                          <Typography variant="caption" display="block">
                            {session.startTime} - {session.endTime || 'Ongoing'}
                          </Typography>
                          <Chip
                            label={session.isActive ? 'Active' : 'Completed'}
                            size="small"
                            color={session.isActive ? 'success' : 'default'}
                            sx={{ mt: 0.5 }}
                          />
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Attendance and Active Members */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccessTime color="primary" />
                Recent Attendance
              </Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Member</TableCell>
                      <TableCell>Check In</TableCell>
                      <TableCell>Check Out</TableCell>
                      <TableCell>Duration</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recentAttendance.map((record) => (
                      <TableRow key={record.id}>
                        <TableCell>{record.memberName}</TableCell>
                        <TableCell>{record.checkInTime}</TableCell>
                        <TableCell>{record.checkOutTime || '-'}</TableCell>
                        <TableCell>{record.duration}</TableCell>
                        <TableCell>
                          <Chip
                            label={record.status}
                            size="small"
                            color={
                              record.status === 'PRESENT' ? 'success' :
                              record.status === 'LATE' ? 'warning' : 'error'
                            }
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Group color="primary" />
                Active Members
              </Typography>
              <List dense>
                {activeMembersList.map((member) => (
                  <ListItem key={member.id}>
                    <ListItemAvatar>
                      <Avatar src={member.profilePicture}>
                        {member.name.charAt(0)}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={member.name}
                      secondary={
                        <Box>
                          <Typography variant="caption" display="block">
                            {member.mobileNumber}
                          </Typography>
                          <Chip
                            label="Active"
                            size="small"
                            color="success"
                            sx={{ mt: 0.5 }}
                          />
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Session Details Dialog */}
      <Dialog open={sessionDetailsOpen} onClose={() => setSessionDetailsOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Session Details</DialogTitle>
        <DialogContent>
          {selectedSession && (
            <Box>
              <Typography variant="h6" gutterBottom>
                {selectedSession.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {selectedSession.description || 'No description available'}
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Start Time:</Typography>
                  <Typography variant="body2">{selectedSession.startTime}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">End Time:</Typography>
                  <Typography variant="body2">{selectedSession.endTime || 'Ongoing'}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Status:</Typography>
                  <Chip
                    label={selectedSession.isActive ? 'Active' : 'Completed'}
                    size="small"
                    color={selectedSession.isActive ? 'success' : 'default'}
                  />
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2">Attendees:</Typography>
                  <Typography variant="body2">{selectedSession.attendeeCount}</Typography>
                </Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSessionDetailsOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AttendanceOverview;
