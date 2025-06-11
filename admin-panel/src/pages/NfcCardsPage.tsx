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
  Grid,
  CircularProgress,
  Tooltip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Paper,
  Divider,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  ExpandMore,
  Nfc,
  Person,
  CheckCircle,
  Cancel,
  Delete,
  PlayArrow,
  Search,
  Business,
  Warning,
  TouchApp,
  Schedule,
  Group,
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

interface Organization {
  id: number;
  entityId: string;
  name: string;
  address: string;
}

interface AttendanceSession {
  id: number;
  name: string;
  startTime: string;
  endTime?: string;
  isActive: boolean;
  organizationName: string;
}

interface EntityCardData {
  organization: Organization;
  assignedCards: NfcCard[];
  unassignedCards: NfcCard[];
  activeSessions: AttendanceSession[];
}

const NfcCardsPage: React.FC = () => {
  const [entityCardData, setEntityCardData] = useState<EntityCardData[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Dialog states
  const [showDeleteAllDialog, setShowDeleteAllDialog] = useState(false);
  const [showSwipeDialog, setShowSwipeDialog] = useState(false);
  const [selectedCard, setSelectedCard] = useState<NfcCard | null>(null);
  const [selectedSession, setSelectedSession] = useState<AttendanceSession | null>(null);
  const [swipeLoading, setSwipeLoading] = useState(false);

  useEffect(() => {
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // Fetch all organizations
      const orgsResponse = await ApiService.get('/super/entities');
      const organizations: Organization[] = orgsResponse.data || [];

      const entityData: EntityCardData[] = [];

      // For each organization, fetch cards and sessions
      for (const org of organizations) {
        try {
          // Fetch cards for this organization
          const cardsResponse = await ApiService.get(`/super/cards/entity/${org.entityId}`);
          const allCards: NfcCard[] = cardsResponse.data || [];

          // Fetch active sessions for this organization
          const sessionsResponse = await ApiService.get(`/super/sessions/entity/${org.entityId}/active`);
          const activeSessions: AttendanceSession[] = sessionsResponse.data || [];

          // Separate assigned and unassigned cards
          const assignedCards = allCards.filter(card => card.subscriberId);
          const unassignedCards = allCards.filter(card => !card.subscriberId);

          entityData.push({
            organization: org,
            assignedCards,
            unassignedCards,
            activeSessions,
          });
        } catch (err) {
          logger.warn(`Failed to fetch data for entity ${org.entityId}`, 'NFC_ADMIN', err);
        }
      }

      setEntityCardData(entityData);
      logger.info('NFC cards data fetched successfully', 'NFC_ADMIN', { 
        entitiesCount: entityData.length,
        totalCards: entityData.reduce((sum, entity) => 
          sum + entity.assignedCards.length + entity.unassignedCards.length, 0)
      });
    } catch (err: any) {
      logger.error('Failed to fetch NFC cards data', 'NFC_ADMIN', err);
      setError('Failed to fetch NFC cards data. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAllCards = async () => {
    try {
      await ApiService.delete('/super/cards/all');
      setSuccessMessage('All NFC cards deleted successfully!');
      setShowDeleteAllDialog(false);
      fetchAllData();
      logger.userAction('All NFC cards deleted', 'NFC_ADMIN');
    } catch (err: any) {
      logger.error('Failed to delete all cards', 'NFC_ADMIN', err);
      setError(err.response?.data?.error || 'Failed to delete all cards');
      setShowDeleteAllDialog(false);
    }
  };

  const handleSwipeSimulation = async () => {
    if (!selectedCard || !selectedSession) {
      setError('Please select both a card and a session');
      return;
    }

    setSwipeLoading(true);
    try {
      const swipeData = {
        cardUid: selectedCard.cardUid,
        sessionId: selectedSession.id,
      };

      const response = await ApiService.post('/super/simulate-swipe', swipeData);
      
      setSuccessMessage(
        `Swipe simulation successful! ${response.data.action} for ${selectedCard.subscriberName} in session "${selectedSession.name}"`
      );
      setShowSwipeDialog(false);
      setSelectedCard(null);
      setSelectedSession(null);
      
      logger.userAction('NFC swipe simulated', 'NFC_ADMIN', {
        cardUid: selectedCard.cardUid,
        sessionId: selectedSession.id,
        action: response.data.action,
      });
    } catch (err: any) {
      logger.error('Failed to simulate swipe', 'NFC_ADMIN', err);
      setError(err.response?.data?.error || 'Failed to simulate swipe');
    } finally {
      setSwipeLoading(false);
    }
  };

  const openSwipeDialog = (card: NfcCard, sessions: AttendanceSession[]) => {
    if (!card.subscriberId) {
      setError('Cannot simulate swipe for unassigned card');
      return;
    }
    if (sessions.length === 0) {
      setError('No active sessions available for this entity');
      return;
    }
    setSelectedCard(card);
    setSelectedSession(sessions[0]); // Default to first session
    setShowSwipeDialog(true);
  };

  const filteredEntityData = entityCardData.filter(entity =>
    entity.organization.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    entity.organization.entityId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    entity.assignedCards.some(card => 
      card.cardUid.toLowerCase().includes(searchTerm.toLowerCase()) ||
      card.subscriberName?.toLowerCase().includes(searchTerm.toLowerCase())
    ) ||
    entity.unassignedCards.some(card => 
      card.cardUid.toLowerCase().includes(searchTerm.toLowerCase())
    )
  );

  const totalCards = entityCardData.reduce((sum, entity) => 
    sum + entity.assignedCards.length + entity.unassignedCards.length, 0);
  const totalAssigned = entityCardData.reduce((sum, entity) => sum + entity.assignedCards.length, 0);
  const totalUnassigned = entityCardData.reduce((sum, entity) => sum + entity.unassignedCards.length, 0);

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
          NFC Cards Management & Simulation
        </Typography>
        <Button
          variant="contained"
          color="error"
          startIcon={<Delete />}
          onClick={() => setShowDeleteAllDialog(true)}
          sx={{
            background: 'linear-gradient(45deg, #f44336 30%, #ff5722 90%)',
            '&:hover': {
              background: 'linear-gradient(45deg, #d32f2f 30%, #f44336 90%)',
            },
          }}
        >
          Delete All Cards
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
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Business color="primary" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {entityCardData.length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Entities
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Nfc color="primary" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalCards}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Total Cards
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <CheckCircle color="success" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalAssigned}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Assigned Cards
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Cancel color="warning" sx={{ fontSize: 40 }} />
                <Box>
                  <Typography variant="h4" fontWeight="bold">
                    {totalUnassigned}
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
            placeholder="Search by entity name, entity ID, card UID, or subscriber name..."
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

      {/* Entity Cards Data */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box>
          {filteredEntityData.map((entityData) => (
            <Accordion key={entityData.organization.id} sx={{ mb: 2 }}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                  <Business color="primary" />
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="h6" fontWeight="bold">
                      {entityData.organization.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Entity ID: {entityData.organization.entityId}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Chip
                      icon={<CheckCircle />}
                      label={`${entityData.assignedCards.length} Assigned`}
                      color="success"
                      size="small"
                    />
                    <Chip
                      icon={<Cancel />}
                      label={`${entityData.unassignedCards.length} Unassigned`}
                      color="warning"
                      size="small"
                    />
                    <Chip
                      icon={<Schedule />}
                      label={`${entityData.activeSessions.length} Active Sessions`}
                      color="info"
                      size="small"
                    />
                  </Box>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={3}>
                  {/* Assigned Cards */}
                  <Grid item xs={12} md={6}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" fontWeight="bold" sx={{ mb: 2, color: 'success.main' }}>
                        Assigned Cards ({entityData.assignedCards.length})
                      </Typography>
                      {entityData.assignedCards.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">
                          No assigned cards
                        </Typography>
                      ) : (
                        <TableContainer>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell>Card UID</TableCell>
                                <TableCell>Subscriber</TableCell>
                                <TableCell>Contact</TableCell>
                                <TableCell align="center">Actions</TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {entityData.assignedCards.map((card) => (
                                <TableRow key={card.id} hover>
                                  <TableCell>
                                    <Typography variant="body2" fontFamily="monospace" fontWeight="bold">
                                      {card.cardUid}
                                    </Typography>
                                  </TableCell>
                                  <TableCell>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                      <Person />
                                      <Typography variant="body2">
                                        {card.subscriberName}
                                      </Typography>
                                    </Box>
                                  </TableCell>
                                  <TableCell>
                                    <Typography variant="body2">
                                      {card.subscriberMobileNumber}
                                    </Typography>
                                  </TableCell>
                                  <TableCell align="center">
                                    <Tooltip title="Simulate Swipe">
                                      <IconButton
                                        color="primary"
                                        onClick={() => openSwipeDialog(card, entityData.activeSessions)}
                                        size="small"
                                        disabled={entityData.activeSessions.length === 0}
                                      >
                                        <TouchApp />
                                      </IconButton>
                                    </Tooltip>
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </Paper>
                  </Grid>

                  {/* Unassigned Cards */}
                  <Grid item xs={12} md={6}>
                    <Paper sx={{ p: 2 }}>
                      <Typography variant="h6" fontWeight="bold" sx={{ mb: 2, color: 'warning.main' }}>
                        Unassigned Cards ({entityData.unassignedCards.length})
                      </Typography>
                      {entityData.unassignedCards.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">
                          No unassigned cards
                        </Typography>
                      ) : (
                        <TableContainer>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell>Card UID</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell align="center">Actions</TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {entityData.unassignedCards.map((card) => (
                                <TableRow key={card.id} hover>
                                  <TableCell>
                                    <Typography variant="body2" fontFamily="monospace" fontWeight="bold">
                                      {card.cardUid}
                                    </Typography>
                                  </TableCell>
                                  <TableCell>
                                    <Chip
                                      label={card.isActive ? 'Active' : 'Inactive'}
                                      color={card.isActive ? 'success' : 'error'}
                                      size="small"
                                    />
                                  </TableCell>
                                  <TableCell align="center">
                                    <Typography variant="body2" color="text.secondary">
                                      Not assigned
                                    </Typography>
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </Paper>
                  </Grid>
                </Grid>

                {/* Active Sessions */}
                {entityData.activeSessions.length > 0 && (
                  <Box sx={{ mt: 3 }}>
                    <Divider sx={{ mb: 2 }} />
                    <Typography variant="h6" fontWeight="bold" sx={{ mb: 2, color: 'info.main' }}>
                      Active Sessions ({entityData.activeSessions.length})
                    </Typography>
                    <Grid container spacing={2}>
                      {entityData.activeSessions.map((session) => (
                        <Grid item xs={12} sm={6} md={4} key={session.id}>
                          <Card variant="outlined">
                            <CardContent>
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                <Schedule color="info" />
                                <Typography variant="subtitle2" fontWeight="bold">
                                  {session.name}
                                </Typography>
                              </Box>
                              <Typography variant="body2" color="text.secondary">
                                Started: {new Date(session.startTime).toLocaleString()}
                              </Typography>
                            </CardContent>
                          </Card>
                        </Grid>
                      ))}
                    </Grid>
                  </Box>
                )}
              </AccordionDetails>
            </Accordion>
          ))}
        </Box>
      )}

      {/* Delete All Cards Confirmation Dialog */}
      <Dialog open={showDeleteAllDialog} onClose={() => setShowDeleteAllDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Warning sx={{ color: 'error.main', fontSize: 40 }} />
            <Typography variant="h6" component="span">
              Delete All NFC Cards
            </Typography>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Are you sure you want to delete ALL NFC cards from the system?
          </Typography>
          <Typography variant="body2" color="error" sx={{ mb: 2 }}>
            This action will permanently delete {totalCards} cards across all entities and cannot be undone.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            This includes:
            • {totalAssigned} assigned cards
            • {totalUnassigned} unassigned cards
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button onClick={() => setShowDeleteAllDialog(false)} variant="outlined">
            Cancel
          </Button>
          <Button onClick={handleDeleteAllCards} variant="contained" color="error">
            Delete All Cards
          </Button>
        </DialogActions>
      </Dialog>

      {/* Swipe Simulation Dialog */}
      <Dialog open={showSwipeDialog} onClose={() => setShowSwipeDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <TouchApp sx={{ color: 'primary.main', fontSize: 40 }} />
            <Typography variant="h6" component="span">
              Simulate NFC Swipe
            </Typography>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedCard && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>
                Card Details:
              </Typography>
              <Typography variant="body2">
                <strong>UID:</strong> {selectedCard.cardUid}
              </Typography>
              <Typography variant="body2">
                <strong>Subscriber:</strong> {selectedCard.subscriberName}
              </Typography>
              <Typography variant="body2">
                <strong>Contact:</strong> {selectedCard.subscriberMobileNumber}
              </Typography>
            </Box>
          )}
          
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>
            Select Session:
          </Typography>
          {entityCardData
            .find(e => e.organization.entityId === selectedCard?.entityId)
            ?.activeSessions.map((session) => (
              <Box
                key={session.id}
                sx={{
                  p: 2,
                  border: 1,
                  borderColor: selectedSession?.id === session.id ? 'primary.main' : 'grey.300',
                  borderRadius: 1,
                  mb: 1,
                  cursor: 'pointer',
                  '&:hover': {
                    borderColor: 'primary.main',
                  },
                }}
                onClick={() => setSelectedSession(session)}
              >
                <Typography variant="body2" fontWeight="bold">
                  {session.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Started: {new Date(session.startTime).toLocaleString()}
                </Typography>
              </Box>
            ))}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button onClick={() => setShowSwipeDialog(false)} variant="outlined">
            Cancel
          </Button>
          <Button
            onClick={handleSwipeSimulation}
            variant="contained"
            disabled={!selectedSession || swipeLoading}
            startIcon={swipeLoading ? <CircularProgress size={20} /> : <PlayArrow />}
          >
            {swipeLoading ? 'Simulating...' : 'Simulate Swipe'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default NfcCardsPage;
