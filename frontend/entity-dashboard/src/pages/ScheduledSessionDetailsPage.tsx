import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Alert,
  CircularProgress,
  Avatar,
  Chip,
  Tabs,
  Tab,
  Grid,
  Divider,
} from '@mui/material';
import {
  ArrowBack,
  Schedule,
  AccessTime,
  Today,

  QrCode,
  Refresh,

} from '@mui/icons-material';
import ApiService from '../services/ApiService';


interface ScheduledSessionDetails {
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

interface DaySession {
  dayOfWeek: string;
  sessionId?: number;
  sessionName: string;
  startTime: string;
  endTime: string;
  status: 'active' | 'completed' | 'scheduled';
  attendeeCount: number;
}

const CHECK_IN_METHODS = [
  { value: 'NFC', label: 'NFC Card' },
  { value: 'QR', label: 'QR Code' },
  { value: 'BLUETOOTH', label: 'Bluetooth' },
  { value: 'WIFI', label: 'WiFi' },
  { value: 'MOBILE_NFC', label: 'Mobile NFC' },
];



const ScheduledSessionDetailsPage: React.FC = () => {
  const { id: scheduledSessionId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [scheduledSession, setScheduledSession] = useState<ScheduledSessionDetails | null>(null);
  const [daySessions, setDaySessions] = useState<DaySession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTab, setSelectedTab] = useState(0);



  const fetchScheduledSessionDetails = useCallback(async (id: number) => {
    try {
      setLoading(true);
      setError(null);

      // Fetch scheduled session details
      const scheduledResponse = await ApiService.get(`/api/scheduled-sessions/${id}`);
      const scheduledData = scheduledResponse.data;
      setScheduledSession(scheduledData);

      // Generate day sessions based on scheduled session
      const daySessionsData: DaySession[] = scheduledData.daysOfWeek.map((day: string) => ({
        dayOfWeek: day,
        sessionName: `${scheduledData.name} - ${day}`,
        startTime: scheduledData.startTime,
        endTime: calculateEndTime(scheduledData.startTime, scheduledData.durationMinutes),
        status: 'scheduled' as const,
        attendeeCount: 0,
      }));

      setDaySessions(daySessionsData);
    } catch (err: any) {
      console.error('Failed to fetch scheduled session details:', err);
      setError('Failed to load scheduled session details. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (scheduledSessionId) {
      fetchScheduledSessionDetails(parseInt(scheduledSessionId));
    }
  }, [scheduledSessionId, fetchScheduledSessionDetails]);

  const calculateEndTime = (startTime: string, durationMinutes: number) => {
    const [hours, minutes] = startTime.split(':').map(Number);
    const startDate = new Date();
    startDate.setHours(hours, minutes, 0, 0);
    const endDate = new Date(startDate.getTime() + durationMinutes * 60000);
    return endDate.toTimeString().slice(0, 5);
  };

  const formatTime = (timeString: string) => {
    return new Date(`2000-01-01T${timeString}`).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const formatDayName = (day: string) => {
    return day.charAt(0) + day.slice(1).toLowerCase();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error || !scheduledSession) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          {error || 'Scheduled session not found'}
        </Alert>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/dashboard/scheduled-sessions')}
          variant="outlined"
        >
          Back to Scheduled Sessions
        </Button>
      </Box>
    );
  }

  const selectedDaySession = daySessions[selectedTab];

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate('/dashboard/scheduled-sessions')}
          variant="outlined"
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" component="h1" fontWeight="bold">
          Scheduled Session Details
        </Typography>
        <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => fetchScheduledSessionDetails(parseInt(scheduledSessionId!))}
            disabled={loading}
            color="primary"
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Scheduled Session Info Card */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Avatar
              sx={{
                bgcolor: scheduledSession.active ? 'success.main' : 'grey.500',
                width: 56,
                height: 56,
                mr: 2,
              }}
            >
              <Schedule />
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h5" fontWeight="bold">
                {scheduledSession.name}
              </Typography>
              {scheduledSession.description && (
                <Typography variant="body1" color="textSecondary" sx={{ mt: 0.5 }}>
                  {scheduledSession.description}
                </Typography>
              )}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Chip
                  icon={<Schedule />}
                  label={scheduledSession.active ? 'Active Schedule' : 'Inactive Schedule'}
                  color={scheduledSession.active ? 'success' : 'default'}
                  variant="outlined"
                />
              </Box>
            </Box>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Grid container spacing={3}>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccessTime color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Start Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {formatTime(scheduledSession.startTime)}
                  </Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Today color="primary" />
                <Box>
                  <Typography variant="body2" color="textSecondary">
                    Duration
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {scheduledSession.durationMinutes} minutes
                  </Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                  Days of Week
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {scheduledSession.daysOfWeek.map((day) => (
                    <Chip
                      key={day}
                      label={formatDayName(day)}
                      size="small"
                      variant="outlined"
                      color="primary"
                    />
                  ))}
                </Box>
              </Box>
            </Grid>
          </Grid>

          {/* Check-in Methods */}
          {scheduledSession.allowedCheckInMethods && scheduledSession.allowedCheckInMethods.length > 0 && (
            <>
              <Divider sx={{ my: 2 }} />
              <Box>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                  Allowed Check-in Methods
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {scheduledSession.allowedCheckInMethods.map((method) => (
                    <Chip
                      key={method}
                      label={CHECK_IN_METHODS.find(m => m.value === method)?.label || method}
                      size="small"
                      variant="outlined"
                      color="secondary"
                    />
                  ))}
                </Box>
              </Box>
            </>
          )}
        </CardContent>
      </Card>

      {/* Day Tabs */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Daily Sessions
          </Typography>
          <Tabs
            value={selectedTab}
            onChange={(_, newValue) => setSelectedTab(newValue)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
          >
            {daySessions.map((daySession, index) => (
              <Tab
                key={daySession.dayOfWeek}
                label={formatDayName(daySession.dayOfWeek)}
                id={`day-tab-${index}`}
                aria-controls={`day-tabpanel-${index}`}
              />
            ))}
          </Tabs>

          {/* Selected Day Content */}
          {selectedDaySession && (
            <Box>
              <Typography variant="h6" gutterBottom>
                {selectedDaySession.sessionName}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                {formatTime(selectedDaySession.startTime)} - {formatTime(selectedDaySession.endTime)}
              </Typography>

              {/* QR Code for this day */}
              {scheduledSession.active && (
                <Card variant="outlined" sx={{ mt: 2 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <QrCode color="primary" />
                      QR Code for {formatDayName(selectedDaySession.dayOfWeek)}
                    </Typography>
                    <Typography variant="body2" color="textSecondary" gutterBottom>
                      Subscribers can scan this QR code to check in for this day's session
                    </Typography>
                    {/* Generate a unique QR code for this day */}
                    <Box sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 2,
                      p: 2,
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 2,
                      bgcolor: 'background.paper'
                    }}>
                      <Box sx={{
                        width: 200,
                        height: 200,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: '2px dashed',
                        borderColor: 'primary.main',
                        borderRadius: 1,
                        bgcolor: 'grey.50'
                      }}>
                        <Typography variant="body2" color="textSecondary" textAlign="center">
                          QR Code for<br />
                          {formatDayName(selectedDaySession.dayOfWeek)}<br />
                          <small>Unique per day/session</small>
                        </Typography>
                      </Box>
                      <Typography variant="caption" color="textSecondary" textAlign="center">
                        Session ID: {scheduledSession.id}-{selectedDaySession.dayOfWeek}
                        <br />
                        Time: {formatTime(selectedDaySession.startTime)} - {formatTime(selectedDaySession.endTime)}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default ScheduledSessionDetailsPage;
