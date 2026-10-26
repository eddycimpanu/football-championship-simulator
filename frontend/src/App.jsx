import { Navigate, Route, Routes } from 'react-router-dom';
import Sidebar from './components/Sidebar.jsx';
import TablePage from './pages/TablePage.jsx';
import RoundsPage, { CurrentRoundRedirect } from './pages/RoundsPage.jsx';
import ComingSoonPage from './pages/ComingSoonPage.jsx';
import MatchPage from './pages/MatchPage.jsx';
import PlayersPage from './pages/PlayersPage.jsx';
import PlayerPage from './pages/PlayerPage.jsx';
import ClubsPage from './pages/ClubsPage.jsx';
import TeamPage from './pages/TeamPage.jsx';
import ReportsPage from './pages/ReportsPage.jsx';
import BettingPage from './pages/BettingPage.jsx';

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
          <Route path="/matches/:id" element={<MatchPage />} />
          <Route path="/teams" element={<ClubsPage />} />
          <Route path="/teams/:id" element={<TeamPage />} />
          <Route path="/players" element={<PlayersPage />} />
          <Route path="/players/:id" element={<PlayerPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/betting" element={<BettingPage />} />
          <Route path="*" element={<Navigate to="/table" replace />} />
        </Routes>
      </main>
    </div>
  );
}
