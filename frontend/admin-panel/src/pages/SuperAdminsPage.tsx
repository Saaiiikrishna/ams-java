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
  Snackbar,
  Divider,
} from '@mui/material';
import {
  Add as AddIcon,
  Person as PersonIcon,
  Block as BlockIcon,
  AdminPanelSettings as AdminIcon,
  Lock as LockIcon,
  MoreVert as MoreVertIcon,
  DeleteForever,
  Warning,
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

  // Database cleanup states
  const [cleanupDialogOpen, setCleanupDialogOpen] = useState(false);
  const [cleanupLoading, setCleanupLoading] = useState(false);
  const [cleanupSnackbar, setCleanupSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error' | 'warning' | 'info';
  }>({
    open: false,
    message: '',
    severity: 'info',
  });

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

  const handleCleanupDatabase = async () => {
    console.log('🚨 [DANGER ZONE] ===== DATABASE CLEANUP INITIATED =====');
    console.log('🚨 [DANGER ZONE] User confirmed database cleanup');
    console.log('🚨 [DANGER ZONE] Current timestamp:', new Date().toISOString());
    console.log('🚨 [DANGER ZONE] Current super admins count:', superAdmins.length);
    console.log('🚨 [DANGER ZONE] About to send POST request to /super/database/cleanup');

    setCleanupLoading(true);

    try {
      console.log('🔄 [DANGER ZONE] Sending cleanup request to backend...');
      console.log('🔄 [DANGER ZONE] API endpoint: /super/database/cleanup');
      console.log('🔄 [DANGER ZONE] Request payload: {}');

      const response = await ApiService.post('/super/database/cleanup', {});

      console.log('✅ [DANGER ZONE] Cleanup response received:', response);
      console.log('✅ [DANGER ZONE] Response status:', response.status);
      console.log('✅ [DANGER ZONE] Response data:', response.data);

      if (response.data && response.data.success) {
        console.log('🎉 [DANGER ZONE] Database cleanup completed successfully!');
        console.log('🎉 [DANGER ZONE] Success message:', response.data.message);

        setCleanupSnackbar({
          open: true,
          message: `Database cleanup completed successfully! ${response.data.message}`,
          severity: 'success',
        });

        // Refresh super admins list after cleanup
        console.log('🔄 [DANGER ZONE] Refreshing super admins list in 1 second...');
        setTimeout(() => {
          fetchSuperAdmins();
        }, 1000);

      } else {
        console.error('❌ [DANGER ZONE] Cleanup failed - success flag is false');
        console.error('❌ [DANGER ZONE] Error message:', response.data?.message);

        setCleanupSnackbar({
          open: true,
          message: `Cleanup failed: ${response.data?.message || 'Unknown error'}`,
          severity: 'error',
        });
      }

    } catch (error: any) {
      console.error('💥 [DANGER ZONE] ===== CLEANUP ERROR =====');
      console.error('💥 [DANGER ZONE] Error object:', error);
      console.error('💥 [DANGER ZONE] Error message:', error.message);
      console.error('💥 [DANGER ZONE] Error response:', error.response);
      console.error('💥 [DANGER ZONE] Error request:', error.request);

      let errorMessage = 'Database cleanup failed due to an unexpected error.';
      if (error.response?.data?.message) {
        errorMessage = `Cleanup failed: ${error.response.data.message}`;
        console.error('💥 [DANGER ZONE] Server error message:', error.response.data.message);
      } else if (error.response?.status) {
        errorMessage = `Cleanup failed: Server returned status ${error.response.status}`;
        console.error('💥 [DANGER ZONE] HTTP status:', error.response.status);
      } else if (error.message) {
        errorMessage = `Cleanup failed: ${error.message}`;
        console.error('💥 [DANGER ZONE] Network/client error:', error.message);
      }

      setCleanupSnackbar({
        open: true,
        message: errorMessage,
        severity: 'error',
      });
    } finally {
      console.log('🏁 [DANGER ZONE] Cleanup process finished, resetting UI state');
      setCleanupLoading(false);
      setCleanupDialogOpen(false);
    }
  };

  const openCleanupDialog = () => {
    console.log('🔍 [DANGER ZONE] Opening database cleanup dialog');
    console.log('🔍 [DANGER ZONE] Current super admins count:', superAdmins.length);
    console.log('🔍 [DANGER ZONE] User is about to see critical warning dialog');
    setCleanupDialogOpen(true);
  };

  const closeCleanupDialog = () => {
    console.log('❌ [DANGER ZONE] User cancelled database cleanup dialog');
    console.log('❌ [DANGER ZONE] Database cleanup aborted by user');
    setCleanupDialogOpen(false);
  };

  const closeSnackbar = () => {
    setCleanupSnackbar(prev => ({ ...prev, open: false }));
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

      {/* DANGER ZONE - Database Management Section */}
      <Card sx={{
        mt: 4,
        border: '2px solid',
        borderColor: 'error.main',
        bgcolor: 'error.light',
        color: 'error.contrastText',
      }}>
        <CardContent>
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Warning color="inherit" />
            ⚠️ DANGER ZONE - Database Management
          </Typography>

          <Typography variant="body2" sx={{ mb: 3, opacity: 0.9 }}>
            This section contains critical database operations that cannot be undone.
            Use with extreme caution and only when absolutely necessary.
          </Typography>

          <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.3)' }} />

          <Box sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            p: 2,
            bgcolor: 'rgba(255,255,255,0.1)',
            borderRadius: 2,
          }}>
            <DeleteForever fontSize="large" />
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1" fontWeight="bold">
                Clean Database
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                Delete ALL data from ALL tables except super_admins. This will permanently remove:
                Organizations, Entity Admins, Subscribers, NFC Cards, Sessions, Orders, and all other data.
              </Typography>
            </Box>
            <Button
              variant="contained"
              color="error"
              size="large"
              onClick={openCleanupDialog}
              disabled={cleanupLoading}
              startIcon={cleanupLoading ? <CircularProgress size={20} /> : <DeleteForever />}
              sx={{
                minWidth: 200,
                fontWeight: 'bold',
                bgcolor: 'error.dark',
                '&:hover': {
                  bgcolor: 'error.darker',
                }
              }}
            >
              {cleanupLoading ? 'Cleaning...' : 'Clean Database'}
            </Button>
          </Box>
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

      {/* Database Cleanup Confirmation Dialog */}
      <Dialog
        open={cleanupDialogOpen}
        onClose={closeCleanupDialog}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{
          bgcolor: 'error.main',
          color: 'error.contrastText',
          display: 'flex',
          alignItems: 'center',
          gap: 1,
        }}>
          <Warning />
          ⚠️ CRITICAL WARNING - Database Cleanup
        </DialogTitle>
        <DialogContent sx={{ mt: 2 }}>
          <Box component="div">
            <Typography variant="h6" color="error" gutterBottom>
              YOU ARE ABOUT TO DELETE ALL DATABASE DATA!
            </Typography>

            <Typography variant="body1" paragraph>
              This action will <strong>permanently delete ALL data</strong> from the following tables:
            </Typography>

            <Box component="ul" sx={{ pl: 3, mb: 2 }}>
              <li>Organizations and all their data</li>
              <li>Entity Admins and their assignments</li>
              <li>Subscribers and their profiles</li>
              <li>NFC Cards and assignments</li>
              <li>Attendance Sessions and records</li>
              <li>Orders and transaction history</li>
              <li>System logs and activity records</li>
              <li>All other application data</li>
            </Box>

            <Typography variant="body1" paragraph color="success.main">
              ✅ <strong>PRESERVED:</strong> super_admins table will remain intact
            </Typography>

            <Typography variant="body1" paragraph color="error">
              ❌ <strong>THIS CANNOT BE UNDONE!</strong>
            </Typography>

            <Typography variant="body2" color="text.secondary">
              Only proceed if you are absolutely certain you want to reset the entire system
              to a clean state. This is typically used for testing or system reset purposes.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button
            onClick={closeCleanupDialog}
            variant="outlined"
            size="large"
            disabled={cleanupLoading}
          >
            Cancel - Keep Data Safe
          </Button>
          <Button
            onClick={handleCleanupDatabase}
            variant="contained"
            color="error"
            size="large"
            disabled={cleanupLoading}
            startIcon={cleanupLoading ? <CircularProgress size={20} /> : <DeleteForever />}
            sx={{ minWidth: 200 }}
          >
            {cleanupLoading ? 'Deleting All Data...' : 'YES - DELETE ALL DATA'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Cleanup Result Snackbar */}
      <Snackbar
        open={cleanupSnackbar.open}
        autoHideDuration={6000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          onClose={closeSnackbar}
          severity={cleanupSnackbar.severity}
          sx={{ width: '100%', fontSize: '1.1rem' }}
        >
          {cleanupSnackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default SuperAdminsPage;
