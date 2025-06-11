import React, { useState, useEffect, FormEvent } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Alert,
  Snackbar,
  Grid,
  CircularProgress,
  Tooltip,
  InputAdornment,
} from '@mui/material';
import {
  Edit,
  Delete,
  Business,
  LocationOn,
  Email,
  Person,
  Search,
  BusinessCenter,
  PersonAdd,
  Warning,
  CheckCircle,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import logger from '../services/LoggingService';

interface Organization {
  id: number; // Keep for internal use, but use entityId for API calls
  entityId: string; // Primary identifier for API operations
  name: string;
  address: string;
  latitude?: number;
  longitude?: number;
  contactPerson?: string;
  email?: string;
  hasAdmin?: boolean;
}

interface NewOrganization {
  name: string;
  address: string;
  latitude?: number;
  longitude?: number;
  contactPerson?: string;
  email?: string;
}

const EntityPage: React.FC = () => {
  const [entities, setEntities] = useState<Organization[]>([]);
  const [entitiesWithoutAdmin, setEntitiesWithoutAdmin] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  const [selectedEntityForAdmin, setSelectedEntityForAdmin] = useState<Organization | null>(null);
  const [adminUsername, setAdminUsername] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [assigningAdmin, setAssigningAdmin] = useState(false);

  // Confirmation dialog for admin assignment after entity creation
  const [showAssignAfterCreateDialog, setShowAssignAfterCreateDialog] = useState(false);
  const [createdEntity, setCreatedEntity] = useState<Organization | null>(null);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editingEntity, setEditingEntity] = useState<Organization | null>(null);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [deletingEntity, setDeletingEntity] = useState<Organization | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [showForceDelete, setShowForceDelete] = useState(false);

  // Form state for creating new entity
  const [newName, setNewName] = useState('');
  const [newAddress, setNewAddress] = useState('');
  const [newLatitude, setNewLatitude] = useState('');
  const [newLongitude, setNewLongitude] = useState('');
  const [newContact, setNewContact] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [formLoading, setFormLoading] = useState(false);


  // Fetch entities
  const fetchEntities = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [entitiesResponse, entitiesWithoutAdminResponse] = await Promise.all([
        ApiService.get<Organization[]>('/super/entities'),
        ApiService.get<Organization[]>('/super/entities/without-admin')
      ]);

      setEntities(entitiesResponse.data || []);
      setEntitiesWithoutAdmin(entitiesWithoutAdminResponse.data || []);
    } catch (err: any) {
      logger.error("Failed to fetch entities", 'ENTITY', err);
      if (err.response && err.response.status === 403) {
        setError('Failed to fetch entities: You do not have permission.');
      } else {
        setError('Failed to fetch entities. Please try again later.');
      }
      setEntities([]);
      setEntitiesWithoutAdmin([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEntities();

  }, []);

  const resetForm = () => {
    setNewName('');
    setNewAddress('');
    setNewLatitude('');
    setNewLongitude('');
    setNewContact('');
    setNewEmail('');
    setShowCreateDialog(false);
    setError(null);
    setSuccessMessage(null);
  };

  const resetAssignForm = () => {
    setAdminUsername('');
    setAdminPassword('');
    setShowAssignDialog(false);
    setSelectedEntityForAdmin(null);
    setAssigningAdmin(false);
  };

  const handleCreateSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setFormLoading(true);

    const newEntityData: NewOrganization = {
      name: newName,
      address: newAddress,
      latitude: newLatitude ? parseFloat(newLatitude) : undefined,
      longitude: newLongitude ? parseFloat(newLongitude) : undefined,
      contactPerson: newContact || undefined,
      email: newEmail || undefined,
    };

    try {
      const response = await ApiService.post('/super/entities', newEntityData);
      setSuccessMessage(`Entity '${response.data.name}' created successfully!`);

      // Show dialog to ask if user wants to assign admin immediately
      setCreatedEntity(response.data);
      setShowAssignAfterCreateDialog(true);

      resetForm();
      fetchEntities();
    } catch (err: any) {
      logger.error("Failed to create entity", 'ENTITY', err);
      if (err.response && err.response.data) {
         if (typeof err.response.data === 'string') {
            setError(`Failed to create entity: ${err.response.data}`);
         } else if (err.response.data.message) {
            setError(`Failed to create entity: ${err.response.data.message}`);
         } else {
            setError('Failed to create entity: An unexpected error occurred.');
         }
      } else {
        setError('Failed to create entity. Please try again.');
      }
    } finally {
      setFormLoading(false);
    }
  };

  const handleAssignAdmin = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedEntityForAdmin) return;

    setAssigningAdmin(true);
    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.post(`/super/entities/${selectedEntityForAdmin.entityId}/assign-admin`, {
        username: adminUsername,
        password: adminPassword,
      });

      setSuccessMessage(`Admin '${adminUsername}' assigned successfully to '${selectedEntityForAdmin.name}'!`);
      resetAssignForm();
      fetchEntities(); // Refresh the lists
    } catch (err: any) {
      logger.error("Failed to assign admin", 'ADMIN', err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          setError(`Failed to assign admin: ${err.response.data}`);
        } else if (err.response.data.message) {
          setError(`Failed to assign admin: ${err.response.data.message}`);
        } else {
          setError('Failed to assign admin: An unexpected error occurred.');
        }
      } else {
        setError('Failed to assign admin. Please try again.');
      }
    } finally {
      setAssigningAdmin(false);
    }
  };

  const handleAssignAdminClick = (entity: Organization) => {
    setSelectedEntityForAdmin(entity);
    setShowAssignDialog(true);
  };

  const handleEditClick = (entity: Organization) => {
    setEditingEntity(entity);
    setNewName(entity.name);
    setNewAddress(entity.address);
    setNewLatitude(entity.latitude?.toString() || '');
    setNewLongitude(entity.longitude?.toString() || '');
    setNewContact(entity.contactPerson || '');
    setNewEmail(entity.email || '');
    setShowEditDialog(true);
  };

  const handleEditSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!editingEntity) return;

    setFormLoading(true);
    setError(null);
    setSuccessMessage(null);

    const updatedEntityData: NewOrganization = {
      name: newName,
      address: newAddress,
      latitude: newLatitude ? parseFloat(newLatitude) : undefined,
      longitude: newLongitude ? parseFloat(newLongitude) : undefined,
      contactPerson: newContact || undefined,
      email: newEmail || undefined,
    };

    try {
      const response = await ApiService.put(`/super/entities/${editingEntity.entityId}`, updatedEntityData);
      setSuccessMessage(`Entity '${response.data.name}' updated successfully!`);
      setShowEditDialog(false);
      setEditingEntity(null);
      resetForm();
      fetchEntities();
    } catch (err: any) {
      logger.error("Failed to update entity", 'ENTITY', err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          setError(`Failed to update entity: ${err.response.data}`);
        } else if (err.response.data.message) {
          setError(`Failed to update entity: ${err.response.data.message}`);
        } else {
          setError('Failed to update entity: An unexpected error occurred.');
        }
      } else {
        setError('Failed to update entity. Please try again.');
      }
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteClick = (entity: Organization) => {
    setDeletingEntity(entity);
    setDeleteError(null);
    setShowForceDelete(false);
    setShowDeleteDialog(true);
  };

  const handleDeleteConfirm = async (force: boolean = false) => {
    if (!deletingEntity) return;

    setFormLoading(true);
    setError(null);
    setSuccessMessage(null);
    setDeleteError(null);

    try {
      const endpoint = force
        ? `/super/entities/${deletingEntity.entityId}/force`
        : `/super/entities/${deletingEntity.entityId}`;

      const response = await ApiService.delete(endpoint);

      if (force && response.data) {
        setSuccessMessage(
          `Entity '${deletingEntity.name}' and all related data deleted successfully! ` +
          `(${response.data.deletedEntityAdmins} admins, ${response.data.deletedSubscribers} subscribers, ${response.data.deletedSessions} sessions removed)`
        );
      } else {
        setSuccessMessage(`Entity '${deletingEntity.name}' deleted successfully!`);
      }

      setShowDeleteDialog(false);
      setDeletingEntity(null);
      setShowForceDelete(false);
      fetchEntities();
    } catch (err: any) {
      logger.error("Failed to delete entity", 'ENTITY', err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          const errorMessage = err.response.data;
          setDeleteError(errorMessage);

          // Show force delete option if there are related records
          if (errorMessage.includes('because it has:')) {
            setShowForceDelete(true);
          }
        } else if (err.response.data.message) {
          setDeleteError(err.response.data.message);
        } else {
          setDeleteError('An unexpected error occurred.');
        }
      } else {
        setDeleteError('Failed to delete entity. Please try again.');
      }
    } finally {
      setFormLoading(false);
    }
  };

  // Filter entities based on search term
  const filteredEntities = entities.filter(entity =>
    entity.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    entity.address.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (entity.contactPerson && entity.contactPerson.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (entity.email && entity.email.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Entity Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<BusinessCenter />}
          onClick={() => setShowCreateDialog(true)}
          size="large"
        >
          Create New Entity
        </Button>
      </Box>

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search entities by name, address, contact person, or email..."
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

      {/* Entities Without Admin Alert */}
      {entitiesWithoutAdmin.length > 0 && (
        <Card sx={{ mb: 3, border: '2px solid', borderColor: 'warning.main' }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <Warning color="warning" />
              <Typography variant="h6" color="warning.main" fontWeight="bold">
                Entities Requiring Admin Assignment ({entitiesWithoutAdmin.length})
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              The following entities don't have an admin assigned yet. Click "Assign Admin" to create admin credentials.
            </Typography>

            <Grid container spacing={2}>
              {entitiesWithoutAdmin.map((entity) => (
                <Grid item xs={12} sm={6} md={4} key={entity.entityId}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <Business color="primary" />
                        <Typography variant="subtitle2" fontWeight="bold">
                          {entity.name}
                        </Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                        Entity ID: {entity.entityId}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        {entity.address}
                      </Typography>
                      <Button
                        variant="contained"
                        size="small"
                        startIcon={<PersonAdd />}
                        onClick={() => handleAssignAdminClick(entity)}
                        fullWidth
                        sx={{
                          background: 'linear-gradient(45deg, #FF9800 30%, #FFB74D 90%)',
                          '&:hover': {
                            background: 'linear-gradient(45deg, #F57C00 30%, #FF9800 90%)',
                          },
                        }}
                      >
                        Assign Admin
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Create Entity Dialog */}
      <Dialog open={showCreateDialog} onClose={() => setShowCreateDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <BusinessCenter color="primary" />
            Create New Entity
          </Box>
        </DialogTitle>
        <form onSubmit={handleCreateSubmit}>
          <DialogContent>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="Entity Name"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Business />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  label="Address"
                  value={newAddress}
                  onChange={(e) => setNewAddress(e.target.value)}
                  disabled={formLoading}
                  multiline
                  rows={2}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <LocationOn />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Latitude (Optional)"
                  value={newLatitude}
                  onChange={(e) => setNewLatitude(e.target.value)}
                  disabled={formLoading}
                  inputProps={{ step: "any" }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Longitude (Optional)"
                  value={newLongitude}
                  onChange={(e) => setNewLongitude(e.target.value)}
                  disabled={formLoading}
                  inputProps={{ step: "any" }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Contact Person (Optional)"
                  value={newContact}
                  onChange={(e) => setNewContact(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Person />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="email"
                  label="Contact Email (Optional)"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Email />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowCreateDialog(false)} disabled={formLoading}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={formLoading || !newName || !newAddress}
              startIcon={formLoading ? <CircularProgress size={20} /> : null}
            >
              Create Entity
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Admin Assignment Dialog */}
      <Dialog open={showAssignDialog} onClose={() => setShowAssignDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonAdd color="primary" />
            <Box>
              <Typography variant="h6">Assign Admin</Typography>
              <Typography variant="body2" color="text.secondary">
                {selectedEntityForAdmin?.name}
              </Typography>
            </Box>
          </Box>
        </DialogTitle>
        <form onSubmit={handleAssignAdmin}>
          <DialogContent>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="Admin Username"
                  value={adminUsername}
                  onChange={(e) => setAdminUsername(e.target.value)}
                  disabled={assigningAdmin}
                  helperText="Choose a unique username for the entity admin"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  type="password"
                  label="Admin Password"
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  disabled={assigningAdmin}
                  helperText="Secure password for the admin account"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowAssignDialog(false)} disabled={assigningAdmin}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={assigningAdmin || !adminUsername || !adminPassword}
              startIcon={assigningAdmin ? <CircularProgress size={20} /> : <PersonAdd />}
              sx={{
                background: 'linear-gradient(45deg, #4CAF50 30%, #8BC34A 90%)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #388E3C 30%, #689F38 90%)',
                },
              }}
            >
              {assigningAdmin ? 'Assigning...' : 'Assign Admin'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Entity Dialog */}
      <Dialog open={showEditDialog} onClose={() => setShowEditDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Edit color="primary" />
            Edit Entity
          </Box>
        </DialogTitle>
        <form onSubmit={handleEditSubmit}>
          <DialogContent>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="Entity Name"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Business />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  label="Address"
                  value={newAddress}
                  onChange={(e) => setNewAddress(e.target.value)}
                  disabled={formLoading}
                  multiline
                  rows={2}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <LocationOn />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Latitude (Optional)"
                  value={newLatitude}
                  onChange={(e) => setNewLatitude(e.target.value)}
                  disabled={formLoading}
                  inputProps={{ step: "any" }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Longitude (Optional)"
                  value={newLongitude}
                  onChange={(e) => setNewLongitude(e.target.value)}
                  disabled={formLoading}
                  inputProps={{ step: "any" }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Contact Person (Optional)"
                  value={newContact}
                  onChange={(e) => setNewContact(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Person />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="email"
                  label="Contact Email (Optional)"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Email />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowEditDialog(false)} disabled={formLoading}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={formLoading || !newName || !newAddress}
              startIcon={formLoading ? <CircularProgress size={20} /> : <Edit />}
            >
              Update Entity
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={showDeleteDialog} onClose={() => setShowDeleteDialog(false)} maxWidth="sm">
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Delete color="error" />
            Delete Entity
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            Are you sure you want to delete the entity "{deletingEntity?.name}"?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            This action cannot be undone. All associated data will be permanently removed.
          </Typography>

          {deleteError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-line' }}>
                {deleteError}
              </Typography>
            </Alert>
          )}

          {showForceDelete && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              <Typography variant="body2" fontWeight="bold">
                Force Delete Option Available
              </Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                You can force delete this entity, which will automatically remove all related data including entity admins, subscribers, and attendance sessions.
              </Typography>
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button
            onClick={() => {
              setShowDeleteDialog(false);
              setDeleteError(null);
              setShowForceDelete(false);
            }}
            disabled={formLoading}
          >
            Cancel
          </Button>

          {showForceDelete && (
            <Button
              onClick={() => handleDeleteConfirm(true)}
              variant="contained"
              color="warning"
              disabled={formLoading}
              startIcon={formLoading ? <CircularProgress size={20} /> : <Delete />}
              sx={{ mr: 1 }}
            >
              {formLoading ? 'Force Deleting...' : 'Force Delete'}
            </Button>
          )}

          <Button
            onClick={() => handleDeleteConfirm(false)}
            variant="contained"
            color="error"
            disabled={formLoading}
            startIcon={formLoading ? <CircularProgress size={20} /> : <Delete />}
          >
            {formLoading ? 'Deleting...' : 'Delete Entity'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Entities Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Entities ({filteredEntities.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredEntities.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Business sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No entities match your search' : 'No entities found'}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {searchTerm ? 'Try adjusting your search terms' : 'Get started by creating your first entity'}
              </Typography>
              {!searchTerm && (
                <Button
                  variant="contained"
                  startIcon={<BusinessCenter />}
                  onClick={() => setShowCreateDialog(true)}
                >
                  Create First Entity
                </Button>
              )}
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Entity ID</TableCell>
                    <TableCell>Entity</TableCell>
                    <TableCell>Address</TableCell>
                    <TableCell>Contact</TableCell>
                    <TableCell>Admin Status</TableCell>
                    <TableCell>Location</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredEntities.map((entity) => {
                    const hasAdmin = !entitiesWithoutAdmin.some(e => e.entityId === entity.entityId);
                    return (
                    <TableRow key={entity.entityId} hover>
                      <TableCell>
                        <Chip
                          label={entity.entityId}
                          color="primary"
                          variant="outlined"
                          size="small"
                          sx={{ fontWeight: 'bold', fontFamily: 'monospace' }}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Box
                            sx={{
                              width: 40,
                              height: 40,
                              borderRadius: 1,
                              bgcolor: 'primary.main',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              color: 'white',
                            }}
                          >
                            <Business />
                          </Box>
                          <Box>
                            <Typography variant="subtitle2" fontWeight="bold">
                              {entity.name}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{entity.address}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box>
                          {entity.contactPerson && (
                            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <Person fontSize="small" />
                              {entity.contactPerson}
                            </Typography>
                          )}
                          {entity.email && (
                            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <Email fontSize="small" />
                              {entity.email}
                            </Typography>
                          )}
                          {!entity.contactPerson && !entity.email && (
                            <Typography variant="body2" color="text.secondary">
                              No contact info
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={hasAdmin ? <CheckCircle /> : <Warning />}
                          label={hasAdmin ? 'Admin Assigned' : 'No Admin'}
                          color={hasAdmin ? 'success' : 'warning'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {entity.latitude && entity.longitude ? (
                          <Chip
                            icon={<LocationOn />}
                            label={`${entity.latitude.toFixed(4)}, ${entity.longitude.toFixed(4)}`}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        ) : (
                          <Chip label="No coordinates" size="small" variant="outlined" />
                        )}
                      </TableCell>
                      <TableCell align="right">
                        {!hasAdmin && (
                          <Tooltip title="Assign admin">
                            <Button
                              variant="outlined"
                              size="small"
                              startIcon={<PersonAdd />}
                              onClick={() => handleAssignAdminClick(entity)}
                              sx={{ mr: 1 }}
                            >
                              Assign Admin
                            </Button>
                          </Tooltip>
                        )}
                        <Tooltip title="Edit entity">
                          <IconButton
                            color="primary"
                            size="small"
                            onClick={() => handleEditClick(entity)}
                          >
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete entity">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() => handleDeleteClick(entity)}
                          >
                            <Delete />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Snackbar for messages */}
      <Snackbar
        open={!!successMessage}
        autoHideDuration={6000}
        onClose={() => setSuccessMessage(null)}
      >
        <Alert onClose={() => setSuccessMessage(null)} severity="success">
          {successMessage}
        </Alert>
      </Snackbar>

      <Snackbar
        open={!!error}
        autoHideDuration={6000}
        onClose={() => setError(null)}
      >
        <Alert onClose={() => setError(null)} severity="error">
          {error}
        </Alert>
      </Snackbar>

      {/* Assign Admin After Entity Creation Dialog */}
      <Dialog open={showAssignAfterCreateDialog} onClose={() => setShowAssignAfterCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <CheckCircle sx={{ color: 'success.main', fontSize: 40 }} />
            <Typography variant="h6" component="span">
              Entity Created Successfully!
            </Typography>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Entity '{createdEntity?.name}' has been created successfully.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Would you like to assign an admin to this entity now?
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button
            onClick={() => setShowAssignAfterCreateDialog(false)}
            variant="outlined"
          >
            Later
          </Button>
          <Button
            onClick={() => {
              if (createdEntity) {
                setSelectedEntityForAdmin(createdEntity);
                setShowAssignDialog(true);
                setShowAssignAfterCreateDialog(false);
                setCreatedEntity(null);
              }
            }}
            variant="contained"
            color="primary"
          >
            Assign Admin Now
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default EntityPage;
