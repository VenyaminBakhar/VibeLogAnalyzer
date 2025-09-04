export interface LogEntry {
  id: number;
  timestamp: string;
  logLevel: string;
  message: string;
}

export interface LogPattern {
  id?: number;
  logLevel: string;
  logTemplate: string;
}

export interface QueryResponse {
  analysis: string;
  logs: LogEntry[];
}

export interface QueryRequest {
  query: string;
}