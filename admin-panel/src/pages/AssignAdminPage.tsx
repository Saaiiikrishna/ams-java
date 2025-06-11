import React, { useState, useEffect, FormEvent } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  Grid,
  Paper,
  Divider,
  Avatar,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  PersonAdd,
  Business,
  AccountCircle,
  Lock,
  CheckCircle,
  Error,
  Search,
  Assignment,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface OrganizationForSelect {
  id: number; // Keep for internal use
  entityId: string; // Primary identifier for API operations
  name: string;
  address?: string;
  contactPerson?: string;
  email?: string;
}

const AssignAdminPage: React.FC = () => {
  const [entitiesWithAdmins, setEntitiesWithAdmins] = useState<OrganizationForSelect[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showChangeDialog, setShowChangeDialog] = useState(false);
  const [selectedEntity, setSelectedEntity] = useState<OrganizationForSelect | null>(null);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [isChanging, setIsChanging] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showRemoveDialog, setShowRemoveDialog] = useState(false);
  const [entityToRemoveAdmin, setEntityToRemoveAdmin] = useState<OrganizationForSelect | null>(null);
  const [isRemoving, setIsRemoving] = useState(false);

  useEffect(() => {
    fetchEntitiesWithAdmins();
  }, []);

  const fetchEntitiesWithAdmins = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [allEntitiesResponse, entitiesWithoutAdminResponse] = await Promise.all([
        ApiService.get<OrganizationForSelect[]>('/super/entities'),
        ApiService.get<OrganizationForSelect[]>('/super/entities/without-admin')
      ]);

      const allEntities = allEntitiesResponse.data || [];
      const entitiesWithoutAdmin = entitiesWithoutAdminResponse.data || [];

      // Filter entities that have admins (not in the without-admin list)
      const entitiesWithAdmins = allEntities.filter(entity =>
        !entitiesWithoutAdmin.some(e => e.entityId === entity.entityId)
      );

      setEntitiesWithAdmins(entitiesWithAdmins);
    } catch (err: any) {
      console.error("Failed to fetch entities:", err);
      setError('Failed to fetch entities. Please try again later.');
      setEntitiesWithAdmins([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChangeAdminClick = (entity: OrganizationForSelect) => {
    setSelectedEntity(entity);
    setNewUsername('');
    setNewPassword('');
    setShowChangeDialog(true);
  };

  const handleChangeAdmin = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedEntity) return;

    setIsChanging(true);
    setError(null);
    setSuccessMessage(null);

    try {
      // For now, we'll use the assign admin endpoint since we don't have a change admin endpoint
      // In a real implementation, you'd want a dedicated endpoint for changing admin credentials
      await ApiService.post(`/super/entities/${selectedEntity.entityId}/assign-admin`, {
        username: newUsername,
        password: newPassword,
      });

      setSuccessMessage(`Admin credentials updated successfully for '${selectedEntity.name}'!`);
      setShowChangeDialog(false);
      setSelectedEntity(null);
      setNewUsername('');
      setNewPassword('');
      fetchEntitiesWithAdmins(); // Refresh the list
    } catch (err: any) {
      console.error("Failed to change admin:", err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          if (err.response.data.includes('already has an entity admin assigned')) {
            setError('Cannot assign new admin: This entity already has an admin. Please remove the existing admin first using the "Remove" button, then try again.');
          } else {
            setError(`Failed to change admin: ${err.response.data}`);
          }
        } else if (err.response.data.message) {
          setError(`Failed to change admin: ${err.response.data.message}`);
        } else {
          setError('Failed to change admin: An unexpected error occurred.');
        }
      } else {
        setError('Failed to change admin. Please try again.');
      }
    } finally {
      setIsChanging(false);
    }
  };

  const handleRemoveAdminClick = (entity: OrganizationForSelect) => {
    setEntityToRemoveAdmin(entity);
    setShowRemoveDialog(true);
  };

  const handleRemoveAdmin = async () => {
    if (!entityToRemoveAdmin) return;

    setIsRemoving(true);
    setError(null);
    setSuccessMessage(null);

    try {
      // For now, we'll use a DELETE endpoint that we need to implement
      await ApiService.delete(`/super/entities/${entityToRemoveAdmin.entityId}/remove-admin`);

      setSuccessMessage(`Admin removed successfully from '${entityToRemoveAdmin.name}'!`);
      setShowRemoveDialog(false);
      setEntityToRemoveAdmin(null);
      fetchEntitiesWithAdmins(); // Refresh the list
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
    } finally {
      setIsRemoving(false);
    }
  };

  // Filter entities based on search term
  const filteredEntities = entitiesWithAdmins.filter(entity =>
    entity.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (entity.address && entity.address.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (entity.contactPerson && entity.contactPerson.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Change Entity Admin
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage admin credentials for organizations with existing admins
          </Typography>
        </Box>
      </Box>

      {/* Alert Messages */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} icon={<Error />}>
          {error}
        </Alert>
      )}

      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }} icon={<CheckCircle />}>
          {successMessage}
        </Alert>
      )}

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search entities by name, address, or contact person..."
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

      {/* Entities with Admins List */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Entities with Admins ({filteredEntities.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredEntities.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Assignment sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No entities found matching your search' : 'No entities with admins found'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {searchTerm ? 'Try adjusting your search terms' : 'All entities may need admin assignment first'}
              </Typography>
            </Box>
          ) : (
            <Grid container spacing={3}>
              {filteredEntities.map((entity) => (
                <Grid item xs={12} sm={6} md={4} key={entity.entityId}>
                  <Card variant="outlined" sx={{ height: '100%', position: 'relative' }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                        <Business color="primary" />
                        <Typography variant="h6" fontWeight="bold">
                          {entity.name}
                        </Typography>
                      </Box>

                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                        Entity ID: {entity.entityId}
                      </Typography>

                      {entity.address && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                          📍 {entity.address}
                        </Typography>
                      )}

                      {entity.contactPerson && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                          👤 {entity.contactPerson}
                        </Typography>
                      )}

                      {entity.email && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          ✉️ {entity.email}
                        </Typography>
                      )}

                      <Box sx={{ display: 'flex', gap: 1 }}>
                        <Button
                          variant="contained"
                          size="small"
                          startIcon={<PersonAdd />}
                          onClick={() => handleChangeAdminClick(entity)}
                          sx={{
                            flex: 1,
                            background: 'linear-gradient(45deg, #FF9800 30%, #FFB74D 90%)',
                            '&:hover': {
                              background: 'linear-gradient(45deg, #F57C00 30%, #FF9800 90%)',
                            },
                          }}
                        >
                          Change
                        </Button>
                        <Button
                          variant="outlined"
                          size="small"
                          color="error"
                          onClick={() => handleRemoveAdminClick(entity)}
                          sx={{ minWidth: 'auto', px: 1 }}
                        >
                          Remove
                        </Button>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </CardContent>
      </Card>

      {/* Change Admin Dialog */}
      <Dialog open={showChangeDialog} onClose={() => setShowChangeDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonAdd color="primary" />
            <Box>
              <Typography variant="h6">Change Admin Credentials</Typography>
              <Typography variant="body2" color="text.secondary">
                {selectedEntity?.name}
              </Typography>
            </Box>
          </Box>
        </DialogTitle>
        <form onSubmit={handleChangeAdmin}>
          <DialogContent>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="New Admin Username"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  disabled={isChanging}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <AccountCircle />
                      </InputAdornment>
                    ),
                  }}
                  helperText="Enter a new username for the entity admin"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  type="password"
                  label="New Admin Password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  disabled={isChanging}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Lock />
                      </InputAdornment>
                    ),
                  }}
                  helperText="Enter a new secure password"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowChangeDialog(false)} disabled={isChanging}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isChanging || !newUsername || !newPassword}
              startIcon={isChanging ? <CircularProgress size={20} /> : <PersonAdd />}
              sx={{
                background: 'linear-gradient(45deg, #4CAF50 30%, #8BC34A 90%)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #388E3C 30%, #689F38 90%)',
                },
              }}
            >
              {isChanging ? 'Changing...' : 'Change Admin'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Remove Admin Confirmation Dialog */}
      <Dialog open={showRemoveDialog} onClose={() => setShowRemoveDialog(false)} maxWidth="sm">
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Error color="error" />
            Remove Admin
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            Are you sure you want to remove the admin from "{entityToRemoveAdmin?.name}"?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            This action will remove all admin access for this entity. The entity will appear in the unassigned entities list.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button onClick={() => setShowRemoveDialog(false)} disabled={isRemoving}>
            Cancel
          </Button>
          <Button
            onClick={handleRemoveAdmin}
            variant="contained"
            color="error"
            disabled={isRemoving}
            startIcon={isRemoving ? <CircularProgress size={20} /> : <Error />}
          >
            {isRemoving ? 'Removing...' : 'Remove Admin'}
          </Button>
        </DialogActions>
      </Dialog>

    </Box>
  );
};

export default AssignAdminPage;
