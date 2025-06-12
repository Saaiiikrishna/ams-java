import React, { useState, useEffect, FormEvent } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Alert,
  Chip,
  Avatar,
  TextField,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Assessment,
  Person,
  Schedule,
  CheckCircle,
  Cancel,
  TrendingUp,
  BarChart,
  FileDownload,
  PictureAsPdf,

} from '@mui/icons-material';
import ApiService from '../services/ApiService';

// DTOs (simplified for frontend representation)
interface SubscriberBasic {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

interface SessionBasic {
  id: number;
  name: string;
  startTime: string;
}

interface AttendanceLogFull {
  id: number;
  subscriberId: number;
  subscriberFirstName: string;
  subscriberLastName: string;
  subscriberEmail: string;
  sessionId: number;
  sessionName: string;
  checkInTime: string;
  checkOutTime?: string | null;
}

const ReportPage: React.FC = () => {
  const [sessions, setSessions] = useState<SessionBasic[]>([]);
  const [subscribers, setSubscribers] = useState<SubscriberBasic[]>([]);
  const [isLoadingData, setIsLoadingData] = useState(true);

  const [selectedSessionIdForAbsentees, setSelectedSessionIdForAbsentees] = useState<string>('');
  const [absentees, setAbsentees] = useState<SubscriberBasic[]>([]);
  const [isLoadingAbsentees, setIsLoadingAbsentees] = useState(false);
  const [absenteesError, setAbsenteesError] = useState<string|null>(null);

  const [selectedSubscriberIdForHistory, setSelectedSubscriberIdForHistory] = useState<string>('');
  const [attendanceHistory, setAttendanceHistory] = useState<AttendanceLogFull[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState<string|null>(null);
  const [startDate, setStartDate] = useState<string>(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]); // Default to 30 days ago
  const [endDate, setEndDate] = useState<string>(new Date().toISOString().split('T')[0]); // Default to today

  // PDF generation states
  const [generatingSessionPdf, setGeneratingSessionPdf] = useState(false);
  const [generatingSubscriberPdf, setGeneratingSubscriberPdf] = useState(false);

  // Fetch initial data for dropdowns
  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      setIsLoadingData(true);
      const [sessionsResponse, subscribersResponse] = await Promise.all([
        ApiService.get<SessionBasic[]>('/api/sessions'),
        ApiService.get<SubscriberBasic[]>('/api/subscribers')
      ]);

      setSessions(sessionsResponse.data || []);
      setSubscribers(subscribersResponse.data || []);
    } catch (error) {
      console.error("Error fetching initial data for report page:", error);
    } finally {
      setIsLoadingData(false);
    }
  };

  const handleFetchAbsentees = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedSessionIdForAbsentees) {
      setAbsenteesError("Please select a session.");
      return;
    }
    setIsLoadingAbsentees(true);
    setAbsenteesError(null);
    setAbsentees([]);
    try {
      const response = await ApiService.get<SubscriberBasic[]>(`/api/reports/sessions/${selectedSessionIdForAbsentees}/absentees`);
      setAbsentees(response.data || []);
    } catch (err:any) {
      console.error("Failed to fetch absentees:", err);
      setAbsenteesError(err.response?.data?.message || "Failed to fetch absentees.");
    } finally {
      setIsLoadingAbsentees(false);
    }
  };

  const handleFetchHistory = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedSubscriberIdForHistory) {
      setHistoryError("Please select a subscriber.");
      return;
    }
    setIsLoadingHistory(true);
    setHistoryError(null);
    setAttendanceHistory([]);
    try {
      const response = await ApiService.get<AttendanceLogFull[]>(`/api/reports/subscribers/${selectedSubscriberIdForHistory}/attendance`, {
        params: { startDate, endDate }
      });
      setAttendanceHistory(response.data || []);
    } catch (err:any) {
      console.error("Failed to fetch attendance history:", err);
      setHistoryError(err.response?.data?.message || "Failed to fetch attendance history.");
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const formatDateTime = (isoString?: string | null) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  };

  const exportToCSV = (data: any[], filename: string, headers: string[]) => {
    const csvContent = [
      headers.join(','),
      ...data.map(row => headers.map(header => {
        const value = row[header.toLowerCase().replace(/\s+/g, '')];
        return `"${value || ''}"`;
      }).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `${filename}_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportAbsentees = () => {
    const selectedSession = sessions.find(s => s.id.toString() === selectedSessionIdForAbsentees);
    const data = absentees.map(sub => ({
      firstname: sub.firstName,
      lastname: sub.lastName,
      email: sub.email,
      session: selectedSession?.name || 'Unknown'
    }));
    exportToCSV(data, 'absentees_report', ['First Name', 'Last Name', 'Email', 'Session']);
  };

  const exportHistory = () => {
    const selectedSubscriber = subscribers.find(s => s.id.toString() === selectedSubscriberIdForHistory);
    const data = attendanceHistory.map(log => ({
      subscriber: `${selectedSubscriber?.firstName} ${selectedSubscriber?.lastName}`,
      session: log.sessionName,
      checkin: formatDateTime(log.checkInTime),
      checkout: formatDateTime(log.checkOutTime),
      duration: log.checkOutTime ?
        Math.round((new Date(log.checkOutTime).getTime() - new Date(log.checkInTime).getTime()) / (1000 * 60)) + ' minutes' :
        'Ongoing'
    }));
    exportToCSV(data, 'attendance_history', ['Subscriber', 'Session', 'Check In', 'Check Out', 'Duration']);
  };

  const generateSessionPdf = async () => {
    if (!selectedSessionIdForAbsentees) return;

    try {
      setGeneratingSessionPdf(true);
      const response = await ApiService.get(`/api/reports/sessions/${selectedSessionIdForAbsentees}/attendance-pdf`, {
        responseType: 'blob',
      });

      // Create blob link to download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `session_${selectedSessionIdForAbsentees}_attendance_report.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Failed to generate session PDF:', err);
      setAbsenteesError('Failed to generate PDF report');
    } finally {
      setGeneratingSessionPdf(false);
    }
  };

  const generateSubscriberPdf = async () => {
    if (!selectedSubscriberIdForHistory || !startDate || !endDate) return;

    try {
      setGeneratingSubscriberPdf(true);
      const startDateTime = new Date(startDate).toISOString();
      const endDateTime = new Date(endDate).toISOString();

      const response = await ApiService.get(
        `/api/reports/subscribers/${selectedSubscriberIdForHistory}/activity-pdf?startDate=${startDateTime}&endDate=${endDateTime}`,
        {
          responseType: 'blob',
        }
      );

      // Create blob link to download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `subscriber_${selectedSubscriberIdForHistory}_activity_report.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Failed to generate subscriber PDF:', err);
      setHistoryError('Failed to generate PDF report');
    } finally {
      setGeneratingSubscriberPdf(false);
    }
  };

  if (isLoadingData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" gutterBottom>
          Reports & Analytics
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Generate detailed attendance reports and analyze patterns
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Absentees Report */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Avatar sx={{ bgcolor: 'error.main' }}>
                  <Cancel />
                </Avatar>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Absentees Report
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Find who missed specific sessions
                  </Typography>
                </Box>
              </Box>

              <form onSubmit={handleFetchAbsentees}>
                <FormControl fullWidth sx={{ mb: 3 }}>
                  <InputLabel>Select Session</InputLabel>
                  <Select
                    value={selectedSessionIdForAbsentees}
                    label="Select Session"
                    onChange={(e) => setSelectedSessionIdForAbsentees(e.target.value)}
                    required
                  >
                    {sessions.map((session) => (
                      <MenuItem key={session.id} value={session.id}>
                        {session.name} - {formatDateTime(session.startTime)}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={isLoadingAbsentees || !selectedSessionIdForAbsentees}
                    startIcon={isLoadingAbsentees ? <CircularProgress size={20} /> : <Assessment />}
                    sx={{ flex: 1 }}
                  >
                    {isLoadingAbsentees ? 'Loading...' : 'Generate Report'}
                  </Button>
                  {selectedSessionIdForAbsentees && (
                    <>
                      <Tooltip title="Generate PDF Report">
                        <IconButton
                          onClick={generateSessionPdf}
                          color="primary"
                          disabled={generatingSessionPdf}
                        >
                          {generatingSessionPdf ? <CircularProgress size={20} /> : <PictureAsPdf />}
                        </IconButton>
                      </Tooltip>
                      {absentees.length > 0 && (
                        <Tooltip title="Export to CSV">
                          <IconButton onClick={exportAbsentees} color="primary">
                            <FileDownload />
                          </IconButton>
                        </Tooltip>
                      )}
                    </>
                  )}
                </Box>
              </form>

              {absenteesError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {absenteesError}
                </Alert>
              )}

              {absentees.length > 0 ? (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Name</TableCell>
                        <TableCell>Email</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {absentees.map((subscriber) => (
                        <TableRow key={subscriber.id}>
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Avatar sx={{ width: 32, height: 32 }}>
                                <Person />
                              </Avatar>
                              {subscriber.firstName} {subscriber.lastName}
                            </Box>
                          </TableCell>
                          <TableCell>{subscriber.email}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : selectedSessionIdForAbsentees && !isLoadingAbsentees ? (
                <Alert severity="info">
                  No absentees found for the selected session. Everyone attended!
                </Alert>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                  Select a session to view absentees
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Attendance History */}
        <Grid item xs={12} lg={6}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Avatar sx={{ bgcolor: 'success.main' }}>
                  <BarChart />
                </Avatar>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Attendance History
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    View individual attendance records
                  </Typography>
                </Box>
              </Box>

              <form onSubmit={handleFetchHistory}>
                <FormControl fullWidth sx={{ mb: 2 }}>
                  <InputLabel>Select Subscriber</InputLabel>
                  <Select
                    value={selectedSubscriberIdForHistory}
                    label="Select Subscriber"
                    onChange={(e) => setSelectedSubscriberIdForHistory(e.target.value)}
                    required
                  >
                    {subscribers.map((subscriber) => (
                      <MenuItem key={subscriber.id} value={subscriber.id}>
                        {subscriber.firstName} {subscriber.lastName} - {subscriber.email}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <Grid container spacing={2} sx={{ mb: 3 }}>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      type="date"
                      label="Start Date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      InputLabelProps={{ shrink: true }}
                      required
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      type="date"
                      label="End Date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      InputLabelProps={{ shrink: true }}
                      required
                    />
                  </Grid>
                </Grid>

                <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={isLoadingHistory || !selectedSubscriberIdForHistory}
                    startIcon={isLoadingHistory ? <CircularProgress size={20} /> : <TrendingUp />}
                    sx={{ flex: 1 }}
                  >
                    {isLoadingHistory ? 'Loading...' : 'Get History'}
                  </Button>
                  {selectedSubscriberIdForHistory && (
                    <>
                      <Tooltip title="Generate PDF Report">
                        <IconButton
                          onClick={generateSubscriberPdf}
                          color="primary"
                          disabled={generatingSubscriberPdf}
                        >
                          {generatingSubscriberPdf ? <CircularProgress size={20} /> : <PictureAsPdf />}
                        </IconButton>
                      </Tooltip>
                      {attendanceHistory.length > 0 && (
                        <Tooltip title="Export to CSV">
                          <IconButton onClick={exportHistory} color="primary">
                            <FileDownload />
                          </IconButton>
                        </Tooltip>
                      )}
                    </>
                  )}
                </Box>
              </form>

              {historyError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {historyError}
                </Alert>
              )}

              {attendanceHistory.length > 0 ? (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Session</TableCell>
                        <TableCell>Check In</TableCell>
                        <TableCell>Check Out</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {attendanceHistory.map((log) => (
                        <TableRow key={log.id}>
                          <TableCell>
                            <Typography variant="body2" fontWeight="medium">
                              {log.sessionName}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2">
                              {formatDateTime(log.checkInTime)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2">
                              {formatDateTime(log.checkOutTime)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              icon={log.checkOutTime ? <CheckCircle /> : <Schedule />}
                              label={log.checkOutTime ? 'Completed' : 'Active'}
                              color={log.checkOutTime ? 'success' : 'warning'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : selectedSubscriberIdForHistory && !isLoadingHistory ? (
                <Alert severity="info">
                  No attendance history found for the selected criteria
                </Alert>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                  Select a subscriber and date range to view history
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportPage;
