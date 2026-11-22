import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  base: '/pingpong-score/',
  plugins: [
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icons/icon.svg'],
      manifest: {
        id: '/pingpong-score/',
        name: 'Ping Pong Score',
        short_name: 'PingPong',
        description: 'Табло для настільного тенісу зі статистикою гравців',
        start_url: '/pingpong-score/',
        scope: '/pingpong-score/',
        display: 'standalone',
        orientation: 'landscape',
        background_color: '#141218',
        theme_color: '#6750a4',
        lang: 'uk',
        icons: [
          { src: 'icons/icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
          { src: 'icons/icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'maskable' },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        navigateFallback: '/pingpong-score/index.html',
      },
    }),
  ],
});
