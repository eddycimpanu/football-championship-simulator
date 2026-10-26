import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { BettingProvider } from './betting.jsx';
// Fonts are bundled with the app, so the page looks the same offline and in Docker
import '@fontsource/albert-sans/400.css';
import '@fontsource/albert-sans/500.css';
import '@fontsource/albert-sans/600.css';
import '@fontsource/albert-sans/700.css';
import '@fontsource/saira-condensed/600.css';
import '@fontsource/saira-condensed/700.css';
import '@fontsource/saira-condensed/800.css';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <BettingProvider>
        <App />
      </BettingProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
