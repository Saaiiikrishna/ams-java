import React, { useState, useEffect, FormEvent } from 'react';
import ApiService from '../services/ApiService';

interface Organization {
  id: number;
  name: string;
  address: string;
  latitude?: number;
  longitude?: number;
  contactPerson?: string;
  email?: string;
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
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state for creating new entity
  const [newName, setNewName] = useState('');
  const [newAddress, setNewAddress] = useState('');
  const [newLatitude, setNewLatitude] = useState('');
  const [newLongitude, setNewLongitude] = useState('');
  const [newContact, setNewContact] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);


  // Fetch entities
  const fetchEntities = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<Organization[]>('/admin/entities');
      setEntities(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch entities:", err);
      if (err.response && err.response.status === 403) {
        setError('Failed to fetch entities: You do not have permission.');
      } else {
        setError('Failed to fetch entities. Please try again later.');
      }
      setEntities([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEntities();
    // console.warn("EntityPage: GET /admin/entities endpoint is not yet implemented on backend. Data fetching is disabled.");
  }, []);

  const handleCreateSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    const newEntityData: NewOrganization = {
      name: newName,
      address: newAddress,
      latitude: newLatitude ? parseFloat(newLatitude) : undefined,
      longitude: newLongitude ? parseFloat(newLongitude) : undefined,
      contactPerson: newContact || undefined,
      email: newEmail || undefined,
    };
    try {
      const response = await ApiService.post('/admin/entities', newEntityData);
      // Assuming the backend returns the created entity
      setEntities(prevEntities => [...prevEntities, response.data]);
      setNewName('');
      setNewAddress('');
      setNewLatitude('');
      setNewLongitude('');
      setNewContact('');
      setNewEmail('');
      setShowCreateForm(false);
      fetchEntities(); // Re-fetch the list after successful creation
    } catch (err: any) {
      console.error("Failed to create entity:", err);
      if (err.response && err.response.data) {
         if (typeof err.response.data === 'string') { // Backend might send plain string error
            setError(`Failed to create entity: ${err.response.data}`);
         } else if (err.response.data.message) { // Or a JSON object with a message field
            setError(`Failed to create entity: ${err.response.data.message}`);
         } else {
            setError('Failed to create entity: An unexpected error occurred. Check console.');
         }
      } else {
        setError('Failed to create entity. An unexpected error occurred.');
      }
    }
  };

  return (
    <div>
      <h2>Entity Management</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <button onClick={() => setShowCreateForm(!showCreateForm)} style={{ marginBottom: '15px' }}>
        {showCreateForm ? 'Cancel' : 'Create New Entity'}
      </button>

      {showCreateForm && (
        <form onSubmit={handleCreateSubmit} style={{ marginBottom: '20px', padding: '15px', border: '1px solid #eee' }}>
          <h3>Create New Entity</h3>
          <div style={{ marginBottom: '10px' }}>
            <label htmlFor="newName" style={{ marginRight: '10px' }}>Name:</label>
            <input
              type="text"
              id="newName"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              required
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <label htmlFor="newAddress" style={{ marginRight: '10px' }}>Address:</label>
            <input
              type="text"
              id="newAddress"
              value={newAddress}
              onChange={(e) => setNewAddress(e.target.value)}
              required
            />
          </div>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
            <input
              type="number"
              step="any"
              placeholder="Latitude"
              value={newLatitude}
              onChange={(e) => setNewLatitude(e.target.value)}
            />
            <input
              type="number"
              step="any"
              placeholder="Longitude"
              value={newLongitude}
              onChange={(e) => setNewLongitude(e.target.value)}
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <input
              type="text"
              placeholder="Contact Person"
              value={newContact}
              onChange={(e) => setNewContact(e.target.value)}
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <input
              type="email"
              placeholder="Contact Email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
            />
          </div>
          <button type="submit">Save Entity</button>
        </form>
      )}

      <h3>Existing Entities</h3>
      {isLoading && <p>Loading entities...</p>}
      {(!isLoading && entities.length === 0 && !error) && <p>No entities found.</p>}
      <ul style={{ listStyleType: 'none', padding: 0 }}>
        {entities.map(entity => (
          <li key={entity.id} style={{ marginBottom: '10px', padding: '10px', border: '1px solid #ddd' }}>
            <strong>ID: {entity.id} - {entity.name}</strong><br />
            <span>{entity.address}</span><br />
            {entity.latitude && entity.longitude && (
              <span>Coords: {entity.latitude}, {entity.longitude}</span>
            )}<br />
            {entity.contactPerson && <span>Contact: {entity.contactPerson}</span>}<br />
            {entity.email && <span>Email: {entity.email}</span>}
            {/* Add view/update/delete buttons here if implementing */}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default EntityPage;
