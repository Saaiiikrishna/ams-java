import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  InputAdornment,
  CircularProgress,
  Alert,
  Grid,
  Button,
  Chip,
} from '@mui/material';
import {
  Search,
  Business,
  LocationOn,
  Person,
  Email,
  PersonAdd,
  Warning,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface UnassignedEntity {
  id: number;
  name: string;
  address?: string;
  contactPerson?: string;
  email?: string;
  latitude?: number;
  longitude?: number;
}

const UnassignedEntitiesPage: React.FC = () => {
  const [entities, setEntities] = useState<UnassignedEntity[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [assigningAdmin, setAssigningAdmin] = useState<number | null>(null);

  useEffect(() => {
    fetchUnassignedEntities();
  }, []);

  const fetchUnassignedEntities = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<UnassignedEntity[]>('/admin/entities/without-admin');
      setEntities(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch unassigned entities:", err);
      setError('Failed to fetch unassigned entities. Please try again later.');
      setEntities([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAssignAdmin = async (entityId: number) => {
    setAssigningAdmin(entityId);
    setError(null);
    setSuccessMessage(null);

    try {
      // For now, we'll create a default admin. In a real app, you'd want a dialog to input credentials
      const defaultUsername = `admin_${entityId}`;
      const defaultPassword = `password_${entityId}`;

      await ApiService.post(`/admin/entities/${entityId}/assign-admin`, {
        username: defaultUsername,
        password: defaultPassword,
      });

      const entity = entities.find(e => e.id === entityId);
      setSuccessMessage(`Admin assigned successfully to '${entity?.name}'! Username: ${defaultUsername}`);
      
      // Remove the entity from the unassigned list
      setEntities(entities.filter(e => e.id !== entityId));
    } catch (err: any) {
      console.error("Failed to assign admin:", err);
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
      setAssigningAdmin(null);
    }
  };

  // Filter entities based on search term
  const filteredEntities = entities.filter(entity =>
    entity.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (entity.address && entity.address.toLowerCase().includes(searchTerm.toLowerCase())) ||
    (entity.contactPerson && entity.contactPerson.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Unassigned Entities
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Entities that need admin assignment
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

      {/* Entities List */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Unassigned Entities ({filteredEntities.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredEntities.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Warning sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No entities found matching your search' : 'All entities have admins assigned!'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {searchTerm ? 'Try adjusting your search terms' : 'Great job! All entities are properly managed.'}
              </Typography>
            </Box>
          ) : (
            <Grid container spacing={3}>
              {filteredEntities.map((entity) => (
                <Grid item xs={12} sm={6} md={4} key={entity.id}>
                  <Card 
                    variant="outlined" 
                    sx={{ 
                      height: '100%', 
                      position: 'relative',
                      border: '2px solid',
                      borderColor: 'warning.main',
                      backgroundColor: 'warning.light',
                      '&:hover': {
                        borderColor: 'warning.dark',
                        backgroundColor: 'warning.main',
                      }
                    }}
                  >
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                        <Business color="warning" />
                        <Typography variant="h6" fontWeight="bold">
                          {entity.name}
                        </Typography>
                        <Chip 
                          label="No Admin" 
                          color="warning" 
                          size="small"
                          icon={<Warning />}
                        />
                      </Box>
                      
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                        ID: {entity.id}
                      </Typography>
                      
                      {entity.address && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                          <LocationOn sx={{ fontSize: 16, mr: 0.5 }} />
                          {entity.address}
                        </Typography>
                      )}
                      
                      {entity.contactPerson && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                          <Person sx={{ fontSize: 16, mr: 0.5 }} />
                          {entity.contactPerson}
                        </Typography>
                      )}
                      
                      {entity.email && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          <Email sx={{ fontSize: 16, mr: 0.5 }} />
                          {entity.email}
                        </Typography>
                      )}
                      
                      <Button
                        variant="contained"
                        size="small"
                        startIcon={assigningAdmin === entity.id ? <CircularProgress size={16} /> : <PersonAdd />}
                        onClick={() => handleAssignAdmin(entity.id)}
                        disabled={assigningAdmin === entity.id}
                        fullWidth
                        sx={{
                          background: 'linear-gradient(45deg, #4CAF50 30%, #8BC34A 90%)',
                          '&:hover': {
                            background: 'linear-gradient(45deg, #388E3C 30%, #689F38 90%)',
                          },
                        }}
                      >
                        {assigningAdmin === entity.id ? 'Assigning...' : 'Assign Admin'}
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default UnassignedEntitiesPage;
