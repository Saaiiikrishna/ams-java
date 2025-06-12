import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,

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
  QrCode,
  Download,
  Refresh,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import QrCodeDisplay from '../components/QrCodeDisplay';

interface SessionDetails {
  id: number;
  name: string;
  description?: string;
  startTime: string;
  endTime?: string;
  status: 'active' | 'completed';
  allowedCheckInMethods?: string[];
  attendees: Attendee[];
}

interface Attendee {
  id: number;
  subscriberId: number;
  subscriberName: string;
  checkInTime: string;
  checkOutTime?: string;
  checkInMethod?: string;
  status: 'checked_in' | 'checked_out';
}

const CHECK_IN_METHODS = [
  { value: 'NFC', label: 'NFC Card' },
  { value: 'QR', label: 'QR Code' },
  { value: 'BLUETOOTH', label: 'Bluetooth' },
  { value: 'WIFI', label: 'WiFi' },
  { value: 'MOBILE_NFC', label: 'Mobile NFC' },
];

const SessionDetailsPage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [session, setSession] = useState<SessionDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloadingReport, setDownloadingReport] = useState(false);

  useEffect(() => {
    if (sessionId) {
      fetchSessionDetails(parseInt(sessionId));
    }
  }, [sessionId]);

  // Remove automatic polling - only update when user manually refreshes
  // This prevents constant database queries and QR code refreshing

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
        description: sessionData.description,
        startTime: sessionData.startTime,
        endTime: sessionData.endTime,
        status: sessionData.endTime ? 'completed' : 'active',
        allowedCheckInMethods: sessionData.allowedCheckInMethods || [],
        attendees: attendanceData.map((log: any) => ({
          id: log.id,
          subscriberId: log.subscriber?.id || 0,
          subscriberName: `${log.subscriber?.firstName || 'Unknown'} ${log.subscriber?.lastName || 'User'}`,
          checkInTime: log.checkInTime,
          checkOutTime: log.checkOutTime,
          checkInMethod: log.checkinMethod,
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

  const downloadReport = async () => {
    try {
      setDownloadingReport(true);
      const response = await ApiService.get(`/api/reports/sessions/${session?.id}/attendance-pdf`, {
        responseType: 'blob',
      });

      // Create blob link to download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `session_${session?.id}_attendance_report.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Failed to download report:', err);
      setError('Failed to download report');
    } finally {
      setDownloadingReport(false);
    }
  };

  // Memoize QR code props to prevent unnecessary re-renders
  const qrCodeProps = useMemo(() => ({
    sessionId: session?.id || 0,
    sessionName: session?.name || '',
  }), [session?.id, session?.name]);

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
          onClick={() => navigate('/dashboard/sessions')}
          variant="outlined"
        >
          Back to Sessions
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
          onClick={() => navigate('/dashboard/sessions')}
          variant="outlined"
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" component="h1" fontWeight="bold">
          Session Details
        </Typography>
        <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<Download />}
            onClick={downloadReport}
            disabled={downloadingReport}
            color="primary"
          >
            {downloadingReport ? 'Generating...' : 'Download Report'}
          </Button>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => fetchSessionDetails(parseInt(sessionId!))}
            disabled={loading}
            color="primary"
          >
            {loading ? 'Refreshing...' : 'Refresh Data'}
          </Button>
        </Box>
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

          {/* Check-in Methods */}
          {session.allowedCheckInMethods && session.allowedCheckInMethods.length > 0 && (
            <>
              <Divider sx={{ my: 2 }} />
              <Box>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                  Allowed Check-in Methods
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {session.allowedCheckInMethods.map((method) => (
                    <Chip
                      key={method}
                      label={CHECK_IN_METHODS.find(m => m.value === method)?.label || method}
                      size="small"
                      variant="outlined"
                      color="primary"
                    />
                  ))}
                </Box>
              </Box>
            </>
          )}
        </CardContent>
      </Card>

      {/* QR Code Display */}
      {session.status === 'active' && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <QrCode color="primary" />
              QR Code for Check-in
            </Typography>
            <QrCodeDisplay
              sessionId={qrCodeProps.sessionId}
              sessionName={qrCodeProps.sessionName}
              onClose={() => {}} // No close button needed since it's always visible
            />
          </CardContent>
        </Card>
      )}

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
                    <TableCell>Method</TableCell>
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
                          label={CHECK_IN_METHODS.find(m => m.value === attendee.checkInMethod)?.label || attendee.checkInMethod || 'Unknown'}
                          size="small"
                          variant="outlined"
                          color="secondary"
                        />
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
