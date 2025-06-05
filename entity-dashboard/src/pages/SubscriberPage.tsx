import React, { useState, useEffect, FormEvent } from 'react';
import ApiService from '../services/ApiService';

interface Subscriber {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  nfcCardUid?: string; // From SubscriberDto
  organizationId?: number; // From SubscriberDto (though not directly used in form for new sub)
  // 'photoUrl' is conceptual for now as backend DTO doesn't have it
}

interface NewSubscriber {
  firstName: string;
  lastName: string;
  email: string;
  nfcCardUid?: string;
  photoUrl?: string; // Conceptual
}

const SubscriberPage: React.FC = () => {
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form state for creating/editing subscriber
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [nfcCardUid, setNfcCardUid] = useState('');
  const [photoUrl, setPhotoUrl] = useState(''); // Conceptual photo URL field

  const [showForm, setShowForm] = useState(false);

  const resetForm = () => {
    setIsEditing(false);
    setEditingId(null);
    setFirstName('');
    setLastName('');
    setEmail('');
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
      const response = await ApiService.get<Subscriber[]>('/entity/subscribers');
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

    const subscriberData: NewSubscriber = { firstName, lastName, email, nfcCardUid, photoUrl };

    try {
      if (isEditing && editingId) {
        const response = await ApiService.put<Subscriber>(`/entity/subscribers/${editingId}`, subscriberData);
        setSuccessMessage(`Subscriber '${response.data.firstName}' updated successfully!`);
      } else {
        const response = await ApiService.post<Subscriber>('/entity/subscribers', subscriberData);
        setSuccessMessage(`Subscriber '${response.data.firstName}' created successfully!`);
      }
      resetForm();
      fetchSubscribers(); // Refresh list
    } catch (err: any) {
      console.error("Failed to save subscriber:", err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to save subscriber.');
    }
  };

  const handleEdit = (subscriber: Subscriber) => {
    setIsEditing(true);
    setEditingId(subscriber.id);
    setFirstName(subscriber.firstName);
    setLastName(subscriber.lastName);
    setEmail(subscriber.email);
    setNfcCardUid(subscriber.nfcCardUid || '');
    // setPhotoUrl(subscriber.photoUrl || ''); // If photoUrl was part of Subscriber interface
    setShowForm(true);
    setSuccessMessage(null);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this subscriber?')) {
      setError(null);
      setSuccessMessage(null);
      try {
        await ApiService.delete(`/entity/subscribers/${id}`);
        setSuccessMessage('Subscriber deleted successfully!');
        fetchSubscribers(); // Refresh list
      } catch (err: any) {
        console.error("Failed to delete subscriber:", err);
        setError(err.response?.data?.message || 'Failed to delete subscriber.');
      }
    }
  };


  return (
    <div>
      <h2>Subscriber Management</h2>
      {error && <p style={{ color: 'red', border: '1px solid red', padding: '10px' }}>Error: {error}</p>}
      {successMessage && <p style={{ color: 'green', border: '1px solid green', padding: '10px' }}>Success: {successMessage}</p>}

      <button onClick={() => { resetForm(); setShowForm(!showForm); }} style={{ marginBottom: '15px' }}>
        {showForm && !isEditing ? 'Cancel' : 'Add New Subscriber'}
      </button>

      {showForm && (
        <form onSubmit={handleSubmit} style={{ marginBottom: '20px', padding: '15px', border: '1px solid #eee' }}>
          <h3>{isEditing ? 'Edit Subscriber' : 'Create New Subscriber'}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <input type="text" placeholder="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            <input type="text" placeholder="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
            <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <input type="text" placeholder="NFC Card UID (Optional)" value={nfcCardUid} onChange={(e) => setNfcCardUid(e.target.value)} />
            <input type="text" placeholder="Photo URL (Optional)" value={photoUrl} onChange={(e) => setPhotoUrl(e.target.value)} />
            <div style={{display: 'flex', gap: '10px'}}>
                <button type="submit">{isEditing ? 'Update Subscriber' : 'Save Subscriber'}</button>
                {isEditing && <button type="button" onClick={resetForm}>Cancel Edit</button>}
            </div>
          </div>
        </form>
      )}

      <h3>Existing Subscribers</h3>
      {isLoading && <p>Loading subscribers...</p>}
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th style={tableHeaderStyle}>ID</th>
            <th style={tableHeaderStyle}>Name</th>
            <th style={tableHeaderStyle}>Email</th>
            <th style={tableHeaderStyle}>NFC Card UID</th>
            <th style={tableHeaderStyle}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {subscribers.map(sub => (
            <tr key={sub.id}>
              <td style={tableCellStyle}>{sub.id}</td>
              <td style={tableCellStyle}>{sub.firstName} {sub.lastName}</td>
              <td style={tableCellStyle}>{sub.email}</td>
              <td style={tableCellStyle}>{sub.nfcCardUid || 'N/A'}</td>
              <td style={tableCellStyle}>
                <button onClick={() => handleEdit(sub)} style={{ marginRight: '5px' }}>Edit</button>
                <button onClick={() => handleDelete(sub.id)} style={{ backgroundColor: '#dc3545', color: 'white' }}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {(!isLoading && subscribers.length === 0 && !error) && <p>No subscribers found.</p>}
    </div>
  );
};

const tableHeaderStyle: React.CSSProperties = {
  borderBottom: '2px solid #dee2e6',
  padding: '8px',
  textAlign: 'left',
  backgroundColor: '#f8f9fa'
};

const tableCellStyle: React.CSSProperties = {
  borderBottom: '1px solid #e9ecef',
  padding: '8px',
  textAlign: 'left'
};

export default SubscriberPage;
