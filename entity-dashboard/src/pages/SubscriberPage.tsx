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
  PersonOff,
  Nfc,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import ConfirmationDialog from '../components/ConfirmationDialog';

interface Subscriber {
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

interface NewSubscriber {
  firstName: string;
  lastName: string;
  email?: string; // Now optional
  mobileNumber: string; // Now required
  nfcCardUid?: string;
  photoUrl?: string; // Conceptual
}

const SubscriberPage: React.FC = () => {
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Confirmation dialog state
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmationData, setConfirmationData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
  } | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Form state for creating/editing subscriber
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

  const fetchSubscribers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<Subscriber[]>('/api/subscribers');
      setSubscribers(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch subscribers:", err);
      setError(err.response?.data?.message || 'Failed to fetch subscribers.');
      setSubscribers([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscribers();
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setFormLoading(true);

    const subscriberData: NewSubscriber = { firstName, lastName, email, mobileNumber, nfcCardUid, photoUrl };

    try {
      if (isEditing && editingId) {
        const response = await ApiService.put<Subscriber>(`/api/subscribers/${editingId}`, subscriberData);
        setSuccessMessage(`Subscriber '${response.data.firstName}' updated successfully!`);
      } else {
        const response = await ApiService.post<Subscriber>('/api/subscribers', subscriberData);
        setSuccessMessage(`Subscriber '${response.data.firstName}' created successfully!`);
      }
      resetForm();
      fetchSubscribers();
    } catch (err: any) {
      console.error("Failed to save subscriber:", err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to save subscriber.');
    } finally {
      setFormLoading(false);
    }
  };

  const handleEdit = (subscriber: Subscriber) => {
    setIsEditing(true);
    setEditingId(subscriber.id);
    setFirstName(subscriber.firstName);
    setLastName(subscriber.lastName);
    setEmail(subscriber.email || '');
    setMobileNumber(subscriber.mobileNumber);
    setNfcCardUid(subscriber.nfcCardUid || '');
    // setPhotoUrl(subscriber.photoUrl || ''); // If photoUrl was part of Subscriber interface
    setShowForm(true);
    setSuccessMessage(null);
  };

  const handleDelete = (id: number) => {
    const subscriber = subscribers.find(s => s.id === id);
    setConfirmationData({
      title: 'Delete Subscriber',
      message: `Are you sure you want to delete subscriber "${subscriber?.firstName} ${subscriber?.lastName}"? This action cannot be undone and will remove all their attendance records.`,
      onConfirm: () => performDelete(id),
    });
    setConfirmationOpen(true);
  };

  const performDelete = async (id: number) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await ApiService.delete(`/api/subscribers/${id}`);
      setSuccessMessage('Subscriber deleted successfully!');
      fetchSubscribers(); // Refresh list
      setConfirmationOpen(false);
    } catch (err: any) {
      console.error("Failed to delete subscriber:", err);
      setError(err.response?.data?.message || 'Failed to delete subscriber.');
      setConfirmationOpen(false);
    }
  };


  // Filter subscribers based on search term
  const filteredSubscribers = subscribers.filter(subscriber =>
    `${subscriber.firstName} ${subscriber.lastName}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (subscriber.email && subscriber.email.toLowerCase().includes(searchTerm.toLowerCase())) ||
    subscriber.mobileNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (subscriber.nfcCardUid && subscriber.nfcCardUid.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          Subscriber Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<PersonAdd />}
          onClick={() => { resetForm(); setShowForm(true); }}
          size="large"
        >
          Add New Subscriber
        </Button>
      </Box>

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search subscribers by name, email, mobile, or NFC card..."
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
            {isEditing ? 'Edit Subscriber' : 'Add New Subscriber'}
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
              {isEditing ? 'Update' : 'Create'} Subscriber
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Subscribers Table */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Subscribers ({filteredSubscribers.length})
            </Typography>
            {isLoading && <CircularProgress size={24} />}
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredSubscribers.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Person sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchTerm ? 'No subscribers match your search' : 'No subscribers found'}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {searchTerm ? 'Try adjusting your search terms' : 'Get started by adding your first subscriber'}
              </Typography>
              {!searchTerm && (
                <Button
                  variant="contained"
                  startIcon={<PersonAdd />}
                  onClick={() => { resetForm(); setShowForm(true); }}
                >
                  Add First Subscriber
                </Button>
              )}
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Subscriber</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>NFC Card</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredSubscribers.map((subscriber) => (
                    <TableRow key={subscriber.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar sx={{ bgcolor: 'primary.main' }}>
                            {subscriber.firstName.charAt(0)}{subscriber.lastName.charAt(0)}
                          </Avatar>
                          <Box>
                            <Typography variant="subtitle2" fontWeight="bold">
                              {subscriber.firstName} {subscriber.lastName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {subscriber.id}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{subscriber.email || 'Not provided'}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{subscriber.mobileNumber}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {subscriber.hasNfcCard && subscriber.nfcCardUid ? (
                            <Chip
                              icon={<Nfc />}
                              label={subscriber.nfcCardUid}
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
                        {!subscriber.hasNfcCard && (
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
                        <Tooltip title="Edit subscriber">
                          <IconButton
                            onClick={() => handleEdit(subscriber)}
                            color="primary"
                            size="small"
                          >
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete subscriber">
                          <IconButton
                            onClick={() => handleDelete(subscriber.id)}
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
