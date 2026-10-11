import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import OverviewPage from './pages/OverviewPage';
import SafetyPage from './pages/SafetyPage';
import CampaignsPage from './pages/CampaignsPage';
import MediaPlanPage from './pages/MediaPlanPage';
import InventoryPage from './pages/InventoryPage';
import InsightsPage from './pages/InsightsPage';
import PlacementsPage from './pages/PlacementsPage';

export default function App() {
  return (
    <BrowserRouter>
      <div style={{
        display: 'flex',
        height: '100vh',
        background: '#0f0f0f',
        color: '#e5e5e5',
        overflow: 'hidden',
      }}>
        <Sidebar />
        <main style={{
          flex: 1,
          overflow: 'auto',
          background: '#0f0f0f',
        }}>
          <Routes>
            <Route path="/" element={<Navigate to="/overview" replace />} />
            <Route path="/overview" element={<OverviewPage />} />
            <Route path="/campaigns" element={<CampaignsPage />} />
            <Route path="/safety" element={<SafetyPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/placements" element={<PlacementsPage />} />
            <Route path="/plans" element={<MediaPlanPage />} />
            <Route path="/insights" element={<InsightsPage/>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
