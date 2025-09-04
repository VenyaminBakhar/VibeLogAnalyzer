import React, { useState } from 'react';
import QueryTab from './components/QueryTab';
import SettingsTab from './components/SettingsTab';

type Tab = 'query' | 'settings';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('query');

  return (
    <div className="app">
      <header className="header">
        <h1>Log Analyzer</h1>
      </header>
      
      <nav className="nav">
        <ul className="nav-tabs">
          <li 
            className={`nav-tab ${activeTab === 'query' ? 'active' : ''}`}
            onClick={() => setActiveTab('query')}
          >
            Query
          </li>
          <li 
            className={`nav-tab ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            Settings
          </li>
        </ul>
      </nav>

      <main className="main-content">
        {activeTab === 'query' && <QueryTab />}
        {activeTab === 'settings' && <SettingsTab />}
      </main>
    </div>
  );
}

export default App;