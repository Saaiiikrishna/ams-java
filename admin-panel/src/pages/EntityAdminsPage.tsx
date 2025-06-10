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
  CircularProgress,
  Alert,
  Chip,
  Avatar,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  Person,
  Business,
  Schedule,
  Search,
  Delete,
  PersonRemove,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface EntityAdmin {
  id: number;
  username: string;
  organizationId: number;
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
  const [removeDialogOpen, setRemoveDialogOpen] = useState(false);
  const [adminToRemove, setAdminToRemove] = useState<EntityAdmin | null>(null);

  useEffect(() => {
    fetchEntityAdmins();
  }, []);

  const fetchEntityAdmins = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // Use the actual entity admins endpoint
      const response = await ApiService.get('/admin/entity-admins');
      const adminsList: EntityAdmin[] = (response.data || []).map((admin: any) => ({
        id: admin.id,
        username: admin.username,
        organizationId: admin.organizationId,
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

  const handleRemoveAdmin = async () => {
    if (!adminToRemove) return;

    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.delete(`/admin/entities/${adminToRemove.organizationId}/remove-admin`);
      setSuccessMessage(`Admin "${adminToRemove.username}" removed successfully from ${adminToRemove.organizationName}.`);
      setRemoveDialogOpen(false);
      setAdminToRemove(null);
      fetchEntityAdmins(); // Refresh the list
    } catch (err: any) {
      console.error("Failed to remove admin:", err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          setError(`Failed to remove admin: ${err.response.data}`);
        } else if (err.response.data.message) {
          setError(`Failed to remove admin: ${err.response.data.message}`);
        } else {
          setError('Failed to remove admin: An unexpected error occurred.');
        }
      } else {
        setError('Failed to remove admin. Please try again.');
      }
      setRemoveDialogOpen(false);
      setAdminToRemove(null);
    }
  };

  const openRemoveDialog = (admin: EntityAdmin) => {
    setAdminToRemove(admin);
    setRemoveDialogOpen(true);
  };

  const closeRemoveDialog = () => {
    setRemoveDialogOpen(false);
    setAdminToRemove(null);
  };

  // Filter admins based on search term
  const filteredAdmins = admins.filter(admin =>
    admin.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    admin.organizationName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Entity Admins
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage entity administrators and their assignments
        </Typography>
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
            placeholder="Search by admin username or organization name..."
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

      {/* Entity Admins Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Entity Administrators ({filteredAdmins.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredAdmins.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Person sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No admins found matching your search' : 'No entity admins found'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {searchTerm ? 'Try adjusting your search terms' : 'Entity admins will appear here once they are assigned to organizations.'}
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
                          <Business color="action" />
                          <Typography variant="body2">
                            {admin.organizationName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={admin.role}
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
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          startIcon={<PersonRemove />}
                          onClick={() => openRemoveDialog(admin)}
                        >
                          Remove
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Remove Admin Confirmation Dialog */}
      <Dialog open={removeDialogOpen} onClose={closeRemoveDialog}>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Delete color="error" />
            Remove Entity Admin
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to remove admin "{adminToRemove?.username}" from "{adminToRemove?.organizationName}"?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            This action cannot be undone. The organization will become unassigned and will need a new admin.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeRemoveDialog}>Cancel</Button>
          <Button onClick={handleRemoveAdmin} color="error" variant="contained">
            Remove Admin
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default EntityAdminsPage;
