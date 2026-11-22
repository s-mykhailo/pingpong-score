import './style.css';
import './components';
import { registerSW } from 'virtual:pwa-register';
import { applyCurrentTheme } from './theme/theme';
import { initRouter } from './router';
import { initSync } from './offline/sync';
import { loadInitial } from './store';
import { loadLiveMatch } from './match/liveMatch';

applyCurrentTheme();

const root = document.getElementById('app')!;
initRouter(root);

void loadLiveMatch();
void loadInitial();
initSync();

registerSW({ immediate: true });
