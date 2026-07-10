# Security & Privacy

NoteNext is built with a "Privacy-First" philosophy. Our security model is designed to ensure that your data remains on your device and is protected even in high-stakes scenarios.

## 🔒 Biometric & PIN App Lock

Secure your notes from prying eyes using your device's built-in security.
- **Integration**: Works with Fingerprint, Face Unlock, or a custom App PIN.
- **PIN Hardening**: All PINs are stored using **PBKDF2-HMAC-SHA256** hashing with 100,000 iterations. We never store your actual PIN.
- **Screenshot Protection**: The App Lock screen blocks screenshots and is hidden from the "Recents" menu to prevent visual data leaks.
- **Behavior**: When enabled, the app asks for authentication every time it is launched.

## 🎭 Decoy Vault (Plausible Deniability)

For scenarios involving coercion, NoteNext features a Decoy Vault.
- **Secondary PIN**: Set a different PIN that reveals a separate, harmless set of notes.
- **Stealth Mode**: When a Decoy PIN is configured, biometric login is automatically hidden from the main lock screen to force PIN entry and preserve the decoy's secrecy.
- **Automatic Enforcement**: Enabling the Decoy Vault automatically enables the App Lock for consistent protection.

## 🛡️ Privacy Principles

1.  **Local-First & Offline**: No account or internet is required. Your notes are stored in a local SQLite database on your device.
2.  **No Tracking**: NoteNext contains zero analytics, trackers, or ads. We do not know how you use the app or what you write.
3.  **Encrypted Backups**: Optional backups to Google Drive can be protected with **AES-256 encryption** using a password only you know.
4.  **Forensic Protection**: Standard Android cloud backups (Android 12+) and device-to-device transfers are **disabled**. This ensures that ciphertext bound to your device's hardware never leaves your device's secure environment.

## 👁️ Note Locking

You can also lock individual notes for an extra layer of security.
- **Locking**: Long-press a note or open the menu inside a note and select "Lock".
- **Access**: Locked notes hide their content in the preview. Viewing them requires biometric authentication.

## 🧨 Pinpoint Self-Destruct

Ephemeral notes are managed with precision.
- **Local Processing**: Expiry timers are handled entirely on-device using exact system alarms.
- **Privacy**: Once a note expires, its content and attachments are wiped from the local database immediately.
