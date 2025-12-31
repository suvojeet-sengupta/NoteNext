# UI Layer

The UI is built entirely with **Jetpack Compose**, implementing Material 3 Design.

## 🎨 Theming

Located in `ui/theme`.
- **`Theme.kt`**: Defines `NoteNextTheme`, handling Light/Dark mode switching and dynamic color support (Android 12+).
- **`Color.kt`**: Color palette definitions.
- **`Type.kt`**: Typography styles (Google Fonts).

## 🧩 Key Screens

Screens are located in `ui/`.
- **`notes/NotesScreen`**: The main dashboard. Displays the grid/list of notes, search bar, and FAB.
- **`add_edit_note/AddEditNoteScreen`**: The editor. Handles text input, rich text formatting logic (`RichTextController`), and checklist management.
- **`settings/SettingsScreen`**: App preferences, Backup & Restore UI, About section.
- **`bin/BinScreen`**: Viewing and cleaning up deleted notes.
- **`project/ProjectNotesScreen`**: Listing notes within a specific project.

## 🧭 Navigation

Navigation is handled by **Navigation Compose** in `MainActivity.kt` (or a dedicated `NavGraph`).
- Routes are defined as sealed classes/objects (`Screen`).
- Arguments (like `noteId`) are passed seamlessly between composables.

## ⚡ ViewModels & State

Each screen has a corresponding Hilt-injected `ViewModel`.
- **State**: Exposed as `StateFlow` (e.g., `NotesState`). The UI observes this state to render.
- **Events**: UI actions (clicks, inputs) are sent to the ViewModel as `Events` (e.g., `NotesEvent.SaveNote`).
- **Effects**: One-off events like Toasts or Navigation are handled via `SharedFlow` (UiEvent).
