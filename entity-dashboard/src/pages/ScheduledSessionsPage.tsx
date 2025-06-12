import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,

  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,

  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,

  Alert,
  CircularProgress,
  FormControlLabel,
  Checkbox,
  FormGroup,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Schedule,
  PlayArrow,
  Stop,
  AccessTime,
} from '@mui/icons-material';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import ApiService from '../services/ApiService';

interface ScheduledSession {
  id: number;
  name: string;
  description: string;
  startTime: string;
  durationMinutes: number;
  daysOfWeek: string[];
  allowedCheckInMethods: string[];
  active: boolean;
  organizationEntityId: string;
}

interface NewScheduledSession {
  name: string;
  description: string;
  startTime: Date | null;
  durationMinutes: number;
  daysOfWeek: string[];
  allowedCheckInMethods: string[];
}

const DAYS_OF_WEEK = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

const CHECK_IN_METHODS = [
  { value: 'NFC', label: 'NFC Card' },
  { value: 'QR', label: 'QR Code' },
  { value: 'BLUETOOTH', label: 'Bluetooth' },
  { value: 'WIFI', label: 'WiFi' },
  { value: 'MOBILE_NFC', label: 'Mobile NFC' },
];

const ScheduledSessionsPage: React.FC = () => {
  const navigate = useNavigate();
  const [scheduledSessions, setScheduledSessions] = useState<ScheduledSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Dialog states
  const [openDialog, setOpenDialog] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [sessionToDelete, setSessionToDelete] = useState<number | null>(null);
  const [editingSession, setEditingSession] = useState<ScheduledSession | null>(null);
  const [formData, setFormData] = useState<NewScheduledSession>({
    name: '',
    description: '',
    startTime: null,
    durationMinutes: 60,
    daysOfWeek: [],
    allowedCheckInMethods: ['NFC'],
  });

  useEffect(() => {
    fetchScheduledSessions();
  }, []);

  const fetchScheduledSessions = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await ApiService.get<ScheduledSession[]>('/api/scheduled-sessions');
      setScheduledSessions(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch scheduled sessions:', err);
      setError('Failed to fetch scheduled sessions');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSession = () => {
    setEditingSession(null);
    resetForm();
    setOpenDialog(true);
  };

  const handleEditSession = (session: ScheduledSession) => {
    setEditingSession(session);
    setFormData({
      name: session.name,
      description: session.description,
      startTime: new Date(`2000-01-01T${session.startTime}`),
      durationMinutes: session.durationMinutes,
      daysOfWeek: session.daysOfWeek,
      allowedCheckInMethods: session.allowedCheckInMethods,
    });
    setOpenDialog(true);
  };

  const handleSaveSession = async () => {
    try {
      if (!formData.name || !formData.startTime || formData.daysOfWeek.length === 0) {
        setError('Please fill in all required fields');
        return;
      }

      const sessionData = {
        ...formData,
        startTime: formData.startTime.toTimeString().slice(0, 5), // HH:mm format
        active: editingSession ? editingSession.active : true, // Preserve active status for updates, default to true for new sessions
      };

      if (editingSession) {
        await ApiService.put(`/api/scheduled-sessions/${editingSession.id}`, sessionData);
        setSuccess('Scheduled session updated successfully');
      } else {
        await ApiService.post('/api/scheduled-sessions', sessionData);
        setSuccess('Scheduled session created successfully');
      }

      setOpenDialog(false);
      setEditingSession(null);
      resetForm();
      await fetchScheduledSessions();
    } catch (err: any) {
      console.error('Failed to save scheduled session:', err);
      setError(err.response?.data?.error || 'Failed to save scheduled session');
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      startTime: null,
      durationMinutes: 60,
      daysOfWeek: [],
      allowedCheckInMethods: ['NFC'],
    });
  };

  const handleDeleteSession = async (sessionId: number) => {
    setSessionToDelete(sessionId);
    setDeleteDialogOpen(true);
  };

  const confirmDeleteSession = async () => {
    if (!sessionToDelete) return;

    try {
      await ApiService.delete(`/api/scheduled-sessions/${sessionToDelete}`);
      setSuccess('Scheduled session deleted successfully');
      setDeleteDialogOpen(false);
      setSessionToDelete(null);
      // Force refresh the list
      await fetchScheduledSessions();
    } catch (err: any) {
      console.error('Failed to delete scheduled session:', err);
      setError('Failed to delete scheduled session');
      setDeleteDialogOpen(false);
      setSessionToDelete(null);
    }
  };

  const handleToggleActive = async (session: ScheduledSession) => {
    try {
      const updatedSession = { ...session, active: !session.active };
      await ApiService.put(`/api/scheduled-sessions/${session.id}`, updatedSession);
      setSuccess(`Scheduled session ${updatedSession.active ? 'activated' : 'deactivated'}`);
      fetchScheduledSessions();
    } catch (err: any) {
      console.error('Failed to toggle session status:', err);
      setError('Failed to update session status');
    }
  };

  const formatTime = (timeString: string) => {
    return new Date(`2000-01-01T${timeString}`).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const formatDays = (days: string[]) => {
    return days.map(day => day.slice(0, 3)).join(', ');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Box>
            <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Schedule color="primary" />
              Scheduled Sessions
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Manage recurring attendance sessions
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={handleCreateSession}
            sx={{ borderRadius: 2 }}
          >
            Create Scheduled Session
          </Button>
        </Box>

        {/* Alerts */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
            {success}
          </Alert>
        )}

        {/* Sessions Table */}
        <Paper sx={{ borderRadius: 2 }}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><strong>Name</strong></TableCell>
                  <TableCell><strong>Time</strong></TableCell>
                  <TableCell><strong>Duration</strong></TableCell>
                  <TableCell><strong>Days</strong></TableCell>
                  <TableCell><strong>Methods</strong></TableCell>
                  <TableCell><strong>Status</strong></TableCell>
                  <TableCell><strong>Actions</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {scheduledSessions.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                      <Typography variant="body1" color="text.secondary">
                        No scheduled sessions found. Create your first scheduled session to get started.
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  scheduledSessions.map((session) => (
                    <TableRow key={session.id}>
                      <TableCell>
                        <Box>
                          <Typography
                            variant="body1"
                            fontWeight="medium"
                            sx={{
                              cursor: 'pointer',
                              color: 'primary.main',
                              '&:hover': { textDecoration: 'underline' }
                            }}
                            onClick={() => navigate(`/dashboard/scheduled-sessions/${session.id}`)}
                          >
                            {session.name}
                          </Typography>
                          {session.description && (
                            <Typography variant="body2" color="text.secondary">
                              {session.description}
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <AccessTime fontSize="small" color="action" />
                          {formatTime(session.startTime)}
                        </Box>
                      </TableCell>
                      <TableCell>{session.durationMinutes} min</TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatDays(session.daysOfWeek)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {session.allowedCheckInMethods.map((method) => (
                            <Chip
                              key={method}
                              label={CHECK_IN_METHODS.find(m => m.value === method)?.label || method}
                              size="small"
                              variant="outlined"
                            />
                          ))}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={session.active ? 'Active' : 'Inactive'}
                          color={session.active ? 'success' : 'default'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                          <Button
                            size="small"
                            variant="outlined"
                            onClick={() => handleToggleActive(session)}
                            color={session.active ? 'warning' : 'success'}
                            startIcon={session.active ? <Stop /> : <PlayArrow />}
                          >
                            {session.active ? 'Deactivate' : 'Activate'}
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            onClick={() => handleEditSession(session)}
                            color="primary"
                            startIcon={<Edit />}
                          >
                            Edit
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            onClick={() => handleDeleteSession(session.id)}
                            color="error"
                            startIcon={<Delete />}
                          >
                            Delete
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>

        {/* Create/Edit Dialog */}
        <Dialog open={openDialog} onClose={() => {
          setOpenDialog(false);
          setEditingSession(null);
          resetForm();
        }} maxWidth="md" fullWidth>
          <DialogTitle>
            {editingSession ? 'Edit Scheduled Session' : 'Create Scheduled Session'}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, pt: 1 }}>
              <TextField
                label="Session Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                fullWidth
                required
              />
              
              <TextField
                label="Description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                fullWidth
                multiline
                rows={2}
              />

              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <TimePicker
                    label="Start Time"
                    value={formData.startTime}
                    onChange={(newValue) => setFormData({ ...formData, startTime: newValue })}
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        required: true
                      }
                    }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    label="Duration (minutes)"
                    type="number"
                    value={formData.durationMinutes}
                    onChange={(e) => setFormData({ ...formData, durationMinutes: parseInt(e.target.value) })}
                    fullWidth
                    required
                  />
                </Grid>
              </Grid>

              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  Days of Week *
                </Typography>
                <FormGroup row>
                  {DAYS_OF_WEEK.map((day) => (
                    <FormControlLabel
                      key={day}
                      control={
                        <Checkbox
                          checked={formData.daysOfWeek.includes(day)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setFormData({
                                ...formData,
                                daysOfWeek: [...formData.daysOfWeek, day]
                              });
                            } else {
                              setFormData({
                                ...formData,
                                daysOfWeek: formData.daysOfWeek.filter(d => d !== day)
                              });
                            }
                          }}
                        />
                      }
                      label={day.slice(0, 3)}
                    />
                  ))}
                </FormGroup>
              </Box>

              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  Allowed Check-in Methods *
                </Typography>
                <FormGroup row>
                  {CHECK_IN_METHODS.map((method) => (
                    <FormControlLabel
                      key={method.value}
                      control={
                        <Checkbox
                          checked={formData.allowedCheckInMethods.includes(method.value)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setFormData({
                                ...formData,
                                allowedCheckInMethods: [...formData.allowedCheckInMethods, method.value]
                              });
                            } else {
                              setFormData({
                                ...formData,
                                allowedCheckInMethods: formData.allowedCheckInMethods.filter(m => m !== method.value)
                              });
                            }
                          }}
                        />
                      }
                      label={method.label}
                    />
                  ))}
                </FormGroup>
              </Box>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => {
              setOpenDialog(false);
              setEditingSession(null);
              resetForm();
            }}>Cancel</Button>
            <Button onClick={handleSaveSession} variant="contained">
              {editingSession ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
          <DialogTitle>Confirm Delete</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to delete this scheduled session? This action cannot be undone.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
            <Button onClick={confirmDeleteSession} color="error" variant="contained">
              Delete
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </LocalizationProvider>
  );
};

export default ScheduledSessionsPage;
