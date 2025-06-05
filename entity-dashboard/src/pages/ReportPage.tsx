import React, { useState, useEffect, FormEvent } from 'react';
import ApiService from '../services/ApiService';

// DTOs (simplified for frontend representation)
interface SubscriberBasic {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

interface SessionBasic {
  id: number;
  name: string;
  startTime: string;
}

interface AttendanceLogFull {
  id: number;
  subscriberId: number;
  subscriberFirstName: string;
  subscriberLastName: string;
  subscriberEmail: string;
  sessionId: number;
  sessionName: string;
  checkInTime: string;
  checkOutTime?: string | null;
}

const ReportPage: React.FC = () => {
  const [sessions, setSessions] = useState<SessionBasic[]>([]);
  const [subscribers, setSubscribers] = useState<SubscriberBasic[]>([]);

  const [selectedSessionIdForAbsentees, setSelectedSessionIdForAbsentees] = useState<string>('');
  const [absentees, setAbsentees] = useState<SubscriberBasic[]>([]);
  const [isLoadingAbsentees, setIsLoadingAbsentees] = useState(false);
  const [absenteesError, setAbsenteesError] = useState<string|null>(null);

  const [selectedSubscriberIdForHistory, setSelectedSubscriberIdForHistory] = useState<string>('');
  const [attendanceHistory, setAttendanceHistory] = useState<AttendanceLogFull[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState<string|null>(null);
  const [startDate, setStartDate] = useState<string>(new Date(Date.now() - 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]); // Default to 1 year ago
  const [endDate, setEndDate] = useState<string>(new Date().toISOString().split('T')[0]); // Default to today

  // Fetch initial data for dropdowns
  useEffect(() => {
    const fetchInitialSessions = async () => {
      try {
        const response = await ApiService.get<SessionBasic[]>('/entity/sessions');
        setSessions(response.data || []);
        // console.warn("ReportPage: GET /entity/sessions for dropdown is disabled (backend endpoint likely missing).");
      } catch (error) {
        console.error("Error fetching sessions for report page:", error);
        // Optionally set an error state for session fetching
      }
    };

    const fetchInitialSubscribers = async () => {
      try {
        const response = await ApiService.get<SubscriberBasic[]>('/entity/subscribers');
        setSubscribers(response.data || []);
      } catch (error) {
        console.error("Error fetching subscribers for report page:", error);
      }
    };

    fetchInitialSessions();
    fetchInitialSubscribers();
  }, []);

  const handleFetchAbsentees = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedSessionIdForAbsentees) {
      setAbsenteesError("Please select a session.");
      return;
    }
    setIsLoadingAbsentees(true);
    setAbsenteesError(null);
    setAbsentees([]);
    try {
      const response = await ApiService.get<SubscriberBasic[]>(`/reports/sessions/${selectedSessionIdForAbsentees}/absentees`);
      setAbsentees(response.data || []);
    } catch (err:any) {
      console.error("Failed to fetch absentees:", err);
      setAbsenteesError(err.response?.data?.message || "Failed to fetch absentees.");
    } finally {
      setIsLoadingAbsentees(false);
    }
  };

  const handleFetchHistory = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedSubscriberIdForHistory) {
      setHistoryError("Please select a subscriber.");
      return;
    }
    setIsLoadingHistory(true);
    setHistoryError(null);
    setAttendanceHistory([]);
    try {
      const response = await ApiService.get<AttendanceLogFull[]>(`/reports/subscribers/${selectedSubscriberIdForHistory}/attendance`, {
        params: { startDate, endDate }
      });
      setAttendanceHistory(response.data || []);
    } catch (err:any) {
      console.error("Failed to fetch attendance history:", err);
      setHistoryError(err.response?.data?.message || "Failed to fetch attendance history.");
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const formatDateTime = (isoString?: string | null) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  }

  return (
    <div style={{display: 'flex', flexDirection: 'row', gap: '20px'}}>
      {/* Absentees Section */}
      <section style={{flex: 1, padding: '15px', border: '1px solid #ccc'}}>
        <h3>Absentees Report</h3>
        <form onSubmit={handleFetchAbsentees}>
          <div style={{marginBottom: '10px'}}>
            <label htmlFor="sessionSelectAbsentees">Select Session: </label>
            <select id="sessionSelectAbsentees" value={selectedSessionIdForAbsentees} onChange={e => setSelectedSessionIdForAbsentees(e.target.value)} required>
              <option value="">-- Select Session --</option>
              {sessions.map(s => <option key={s.id} value={s.id}>{s.name} ({formatDateTime(s.startTime)})</option>)}
            </select>
          </div>
          <button type="submit" disabled={isLoadingAbsentees || sessions.length === 0}>
            {isLoadingAbsentees ? 'Loading...' : 'Get Absentees'}
          </button>
        </form>
        {absenteesError && <p style={{color: 'red'}}>{absenteesError}</p>}
        { !isLoadingAbsentees && absentees.length === 0 && selectedSessionIdForAbsentees && <p>No absentees found for the selected session.</p> }
        { !isLoadingAbsentees && absentees.length === 0 && !selectedSessionIdForAbsentees && <p>Please select a session to view absentees.</p> }
        { absentees.length > 0 && <h4>Results:</h4> }
        {absentees.length > 0 && (
          <ul>{absentees.map(sub => <li key={sub.id}>{sub.firstName} {sub.lastName} ({sub.email})</li>)}</ul>
        )}
      </section>

      {/* Attendance History Section */}
      <section style={{flex: 1, padding: '15px', border: '1px solid #ccc'}}>
        <h3>Subscriber Attendance History</h3>
        <form onSubmit={handleFetchHistory}>
          <div style={{marginBottom: '10px'}}>
            <label htmlFor="subscriberSelectHistory">Select Subscriber: </label>
            <select id="subscriberSelectHistory" value={selectedSubscriberIdForHistory} onChange={e => setSelectedSubscriberIdForHistory(e.target.value)} required>
              <option value="">-- Select Subscriber --</option>
              {subscribers.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.email})</option>)}
            </select>
          </div>
          <div style={{marginBottom: '10px'}}>
            <label htmlFor="startDate">Start Date: </label>
            <input type="date" id="startDate" value={startDate} onChange={e => setStartDate(e.target.value)} required />
          </div>
          <div style={{marginBottom: '10px'}}>
            <label htmlFor="endDate">End Date: </label>
            <input type="date" id="endDate" value={endDate} onChange={e => setEndDate(e.target.value)} required />
          </div>
          <button type="submit" disabled={isLoadingHistory}>
            {isLoadingHistory ? 'Loading...' : 'Get History'}
          </button>
        </form>
        {historyError && <p style={{color: 'red'}}>{historyError}</p>}
        <h4>Results:</h4>
        {attendanceHistory.length > 0 ? (
          <table style={{width: '100%', fontSize: '0.9em'}}>
            <thead><tr><th>Session</th><th>Check-In</th><th>Check-Out</th></tr></thead>
            <tbody>
              {attendanceHistory.map(log => (
                <tr key={log.id}>
                  <td>{log.sessionName} (ID: {log.sessionId})</td>
                  <td>{formatDateTime(log.checkInTime)}</td>
                  <td>{formatDateTime(log.checkOutTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <p>No attendance history found for the selected criteria.</p>}
      </section>
    </div>
  );
};

export default ReportPage;
