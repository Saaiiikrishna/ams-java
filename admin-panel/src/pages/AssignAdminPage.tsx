import React, { useState, useEffect, FormEvent } from 'react';
import ApiService from '../services/ApiService';

interface OrganizationForSelect {
  id: number;
  name: string;
}

const AssignAdminPage: React.FC = () => {
  const [organizations, setOrganizations] = useState<OrganizationForSelect[]>([]);
  const [isLoadingOrgs, setIsLoadingOrgs] = useState(false);
  const [selectedOrgId, setSelectedOrgId] = useState<string>('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Fetch organizations for the select dropdown
  // This relies on GET /admin/entities.
  const fetchOrganizations = async () => {
    setIsLoadingOrgs(true);
    setError(null); // Clear previous errors specific to this operation
    try {
      const response = await ApiService.get<OrganizationForSelect[]>('/admin/entities');
      setOrganizations(response.data || []);
    } catch (err: any) {
      console.error("Failed to fetch organizations for admin assignment:", err);
      if (err.response && err.response.status === 403) {
         setError('Could not load organizations: You do not have permission.');
      } else {
         setError('Could not load organizations. Please try again later.');
      }
      setOrganizations([]);
    } finally {
      setIsLoadingOrgs(false);
    }
  };

  useEffect(() => {
    fetchOrganizations();
    // console.warn("AssignAdminPage: GET /admin/entities endpoint is not yet implemented on backend. Organization list is disabled.");
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!selectedOrgId) {
      setError("Please select an organization.");
      return;
    }

    try {
      const response = await ApiService.post('/admin/entity-admins', {
        organizationId: parseInt(selectedOrgId, 10),
        username,
        password,
      });
      setSuccessMessage(`Entity admin '${response.data.username}' created successfully for organization ID ${response.data.organizationId}.`);
      setUsername('');
      setPassword('');
      setSelectedOrgId('');
    } catch (err: any) {
      console.error("Failed to assign entity admin:", err);
      if (err.response && err.response.data) {
         if (typeof err.response.data === 'string') {
            setError(`Failed to assign admin: ${err.response.data}`);
         } else if (err.response.data.message) {
            setError(`Failed to assign admin: ${err.response.data.message}`);
         } else {
            setError('Failed to assign admin. Check console for details.');
         }
      } else {
        setError('Failed to assign admin. An unexpected error occurred.');
      }
    }
  };

  return (
    <div>
      <h2>Assign Entity Admin Credentials</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {successMessage && <p style={{ color: 'green' }}>{successMessage}</p>}

      <form onSubmit={handleSubmit} style={{ padding: '15px', border: '1px solid #eee' }}>
        <div style={{ marginBottom: '10px' }}>
          <label htmlFor="organizationId" style={{ marginRight: '10px' }}>Target Organization:</label>
          <select
            id="organizationId"
            value={selectedOrgId}
            onChange={(e) => setSelectedOrgId(e.target.value)}
            required
            disabled={isLoadingOrgs || organizations.length === 0}
          >
            <option value="">{isLoadingOrgs ? "Loading organizations..." : "Select Organization"}</option>
            {organizations.map(org => (
              <option key={org.id} value={org.id.toString()}>
                {org.name} (ID: {org.id})
              </option>
            ))}
          </select>
           {organizations.length === 0 && !isLoadingOrgs && !error && <small> No organizations found or list is empty.</small>}
           {/* Message about manual ID input can be removed or adjusted if list is expected to work now */}
        </div>
        {/* <p>If organization list is empty, ensure GET /admin/entities is implemented or enter ID manually (not supported by this form yet).</p> */}


        <div style={{ marginBottom: '10px' }}>
          <label htmlFor="username" style={{ marginRight: '10px' }}>New Admin Username:</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div style={{ marginBottom: '10px' }}>
          <label htmlFor="password" style={{ marginRight: '10px' }}>New Admin Password:</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={isLoadingOrgs}>Assign Admin</button>
      </form>
    </div>
  );
};

export default AssignAdminPage;
