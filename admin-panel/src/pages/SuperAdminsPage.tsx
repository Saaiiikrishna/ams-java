import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  Alert,
  CircularProgress,
  Avatar,
  Tooltip,
  Menu,
  MenuItem,
  ListItemIcon,
} from '@mui/material';
import {
  Add as AddIcon,
  Person as PersonIcon,
  Block as BlockIcon,
  AdminPanelSettings as AdminIcon,
  Lock as LockIcon,
  MoreVert as MoreVertIcon,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import ChangePasswordDialog from '../components/ChangePasswordDialog';
import ConfirmationDialog from '../components/ConfirmationDialog';

interface SuperAdmin {
  id: number;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  fullName: string;
  createdAt: string;
  isActive: boolean;
}

interface CreateSuperAdminRequest {
  username: string;
  password: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

const SuperAdminsPage: React.FC = () => {
  const [superAdmins, setSuperAdmins] = useState<SuperAdmin[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Create dialog state
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createForm, setCreateForm] = useState<CreateSuperAdminRequest>({
    username: '',
    password: '',
    email: '',
    firstName: '',
    lastName: '',
  });

  // Password change state
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);

  // Menu state
  const [menuAnchorEl, setMenuAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedAdminId, setSelectedAdminId] = useState<number | null>(null);

  // Confirmation dialog state
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmationData, setConfirmationData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
  } | null>(null);

  useEffect(() => {
    fetchSuperAdmins();
  }, []);

  const fetchSuperAdmins = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get('/super/super-admins');
      setSuperAdmins(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch super admins:', err);
      setError('Failed to fetch super admins. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateSuperAdmin = async () => {
    if (!createForm.username || !createForm.password) {
      setError('Username and password are required');
      return;
    }

    setCreateLoading(true);
    setError(null);
    try {
      const response = await ApiService.post('/super/super-admins', createForm);
      setSuccessMessage(`Super Admin '${response.data.username}' created successfully!`);
      setCreateDialogOpen(false);
      resetCreateForm();
      fetchSuperAdmins(); // Refresh the list
    } catch (err: any) {
      console.error('Failed to create super admin:', err);
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('Failed to create super admin. Please try again.');
      }
    } finally {
      setCreateLoading(false);
    }
  };

  const handleDeactivateSuperAdmin = (id: number, username: string) => {
    setConfirmationData({
      title: 'Deactivate Super Admin',
      message: `Are you sure you want to deactivate Super Admin '${username}'? This action cannot be undone.`,
      onConfirm: () => performDeactivation(id, username),
    });
    setConfirmationOpen(true);
  };

  const performDeactivation = async (id: number, username: string) => {
    try {
      await ApiService.put(`/super/super-admins/${id}/deactivate`);
      setSuccessMessage(`Super Admin '${username}' deactivated successfully!`);
      fetchSuperAdmins(); // Refresh the list
      setConfirmationOpen(false);
    } catch (err: any) {
      console.error('Failed to deactivate super admin:', err);
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('Failed to deactivate super admin. Please try again.');
      }
      setConfirmationOpen(false);
    }
  };

  const resetCreateForm = () => {
    setCreateForm({
      username: '',
      password: '',
      email: '',
      firstName: '',
      lastName: '',
    });
  };

  const handleCreateDialogClose = () => {
    setCreateDialogOpen(false);
    resetCreateForm();
    setError(null);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, adminId: number) => {
    setMenuAnchorEl(event.currentTarget);
    setSelectedAdminId(adminId);
  };

  const handleMenuClose = () => {
    setMenuAnchorEl(null);
    setSelectedAdminId(null);
  };

  const handleChangePasswordClick = () => {
    setChangePasswordOpen(true);
    handleMenuClose();
  };

  const handlePasswordChangeSuccess = () => {
    setSuccessMessage('Password changed successfully!');
    setTimeout(() => setSuccessMessage(null), 5000);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getTimeAgo = (dateString: string) => {
    const now = new Date();
    const date = new Date(dateString);
    const diffInMs = now.getTime() - date.getTime();
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
    
    if (diffInDays === 0) return 'Today';
    if (diffInDays === 1) return '1 day ago';
    if (diffInDays < 30) return `${diffInDays} days ago`;
    if (diffInDays < 365) return `${Math.floor(diffInDays / 30)} months ago`;
    return `${Math.floor(diffInDays / 365)} years ago`;
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Super Admin Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
          sx={{ bgcolor: 'primary.main' }}
        >
          Create Super Admin
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {/* Stats Card */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
              <AdminIcon />
            </Avatar>
            <Box>
              <Typography variant="h6">
                Total Super Admins: {superAdmins.length}
              </Typography>
              <Typography variant="body2" color="textSecondary">
                Active: {superAdmins.filter(admin => admin.isActive).length} | 
                Inactive: {superAdmins.filter(admin => !admin.isActive).length}
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Super Admins Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Super Admins List
          </Typography>
          
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Admin</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell>Duration</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {superAdmins.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography color="textSecondary">
                          No super admins found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    superAdmins.map((admin) => (
                      <TableRow key={admin.id} hover>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Avatar sx={{ mr: 2, bgcolor: 'primary.main' }}>
                              <PersonIcon />
                            </Avatar>
                            <Box>
                              <Typography variant="subtitle2">
                                {admin.fullName}
                              </Typography>
                              <Typography variant="body2" color="textSecondary">
                                @{admin.username}
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {admin.email || 'Not provided'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={admin.isActive ? 'Active' : 'Inactive'}
                            color={admin.isActive ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {formatDate(admin.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" color="textSecondary">
                            {getTimeAgo(admin.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          {admin.isActive && (
                            <Tooltip title="More actions">
                              <IconButton
                                onClick={(e) => handleMenuOpen(e, admin.id)}
                                size="small"
                              >
                                <MoreVertIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create Super Admin Dialog */}
      <Dialog 
        open={createDialogOpen} 
        onClose={handleCreateDialogClose}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <AdminIcon sx={{ mr: 1 }} />
            Create New Super Admin
          </Box>
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Username"
                value={createForm.username}
                onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                required
                disabled={createLoading}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Password"
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                required
                disabled={createLoading}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="First Name"
                value={createForm.firstName}
                onChange={(e) => setCreateForm({ ...createForm, firstName: e.target.value })}
                disabled={createLoading}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Last Name"
                value={createForm.lastName}
                onChange={(e) => setCreateForm({ ...createForm, lastName: e.target.value })}
                disabled={createLoading}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={createForm.email}
                onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                disabled={createLoading}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCreateDialogClose} disabled={createLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleCreateSuperAdmin}
            variant="contained"
            disabled={createLoading}
            startIcon={createLoading ? <CircularProgress size={20} /> : <AddIcon />}
          >
            {createLoading ? 'Creating...' : 'Create Super Admin'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Actions Menu */}
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <MenuItem onClick={handleChangePasswordClick}>
          <ListItemIcon>
            <LockIcon fontSize="small" />
          </ListItemIcon>
          Change Password
        </MenuItem>
        <MenuItem
          onClick={() => {
            const admin = superAdmins.find(a => a.id === selectedAdminId);
            if (admin) {
              handleDeactivateSuperAdmin(admin.id, admin.username);
            }
            handleMenuClose();
          }}
          sx={{ color: 'error.main' }}
        >
          <ListItemIcon>
            <BlockIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          Deactivate
        </MenuItem>
      </Menu>

      {/* Change Password Dialog */}
      <ChangePasswordDialog
        open={changePasswordOpen}
        onClose={() => setChangePasswordOpen(false)}
        onSuccess={handlePasswordChangeSuccess}
      />

      {/* Confirmation Dialog */}
      {confirmationData && (
        <ConfirmationDialog
          open={confirmationOpen}
          title={confirmationData.title}
          message={confirmationData.message}
          onConfirm={confirmationData.onConfirm}
          onCancel={() => setConfirmationOpen(false)}
          confirmText="Deactivate"
          severity="error"
        />
      )}
    </Box>
  );
};

export default SuperAdminsPage;
