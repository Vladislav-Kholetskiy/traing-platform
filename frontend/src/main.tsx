import React from 'react';
import ReactDOM from 'react-dom/client';
import 'antd/dist/reset.css';
import 'dayjs/locale/ru';
import { AppProviders } from './app/providers/AppProviders';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProviders />
  </React.StrictMode>,
);
