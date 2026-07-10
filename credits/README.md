# NoteNext Credits Module 🌟

This module manages all the attributions, contributors, and open-source library information for NoteNext. It provides a centralized, dynamic way to handle credits without cluttering the main app logic.

## 📂 Structure

- **`assets/credits.json`**: The single source of truth for all credit information.
- **`CreditsProvider.kt`**: Helper object to load and parse the JSON data.
- **`CreditsModels.kt`**: Data classes representing the structure of the credits.

## 📝 How to update Credits

To add a new contributor or library, simply edit `credits/src/main/assets/credits.json`.

### Example Entry:
```json
{
  "name": "Contributor Name",
  "role": "Designer",
  "avatarUrl": "https://link-to-image.png",
  "githubUrl": "https://github.com/username",
  "telegramUrl": "https://t.me/username"
}
```

Both `githubUrl` and `telegramUrl` are optional. If both are provided, `telegramUrl` will take precedence in the UI link.

## 🚀 Usage in UI

In your Compose screen, you can fetch the data easily:

```kotlin
val creditsData = remember { CreditsProvider.getCredits(context) }
```

## 🛡️ ProGuard / R8
This module includes `consumer-rules.pro` to ensure that the data models are not stripped or renamed during minification, as they are required for JSON deserialization.
