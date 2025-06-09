import React, { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';
import ApiService from '../services/ApiService';
import { Container, TextField, Button, Typography, Box, Alert } from '@mui/material';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      const response = await ApiService.post('/admin/authenticate', {
        username,
        password,
      });
      // Assuming backend response is { jwt: "accessTokenValue", refreshToken: "refreshTokenValue" }
      // The LoginResponse.java DTO uses "jwt" for accessToken and "refreshToken" for refreshToken.
      if (response.data && response.data.jwt && response.data.refreshToken) {
        AuthService.storeTokens(response.data.jwt, response.data.refreshToken);
        // Navigate to the appropriate dashboard based on the panel
        // For admin-panel, it's '/admin/entities'
        // For entity-dashboard, it might be '/dashboard' or similar
        // This logic might need to be conditional if this exact file is shared,
        // but for now, assuming it's specific to admin-panel.
        // The entity-dashboard LoginPage.tsx would navigate to its own main page.
        navigate('/admin/entities');
      } else {
        setError('Login failed: Invalid response from server.');
      }
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(`Login failed: ${err.response.data.message}`);
      } else if (err.response && err.response.status === 401) {
        setError('Login failed: Invalid username or password.');
      } else {
        setError('Login failed: An unexpected error occurred.');
        console.error(err);
      }
    }
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Super Admin Login
      </Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <TextField
          label="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          fullWidth
          margin="normal"
        />
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          fullWidth
          margin="normal"
        />
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        <Button type="submit" variant="contained" sx={{ mt: 2 }} fullWidth>
          Login
        </Button>
      </Box>
    </Container>
  );
};

export default LoginPage;
