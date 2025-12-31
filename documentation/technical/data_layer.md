# Data Layer

The Data Layer is responsible for data persistence and retrieval. It uses **Room** as the local database.

## 🗄️ Database Schema

The database (`NoteDatabase`) consists of the following entities:

### 1. Note (`notes`)
The primary entity storage.
- `id` (PK, Auto-inc)
- `title`, `content` (HTML/Markdown)
- `createdAt`, `lastEdited` (Timestamps)
- `color` (Int)
- `isPinned`, `isArchived`, `isBinned` (Flags)
- `label`, `projectId` (Foreign Keys/Metadata)
- `isLocked` (Security flag)

### 2. ChecklistItem (`checklist_items`)
Items belonging to a checklist note.
- `id` (PK, String UUID)
- `noteId` (FK -> Note.id)
- `text`, `isChecked`, `position`

### 3. Attachment (`attachments`)
Files attached to notes.
- `id` (PK, Auto-inc)
- `noteId` (FK -> Note.id)
- `uri` (File path/URI), `type` (IMAGE/AUDIO)

### 4. Label (`labels`)
User-defined tags.
- `name` (PK)

### 5. Project (`projects`)
Higher-level containers.
- `id` (PK, Auto-inc)
- `name`, `description`, `timestamp`, `color`

## 🔗 Relationships

- **1-to-Many**: `Note` -> `ChecklistItem`
- **1-to-Many**: `Note` -> `Attachment`
- **Many-to-1**: `Note` -> `Project`

## 🧬 Repository Pattern

The app uses the repository pattern to abstract data sources.
- **`NoteRepository`**: The main interface for accessing notes, checklists, and attachments. It communicates with `NoteDao`.
- **`SettingsRepository`**: Wraps `DataStore`/`SharedPreferences` for app settings (Theme, Auto-delete days, etc.).
- **`BackupRepository`**: Handles Local ZIP export/import logic.
- **`GoogleDriveManager`**: Handles Google Drive API interactions.
