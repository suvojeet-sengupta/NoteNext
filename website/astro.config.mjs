import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  site: 'https://notenext.suvojeetsengupta.in',
  integrations: [
    sitemap({
      changefreq: 'weekly',
      priority: 0.7,
      lastmod: new Date(),
      serialize(item) {
        const url = new URL(item.url);
        if (url.pathname === '/') {
          item.priority = 1.0;
          item.changefreq = 'weekly';
        } else if (
          url.pathname.startsWith('/features') ||
          url.pathname.startsWith('/download')
        ) {
          item.priority = 0.9;
          item.changefreq = 'weekly';
        } else if (url.pathname.startsWith('/changelog')) {
          item.priority = 0.8;
          item.changefreq = 'weekly';
        } else if (
          url.pathname.startsWith('/privacy-policy')
        ) {
          item.priority = 0.5;
          item.changefreq = 'monthly';
        }
        return item;
      },
    }),
  ],
  vite: {
    plugins: [tailwindcss()],
  },
  build: {
    inlineStylesheets: 'auto',
  },
  compressHTML: true,
});
