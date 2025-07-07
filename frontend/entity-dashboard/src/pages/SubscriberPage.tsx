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
  Avatar,

  CircularProgress,
  Tooltip,
  InputAdornment,
} from '@mui/material';
import {
  Edit,
  Delete,
  Person,
  Email,
  Phone,
  ContactlessOutlined,
  Search,
  PersonAdd,
  Assignment,
  Security,
  Nfc,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import ConfirmationDialog from '../components/ConfirmationDialog';

interface Member {
  id: number;
  firstName: string;
  lastName: string;
  email?: string;
  mobileNumber: string;
  nfcCardUid?: string;
  hasNfcCard: boolean;
  organizationId?: number;
  entityId?: string;
}

interface NewMember {
  firstName: string;
  lastName: string;
  email?: string; // Now optional
  mobileNumber: string; // Now required
  nfcCardUid?: string;
  photoUrl?: string; // Conceptual
}

const SubscriberPage: React.FC = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [permissionLoading, setPermissionLoading] = useState(true);

  // Confirmation dialog state
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmationData, setConfirmationData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
  } | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Form state for creating/editing member
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [nfcCardUid, setNfcCardUid] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [formLoading, setFormLoading] = useState(false);

  const resetForm = () => {
    setIsEditing(false);
    setEditingId(null);
    setFirstName('');
    setLastName('');
    setEmail('');
    setMobileNumber('');
    setNfcCardUid('');
    setPhotoUrl('');
    setShowForm(false);
    setError(null);
    setSuccessMessage(null);
  };

  const checkPermission = async () => {
    setPermissionLoading(true);
    try {
      const response = await ApiService.get('/api/auth/permissions/check/MEMBER_MANAGEMENT');
      setHasPermission(response.data.hasPermission);
    } catch (err: any) {
      console.error('Failed to check permission:', err);
      console.error('Error details:', err.response?.data || err.message);
      setHasPermission(false);
    } finally {
      setPermissionLoading(false);
    }
  };

  const fetchMembers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<Member[]>('/api/subscriber/subscribers');
      setMembers(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch members:", err);
      setError(err.response?.data?.message || 'Failed to fetch members.');
      setMembers([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    checkPermission();
  }, []);

  useEffect(() => {
    if (hasPermission) {
      fetchMembers();
    }
  }, [hasPermission]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setFormLoading(true);

    const memberData: NewMember = { firstName, lastName, email, mobileNumber, nfcCardUid, photoUrl };

    try {
      if (isEditing && editingId) {
        const response = await ApiService.put<Member>(`/api/subscriber/subscribers/${editingId}`, memberData);
        setSuccessMessage(`Member '${response.data.firstName}' updated successfully!`);
      } else {
        const response = await ApiService.post<Member>('/api/subscriber/subscribers', memberData);
        setSuccessMessage(`Member '${response.data.firstName}' created successfully!`);
      }
      resetForm();
      fetchMembers();
    } catch (err: any) {
      console.error("Failed to save member:", err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to save member.');
    } finally {
      setFormLoading(false);
    }
  };

  const handleEdit = (member: Member) => {
    setIsEditing(true);
    setEditingId(member.id);
    setFirstName(member.firstName);
    setLastName(member.lastName);
    setEmail(member.email || '');
    setMobileNumber(member.mobileNumber);
    setNfcCardUid(member.nfcCardUid || '');
    // setPhotoUrl(member.photoUrl || ''); // If photoUrl was part of Member interface
    setShowForm(true);
    setSuccessMessage(null);
  };

  const handleDelete = (id: number) => {
    const member = members.find(s => s.id === id);
    setConfirmationData({
      title: 'Delete Member',
      message: `Are you sure you want to delete member "${member?.firstName} ${member?.lastName}"? This action cannot be undone and will remove all their attendance records.`,
      onConfirm: () => performDelete(id),
    });
    setConfirmationOpen(true);
  };

  const performDelete = async (id: number) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await ApiService.delete(`/api/subscriber/subscribers/${id}`);
      setSuccessMessage('Member deleted successfully!');
      fetchMembers(); // Refresh list
      setConfirmationOpen(false);
    } catch (err: any) {
      console.error("Failed to delete member:", err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to delete member.');
      setConfirmationOpen(false);
    }
  };


  // Filter members based on search term
  const filteredMembers = members.filter(member =>
    `${member.firstName} ${member.lastName}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (member.email && member.email.toLowerCase().includes(searchTerm.toLowerCase())) ||
    member.mobileNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (member.nfcCardUid && member.nfcCardUid.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  // Permission loading state
  if (permissionLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
        <Typography variant="h6" sx={{ ml: 2 }}>
          Checking permissions...
        </Typography>
      </Box>
    );
  }

  // No permission state
  if (!hasPermission) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning" sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Member Management Access Restricted
          </Typography>
          <Typography variant="body1">
            You don't have permission to access the Member Management system.
            Please contact your super administrator to request access to member management features.
          </Typography>
        </Alert>

        <Card sx={{ mt: 3 }}>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Security sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h5" color="text.secondary" gutterBottom>
              Member Management Unavailable
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Contact your administrator to enable member management features for your organization.
            </Typography>
            <Button
              variant="outlined"
              onClick={checkPermission}
              startIcon={<Security />}
            >
              Check Permissions Again
            </Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Member Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<PersonAdd />}
          onClick={() => { resetForm(); setShowForm(true); }}
          size="large"
        >
          Add New Member
        </Button>
      </Box>

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search members by name, email, mobile, or NFC card..."
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

      {/* Form Dialog */}
      <Dialog open={showForm} onClose={() => setShowForm(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonAdd color="primary" />
            {isEditing ? 'Edit Member' : 'Add New Member'}
          </Box>
        </DialogTitle>
        <form onSubmit={handleSubmit}>
          <DialogContent>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  autoFocus
                  required
                  fullWidth
                  label="First Name"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  disabled={formLoading}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  label="Last Name"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  disabled={formLoading}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type="email"
                  label="Email Address (Optional)"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
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
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  type="tel"
                  label="Mobile Number"
                  value={mobileNumber}
                  onChange={(e) => setMobileNumber(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Phone />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="NFC Card UID (Optional)"
                  value={nfcCardUid}
                  onChange={(e) => setNfcCardUid(e.target.value)}
                  disabled={formLoading}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <ContactlessOutlined />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Photo URL (Optional)"
                  value={photoUrl}
                  onChange={(e) => setPhotoUrl(e.target.value)}
                  disabled={formLoading}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={() => setShowForm(false)} disabled={formLoading}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={formLoading || !firstName || !lastName || !mobileNumber}
              startIcon={formLoading ? <CircularProgress size={20} /> : null}
            >
              {isEditing ? 'Update' : 'Create'} Member
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Members Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Members ({filteredMembers.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredMembers.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Person sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No members match your search' : 'No members found'}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {searchTerm ? 'Try adjusting your search terms' : 'Get started by adding your first member'}
              </Typography>
              {!searchTerm && (
                <Button
                  variant="contained"
                  startIcon={<PersonAdd />}
                  onClick={() => { resetForm(); setShowForm(true); }}
                >
                  Add First Member
                </Button>
              )}
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Member</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>NFC Card</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredMembers.map((member) => (
                    <TableRow key={member.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar sx={{ bgcolor: 'primary.main' }}>
                            {member.firstName.charAt(0)}{member.lastName.charAt(0)}
                          </Avatar>
                          <Box>
                            <Typography variant="subtitle2" fontWeight="bold">
                              {member.firstName} {member.lastName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {member.id}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{member.email || 'Not provided'}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{member.mobileNumber}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {member.hasNfcCard && member.nfcCardUid ? (
                            <Chip
                              icon={<Nfc />}
                              label={member.nfcCardUid}
                              size="small"
                              color="success"
                              variant="outlined"
                            />
                          ) : (
                            <Chip
                              icon={<ContactlessOutlined />}
                              label="No Card"
                              size="small"
                              color="warning"
                              variant="outlined"
                            />
                          )}
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        {!member.hasNfcCard && (
                          <Tooltip title="Assign NFC Card">
                            <IconButton
                              onClick={() => window.open('/dashboard/cards', '_blank')}
                              color="info"
                              size="small"
                            >
                              <Assignment />
                            </IconButton>
                          </Tooltip>
                        )}
                        <Tooltip title="Edit member">
                          <IconButton
                            onClick={() => handleEdit(member)}
                            color="primary"
                            size="small"
                          >
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete member">
                          <IconButton
                            onClick={() => handleDelete(member.id)}
                            color="error"
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

      {/* Confirmation Dialog */}
      {confirmationData && (
        <ConfirmationDialog
          open={confirmationOpen}
          title={confirmationData.title}
          message={confirmationData.message}
          onConfirm={confirmationData.onConfirm}
          onCancel={() => setConfirmationOpen(false)}
          confirmText="Delete"
          severity="error"
        />
      )}
    </Box>
  );
};

export default SubscriberPage;
