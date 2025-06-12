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

  CleaningServices,
  Security,
  Update,
  DeleteForever,
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
  const [isCleaningUp, setIsCleaningUp] = useState(false);
  const [isCleaningSuperAdmin, setIsCleaningSuperAdmin] = useState(false);
  const [isMigrating, setIsMigrating] = useState(false);
  const [isCleaningDatabase, setIsCleaningDatabase] = useState(false);

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
      const response = await ApiService.get('/super/entity-admins');
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
      await ApiService.delete(`/super/entity-admins/${adminId}`);
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

  const handleCleanupDuplicates = () => {
    setConfirmationData({
      title: 'Cleanup Duplicate Admins',
      message: 'This will remove duplicate entity admins, keeping only the oldest admin for each organization. This action cannot be undone. Are you sure you want to proceed?',
      onConfirm: () => performCleanupDuplicates(),
    });
    setConfirmationOpen(true);
  };

  const performCleanupDuplicates = async () => {
    setIsCleaningUp(true);
    try {
      const response = await ApiService.post('/super/cleanup-duplicate-admins');
      setSuccessMessage(`Cleanup completed! Removed ${response.data.duplicatesRemoved} duplicate admin(s).`);
      fetchEntityAdmins(); // Refresh the list
      setConfirmationOpen(false);
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      console.error('Failed to cleanup duplicates:', err);
      setError('Failed to cleanup duplicate admins. Please try again.');
      setConfirmationOpen(false);
      setTimeout(() => setError(null), 5000);
    } finally {
      setIsCleaningUp(false);
    }
  };

  const handleCleanupSuperAdmin = () => {
    setConfirmationData({
      title: 'Cleanup SuperAdmin Records',
      message: 'This will remove any SuperAdmin records that are incorrectly stored in the Entity Admins table. This should fix the issue of SuperAdmin appearing in the Entity Admins list. Are you sure you want to proceed?',
      onConfirm: () => performCleanupSuperAdmin(),
    });
    setConfirmationOpen(true);
  };

  const performCleanupSuperAdmin = async () => {
    setIsCleaningSuperAdmin(true);
    try {
      const response = await ApiService.post('/super/cleanup-superadmin-from-entity-admins');
      setSuccessMessage(`SuperAdmin cleanup completed! Removed ${response.data.superAdminsRemoved} SuperAdmin record(s) from Entity Admins table.`);
      fetchEntityAdmins(); // Refresh the list
      setConfirmationOpen(false);
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      console.error('Failed to cleanup SuperAdmin records:', err);
      setError('Failed to cleanup SuperAdmin records. Please try again.');
      setConfirmationOpen(false);
      setTimeout(() => setError(null), 5000);
    } finally {
      setIsCleaningSuperAdmin(false);
    }
  };

  const handleCleanupDatabase = () => {
    setConfirmationData({
      title: '⚠️ DANGER: Complete Database Cleanup',
      message: 'This will DELETE ALL DATA from the database except Super Admin accounts. This includes:\n\n• All Organizations\n• All Entity Admins\n• All Subscribers\n• All Sessions\n• All Attendance Logs\n• All NFC Cards\n\nThis action is IRREVERSIBLE and should only be used for development/testing purposes.\n\nAre you absolutely sure you want to proceed?',
      onConfirm: () => performCleanupDatabase(),
    });
    setConfirmationOpen(true);
  };

  const performCleanupDatabase = async () => {
    setIsCleaningDatabase(true);
    try {
      const response = await ApiService.delete('/super/cleanup-all-data');
      const deletedRecords = response.data.deletedRecords;
      setSuccessMessage(
        `Database cleanup completed! Deleted ${deletedRecords.total} total records:\n` +
        `• ${deletedRecords.organizations} Organizations\n` +
        `• ${deletedRecords.entityAdmins} Entity Admins\n` +
        `• ${deletedRecords.subscribers} Subscribers\n` +
        `• ${deletedRecords.attendanceSessions} Sessions\n` +
        `• ${deletedRecords.attendanceLogs} Attendance Logs\n` +
        `• ${deletedRecords.nfcCards} NFC Cards`
      );
      fetchEntityAdmins(); // Refresh the list
      setConfirmationOpen(false);
      setTimeout(() => setSuccessMessage(null), 10000);
    } catch (err: any) {
      console.error('Failed to cleanup database:', err);
      setError('Failed to cleanup database. Please try again.');
      setConfirmationOpen(false);
      setTimeout(() => setError(null), 5000);
    } finally {
      setIsCleaningDatabase(false);
    }
  };

  const handleMigrateEntityIds = () => {
    setConfirmationData({
      title: 'Migrate Entity IDs',
      message: 'This will assign Entity IDs to existing organizations that don\'t have them, and remove unused organizations. Organizations with entity admins will be updated with new MSD-prefixed IDs. Are you sure you want to proceed?',
      onConfirm: () => performMigrateEntityIds(),
    });
    setConfirmationOpen(true);
  };

  const performMigrateEntityIds = async () => {
    setIsMigrating(true);
    try {
      const response = await ApiService.post('/super/migrate-entity-ids');
      setSuccessMessage(`Migration completed! Updated ${response.data.entitiesUpdated} entities and removed ${response.data.entitiesRemoved} unused entities.`);
      setConfirmationOpen(false);
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      console.error('Failed to migrate Entity IDs:', err);
      setError('Failed to migrate Entity IDs. Please try again.');
      setConfirmationOpen(false);
      setTimeout(() => setError(null), 5000);
    } finally {
      setIsMigrating(false);
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
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<Update />}
            onClick={handleMigrateEntityIds}
            disabled={isMigrating}
            size="small"
          >
            {isMigrating ? 'Migrating...' : 'Migrate Entity IDs'}
          </Button>
          <Button
            variant="outlined"
            color="error"
            startIcon={<Security />}
            onClick={handleCleanupSuperAdmin}
            disabled={isCleaningSuperAdmin}
            size="small"
          >
            {isCleaningSuperAdmin ? 'Cleaning...' : 'Fix SuperAdmin'}
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<CleaningServices />}
            onClick={handleCleanupDuplicates}
            disabled={isCleaningUp}
            size="small"
          >
            {isCleaningUp ? 'Cleaning...' : 'Cleanup Duplicates'}
          </Button>
          <Button
            variant="contained"
            color="error"
            startIcon={<DeleteForever />}
            onClick={handleCleanupDatabase}
            disabled={isCleaningDatabase}
            size="small"
            sx={{
              backgroundColor: '#d32f2f',
              '&:hover': { backgroundColor: '#b71c1c' },
              fontWeight: 'bold'
            }}
          >
            {isCleaningDatabase ? 'Cleaning...' : 'CLEANUP ALL DATA'}
          </Button>
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
