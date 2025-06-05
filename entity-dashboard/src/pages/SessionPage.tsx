import React, { useState, useEffect, FormEvent } from 'react';
import ApiService from '../services/ApiService';

interface AttendanceSession {
  id: number;
  name: string;
  startTime: string; // ISO string, consider formatting for display
  endTime?: string | null; // ISO string or null
  organizationId?: number;
  // attendanceLogs count or list could be part of a more detailed view
}

interface NewAttendanceSession {
  name: string;
  startTime?: string; // Optional, backend might default to now if not provided
}

const SessionPage: React.FC = () => {
  const [sessions, setSessions] = useState<AttendanceSession[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form state for creating new session
  const [sessionName, setSessionName] = useState('');
  const [sessionStartTime, setSessionStartTime] = useState(''); // Store as string for input type datetime-local

  const [showCreateForm, setShowCreateForm] = useState(false);

  const resetForm = () => {
    setSessionName('');
    setSessionStartTime('');
    setShowCreateForm(false);
    setError(null);
    setSuccessMessage(null);
  };

  // TODO: The backend /entity/sessions GET endpoint is not defined in the current plan.
  // The EntityController has POST /entity/sessions and PUT /entity/sessions/{id}/end.
  // A GET /entity/sessions endpoint would be needed to list sessions.
  const fetchSessions = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await ApiService.get<AttendanceSession[]>('/entity/sessions');
      setSessions(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch sessions:", err);
      setError(err.response?.data?.message || 'Failed to fetch sessions.');
      setSessions([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
    // console.warn("SessionPage: GET /entity/sessions endpoint is not implemented or confirmed on backend. Data fetching for session list is disabled.");
  }, []);

  const handleCreateSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);

    const newSessionData: NewAttendanceSession = { name: sessionName };
    if (sessionStartTime) {
      newSessionData.startTime = new Date(sessionStartTime).toISOString();
    }

    try {
      const response = await ApiService.post<AttendanceSession>('/entity/sessions', newSessionData);
      setSuccessMessage(`Session '${response.data.name}' created successfully!`);
      resetForm();
      fetchSessions(); // Refresh list
    } catch (err: any) {
      console.error("Failed to create session:", err);
      setError(err.response?.data?.message || 'Failed to create session.');
    }
  };

  const handleEndSession = async (sessionId: number) => {
    setError(null);
    setSuccessMessage(null);
    if (window.confirm('Are you sure you want to end this session?')) {
      try {
        const response = await ApiService.put<AttendanceSession>(`/entity/sessions/${sessionId}/end`, {});
        setSuccessMessage(`Session '${response.data.name}' (ID: ${response.data.id}) ended successfully.`);
        fetchSessions(); // Refresh list
      } catch (err: any) {
        console.error("Failed to end session:", err);
        setError(err.response?.data?.message || 'Failed to end session.');
      }
    }
  };

  const formatDateTime = (isoString?: string | null) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  }

  return (
    <div>
      <h2>Attendance Session Management</h2>
      {error && <p style={{ color: 'red', border: '1px solid red', padding: '10px' }}>Error: {error}</p>}
      {successMessage && <p style={{ color: 'green', border: '1px solid green', padding: '10px' }}>Success: {successMessage}</p>}

      <button onClick={() => { resetForm(); setShowCreateForm(!showCreateForm); }} style={{ marginBottom: '15px' }}>
        {showCreateForm ? 'Cancel Creation' : 'Create New Session'}
      </button>

      {showCreateForm && (
        <form onSubmit={handleCreateSubmit} style={{ marginBottom: '20px', padding: '15px', border: '1px solid #eee' }}>
          <h3>Create New Session</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <input type="text" placeholder="Session Name/Purpose" value={sessionName} onChange={(e) => setSessionName(e.target.value)} required />
            <input
              type="datetime-local"
              placeholder="Start Time (Optional)"
              value={sessionStartTime}
              onChange={(e) => setSessionStartTime(e.target.value)}
              title="If not set, defaults to current time on backend."
            />
            <button type="submit">Save Session</button>
          </div>
        </form>
      )}

      <h3>Sessions List</h3>
      {/* <p><em>Note: Listing existing sessions requires a GET /entity/sessions endpoint, which is currently not implemented on the backend. Only newly created sessions via this UI might appear below if not refreshed.</em></p> */}
      {isLoading && <p>Loading sessions...</p>}
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th style={tableHeaderStyle}>ID</th>
            <th style={tableHeaderStyle}>Name</th>
            <th style={tableHeaderStyle}>Start Time</th>
            <th style={tableHeaderStyle}>End Time</th>
            <th style={tableHeaderStyle}>Status</th>
            <th style={tableHeaderStyle}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {sessions.map(session => (
            <tr key={session.id}>
              <td style={tableCellStyle}>{session.id}</td>
              <td style={tableCellStyle}>{session.name}</td>
              <td style={tableCellStyle}>{formatDateTime(session.startTime)}</td>
              <td style={tableCellStyle}>{formatDateTime(session.endTime)}</td>
              <td style={tableCellStyle}>{session.endTime ? 'Ended' : 'Active'}</td>
              <td style={tableCellStyle}>
                {!session.endTime && (
                  <button onClick={() => handleEndSession(session.id)}>End Session</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {(!isLoading && sessions.length === 0 && !error) && <p>No sessions found.</p>}

      {/* Placeholder for real-time NFC scans */}
      <div style={{marginTop: '30px', padding: '15px', border: '1px dashed #ccc'}}>
        <h4>Real-time NFC Scans (Placeholder)</h4>
        <p><em>This area would display incoming NFC scan events for the selected active session. Requires WebSocket or polling integration.</em></p>
      </div>
    </div>
  );
};

// Reusing styles from SubscriberPage for consistency
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

export default SessionPage;
