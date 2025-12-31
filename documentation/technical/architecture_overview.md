# Architecture Overview

NoteNext follows modern Android development practices, utilizing **Jetpack Compose** and **Clean Architecture** principles.

## 🏗️ Clean Architecture

The project is structured into three main layers:

1.  **Data Layer** (`com.suvojeet.notenext.data`)
    -   **Responsibility**: Handling data sources (Room DB, SharedPreferences/DataStore, File System).
    -   **Components**: Entities, DAOs, Repositories (Implementation), Data Sources.
2.  **Domain Layer** (`com.suvojeet.notenext.domain`)
    -   **Responsibility**: Business logic and use cases. Pure Kotlin, no Android dependencies (ideally).
    -   **Components**: Use Cases (`NoteUseCases`), Domain Models (if separated), Repository Interfaces.
3.  **UI/Presentation Layer** (`com.suvojeet.notenext.ui`)
    -   **Responsibility**: Displaying data and handling user interactions.
    -   **Components**: Activities, ViewModels, Composables, State Management.

## 🛠️ Tech Stack

-   **Language**: Kotlin
-   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Design)
-   **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
-   **Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
-   **Async**: Coroutines & Flow
-   **Navigation**: Navigation-Compose
-   **JSON Parsing**: Gson (for Backup/Import)
-   **Image Loading**: Coil

## 📦 Key Concepts

-   **Unidirectional Data Flow (UDF)**: ViewModels expose `StateFlow` (State) and accept `Events`. The UI observes state and triggers events.
-   **Offline-First**: The app functions entirely without internet. Internet is only used for optional Cloud Backup and Help links.
