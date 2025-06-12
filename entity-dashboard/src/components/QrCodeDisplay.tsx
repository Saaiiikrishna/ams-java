import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
  Grid,
  Card,
  CardContent,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  QrCode,
  Refresh,
  Download,
  Share,
  Timer,
  Fullscreen,
  Close,
} from '@mui/icons-material';
import QRCode from 'qrcode';
import ApiService from '../services/ApiService';

interface QrCodeData {
  qrCode: string;
  sessionId: number;
  sessionName: string;
  expiryTime: string;
  deepLinkUrl: string;
}

interface QrCodeDisplayProps {
  sessionId: number;
  sessionName: string;
  onClose?: () => void;
}

const QrCodeDisplay: React.FC<QrCodeDisplayProps> = ({ sessionId, sessionName, onClose }) => {
  const [qrData, setQrData] = useState<QrCodeData | null>(null);
  const [qrImageUrl, setQrImageUrl] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<string>('');
  const [fullscreenOpen, setFullscreenOpen] = useState(false);

  // Use ref to track if QR code has been fetched for this session
  const fetchedSessionId = useRef<number | null>(null);

  const fetchQrCode = useCallback(async () => {
    // Prevent refetching if we already have QR code for this session
    if (fetchedSessionId.current === sessionId && qrData && qrImageUrl) {
      console.log(`QR Code: Skipping fetch for session ${sessionId} - already have data`);
      return;
    }

    console.log(`QR Code: Fetching QR code for session ${sessionId}`);
    try {
      setLoading(true);
      setError(null);

      const response = await ApiService.get<QrCodeData>(`/api/sessions/${sessionId}/qr-code`);
      const data = response.data;

      setQrData(data);
      fetchedSessionId.current = sessionId;
      console.log(`QR Code: Successfully fetched and cached for session ${sessionId}`);

      // Generate QR code image
      const qrImageDataUrl = await QRCode.toDataURL(data.deepLinkUrl, {
        width: 300,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF',
        },
      });

      setQrImageUrl(qrImageDataUrl);

    } catch (err: any) {
      console.error('Failed to fetch QR code:', err);
      setError('Failed to generate QR code');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => {
    // Only fetch if we don't already have QR code for this session
    if (fetchedSessionId.current !== sessionId) {
      fetchQrCode();
    }
  }, [sessionId, fetchQrCode]);

  const updateTimeRemaining = useCallback(() => {
    if (!qrData?.expiryTime) {
      setTimeRemaining('Valid until session ends');
      return;
    }

    const now = new Date();
    const expiry = new Date(qrData.expiryTime);
    const diff = expiry.getTime() - now.getTime();

    if (diff <= 0) {
      setTimeRemaining('Session ended');
      return;
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    if (hours > 0) {
      setTimeRemaining(`${hours}h ${minutes}m ${seconds}s`);
    } else if (minutes > 0) {
      setTimeRemaining(`${minutes}m ${seconds}s`);
    } else {
      setTimeRemaining(`${seconds}s`);
    }
  }, [qrData?.expiryTime]);

  useEffect(() => {
    if (qrData?.expiryTime) {
      // Only update timer if there's an actual expiry time
      const interval = setInterval(updateTimeRemaining, 1000);
      return () => clearInterval(interval);
    } else if (qrData) {
      // If no expiry time, QR is valid until session ends
      updateTimeRemaining();
    }
  }, [qrData?.expiryTime, qrData, updateTimeRemaining]);



  const refreshQrCode = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await ApiService.post<QrCodeData>(`/api/sessions/${sessionId}/refresh-qr`);
      const data = response.data;

      setQrData(data);
      fetchedSessionId.current = sessionId; // Update the ref

      // Generate new QR code image
      const qrImageDataUrl = await QRCode.toDataURL(data.deepLinkUrl, {
        width: 300,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF',
        },
      });

      setQrImageUrl(qrImageDataUrl);

    } catch (err: any) {
      console.error('Failed to refresh QR code:', err);
      setError('Failed to refresh QR code');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);



  const downloadQrCode = () => {
    if (!qrImageUrl) return;
    
    const link = document.createElement('a');
    link.download = `session_${sessionId}_qr_code.png`;
    link.href = qrImageUrl;
    link.click();
  };

  const shareQrCode = async () => {
    if (!qrData?.deepLinkUrl) return;
    
    if (navigator.share) {
      try {
        await navigator.share({
          title: `Check-in QR Code - ${sessionName}`,
          text: 'Scan this QR code to check in to the session',
          url: qrData.deepLinkUrl,
        });
      } catch (err) {
        console.log('Share cancelled');
      }
    } else {
      // Fallback: copy to clipboard
      try {
        await navigator.clipboard.writeText(qrData.deepLinkUrl);
        alert('QR code link copied to clipboard!');
      } catch (err) {
        console.error('Failed to copy to clipboard');
      }
    }
  };

  const isExpired = timeRemaining === 'Session ended';
  const isActive = timeRemaining === 'Valid until session ends';
  const isExpiringSoon = qrData?.expiryTime &&
    new Date(qrData.expiryTime).getTime() - new Date().getTime() < 10 * 60 * 1000; // 10 minutes

  return (
    <Box>
      <Paper sx={{ p: 3, borderRadius: 2 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <QrCode color="primary" />
            <Typography variant="h6" fontWeight="bold">
              QR Code Check-in
            </Typography>
          </Box>
          {onClose && (
            <IconButton onClick={onClose} size="small">
              <Close />
            </IconButton>
          )}
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : qrData ? (
          <Grid container spacing={3}>
            {/* QR Code Display */}
            <Grid item xs={12} md={6}>
              <Card sx={{ textAlign: 'center', p: 2 }}>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    {sessionName}
                  </Typography>
                  
                  <Box sx={{ position: 'relative', display: 'inline-block' }}>
                    <img
                      src={qrImageUrl}
                      alt="QR Code"
                      style={{
                        maxWidth: '100%',
                        height: 'auto',
                        border: '1px solid #e0e0e0',
                        borderRadius: 8,
                        opacity: isExpired ? 0.5 : 1,
                      }}
                    />
                    {isExpired && (
                      <Box
                        sx={{
                          position: 'absolute',
                          top: '50%',
                          left: '50%',
                          transform: 'translate(-50%, -50%)',
                          bgcolor: 'rgba(255, 255, 255, 0.9)',
                          px: 2,
                          py: 1,
                          borderRadius: 1,
                        }}
                      >
                        <Typography variant="body2" color="error" fontWeight="bold">
                          SESSION ENDED
                        </Typography>
                      </Box>
                    )}
                  </Box>

                  <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center', gap: 1 }}>
                    <Tooltip title="View Fullscreen">
                      <IconButton onClick={() => setFullscreenOpen(true)} color="primary">
                        <Fullscreen />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Download QR Code">
                      <IconButton onClick={downloadQrCode} color="primary">
                        <Download />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Share QR Code">
                      <IconButton onClick={shareQrCode} color="primary">
                        <Share />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* QR Code Info */}
            <Grid item xs={12} md={6}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Session
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {sessionName}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Chip
                    label={isExpired ? 'Session Ended' : 'Active'}
                    color={isExpired ? 'error' : isActive ? 'success' : isExpiringSoon ? 'warning' : 'success'}
                    size="small"
                    icon={<Timer />}
                  />
                </Box>

                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Time Remaining
                  </Typography>
                  <Typography 
                    variant="body1" 
                    fontWeight="medium"
                    color={isExpired ? 'error' : isExpiringSoon ? 'warning.main' : 'text.primary'}
                  >
                    {timeRemaining || 'Calculating...'}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Instructions
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    1. Subscribers should open their mobile app
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    2. Tap "Scan QR Code" in the app
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    3. Point camera at this QR code
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    4. Attendance will be recorded automatically
                  </Typography>
                </Box>

                <Box sx={{ mt: 2 }}>
                  <Button
                    variant="outlined"
                    startIcon={<Refresh />}
                    onClick={refreshQrCode}
                    disabled={loading}
                    fullWidth
                  >
                    Refresh QR Code
                  </Button>
                </Box>
              </Box>
            </Grid>
          </Grid>
        ) : null}
      </Paper>

      {/* Fullscreen QR Code Dialog */}
      <Dialog
        open={fullscreenOpen}
        onClose={() => setFullscreenOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ textAlign: 'center' }}>
          QR Code - {sessionName}
        </DialogTitle>
        <DialogContent sx={{ textAlign: 'center', py: 4 }}>
          {qrImageUrl && (
            <img
              src={qrImageUrl}
              alt="QR Code"
              style={{
                maxWidth: '100%',
                height: 'auto',
                border: '1px solid #e0e0e0',
                borderRadius: 8,
              }}
            />
          )}
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            Scan with subscriber mobile app to check in
          </Typography>
          {timeRemaining && (
            <Typography 
              variant="body2" 
              color={isExpired ? 'error' : isExpiringSoon ? 'warning.main' : 'success.main'}
              sx={{ mt: 1 }}
            >
              {isExpired ? 'QR Code Expired' : `Expires in: ${timeRemaining}`}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFullscreenOpen(false)}>Close</Button>
          <Button onClick={downloadQrCode} variant="contained" startIcon={<Download />}>
            Download
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default React.memo(QrCodeDisplay);
