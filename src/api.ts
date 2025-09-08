import axios from 'axios';
import { LogPattern, LogEntry, QueryRequest, QueryResponse } from './types';

// Use port 8080 for development, same port for production
const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname.includes('replit.dev')
  ? `${window.location.protocol}//${window.location.hostname}:8080/api`
  : `${window.location.protocol}//${window.location.hostname}/api`;

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Log Patterns API
export const logPatternApi = {
  getAll: (): Promise<LogPattern[]> => 
    api.get('/patterns').then(res => res.data),
  
  create: (pattern: LogPattern): Promise<LogPattern> => 
    api.post('/patterns', pattern).then(res => res.data),
  
  update: (id: number, pattern: LogPattern): Promise<LogPattern> => 
    api.put(`/patterns/${id}`, pattern).then(res => res.data),
  
  delete: (id: number): Promise<void> => 
    api.delete(`/patterns/${id}`).then(() => undefined),
};

// Settings API
export const settingsApi = {
  getApiKey: (): Promise<{ apiKey: string }> => 
    api.get('/settings/deepseek_api_key').then(res => res.data),
  
  saveApiKey: (apiKey: string): Promise<{ message: string }> => 
    api.post('/settings/deepseek_api_key', { apiKey }).then(res => res.data),
};

// Query API
export const queryApi = {
  processQuery: (request: QueryRequest): Promise<QueryResponse> => 
    api.post('/query', request).then(res => res.data),
};

// Log Entries API
export const logEntryApi = {
  getAll: (): Promise<LogEntry[]> => 
    api.get('/logs').then(res => res.data),
  
  create: (logEntry: Omit<LogEntry, 'id'>): Promise<LogEntry> => 
    api.post('/logs', logEntry).then(res => res.data),
  
  update: (id: number, logEntry: LogEntry): Promise<LogEntry> => 
    api.put(`/logs/${id}`, logEntry).then(res => res.data),
  
  delete: (id: number): Promise<void> => 
    api.delete(`/logs/${id}`).then(() => undefined),
};