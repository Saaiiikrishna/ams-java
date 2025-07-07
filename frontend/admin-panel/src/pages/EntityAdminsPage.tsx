import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,

  TextField,
  InputAdornment,
  CircularProgress,
  Alert,
  Chip,
  Avatar,
  IconButton,
  Tooltip,

  Button,
} from '@mui/material';
import {
  Search,
  Person,
  Business,
  Schedule,
  AdminPanelSettings,
  Delete,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import ConfirmationDialog from '../components/ConfirmationDialog';

interface EntityAdmin {
  id: number;
  username: string;
  entityId: string; // Use entityId instead of organizationId
  organizationName: string;
  createdAt: string;
  role: string;
}

const EntityAdminsPage: React.FC = () => {
  const [admins, setAdmins] = useState<EntityAdmin[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Confirmation dialog state
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmationData, setConfirmationData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
  } | null>(null);

  useEffect(() => {
    fetchEntityAdmins();
  }, []);

  const fetchEntityAdmins = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // Use the actual entity admins endpoint
      const response = await ApiService.get('/api/auth/super/entity-admins');
      const adminsList: EntityAdmin[] = (response.data || []).map((admin: any) => ({
        id: admin.id,
        username: admin.username,
        entityId: admin.entityId, // Use entityId from backend response
        organizationName: admin.organizationName,
        createdAt: admin.createdAt,
        role: admin.role
      }));

      setAdmins(adminsList);
    } catch (err: any) {
      console.error("Failed to fetch entity admins:", err);
      setError('Failed to fetch entity admins. Please try again later.');
      setAdmins([]);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const getTimeAgo = (dateString: string) => {
    const now = new Date();
    const past = new Date(dateString);
    const diffInMs = now.getTime() - past.getTime();

    const minutes = Math.floor(diffInMs / (1000 * 60));
    const hours = Math.floor(diffInMs / (1000 * 60 * 60));
    const days = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
    const months = Math.floor(diffInMs / (1000 * 60 * 60 * 24 * 30));
    const years = Math.floor(diffInMs / (1000 * 60 * 60 * 24 * 365));

    if (years > 0) return `${years} year${years > 1 ? 's' : ''} ago`;
    if (months > 0) return `${months} month${months > 1 ? 's' : ''} ago`;
    if (days > 0) return `${days} day${days > 1 ? 's' : ''} ago`;
    if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''} ago`;
    if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
    return 'Just now';
  };

  const handleRemoveAdmin = (admin: EntityAdmin) => {
    setConfirmationData({
      title: 'Remove Entity Admin',
      message: `Are you sure you want to remove Entity Admin "${admin.username}" from organization "${admin.organizationName}"? This action cannot be undone and will revoke their access to the system.`,
      onConfirm: () => performRemoveAdmin(admin.id, admin.username),
    });
    setConfirmationOpen(true);
  };

  const performRemoveAdmin = async (adminId: number, username: string) => {
    try {
      await ApiService.delete(`/api/auth/super/entity-admins/${adminId}`);
      setSuccessMessage(`Entity Admin '${username}' removed successfully!`);
      fetchEntityAdmins(); // Refresh the list
      setConfirmationOpen(false);
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      console.error('Failed to remove entity admin:', err);
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('Failed to remove entity admin. Please try again.');
      }
      setConfirmationOpen(false);
      setTimeout(() => setError(null), 5000);
    }
  };









  // Filter admins based on search term
  const filteredAdmins = admins.filter(admin =>
    admin.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    admin.organizationName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Entity Admins
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage and view all entity administrators
          </Typography>
        </Box>

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

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search admins by username or organization..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
        </CardContent>
      </Card>

      {/* Admins Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Entity Admins ({filteredAdmins.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredAdmins.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <AdminPanelSettings sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No admins found matching your search' : 'No entity admins found'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {searchTerm ? 'Try adjusting your search terms' : 'Entity admins will appear here once they are created'}
              </Typography>
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Admin</TableCell>
                    <TableCell>Organization</TableCell>
                    <TableCell>Role</TableCell>
                    <TableCell>Created Date</TableCell>
                    <TableCell>Duration</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAdmins.map((admin) => (
                    <TableRow key={admin.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar sx={{ bgcolor: 'primary.main' }}>
                            <Person />
                          </Avatar>
                          <Box>
                            <Typography variant="subtitle2" fontWeight="bold">
                              {admin.username}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {admin.id}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Business color="primary" />
                          <Box>
                            <Typography variant="body2" fontWeight="medium">
                              {admin.organizationName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Entity ID: {admin.entityId}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={<AdminPanelSettings />}
                          label="Entity Admin"
                          color="primary"
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Schedule color="action" />
                          <Typography variant="body2">
                            {formatDateTime(admin.createdAt)}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getTimeAgo(admin.createdAt)}
                          color="info"
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip title="Remove Entity Admin">
                          <IconButton
                            color="error"
                            onClick={() => handleRemoveAdmin(admin)}
                            size="small"
                          >
                            <Delete />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
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
          confirmText="Remove"
          severity="error"
        />
      )}
    </Box>
  );
};

export default EntityAdminsPage;
