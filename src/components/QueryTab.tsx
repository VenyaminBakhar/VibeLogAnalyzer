import React, { useState } from 'react';
import { queryApi } from '../api';
import { QueryResponse } from '../types';

const QueryTab: React.FC = () => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<QueryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showLogs, setShowLogs] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await queryApi.processQuery({ query });
      setResult(response);
      setShowLogs(false);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="card">
        <h2>Log Query Analysis</h2>
        <p>Enter your query in natural language to analyze logs using AI-powered insights.</p>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="query">Your Query</label>
            <textarea
              id="query"
              className="form-control"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="e.g., Why did the service crash yesterday evening?"
              rows={3}
              disabled={loading}
            />
          </div>
          
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading || !query.trim()}
          >
            {loading ? 'Analyzing...' : 'Analyze Logs'}
          </button>
        </form>
      </div>

      {error && (
        <div className="error">
          <strong>Error:</strong> {error}
        </div>
      )}

      {result && (
        <div className="card">
          <h3>Analysis Result</h3>
          <div className="analysis-result">
            <p>{result.analysis}</p>
          </div>
          
          {result.logs && result.logs.length > 0 && (
            <div className="log-details">
              <div className="flex justify-between align-center mb-4">
                <h4>Found {result.logs.length} relevant logs</h4>
                <button 
                  className="btn btn-secondary"
                  onClick={() => setShowLogs(!showLogs)}
                >
                  {showLogs ? 'Hide Details' : 'Show Details'}
                </button>
              </div>
              
              {showLogs && (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Timestamp</th>
                        <th>Level</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.logs.map((log) => (
                        <tr key={log.id}>
                          <td>{new Date(log.timestamp).toLocaleString()}</td>
                          <td>
                            <span 
                              className={`badge ${
                                log.logLevel === 'ERROR' ? 'badge-danger' : 
                                log.logLevel === 'WARN' ? 'badge-warning' : 
                                'badge-info'
                              }`}
                            >
                              {log.logLevel}
                            </span>
                          </td>
                          <td>{log.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default QueryTab;