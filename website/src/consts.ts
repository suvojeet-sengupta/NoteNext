export const SITE = {
  name: 'NoteNext',
  url: 'https://notenext.suvojeetsengupta.in',
  title: 'NoteNext — Private, Local-First Note-Taking for Android',
  description:
    'A premium privacy-first note-taking app for Android. Material 3 Expressive design, biometric vault, AES-256 encrypted Google Drive backups, decoy vault, and optional AI assistance — without selling your data.',
  shortDescription:
    'Private, local-first note-taking for Android. Beautifully designed. Quietly powerful.',
  author: 'Suvojeet Sengupta',
  authorUrl: 'https://suvojeetsengupta.in',
  email: 'support@suvojeetsengupta.in',
  github: 'https://github.com/suvojeet-sengupta/NoteNext',
  githubReleaseUrl: 'https://github.com/suvojeet-sengupta/NoteNext/releases/latest',
  playStoreUrl: 'https://play.google.com/store/apps/details?id=com.suvojeet.notenext',
  packageName: 'com.suvojeet.notenext',
  version: '1.5.2',
  releaseDate: '2026-08-03',
  themeColor: '#6200ee',
  twitter: '@suvojeet_',
  ogImage: '/og-image.png',
  keywords: [
    'private note-taking app',
    'offline notes Android',
    'local-first notes',
    'encrypted notes app',
    'biometric note app',
    'Material 3 Expressive notes',
    'Google Keep alternative',
    'decoy vault notes',
    'secure notes Android',
    'AES-256 notes backup',
  ],
};

export const NAV = [
  { href: '/', label: 'Home' },
  { href: '/features', label: 'Features' },
  { href: '/privacy', label: 'Privacy' },
  { href: '/security', label: 'Security' },
  { href: '/changelog', label: 'Changelog' },
  { href: '/download', label: 'Download' },
];

export const SCREENSHOTS = [
  {
    src: '/screenshots/notes-list.jpeg',
    alt: 'NoteNext notes grid view with rich previews, color labels, and a checklist note',
    caption: 'A grid that scales from 5 notes to 5,000.',
  },
  {
    src: '/screenshots/fab-menu.jpeg',
    alt: 'Floating action button expanded showing Note, Checklist, Drawing, Todo, and Projects options',
    caption: 'One tap. Five ways to capture.',
  },
  {
    src: '/screenshots/drawer.jpeg',
    alt: 'NoteNext navigation drawer with Notes, Projects, Archive, Reminders, Todos, Bin, Settings',
    caption: 'Everything where you expect it.',
  },
  {
    src: '/screenshots/ai-settings.jpeg',
    alt: 'AI settings page showing master switch off by default, with feature management options',
    caption: 'AI is opt-in. Off by default.',
  },
  {
    src: '/screenshots/privacy-security.jpeg',
    alt: 'Privacy and Security settings showing App Lock, Decoy Vault, screenshot blocking, and clipboard clear',
    caption: 'Privacy is a top-level setting.',
  },
  {
    src: '/screenshots/notescreen.jpeg',
    alt: 'NoteNext notes screen with rich previews and color labels',
    caption: 'A clean, intuitive interface for managing your notes.',
  },
];

export const FOOTER_LINKS = [
  {
    title: 'Product',
    items: [
      { href: '/features', label: 'Features' },
      { href: '/changelog', label: 'Changelog' },
      { href: '/download', label: 'Download' },
      { href: '/faq', label: 'FAQ' },
    ],
  },
  {
    title: 'Trust',
    items: [
      { href: '/privacy', label: 'Privacy' },
      { href: '/security', label: 'Security' },
      { href: '/privacy-policy', label: 'Privacy Policy' },
    ],
  },
  {
    title: 'Project',
    items: [
      { href: 'https://github.com/suvojeet-sengupta/NoteNext', label: 'GitHub', external: true },
      {
        href: 'https://github.com/suvojeet-sengupta/NoteNext/releases',
        label: 'Releases',
        external: true,
      },
      { href: '/contact', label: 'Contact' },
    ],
  },
];
