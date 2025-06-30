import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  CircularProgress,
  Tooltip,
  InputAdornment,
} from '@mui/material';
import {
  Add,

  Search,
  Nfc,
  Person,
  CheckCircle,
  Cancel,
  Assignment,
  PersonOff,
  Warning,
} from '@mui/icons-material';
import ApiService from '../services/ApiService';
import logger from '../services/LoggingService';

interface NfcCard {
  id: number;
  cardUid: string;
  isActive: boolean;
  subscriberId?: number;
  subscriberName?: string;
  subscriberEmail?: string;
  subscriberMobileNumber?: string;
  organizationName?: string;
  entityId?: string;
  assignedAt?: string;
}

interface Subscriber {
  id: number;
  firstName: string;
  lastName: string;
  email?: string;
  mobileNumber: string;
  hasNfcCard: boolean;
  nfcCardUid?: string;
}

const CardsPage: React.FC = () => {
  const [cards, setCards] = useState<NfcCard[]>([]);
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [unassignedCards, setUnassignedCards] = useState<NfcCard[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Dialog states
  const [showRegisterDialog, setShowRegisterDialog] = useState(false);
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  const [selectedSubscriber, setSelectedSubscriber] = useState<Subscriber | null>(null);
  const [newCardUid, setNewCardUid] = useState('');
  const [selectedCardForAssignment, setSelectedCardForAssignment] = useState<string>('');

  // Confirmation dialog for unassigning cards
  const [showUnassignDialog, setShowUnassignDialog] = useState(false);
  const [cardToUnassign, setCardToUnassign] = useState<string>('');

  useEffect(() => {
    fetchCards();
    fetchSubscribers();
  }, []);

  const fetchCards = async () => {
    setIsLoading(true);
    setError(null); // Clear previous errors
    try {
      logger.debug('Fetching cards from /api/cards', 'CARDS');
      const response = await ApiService.get('/api/cards');
      logger.debug('Cards API response', 'CARDS', {
        status: response.status,
        dataLength: response.data?.length,
        data: response.data
      });
      setCards(response.data || []);
      logger.info('Cards fetched successfully', 'CARDS', { count: response.data?.length });
    } catch (err: any) {
      logger.error('Failed to fetch cards', 'CARDS', err);
      if (err.response?.status === 401) {
        setError('Authentication failed. Please log in again.');
      } else if (err.response?.status === 403) {
        setError('Access denied. You do not have permission to view cards.');
      } else {
        setError(err.response?.data?.error || 'Failed to fetch cards. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const fetchSubscribers = async () => {
    try {
      const response = await ApiService.get('/api/subscribers');
      setSubscribers(response.data || []);
      logger.info('Subscribers fetched successfully', 'CARDS', { count: response.data?.length });
    } catch (err: any) {
      logger.error('Failed to fetch subscribers', 'CARDS', err);
    }
  };

  const fetchUnassignedCards = async () => {
    try {
      logger.debug('Fetching unassigned cards from /api/cards/unassigned', 'CARDS');
      const response = await ApiService.get('/api/cards/unassigned');
      logger.debug('Unassigned cards API response', 'CARDS', {
        status: response.status,
        dataLength: response.data?.length,
        data: response.data
      });
      setUnassignedCards(response.data || []);
      logger.info('Unassigned cards fetched successfully', 'CARDS', { count: response.data?.length });
    } catch (err: any) {
      logger.error('Failed to fetch unassigned cards', 'CARDS', err);
      // Don't set error here as this is called from assignment dialog
    }
  };

  const handleRegisterCard = async () => {
    if (!newCardUid.trim()) {
      setError('Card UID is required');
      return;
    }

    try {
      await ApiService.post('/api/cards/register', {
        cardUid: newCardUid.trim(),
        isActive: true
      });

      setSuccessMessage('Card registered successfully!');
      setShowRegisterDialog(false);
      setNewCardUid('');
      fetchCards();
      logger.userAction('Card registered', 'CARDS', { cardUid: newCardUid });
    } catch (err: any) {
      logger.error('Failed to register card', 'CARDS', err);
      setError(err.response?.data?.error || 'Failed to register card');
    }
  };

  const handleAssignCard = async () => {
    if (!selectedSubscriber || !selectedCardForAssignment) {
      setError('Please select both a subscriber and a card');
      return;
    }

    try {
      await ApiService.post('/api/cards/assign', {
        cardUid: selectedCardForAssignment,
        subscriberId: selectedSubscriber.id
      });

      setSuccessMessage(`Card assigned to ${selectedSubscriber.firstName} ${selectedSubscriber.lastName} successfully!`);
      setShowAssignDialog(false);
      setSelectedSubscriber(null);
      setSelectedCardForAssignment('');
      fetchCards();
      fetchSubscribers();
      logger.userAction('Card assigned', 'CARDS', { 
        cardUid: selectedCardForAssignment, 
        subscriberId: selectedSubscriber.id 
      });
    } catch (err: any) {
      logger.error('Failed to assign card', 'CARDS', err);
      setError(err.response?.data?.error || 'Failed to assign card');
    }
  };

  const handleUnassignCard = (cardUid: string) => {
    setCardToUnassign(cardUid);
    setShowUnassignDialog(true);
  };

  const confirmUnassignCard = async () => {
    try {
      await ApiService.post(`/api/cards/unassign/${cardToUnassign}`);
      setSuccessMessage('Card unassigned successfully!');
      setShowUnassignDialog(false);
      setCardToUnassign('');
      fetchCards();
      fetchSubscribers();
      logger.userAction('Card unassigned', 'CARDS', { cardUid: cardToUnassign });
    } catch (err: any) {
      logger.error('Failed to unassign card', 'CARDS', err);
      setError(err.response?.data?.error || 'Failed to unassign card');
      setShowUnassignDialog(false);
      setCardToUnassign('');
    }
  };

  const openAssignDialog = async (subscriber: Subscriber) => {
    setSelectedSubscriber(subscriber);
    await fetchUnassignedCards();
    setShowAssignDialog(true);
  };

  const filteredCards = cards.filter(card =>
    card.cardUid.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (card.subscriberName && card.subscriberName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const subscribersWithoutCards = subscribers.filter(sub => !sub.hasNfcCard);

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          NFC Cards Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setShowRegisterDialog(true)}
          sx={{
            background: 'linear-gradient(45deg, #2196F3 30%, #21CBF3 90%)',
            '&:hover': {
              background: 'linear-gradient(45deg, #1976D2 30%, #2196F3 90%)',
            },
          }}
        >
          Register Card
        </Button>
      </Box>

      {/* Alert Messages */}
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

      {/* Statistics Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Nfc color="primary" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {cards.length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Total Cards
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <CheckCircle color="success" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {cards.filter(c => c.subscriberId).length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Assigned Cards
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Cancel color="warning" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {cards.filter(c => !c.subscriberId).length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Unassigned Cards
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search cards by UID or subscriber name..."
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

      {/* Cards Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
            All Cards ({filteredCards.length})
          </Typography>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Card UID</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Assigned To</TableCell>
                    <TableCell>Contact</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredCards.map((card) => (
                    <TableRow key={card.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Nfc color="primary" />
                          <Typography variant="body2" fontFamily="monospace" fontWeight="bold">
                            {card.cardUid}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={card.subscriberId ? <CheckCircle /> : <Cancel />}
                          label={card.subscriberId ? 'Assigned' : 'Unassigned'}
                          color={card.subscriberId ? 'success' : 'warning'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {card.subscriberName ? (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Person />
                            <Typography variant="body2">
                              {card.subscriberName}
                            </Typography>
                          </Box>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            Not assigned
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {card.subscriberMobileNumber && (
                          <Typography variant="body2">
                            {card.subscriberMobileNumber}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">
                        {card.subscriberId ? (
                          <Tooltip title="Unassign Card">
                            <IconButton
                              color="warning"
                              onClick={() => handleUnassignCard(card.cardUid)}
                              size="small"
                            >
                              <PersonOff />
                            </IconButton>
                          </Tooltip>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            Available
                          </Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Subscribers without cards section */}
      {subscribersWithoutCards.length > 0 && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Subscribers Without Cards ({subscribersWithoutCards.length})
            </Typography>
            <Grid container spacing={2}>
              {subscribersWithoutCards.map((subscriber) => (
                <Grid item xs={12} sm={6} md={4} key={subscriber.id}>
                  <Card variant="outlined">
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <Person />
                        <Typography variant="subtitle2" fontWeight="bold">
                          {subscriber.firstName} {subscriber.lastName}
                        </Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        {subscriber.mobileNumber}
                      </Typography>
                      <Button
                        variant="contained"
                        size="small"
                        startIcon={<Assignment />}
                        onClick={() => openAssignDialog(subscriber)}
                        fullWidth
                      >
                        Assign Card
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Register Card Dialog */}
      <Dialog open={showRegisterDialog} onClose={() => setShowRegisterDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Register New NFC Card</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Card UID"
            fullWidth
            variant="outlined"
            value={newCardUid}
            onChange={(e) => setNewCardUid(e.target.value)}
            placeholder="Enter the NFC card UID"
            helperText="Scan the NFC card or enter its UID manually"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowRegisterDialog(false)}>Cancel</Button>
          <Button onClick={handleRegisterCard} variant="contained">
            Register
          </Button>
        </DialogActions>
      </Dialog>

      {/* Assign Card Dialog */}
      <Dialog open={showAssignDialog} onClose={() => setShowAssignDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Assign Card to {selectedSubscriber?.firstName} {selectedSubscriber?.lastName}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Select an unassigned card to assign to this subscriber:
          </Typography>
          {unassignedCards.length === 0 ? (
            <Alert severity="warning">
              No unassigned cards available. Please register a new card first.
            </Alert>
          ) : (
            <Box>
              {unassignedCards.map((card) => (
                <Box
                  key={card.id}
                  sx={{
                    p: 2,
                    border: 1,
                    borderColor: selectedCardForAssignment === card.cardUid ? 'primary.main' : 'grey.300',
                    borderRadius: 1,
                    mb: 1,
                    cursor: 'pointer',
                    '&:hover': {
                      borderColor: 'primary.main',
                    },
                  }}
                  onClick={() => setSelectedCardForAssignment(card.cardUid)}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Nfc color="primary" />
                    <Typography variant="body2" fontFamily="monospace">
                      {card.cardUid}
                    </Typography>
                  </Box>
                </Box>
              ))}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowAssignDialog(false)}>Cancel</Button>
          <Button
            onClick={handleAssignCard}
            variant="contained"
            disabled={!selectedCardForAssignment}
          >
            Assign Card
          </Button>
        </DialogActions>
      </Dialog>

      {/* Unassign Card Confirmation Dialog */}
      <Dialog open={showUnassignDialog} onClose={() => setShowUnassignDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Warning sx={{ color: 'warning.main', fontSize: 40 }} />
            <Typography variant="h6" component="span">
              Unassign NFC Card
            </Typography>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Are you sure you want to unassign this card?
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Card UID: <strong>{cardToUnassign}</strong>
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            The card will become available for assignment to other subscribers.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button
            onClick={() => {
              setShowUnassignDialog(false);
              setCardToUnassign('');
            }}
            variant="outlined"
          >
            Cancel
          </Button>
          <Button
            onClick={confirmUnassignCard}
            variant="contained"
            color="warning"
          >
            Unassign Card
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CardsPage;
