import { createRoot } from 'react-dom/client';
import App from './App';
import '../../src/main/resources/static/styles.css';
import './web.css';

createRoot(document.getElementById('root')!).render(<App />);
