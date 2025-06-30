import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Switch,
  FormControlLabel,
  Button,
  Alert,
  CircularProgress,
  Chip,
  Divider,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Restaurant as RestaurantIcon,
  People as PeopleIcon,
  Assessment as ReportsIcon,
  Settings as AdvancedIcon,
  Save as SaveIcon,
  Security as SecurityIcon
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface FeaturePermission {
  permission: string;
  name: string;
  description: string;
}

interface PermissionGroup {
  name: string;
  icon: React.ReactNode;
  permissions: FeaturePermission[];
}

interface EntityPermissionManagerProps {
  entityId: string;
  entityName: string;
  onPermissionsUpdated?: () => void;
}

const EntityPermissionManager: React.FC<EntityPermissionManagerProps> = ({
  entityId,
  entityName,
  onPermissionsUpdated
}) => {
  const [permissions, setPermissions] = useState<{[key: string]: boolean}>({});
  const [permissionGroups, setPermissionGroups] = useState<PermissionGroup[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [expandedGroup, setExpandedGroup] = useState<string | false>('menuOrdering');
  const [notesDialogOpen, setNotesDialogOpen] = useState(false);
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (entityId) {
      setIsLoading(true);
      fetchPermissions();
      fetchAvailableFeatures();
    }
  }, [entityId]);

  const fetchPermissions = async () => {
    try {
      const response = await ApiService.get(`/super/permissions/organization/${entityId}`);
      const permissionData = response.data.permissions || [];

      const permissionMap: {[key: string]: boolean} = {};
      permissionData.forEach((perm: any) => {
        permissionMap[perm.featurePermission] = perm.isEnabled || false;
      });

      setPermissions(permissionMap);
    } catch (err: any) {
      console.error('Failed to fetch permissions:', err);
      console.error('Error details:', err.response?.data || err.message);
      setError(`Failed to fetch permissions: ${err.response?.data?.error || err.message}`);
    }
  };

  const fetchAvailableFeatures = async () => {
    try {
      const response = await ApiService.get('/super/permissions/features');
      const features = response.data.features || [];
      const groups = response.data.groups || {};

      const permissionGroups: PermissionGroup[] = [
        {
          name: 'Menu & Ordering System',
          icon: <RestaurantIcon />,
          permissions: groups.menuOrdering?.map((perm: string) =>
            features.find((f: any) => f.permission === perm)
          ).filter(Boolean) || []
        },
        {
          name: 'Attendance & Members',
          icon: <PeopleIcon />,
          permissions: groups.attendance?.map((perm: string) =>
            features.find((f: any) => f.permission === perm)
          ).filter(Boolean) || []
        },
        {
          name: 'Reports & Analytics',
          icon: <ReportsIcon />,
          permissions: groups.reports?.map((perm: string) =>
            features.find((f: any) => f.permission === perm)
          ).filter(Boolean) || []
        },
        {
          name: 'Advanced Features',
          icon: <AdvancedIcon />,
          permissions: groups.advanced?.map((perm: string) =>
            features.find((f: any) => f.permission === perm)
          ).filter(Boolean) || []
        }
      ];

      setPermissionGroups(permissionGroups);
    } catch (err: any) {
      console.error('Failed to fetch available features:', err);
      console.error('Error details:', err.response?.data || err.message);
      setError(`Failed to fetch available features: ${err.response?.data?.error || err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePermissionChange = (permission: string, enabled: boolean) => {
    setPermissions(prev => ({
      ...prev,
      [permission]: enabled
    }));
  };

  const handleSavePermissions = async () => {
    setIsSaving(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const enabledPermissions = Object.entries(permissions)
        .filter(([_, enabled]) => enabled)
        .map(([permission, _]) => permission);

      await ApiService.post(`/super/permissions/organization/${entityId}/update`, {
        entityId,
        permissions: enabledPermissions,
        notes: notes || `Permissions updated for ${entityName}`
      });

      setSuccessMessage('Permissions updated successfully!');

      // Refresh permissions to reflect current state
      await fetchPermissions();

      if (onPermissionsUpdated) {
        onPermissionsUpdated();
      }
    } catch (err: any) {
      console.error('Failed to update permissions:', err);
      setError(err.response?.data?.error || 'Failed to update permissions');
    } finally {
      setIsSaving(false);
    }
  };

  const handleGroupChange = (panel: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
    setExpandedGroup(isExpanded ? panel : false);
  };

  const getEnabledCount = (groupPermissions: FeaturePermission[]) => {
    return groupPermissions.filter(perm => permissions[perm.permission]).length;
  };

  const getTotalEnabledPermissions = () => {
    return Object.values(permissions).filter(Boolean).length;
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h6" fontWeight="bold" gutterBottom>
          Permission Management
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Configure feature access for {entityName}
        </Typography>
      </Box>

      {/* Messages */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {/* Permission Summary */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <Typography variant="h6" fontWeight="bold">
                {getTotalEnabledPermissions()} Permissions Enabled
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Entity: {entityName} ({entityId})
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                onClick={() => setNotesDialogOpen(true)}
              >
                Add Notes
              </Button>
              <Button
                variant="contained"
                startIcon={isSaving ? <CircularProgress size={20} /> : <SaveIcon />}
                onClick={handleSavePermissions}
                disabled={isSaving}
              >
                Save Changes
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Permission Groups */}
      {permissionGroups.map((group, index) => (
        <Accordion
          key={index}
          expanded={expandedGroup === group.name.toLowerCase().replace(/\s+/g, '')}
          onChange={handleGroupChange(group.name.toLowerCase().replace(/\s+/g, ''))}
          sx={{ mb: 2 }}
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon />}
            aria-controls={`${group.name}-content`}
            id={`${group.name}-header`}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
              {group.icon}
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="h6" fontWeight="bold">
                  {group.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {group.permissions.length} features available
                </Typography>
              </Box>
              <Chip
                label={`${getEnabledCount(group.permissions)}/${group.permissions.length} enabled`}
                color={getEnabledCount(group.permissions) > 0 ? 'success' : 'default'}
                size="small"
              />
            </Box>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              {group.permissions.map((permission) => (
                <Grid item xs={12} key={permission.permission}>
                  <Card variant="outlined">
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Box sx={{ flexGrow: 1 }}>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {permission.name}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {permission.description}
                          </Typography>
                        </Box>
                        <FormControlLabel
                          control={
                            <Switch
                              checked={permissions[permission.permission] || false}
                              onChange={(e) => handlePermissionChange(permission.permission, e.target.checked)}
                              color="primary"
                            />
                          }
                          label={permissions[permission.permission] ? 'Enabled' : 'Disabled'}
                          labelPlacement="start"
                        />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </AccordionDetails>
        </Accordion>
      ))}

      {/* Notes Dialog */}
      <Dialog open={notesDialogOpen} onClose={() => setNotesDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <SecurityIcon />
            Add Permission Notes
          </Box>
        </DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Notes (Optional)"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Add any notes about these permission changes..."
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNotesDialogOpen(false)}>Cancel</Button>
          <Button onClick={() => setNotesDialogOpen(false)} variant="contained">
            Save Notes
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default EntityPermissionManager;
