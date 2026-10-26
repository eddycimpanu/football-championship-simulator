import { Navigate, Route, Routes } from 'react-router-dom';
import Sidebar from './components/Sidebar.jsx';
import TablePage from './pages/TablePage.jsx';
import RoundsPage, { CurrentRoundRedirect } from './pages/RoundsPage.jsx';
import ComingSoonPage from './pages/ComingSoonPage.jsx';

export default function App() {
  return (
    <div className="app">
      <Sidebar />
      <main className="content">
        <Routes>
          <Route path="/" element={<Navigate to="/table" replace />} />
          <Route path="/table" element={<TablePage />} />
          <Route path="/rounds" element={<CurrentRoundRedirect />} />
          <Route path="/rounds/:number" element={<RoundsPage />} />
          <Route path="/players" element={<ComingSoonPage title="Players" />} />
          <Route path="/reports" element={<ComingSoonPage title="Season reports" />} />
          <Route path="/betting" element={<ComingSoonPage title="Betting" />} />
          <Route path="*" element={<Navigate to="/table" replace />} />
        </Routes>
      </main>
    </div>
  );
}
