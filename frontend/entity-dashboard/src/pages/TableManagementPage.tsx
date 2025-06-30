import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Alert,
  CircularProgress,
  Chip,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Fab,
  Tooltip,
  Paper
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  TableRestaurant as TableIcon,
  QrCode as QrCodeIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  Print as PrintIcon,
  Visibility as ViewIcon,
  AutoFixHigh as GenerateIcon
} from '@mui/icons-material';
import ApiService from '../services/ApiService';

interface RestaurantTable {
  id: number;
  tableNumber: number;
  qrCode: string;
  qrCodeUrl?: string;
  isActive: boolean;
  capacity?: number;
  locationDescription?: string;
  menuUrl?: string;
  organizationEntityId?: string;
  createdAt: string;
  updatedAt: string;
}

const TableManagementPage: React.FC = () => {
  const [tables, setTables] = useState<RestaurantTable[]>([]);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [generateDialogOpen, setGenerateDialogOpen] = useState(false);
  const [qrDialogOpen, setQrDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // Form states
  const [formData, setFormData] = useState({
    tableNumber: 0,
    capacity: 4,
    locationDescription: '',
    isActive: true
  });

  const [numberOfTables, setNumberOfTables] = useState(10);
  const [selectedTable, setSelectedTable] = useState<RestaurantTable | null>(null);

  // Menu states
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [menuSelectedTable, setMenuSelectedTable] = useState<RestaurantTable | null>(null);

  useEffect(() => {
    checkTablePermission();
  }, []);

  useEffect(() => {
    if (hasPermission) {
      fetchTables();
    }
  }, [hasPermission]);

  const checkTablePermission = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get('/api/entity/permissions/check/TABLE_MANAGEMENT');
      setHasPermission(response.data.hasPermission);
    } catch (err: any) {
      console.error('Failed to check table permission:', err);
      setError('Failed to check permissions');
      setHasPermission(false);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchTables = async () => {
    setError(null);
    try {
      const response = await ApiService.get<RestaurantTable[]>('/api/tables');
      setTables(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch tables:', err);
      setError(err.response?.data?.error || 'Failed to fetch tables');
    }
  };

  const handleOpenDialog = (table?: RestaurantTable) => {
    if (table) {
      setIsEditing(true);
      setEditingId(table.id);
      setFormData({
        tableNumber: table.tableNumber,
        capacity: table.capacity || 4,
        locationDescription: table.locationDescription || '',
        isActive: table.isActive
      });
    } else {
      setIsEditing(false);
      setEditingId(null);
      const nextTableNumber = Math.max(...tables.map(t => t.tableNumber), 0) + 1;
      setFormData({
        tableNumber: nextTableNumber,
        capacity: 4,
        locationDescription: '',
        isActive: true
      });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setIsEditing(false);
    setEditingId(null);
  };

  const handleSubmit = async () => {
    setError(null);
    setSuccessMessage(null);

    if (formData.tableNumber <= 0) {
      setError('Table number must be greater than 0');
      return;
    }

    try {
      if (isEditing && editingId) {
        await ApiService.put(`/api/tables/${editingId}`, formData);
        setSuccessMessage('Table updated successfully!');
      } else {
        await ApiService.post('/api/tables', formData);
        setSuccessMessage('Table created successfully!');
      }
      
      handleCloseDialog();
      fetchTables();
    } catch (err: any) {
      console.error('Failed to save table:', err);
      setError(err.response?.data?.error || 'Failed to save table');
    }
  };

  const handleGenerateTables = async () => {
    setError(null);
    setSuccessMessage(null);

    if (numberOfTables <= 0 || numberOfTables > 100) {
      setError('Number of tables must be between 1 and 100');
      return;
    }

    try {
      const response = await ApiService.post('/api/tables/generate', { numberOfTables });
      setSuccessMessage(`Generated ${numberOfTables} tables successfully!`);
      setGenerateDialogOpen(false);
      fetchTables();
    } catch (err: any) {
      console.error('Failed to generate tables:', err);
      setError(err.response?.data?.error || 'Failed to generate tables');
    }
  };

  const handleDelete = async (tableId: number) => {
    if (!window.confirm('Are you sure you want to delete this table? This action cannot be undone.')) {
      return;
    }

    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.delete(`/api/tables/${tableId}`);
      setSuccessMessage('Table deleted successfully!');
      fetchTables();
    } catch (err: any) {
      console.error('Failed to delete table:', err);
      setError(err.response?.data?.error || 'Failed to delete table');
    }
  };

  const handleRegenerateQR = async (tableId: number) => {
    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.post(`/api/tables/${tableId}/regenerate-qr`);
      setSuccessMessage('QR code regenerated successfully!');
      fetchTables();
    } catch (err: any) {
      console.error('Failed to regenerate QR code:', err);
      setError(err.response?.data?.error || 'Failed to regenerate QR code');
    }
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>, table: RestaurantTable) => {
    setAnchorEl(event.currentTarget);
    setMenuSelectedTable(table);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setMenuSelectedTable(null);
  };

  const handleViewQR = (table: RestaurantTable) => {
    setSelectedTable(table);
    setQrDialogOpen(true);
    handleMenuClose();
  };

  const handleInputChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
        <Typography variant="h6" sx={{ ml: 2 }}>
          Checking permissions...
        </Typography>
      </Box>
    );
  }

  if (!hasPermission) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning" sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Table Management Access Restricted
          </Typography>
          <Typography variant="body1">
            You don't have permission to access the Table Management system. 
            Please contact your super administrator to request access.
          </Typography>
        </Alert>
        
        <Card sx={{ mt: 3 }}>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <TableIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h5" color="text.secondary" gutterBottom>
              Table Management Unavailable
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Contact your administrator to enable table management features.
            </Typography>
            <Button 
              variant="outlined" 
              onClick={checkTablePermission}
              startIcon={<TableIcon />}
            >
              Check Permissions Again
            </Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" component="h1" fontWeight="bold" color="primary">
            Table Management
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage restaurant tables and generate QR codes for ordering
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            onClick={fetchTables}
            startIcon={<RefreshIcon />}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            onClick={() => setGenerateDialogOpen(true)}
            startIcon={<GenerateIcon />}
          >
            Generate Tables
          </Button>
        </Box>
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

      {/* Table Stats */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <TableIcon sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {tables.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Tables
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <QrCodeIcon sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {tables.filter(t => t.isActive).length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Active Tables
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <ViewIcon sx={{ fontSize: 40, color: 'info.main', mb: 1 }} />
              <Typography variant="h4" fontWeight="bold">
                {tables.reduce((sum, t) => sum + (t.capacity || 4), 0)}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Capacity
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tables Grid */}
      {tables.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <TableIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No Tables Found
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Get started by generating tables or adding them individually
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
              <Button
                variant="contained"
                startIcon={<GenerateIcon />}
                onClick={() => setGenerateDialogOpen(true)}
              >
                Generate Tables
              </Button>
              <Button
                variant="outlined"
                startIcon={<AddIcon />}
                onClick={() => handleOpenDialog()}
              >
                Add Single Table
              </Button>
            </Box>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {tables.map((table) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={table.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TableIcon color="primary" />
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          Table {table.tableNumber}
                        </Typography>
                        <Chip
                          size="small"
                          label={table.isActive ? 'Active' : 'Inactive'}
                          color={table.isActive ? 'success' : 'default'}
                        />
                      </Box>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={(e) => handleMenuClick(e, table)}
                    >
                      <MoreVertIcon />
                    </IconButton>
                  </Box>

                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Capacity: {table.capacity || 4} people
                    </Typography>
                    {table.locationDescription && (
                      <Typography variant="body2" color="text.secondary">
                        Location: {table.locationDescription}
                      </Typography>
                    )}
                    {table.qrCodeUrl && (
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{
                          display: 'block',
                          mt: 1,
                          wordBreak: 'break-all',
                          fontSize: '0.7rem',
                          lineHeight: 1.2
                        }}
                      >
                        URL: {table.menuUrl || `http://restaurant.local:8080/menu.html?entityId=${table.organizationEntityId}&table=${table.tableNumber}&qr=${table.qrCode}`}
                      </Typography>
                    )}
                  </Box>



                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Button
                      size="small"
                      startIcon={<QrCodeIcon />}
                      onClick={() => handleViewQR(table)}
                      disabled={!table.qrCodeUrl}
                      color={table.qrCodeUrl ? 'primary' : 'inherit'}
                    >
                      {table.qrCodeUrl ? 'View QR' : 'No QR'}
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                      #{table.qrCode.slice(-6)}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => {
          handleViewQR(menuSelectedTable!);
        }}>
          <ListItemIcon>
            <QrCodeIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>View QR Code</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => {
          handleOpenDialog(menuSelectedTable!);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit Table</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => {
          handleRegenerateQR(menuSelectedTable!.id);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <RefreshIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Regenerate QR</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => {
          handleDelete(menuSelectedTable!.id);
          handleMenuClose();
        }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Delete Table</ListItemText>
        </MenuItem>
      </Menu>

      {/* Add/Edit Table Dialog */}
      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {isEditing ? 'Edit Table' : 'Add New Table'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Table Number"
              type="number"
              value={formData.tableNumber}
              onChange={(e) => handleInputChange('tableNumber', parseInt(e.target.value) || 0)}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Capacity (Number of People)"
              type="number"
              value={formData.capacity}
              onChange={(e) => handleInputChange('capacity', parseInt(e.target.value) || 4)}
              margin="normal"
            />
            <TextField
              fullWidth
              label="Location Description (Optional)"
              value={formData.locationDescription}
              onChange={(e) => handleInputChange('locationDescription', e.target.value)}
              margin="normal"
              placeholder="e.g., Near window, Corner table, etc."
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained"
            disabled={formData.tableNumber <= 0}
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Generate Tables Dialog */}
      <Dialog open={generateDialogOpen} onClose={() => setGenerateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate Multiple Tables</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <Alert severity="warning" sx={{ mb: 2 }}>
              This will replace all existing tables. This action cannot be undone.
            </Alert>
            <TextField
              fullWidth
              label="Number of Tables"
              type="number"
              value={numberOfTables}
              onChange={(e) => setNumberOfTables(parseInt(e.target.value) || 10)}
              margin="normal"
              inputProps={{ min: 1, max: 100 }}
              helperText="Enter a number between 1 and 100"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGenerateDialogOpen(false)}>Cancel</Button>
          <Button 
            onClick={handleGenerateTables} 
            variant="contained"
            color="warning"
            disabled={numberOfTables <= 0 || numberOfTables > 100}
          >
            Generate {numberOfTables} Tables
          </Button>
        </DialogActions>
      </Dialog>

      {/* QR Code Dialog */}
      <Dialog open={qrDialogOpen} onClose={() => setQrDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          QR Code - Table {selectedTable?.tableNumber}
        </DialogTitle>
        <DialogContent>
          {selectedTable && (
            <Box sx={{ textAlign: 'center', pt: 2 }}>
              <Paper sx={{ p: 3, mb: 2, bgcolor: 'grey.50' }}>
                <Typography variant="h6" gutterBottom>
                  Table {selectedTable.tableNumber}
                </Typography>
                <Box sx={{
                  width: 200,
                  height: 200,
                  bgcolor: 'white',
                  border: '1px solid #ddd',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  mx: 'auto',
                  mb: 2
                }}>
                  {selectedTable.qrCodeUrl ? (
                    <img
                      src={selectedTable.qrCodeUrl}
                      alt={`QR Code for Table ${selectedTable.tableNumber}`}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'contain'
                      }}
                      onError={(e) => {
                        // Fallback to icon if image fails to load
                        e.currentTarget.style.display = 'none';
                        e.currentTarget.nextElementSibling?.setAttribute('style', 'display: block');
                      }}
                    />
                  ) : null}
                  <QrCodeIcon
                    sx={{
                      fontSize: 100,
                      color: 'text.secondary',
                      display: selectedTable.qrCodeUrl ? 'none' : 'block'
                    }}
                  />
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  QR Code: {selectedTable.qrCode}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1, wordBreak: 'break-all' }}>
                  URL: {selectedTable.menuUrl || `http://restaurant.local:8080/menu.html?entityId=${selectedTable.organizationEntityId}&table=${selectedTable.tableNumber}&qr=${selectedTable.qrCode}`}
                </Typography>
              </Paper>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Customers can scan this QR code to access the menu and place orders for this table.
                <br />
                <strong>Manual Access:</strong> If scanning doesn't work, you can manually open the URL above in your mobile browser.
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
                <Button
                  variant="outlined"
                  startIcon={<DownloadIcon />}
                  onClick={() => {
                    if (selectedTable?.qrCodeUrl) {
                      const link = document.createElement('a');
                      link.href = selectedTable.qrCodeUrl;
                      link.download = `table-${selectedTable.tableNumber}-qr-code.png`;
                      document.body.appendChild(link);
                      link.click();
                      document.body.removeChild(link);
                    }
                  }}
                  disabled={!selectedTable?.qrCodeUrl}
                >
                  Download
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<PrintIcon />}
                  onClick={() => {
                    if (selectedTable?.qrCodeUrl) {
                      const printWindow = window.open('', '_blank');
                      if (printWindow) {
                        printWindow.document.write(`
                          <html>
                            <head>
                              <title>Table ${selectedTable.tableNumber} QR Code</title>
                              <style>
                                body { text-align: center; font-family: Arial, sans-serif; }
                                img { max-width: 300px; max-height: 300px; }
                                h2 { margin-bottom: 20px; }
                              </style>
                            </head>
                            <body>
                              <h2>Table ${selectedTable.tableNumber} QR Code</h2>
                              <img src="${selectedTable.qrCodeUrl}" alt="QR Code" />
                              <p>QR Code: ${selectedTable.qrCode}</p>
                            </body>
                          </html>
                        `);
                        printWindow.document.close();
                        printWindow.print();
                      }
                    }
                  }}
                  disabled={!selectedTable?.qrCodeUrl}
                >
                  Print
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => {
                    if (selectedTable?.qrCodeUrl) {
                      navigator.clipboard.writeText(selectedTable.qrCodeUrl).then(() => {
                        setSuccessMessage('QR code image URL copied to clipboard!');
                      }).catch(() => {
                        setError('Failed to copy URL to clipboard');
                      });
                    }
                  }}
                  disabled={!selectedTable?.qrCodeUrl}
                >
                  Copy QR Image URL
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => {
                    if (selectedTable) {
                      const menuUrl = selectedTable.menuUrl || `http://restaurant.local:8080/menu.html?entityId=${selectedTable.organizationEntityId}&table=${selectedTable.tableNumber}&qr=${selectedTable.qrCode}`;
                      navigator.clipboard.writeText(menuUrl).then(() => {
                        setSuccessMessage('Menu URL copied to clipboard!');
                      }).catch(() => {
                        setError('Failed to copy URL to clipboard');
                      });
                    }
                  }}
                  disabled={!selectedTable}
                >
                  Copy Menu URL
                </Button>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setQrDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Floating Action Button */}
      <Fab
        color="primary"
        aria-label="add table"
        sx={{ position: 'fixed', bottom: 24, right: 24 }}
        onClick={() => handleOpenDialog()}
      >
        <AddIcon />
      </Fab>
    </Box>
  );
};

export default TableManagementPage;
