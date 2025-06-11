import React, { useState, useEffect, FormEvent } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,

  IconButton,
  Chip,
  Alert,
  Grid,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  InputAdornment,
  Avatar,
  Tooltip,

  Divider,
} from '@mui/material';
import {
  Add,
  AccessTime,
  PlayArrow,
  Stop,
  Schedule,
  Event,
  CheckCircle,
  Cancel,
  Refresh,
  NearMe,
  Delete,
  Person,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/ApiService';
import ConfirmationDialog from '../components/ConfirmationDialog';
import logger from '../services/LoggingService';

interface AttendanceSession {
  id: number;
  name: string;
  startTime: string; // ISO string, consider formatting for display
  endTime?: string | null; // ISO string or null
  organizationId?: number;
  // attendanceLogs count or list could be part of a more detailed view
}

interface NewAttendanceSession {
  name: string;
  startTime?: string; // Optional, backend might default to now if not provided
}

const SessionPage: React.FC = () => {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<AttendanceSession[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [recentScans, setRecentScans] = useState<any[]>([]);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Confirmation dialog state
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmationData, setConfirmationData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
    severity?: 'warning' | 'error' | 'info' | 'success';
  } | null>(null);

  // Form state for creating new session
  const [sessionName, setSessionName] = useState('');
  const [sessionStartTime, setSessionStartTime] = useState(''); // Store as string for input type datetime-local
  const [formLoading, setFormLoading] = useState(false);

  const [showCreateForm, setShowCreateForm] = useState(false);

  const resetForm = () => {
    setSessionName('');
    setSessionStartTime('');
    setShowCreateForm(false);
    setFormLoading(false);
    setError(null);
    setSuccessMessage(null);
  };

  // Fetch all sessions for the current organization
  const fetchSessions = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<AttendanceSession[]>('/api/sessions');
      setSessions(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch sessions:", err);
      setError(err.response?.data?.message || 'Failed to fetch sessions.');
      setSessions([]);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchRecentScans = async () => {
    try {
      const response = await ApiService.get('/api/attendance/recent?limit=10');
      setRecentScans(response.data);
      logger.info('Recent scans fetched successfully', 'SessionPage', { count: response.data.length });
    } catch (err: any) {
      logger.error('Failed to fetch recent scans', 'SessionPage', err);
      // Don't set error state for this as it's not critical
    }
  };

  useEffect(() => {
    fetchSessions();
    fetchRecentScans();

    // Set up polling for real-time updates
    const interval = setInterval(() => {
      fetchRecentScans();
    }, 10000); // Update every 10 seconds

    return () => clearInterval(interval);
  }, []);

  const handleCreateSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setFormLoading(true);

    const newSessionData: NewAttendanceSession = { name: sessionName };
    if (sessionStartTime) {
      // Send the datetime as-is without timezone conversion
      // Format: YYYY-MM-DDTHH:mm:ss (LocalDateTime format)
      const formattedTime = sessionStartTime.includes(':') && sessionStartTime.split(':').length === 2
        ? sessionStartTime + ':00'
        : sessionStartTime;
      newSessionData.startTime = formattedTime;
    }
    // If no start time specified, let backend use current server time

    try {
      const response = await ApiService.post<AttendanceSession>('/api/sessions', newSessionData);
      setSuccessMessage(`Session '${response.data.name}' created successfully!`);
      resetForm();
      fetchSessions(); // Refresh list
    } catch (err: any) {
      console.error("Failed to create session:", err);
      setError(err.response?.data?.message || 'Failed to create session.');
    } finally {
      setFormLoading(false);
    }
  };

  const handleEndSession = (sessionId: number) => {
    const session = sessions.find(s => s.id === sessionId);
    setConfirmationData({
      title: 'End Session',
      message: `Are you sure you want to end session "${session?.name}"? This will stop attendance tracking for this session.`,
      onConfirm: () => performEndSession(sessionId),
      severity: 'warning',
    });
    setConfirmationOpen(true);
  };

  const performEndSession = async (sessionId: number) => {
    setError(null);
    setSuccessMessage(null);
    try {
      const response = await ApiService.put<AttendanceSession>(`/api/sessions/${sessionId}/end`, {});
      setSuccessMessage(`Session '${response.data.name}' (ID: ${response.data.id}) ended successfully.`);
      fetchSessions(); // Refresh list
      setConfirmationOpen(false);
    } catch (err: any) {
      console.error("Failed to end session:", err);
      setError(err.response?.data?.message || 'Failed to end session.');
      setConfirmationOpen(false);
    }
  };

  const handleDeleteSession = (sessionId: number, sessionName: string) => {
    setConfirmationData({
      title: 'Delete Session',
      message: `Are you sure you want to DELETE session "${sessionName}"? This action cannot be undone and will remove all associated attendance data.`,
      onConfirm: () => performDeleteSession(sessionId, sessionName),
      severity: 'error',
    });
    setConfirmationOpen(true);
  };

  const performDeleteSession = async (sessionId: number, sessionName: string) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await ApiService.delete(`/api/sessions/${sessionId}`);
      setSuccessMessage(`Session '${sessionName}' deleted successfully.`);
      fetchSessions(); // Refresh list
      setConfirmationOpen(false);
    } catch (err: any) {
      console.error("Failed to delete session:", err);
      setError(err.response?.data?.message || 'Failed to delete session.');
      setConfirmationOpen(false);
    }
  };

  const formatDateTime = (isoString?: string | null) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Session Management
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Create and manage attendance sessions
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setShowCreateForm(true)}
          size="large"
          sx={{
            background: 'linear-gradient(45deg, #2196F3 30%, #21CBF3 90%)',
            '&:hover': {
              background: 'linear-gradient(45deg, #1976D2 30%, #1CB5E0 90%)',
            },
          }}
        >
          Create New Session
        </Button>
      </Box>

      {/* Alert Messages */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {successMessage}
        </Alert>
      )}

      {/* Create Session Dialog */}
      <Dialog open={showCreateForm} onClose={() => setShowCreateForm(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar sx={{ bgcolor: 'primary.main' }}>
              <Event />
            </Avatar>
            <Box>
              <Typography variant="h6">Create New Session</Typography>
              <Typography variant="body2" color="text.secondary">
                Set up a new attendance tracking session
              </Typography>
            </Box>
          </Box>
        </DialogTitle>
        <form onSubmit={handleCreateSubmit}>
          <DialogContent>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="Session Name/Purpose"
                  value={sessionName}
                  onChange={(e) => setSessionName(e.target.value)}
                  disabled={formLoading}
                  placeholder="e.g., Morning Meeting, Training Session, Daily Standup"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Schedule />
                      </InputAdornment>
                    ),
                  }}
                  helperText="Enter a descriptive name for this attendance session"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type="datetime-local"
                  label="Start Time (Optional)"
                  value={sessionStartTime}
                  onChange={(e) => setSessionStartTime(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <AccessTime />
                      </InputAdornment>
                    ),
                  }}
                  helperText="Leave empty to start immediately, or schedule for a specific time"
                  InputLabelProps={{
                    shrink: true,
                  }}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowCreateForm(false)} disabled={formLoading}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={formLoading || !sessionName}
              startIcon={formLoading ? <CircularProgress size={20} /> : <PlayArrow />}
              sx={{
                background: 'linear-gradient(45deg, #4CAF50 30%, #8BC34A 90%)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #388E3C 30%, #689F38 90%)',
                },
              }}
            >
              {formLoading ? 'Creating...' : 'Create Session'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Sessions List */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Sessions ({sessions.length})
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              {isLoading && <CircularProgress size={24} />}
              <Tooltip title="Refresh sessions">
                <IconButton onClick={fetchSessions} disabled={isLoading}>
                  <Refresh />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : sessions.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Schedule sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                No sessions found
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Get started by creating your first attendance session
              </Typography>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={() => setShowCreateForm(true)}
              >
                Create First Session
              </Button>
            </Box>
          ) : (
            <TableContainer sx={{ maxHeight: 400, overflow: 'auto' }}>
              <Table stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell>Session</TableCell>
                    <TableCell>Start Time</TableCell>
                    <TableCell>End Time</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Duration</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sessions
                    .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()) // Most recent first
                    .map((session) => {
                    const isActive = !session.endTime;
                    const startTime = new Date(session.startTime);
                    const endTime = session.endTime ? new Date(session.endTime) : null;
                    const duration = endTime
                      ? Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60))
                      : Math.round((new Date().getTime() - startTime.getTime()) / (1000 * 60));

                    return (
                      <TableRow
                        key={session.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => navigate(`/dashboard/sessions/${session.id}`)}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Avatar sx={{ bgcolor: isActive ? 'success.main' : 'grey.500' }}>
                              {isActive ? <PlayArrow /> : <Stop />}
                            </Avatar>
                            <Box>
                              <Typography variant="subtitle2" fontWeight="bold">
                                {session.name}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                ID: {session.id}
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {formatDateTime(session.startTime)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {session.endTime ? formatDateTime(session.endTime) : '-'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            icon={isActive ? <CheckCircle /> : <Cancel />}
                            label={isActive ? 'Active' : 'Ended'}
                            color={isActive ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" color="text.secondary">
                            {duration} min{isActive ? ' (ongoing)' : ''}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                            {isActive && (
                              <Tooltip title="End session">
                                <Button
                                  variant="outlined"
                                  color="warning"
                                  size="small"
                                  startIcon={<Stop />}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleEndSession(session.id);
                                  }}
                                >
                                  End
                                </Button>
                              </Tooltip>
                            )}
                            <Tooltip title="Delete session (Development only)">
                              <Button
                                variant="outlined"
                                color="error"
                                size="small"
                                startIcon={<Delete />}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDeleteSession(session.id, session.name);
                                }}
                              >
                                Delete
                              </Button>
                            </Tooltip>
                          </Box>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Recent NFC Scans Section */}
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <NearMe color="primary" />
            Recent NFC Scans
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Box sx={{ maxHeight: 300, overflow: 'auto', border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
            <TableContainer>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Subscriber</TableCell>
                    <TableCell>Session</TableCell>
                    <TableCell>Scan Time</TableCell>
                    <TableCell>Type</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recentScans.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Box sx={{ textAlign: 'center', py: 4 }}>
                          <NearMe sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                          <Typography variant="h6" color="text.secondary" gutterBottom>
                            No Recent NFC Scans
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            NFC scan events will appear here when subscribers check in/out
                          </Typography>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ) : (
                    recentScans.map((scan) => (
                    <TableRow key={scan.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Avatar sx={{ width: 24, height: 24, bgcolor: 'primary.main' }}>
                            <Person sx={{ fontSize: 16 }} />
                          </Avatar>
                          {scan.subscriber}
                        </Box>
                      </TableCell>
                      <TableCell>{scan.session}</TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {new Date(scan.time).toLocaleTimeString()}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={scan.type}
                          color={scan.type === 'Check-in' ? 'success' : 'warning'}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                  ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>


          </Box>
        </CardContent>
      </Card>

      {/* Confirmation Dialog */}
      {confirmationData && (
        <ConfirmationDialog
          open={confirmationOpen}
          title={confirmationData.title}
          message={confirmationData.message}
          onConfirm={confirmationData.onConfirm}
          onCancel={() => setConfirmationOpen(false)}
          confirmText={confirmationData.severity === 'error' ? 'Delete' : 'Confirm'}
          severity={confirmationData.severity}
        />
      )}
    </Box>
  );
};

export default SessionPage;
