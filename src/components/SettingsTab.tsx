import React, { useState, useEffect } from 'react';
import { logPatternApi, settingsApi } from '../api';
import { LogPattern } from '../types';

const SettingsTab: React.FC = () => {
  const [patterns, setPatterns] = useState<LogPattern[]>([]);
  const [newPattern, setNewPattern] = useState<LogPattern>({ logLevel: '', logTemplate: '' });
  const [editingPattern, setEditingPattern] = useState<LogPattern | null>(null);
  const [apiKey, setApiKey] = useState('');
  const [currentApiKey, setCurrentApiKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  useEffect(() => {
    loadPatterns();
    loadCurrentApiKey();
  }, []);

  const loadPatterns = async () => {
    try {
      const data = await logPatternApi.getAll();
      setPatterns(data);
    } catch (error) {
      console.error('Error loading patterns:', error);
    }
  };

  const loadCurrentApiKey = async () => {
    try {
      const data = await settingsApi.getApiKey();
      setCurrentApiKey(data.apiKey);
    } catch (error) {
      console.error('Error loading API key:', error);
    }
  };

  const handleSavePattern = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPattern.logLevel || !newPattern.logTemplate) return;

    try {
      await logPatternApi.create(newPattern);
      setNewPattern({ logLevel: '', logTemplate: '' });
      setMessage({ type: 'success', text: 'Pattern saved successfully!' });
      loadPatterns();
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Error saving pattern: ' + error.message });
    }
  };

  const handleUpdatePattern = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPattern || !editingPattern.id) return;

    try {
      await logPatternApi.update(editingPattern.id, editingPattern);
      setEditingPattern(null);
      setMessage({ type: 'success', text: 'Pattern updated successfully!' });
      loadPatterns();
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Error updating pattern: ' + error.message });
    }
  };

  const handleDeletePattern = async (id: number) => {
    if (!confirm('Are you sure you want to delete this pattern?')) return;

    try {
      await logPatternApi.delete(id);
      setMessage({ type: 'success', text: 'Pattern deleted successfully!' });
      loadPatterns();
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Error deleting pattern: ' + error.message });
    }
  };

  const handleSaveApiKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!apiKey.trim()) return;

    setLoading(true);
    try {
      await settingsApi.saveApiKey(apiKey);
      setApiKey('');
      setMessage({ type: 'success', text: 'API key saved successfully!' });
      loadCurrentApiKey();
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Error saving API key: ' + error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {message && (
        <div className={message.type === 'success' ? 'success' : 'error'}>
          {message.text}
        </div>
      )}

      {/* API Key Configuration */}
      <div className="card">
        <h2>DeepSeek API Configuration</h2>
        <div className="mb-4">
          <strong>Current API Key:</strong> {currentApiKey || 'Not configured'}
        </div>
        
        <form onSubmit={handleSaveApiKey}>
          <div className="form-group">
            <label htmlFor="apiKey">DeepSeek API Key</label>
            <input
              type="password"
              id="apiKey"
              className="form-control"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Enter your DeepSeek API key"
              disabled={loading}
            />
          </div>
          
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading || !apiKey.trim()}
          >
            {loading ? 'Saving...' : 'Save API Key'}
          </button>
        </form>
      </div>

      {/* Log Patterns Management */}
      <div className="card">
        <h2>Log Patterns</h2>
        
        {/* Add New Pattern */}
        <div className="mb-4">
          <h3>Add New Pattern</h3>
          <form onSubmit={handleSavePattern}>
            <div className="flex gap-4 mb-4">
              <div className="form-group" style={{ flex: '0 0 200px' }}>
                <label htmlFor="logLevel">Log Level</label>
                <select
                  id="logLevel"
                  className="form-control"
                  value={newPattern.logLevel}
                  onChange={(e) => setNewPattern({ ...newPattern, logLevel: e.target.value })}
                >
                  <option value="">Select level</option>
                  <option value="INFO">INFO</option>
                  <option value="ERROR">ERROR</option>
                  <option value="WARN">WARN</option>
                  <option value="DEBUG">DEBUG</option>
                </select>
              </div>
              
              <div className="form-group" style={{ flex: 1 }}>
                <label htmlFor="logTemplate">Log Template</label>
                <input
                  type="text"
                  id="logTemplate"
                  className="form-control"
                  value={newPattern.logTemplate}
                  onChange={(e) => setNewPattern({ ...newPattern, logTemplate: e.target.value })}
                  placeholder="e.g., logger.info('User {} logged in', userId)"
                />
              </div>
            </div>
            
            <button 
              type="submit" 
              className="btn btn-success"
              disabled={!newPattern.logLevel || !newPattern.logTemplate}
            >
              Add Pattern
            </button>
          </form>
        </div>

        {/* Existing Patterns */}
        <div>
          <h3>Existing Patterns ({patterns.length})</h3>
          {patterns.length === 0 ? (
            <p>No patterns configured yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Log Level</th>
                  <th>Template</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {patterns.map((pattern) => (
                  <tr key={pattern.id}>
                    <td>
                      {editingPattern?.id === pattern.id ? (
                        <select
                          className="form-control"
                          value={editingPattern.logLevel}
                          onChange={(e) => setEditingPattern({ ...editingPattern, logLevel: e.target.value })}
                        >
                          <option value="INFO">INFO</option>
                          <option value="ERROR">ERROR</option>
                          <option value="WARN">WARN</option>
                          <option value="DEBUG">DEBUG</option>
                        </select>
                      ) : (
                        pattern.logLevel
                      )}
                    </td>
                    <td>
                      {editingPattern?.id === pattern.id ? (
                        <input
                          type="text"
                          className="form-control"
                          value={editingPattern.logTemplate}
                          onChange={(e) => setEditingPattern({ ...editingPattern, logTemplate: e.target.value })}
                        />
                      ) : (
                        pattern.logTemplate
                      )}
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {editingPattern?.id === pattern.id ? (
                          <>
                            <button 
                              className="btn btn-success"
                              onClick={handleUpdatePattern}
                            >
                              Save
                            </button>
                            <button 
                              className="btn btn-secondary"
                              onClick={() => setEditingPattern(null)}
                            >
                              Cancel
                            </button>
                          </>
                        ) : (
                          <>
                            <button 
                              className="btn btn-secondary"
                              onClick={() => setEditingPattern(pattern)}
                            >
                              Edit
                            </button>
                            <button 
                              className="btn btn-danger"
                              onClick={() => pattern.id && handleDeletePattern(pattern.id)}
                            >
                              Delete
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};

export default SettingsTab;