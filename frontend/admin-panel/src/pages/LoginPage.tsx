import React, { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';
import {
  Container,
  TextField,
  Button,
  Typography,
  Box,
  Alert,
  Paper,
  Avatar,
  Grid,
  Card,
  CardContent,
  Fade,
  CircularProgress,
  InputAdornment,
} from '@mui/material';
import {
  AdminPanelSettings,
  Business,
  People,
  Assessment,
  Person,
  Lock,
} from '@mui/icons-material';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setIsLoading(true);

    console.log('🔐 Attempting login with:', { username, password: '***' });

    try {
      const response = await ApiService.post('/api/auth/super/login', {
        username,
        password,
      });

      console.log('✅ Login response:', response);

      // Assuming backend response is { jwt: "accessTokenValue", refreshToken: "refreshTokenValue" }
      // The LoginResponse.java DTO uses "jwt" for accessToken and "refreshToken" for refreshToken.
      if (response.data && response.data.jwt && response.data.refreshToken) {
        AuthService.storeTokens(response.data.jwt, response.data.refreshToken);
        console.log('✅ Tokens stored, navigating to dashboard');
        // Navigate to the dashboard after successful login
        navigate('/dashboard');
      } else {
        console.error('❌ Invalid response structure:', response.data);
        setError('Login failed: Invalid response from server.');
      }
    } catch (err: any) {
      console.error('❌ Login error:', err);

      if (err.response) {
        console.error('❌ Error response:', err.response);
        if (err.response.data && err.response.data.message) {
          setError(`Login failed: ${err.response.data.message}`);
        } else if (err.response.status === 401) {
          setError('Login failed: Invalid username or password.');
        } else {
          setError(`Login failed: Server error (${err.response.status})`);
        }
      } else if (err.request) {
        console.error('❌ Network error:', err.request);
        setError('Login failed: Cannot connect to server. Please check your network connection.');
      } else {
        console.error('❌ Unknown error:', err.message);
        setError(`Login failed: ${err.message}`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #1976d2 0%, #1565c0 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 2,
      }}
    >
      <Container maxWidth="lg">
        <Grid container spacing={4} alignItems="center">
          {/* Left side - Features */}
          <Grid item xs={12} md={6}>
            <Fade in timeout={1000}>
              <Box sx={{ color: 'white', mb: 4 }}>
                <Typography variant="h2" component="h1" gutterBottom fontWeight="bold">
                  Super Admin Portal
                </Typography>
                <Typography variant="h5" component="p" sx={{ mb: 4, opacity: 0.9 }}>
                  Manage your attendance management system with complete control
                </Typography>

                <Grid container spacing={3}>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <Business sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          Manage Entities
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <People sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          Assign Admins
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <Assessment sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          System Reports
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                </Grid>
              </Box>
            </Fade>
          </Grid>

          {/* Right side - Login Form */}
          <Grid item xs={12} md={6}>
            <Fade in timeout={1500}>
              <Paper
                elevation={24}
                sx={{
                  p: 4,
                  borderRadius: 3,
                  backgroundColor: 'rgba(255,255,255,0.95)',
                  backdropFilter: 'blur(10px)',
                  maxWidth: 400,
                  mx: 'auto',
                }}
              >
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
                  <Avatar sx={{ m: 1, bgcolor: 'primary.main', width: 56, height: 56 }}>
                    <AdminPanelSettings sx={{ fontSize: 30 }} />
                  </Avatar>
                  <Typography component="h1" variant="h4" fontWeight="bold" color="primary">
                    Admin Login
                  </Typography>
                  <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mt: 1 }}>
                    Enter your credentials to access the admin dashboard
                  </Typography>
                </Box>

                <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
                  <TextField
                    margin="normal"
                    required
                    fullWidth
                    id="username"
                    label="Username"
                    name="username"
                    autoComplete="username"
                    autoFocus
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    disabled={isLoading}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <Person />
                        </InputAdornment>
                      ),
                    }}
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    margin="normal"
                    required
                    fullWidth
                    name="password"
                    label="Password"
                    type="password"
                    id="password"
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={isLoading}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <Lock />
                        </InputAdornment>
                      ),
                    }}
                    sx={{ mb: 3 }}
                  />

                  {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                      {error}
                    </Alert>
                  )}

                  <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    size="large"
                    disabled={isLoading || !username || !password}
                    sx={{
                      mt: 2,
                      mb: 2,
                      py: 1.5,
                      fontSize: '1.1rem',
                      fontWeight: 'bold',
                    }}
                  >
                    {isLoading ? (
                      <CircularProgress size={24} color="inherit" />
                    ) : (
                      'Sign In'
                    )}
                  </Button>
                </Box>
              </Paper>
            </Fade>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
};

export default LoginPage;
