# Log Analyzer Application

## Overview
Full-stack web application for log analysis using Java Spring Boot backend, React TypeScript frontend, and H2 database. The application uses DeepSeek API for intelligent log analysis through a two-step process:

1. **SQL Generation**: Converts natural language queries to SQL using log patterns and database structure
2. **Log Analysis**: Analyzes retrieved logs to provide human-readable insights

## Architecture
- **Backend**: Java Spring Boot with REST API
- **Frontend**: React TypeScript with Vite
- **Database**: H2 in-memory database for log storage and analysis
- **AI Integration**: DeepSeek API for natural language processing

## Project Structure
```
├── src/main/java/com/loganalyzer/     # Java backend source
│   ├── controller/                    # REST API controllers
│   ├── service/                      # Business logic
│   ├── model/                        # Data models
│   ├── repository/                   # Database access
│   ├── dto/                         # Data transfer objects
│   └── config/                      # Configuration classes
├── src/                             # React frontend source
│   ├── components/                  # React components
│   ├── types.ts                     # TypeScript types
│   └── api.ts                       # API client
├── pom.xml                          # Maven dependencies
└── package.json                     # Node.js dependencies
```

## Recent Changes
- ✅ Complete project setup with full-stack architecture
- ✅ Implemented advanced two-step DeepSeek integration for log analysis:
  1. SQL Generation: New optimized clean SQL-only prompt focused on communication log analysis with traceId tracking
  2. Log Analysis: Analyzes retrieved logs for human-readable insights
- ✅ **UPDATED**: PostgreSQL compatibility mode for H2 database (MODE=PostgreSQL)
- ✅ **UPDATED**: Enhanced SQL generation prompt with H2-specific REGEXP_SUBSTR functions, ILIKE, and CTE support
- ✅ **UPDATED**: New clean SQL prompt that returns pure SQL code without markdown formatting, adapted for communication sending operations with examples using real log patterns from application
- ✅ Created CRUD operations for log patterns and settings management
- ✅ Added CRUD operations for log entries management in Settings tab
- ✅ Built responsive React frontend with Query and Settings tabs
- ✅ H2 database with PostgreSQL compatibility for advanced SQL support
- ✅ Clean database setup - no auto-generated sample data
- ✅ Both frontend (port 5000) and backend (port 8080) successfully running
- ✅ All entities (LogEntry, LogPattern, AppSetting) with JPA annotations
- ✅ Complete REST API endpoints for all CRUD operations
- ✅ Fixed ClassCastException issues with SQL query execution

## API Endpoints
- `GET /api/patterns` - Retrieve all log patterns
- `POST /api/patterns` - Create new log pattern
- `PUT /api/patterns/{id}` - Update existing pattern
- `DELETE /api/patterns/{id}` - Delete pattern
- `GET /api/logs` - Retrieve all log entries
- `POST /api/logs` - Create new log entry
- `PUT /api/logs/{id}` - Update existing log entry
- `DELETE /api/logs/{id}` - Delete log entry
- `GET /api/settings/deepseek_api_key` - Get current API key (masked)
- `POST /api/settings/deepseek_api_key` - Save/update API key
- `POST /api/query` - Process natural language query for log analysis

## User Preferences
- H2 database only for all data storage
- Java backend with Spring Boot
- Two-step log analysis process using DeepSeek API
- Clean, responsive React frontend

## Development Setup
- Frontend runs on port 5000
- Backend runs on port 8080
- H2 in-memory database
- H2 console available at /h2-console
- CORS configured for development