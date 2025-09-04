import axios from 'axios';
import { LogPattern, QueryRequest, QueryResponse } from './types';

const API_BASE_URL = `${window.location.protocol}//${window.location.hostname}:8080/api`;

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