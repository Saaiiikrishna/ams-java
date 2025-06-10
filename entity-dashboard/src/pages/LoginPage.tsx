import React, { useState, FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Box,
  Alert,
  Avatar,
  Grid,
  Card,
  CardContent,
  Fade,
  CircularProgress,
} from '@mui/material';
import { LockOutlined, Business, Dashboard, People } from '@mui/icons-material';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  // Redirect to dashboard if already logged in
  if (AuthService.isLoggedIn() && location.pathname === '/login') {
    navigate('/dashboard/subscribers');
  }


  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const response = await ApiService.post('/api/auth/login', {
        username,
        password,
      });

      if (response.data && response.data.jwt) {
        const token = response.data.jwt;
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const authorities: string[] = Array.isArray(payload.authorities)
            ? payload.authorities.map((a: any) => a.authority)
            : [];
          if (!authorities.includes('ROLE_ENTITY_ADMIN')) {
            setError('Access denied: You are not authorized as an entity admin.');
            return;
          }
        } catch (e) {
          console.error('Failed to decode token', e);
          setError('Login failed: Invalid token received.');
          return;
        }
        AuthService.login(token);
        navigate('/dashboard/subscribers');
      } else {
        setError('Login failed: No authentication token received.');
      }
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(`Login failed: ${err.response.data.message}`);
      } else if (err.response && err.response.status === 401) {
        setError('Invalid username or password. Please try again.');
      } else {
        setError('An unexpected error occurred. Please try again later.');
        console.error(err);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
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
                  Entity Admin Portal
                </Typography>
                <Typography variant="h5" component="p" sx={{ mb: 4, opacity: 0.9 }}>
                  Manage your organization's attendance system with ease
                </Typography>

                <Grid container spacing={3}>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <People sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          Manage Subscribers
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <Dashboard sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          Track Sessions
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Card sx={{ textAlign: 'center', p: 2, backgroundColor: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)' }}>
                      <CardContent>
                        <Business sx={{ fontSize: 40, color: 'white', mb: 1 }} />
                        <Typography variant="h6" sx={{ color: 'white' }}>
                          Generate Reports
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
                    <LockOutlined sx={{ fontSize: 30 }} />
                  </Avatar>
                  <Typography component="h1" variant="h4" fontWeight="bold" color="primary">
                    Sign In
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
