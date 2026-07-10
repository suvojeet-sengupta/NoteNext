# NoteNext AI — Complete Reference

**Last updated:** April 25, 2026
**Applies to:** NoteNext v1.4.0+

This document is the single source of truth for **how AI works in NoteNext, what data leaves your device, and how to control it**. NoteNext is a privacy-focused, local-first note app — and that promise extends to AI.

---

## TL;DR

- **AI is OFF by default.** Nothing leaves your device until you flip the master switch in Settings → AI.
- **Even with the master switch on, every individual feature is OFF.** You opt in to each one.
- **Only the relevant text** for the feature you triggered is sent to the provider you chose. Never your whole database, never attachments, never labels in bulk.
- **A bundled AI API key** ships with the app for convenience. You can replace it with your own (AI, OpenAI, Anthropic, or Google Gemini) at any time.
- **All usage is tracked locally** so you can see exactly what AI did. Nothing in the dashboard ever leaves your device. You can clear it with one tap.

---

## Settings hierarchy

Settings → **AI**

```
AI Settings (hub)
├── Master switch ─────────────── one-tap kill switch for all AI
├── AI Features ──────────────── per-feature on/off (10 features)
├── Usage Dashboard ───────────── local-only stats, charts, history
├── Provider picker ──────────── AI / OpenAI / Anthropic / Gemini
├── (per-provider) API key + model + base URL
└── Track usage locally ────────── opt-out telemetry (default ON, never leaves device)
```

**Where to find it:** Open the app → Settings (gear icon) → tap **AI** under "Data & Maintenance".

---

## The 9 AI features

| # | Feature | Trigger | Data sent | Provider call |
|---|---------|---------|-----------|---------------|
| 1 | **Summarize Notes** | Tap summarize icon (✨) in editor | Note title + content | Yes |
| 2 | **Generate Checklist** | AI Assistant button → "Generate" | Topic you typed | Yes |
| 3 | **Generate Todos** | AI Assistant button → "Convert to Todo" | Selected note text | Yes |
| 4 | **Fix Grammar** | Toolbar grammar button | Selected text (or whole note) | Yes |
| 5 | **Auto-Tagging** *(new)* | Auto, after every save | Note title + content | Yes |
| 6 | **Smart Reminder** *(new)* | Auto, after every save | Note content + current date/time | Yes |
| 7 | **Linked Notes** *(new)* | When opening a note | **NONE** — runs locally | No |
| 8 | **Tone Rewriter** *(new)* | Tap wand icon (🪄) in top bar | Note text + selected tone label | Yes |
| 9 | **Custom Prompt** | Power-user JSON editing | Your custom system + user prompts | Yes |

**Default state for every row above: OFF.**

### Feature 7 (Linked Notes) is special

It uses local-only keyword overlap (a Jaccard similarity over note tokens) to find related notes. **No network call is ever made.** The toggle just controls whether the section appears in the UI. The computation runs in milliseconds even on large note collections.

---

## How a single AI call works

When you tap (say) "Summarize" on a note, here's the exact sequence:

1. **Gate check.** `AIFeatureGate.isEnabled(SUMMARIZE)` returns `true` only if **both** the master switch AND the per-feature toggle are on. If either is off, you get a toast: *"Summarize is disabled in AI Settings"* and **no network call is made**.
2. **Provider lookup.** `AIProviderManager.getActiveProvider()` reads your preferred provider from local DataStore (default: AI).
3. **Network request.** The relevant note text (and only the relevant text) is sent over HTTPS to the provider's official endpoint:
   - AI → `https://api.ai.com/openai/v1/chat/completions`
   - OpenAI → `https://api.openai.com/v1/chat/completions` (or your custom base URL)
   - Anthropic → `https://api.anthropic.com/v1/messages`
   - Gemini → `https://generativelanguage.googleapis.com/`
4. **Response handling.** The reply is shown to you in a sheet/dialog. **Nothing is auto-applied** for suggestion features — you tap to accept.
5. **Local recording.** A row is added to the local `ai_usage_events` Room table: `(featureId, provider, timestamp, success, durationMs, accepted=null)`. Note content is never recorded here.

You can audit every step of this in:
- `data/ai/AIProviderManager.kt` (gate + routing + tracking)
- `data/ai/AIFeatureGate.kt` (the two-level gate)
- `data/ai/AIUsageRepository.kt` (telemetry; honors the local-tracking toggle)

---

## Privacy guarantees, in order of importance

1. **No outbound network call without an explicit opt-in.** Every AI feature requires (a) the master switch ON and (b) its specific feature toggle ON. The default for both is OFF.
2. **No bulk uploads.** AI features only ever send the text relevant to the action you triggered. Your other notes, attachments, labels, and project structure never leave the device for AI purposes.
3. **No telemetry to NoteNext.** The usage dashboard reads from a local Room table. We do not send analytics. The only network endpoints contacted by AI features are the provider you chose.
4. **API keys stay on-device.** Your custom keys are stored in Android DataStore (the same secure store as your preferences). They are sent only as `Authorization` headers to the provider.
5. **Master switch is instant.** Toggling master OFF stops all AI features immediately. No background tasks continue.
6. **Usage tracking is opt-out.** It's ON by default because the dashboard is the only way for *you* to verify what AI did. Toggle it OFF in Settings → AI → Privacy if you'd rather not record anything.
7. **You can wipe the dashboard.** Open the dashboard → trash icon (top right) → "Clear" — every row is deleted.

---

## Providers — choosing one

| Provider | Cost | Latency | Quality | Key required? |
|----------|------|---------|---------|---------------|
| **AI** *(default)* | Free with bundled key (rate-limited, shared) | Very fast (sub-second) | Good (Llama 3.3, Llama 4) | Optional |
| **OpenAI** | Paid (cents per call) | ~1–2s | Excellent (GPT-4o, GPT-4.1) | Yes |
| **Anthropic** | Paid (cents per call) | ~1–2s | Excellent (Claude Sonnet, Haiku) | Yes |
| **Google Gemini** | Free tier available | ~1s | Very good (Gemini Flash, Pro) | Yes |

### Bundled AI key

The app ships with an encrypted, app-wide AI API key (`BuildConfig.GROQ_API_KEY_ENC`, XOR-decoded at runtime). It's the *default* provider so AI works out of the box.

**Caveats of the bundled key:**
- It's shared with all NoteNext users → can hit rate limits during peak usage.
- We may rotate or revoke it at any time.
- Some advanced features (e.g. extra-long contexts) may be capped.

**To use your own AI key:**
Settings → AI → Provider: AI → toggle "Use my own AI key" → paste key from https://console.ai.com/keys.

### Bringing your own key (any provider)

1. Open the provider's console (each config card has a "Get a key" button that links straight there).
2. Copy the API key.
3. Settings → AI → tap the provider → paste the key into the text field.
4. (Optional) Tap "Refresh" to fetch the model list, then pick a model.

The selected provider takes effect for **every** AI feature, immediately. No restart needed.

---

## The Usage Dashboard

Settings → AI → **Usage Dashboard**

What it shows:
- **Hero card:** Total AI invocations, count of successes, success rate.
- **Helpfulness card:** For suggestion features (auto-tag, smart-reminder), the percentage you accepted vs dismissed.
- **By feature (bar chart):** How often each feature ran, sorted by frequency.
- **Details:** Per-feature breakdown — count, success rate, average latency, acceptance rate (where applicable).
- **By provider:** Which provider serviced your requests, as a bar chart.
- **Trash icon (top right):** Clears all local records permanently.

What it does NOT show, ever:
- The note text you sent.
- The AI's response.
- Your API keys.
- Anything that lives outside this device.

---

## What changes for existing users (v1.3.x → v1.4.0)

- **Two old screens replaced with one:** "AI AI Settings" + "AI Providers" → unified into "AI".
- **All AI features now default to OFF.** This is a privacy hardening change. If you used summarize/checklist/grammar before, you'll need to flip them back on under Settings → AI → AI Features. (We considered preserving prior usage as implicit consent but decided in favor of explicit opt-in, even with the friction.)
- **A new database migration runs on first launch (v27 → v28).** It creates the local `ai_usage_events` table. Empty by default.
- **Your API keys (custom AI key, OpenAI key, Anthropic key, Gemini key) are preserved** — they were stored in DataStore, not the database.

---

## Developer notes

### File map

```
data/.../data/ai/
├── AIProviderService.kt         interface + default impls for advanced features
├── AIProviderManager.kt         routing + gating + tracking
├── AIFeature.kt                 enum of all 9 features + help copy
├── AIFeatureGate.kt             two-level gate (master + per-feature)
├── AIUsageEvent.kt              Room entity
├── AIUsageDao.kt                queries powering the dashboard
├── AIUsageRepository.kt         honors usageTrackingEnabled
├── ToneOption.kt                tone enum + ExtractedReminder + LabelSuggestion
├── AIProvider.kt              auth via NetworkModule.authInterceptor
├── OpenAIProvider.kt            auto-loads key from settings
├── AnthropicProvider.kt         auto-loads key from settings
└── GeminiProvider.kt            auto-loads key from settings

app/.../ui/settings/ai/
├── AISettingsScreen.kt          unified hub (replaces both legacy screens)
├── AISettingsViewModel.kt
├── AIProviderConfigSections.kt  per-provider config UI (AI/OpenAI/Anthropic/Gemini)
├── AIFeaturesScreen.kt          per-feature toggles, grouped by trigger style
├── AIFeaturesViewModel.kt
├── AIUsageDashboardScreen.kt    bar charts + per-feature details
└── AIUsageDashboardViewModel.kt

app/.../ui/notes/delegate/
├── AIDelegate.kt                LEGACY — wraps AiRepository for old features
└── AISuggestionsDelegate.kt     NEW — auto-tag, smart reminder, linked notes, tone rewrite

app/.../ui/add_edit_note/components/
├── AiSummarySheet.kt            (existing)
├── AiChecklistDialog.kt         (existing)
├── AiAssistantButton.kt         (existing)
├── AiSuggestedLabelsRow.kt      NEW — auto-tag chip row
├── AiSmartReminderChip.kt       NEW — detected reminder card
├── AiLinkedNotesSection.kt      NEW — related notes carousel
└── AiToneRewriteSheet.kt        NEW — tone picker + diff preview
```

### Settings keys (DataStore)

```kotlin
ai_master_enabled                : Boolean (default false)
ai_usage_tracking_enabled        : Boolean (default true)
ai_feature_summarize             : Boolean (default false)
ai_feature_checklist             : Boolean (default false)
ai_feature_todos                 : Boolean (default false)
ai_feature_grammar               : Boolean (default false)
ai_feature_auto_tag              : Boolean (default false)
ai_feature_smart_reminder        : Boolean (default false)
ai_feature_linked_notes          : Boolean (default false)
ai_feature_tone_rewrite          : Boolean (default false)
ai_feature_custom_prompt         : Boolean (default false)
preferred_ai_provider            : String  (default "GROQ")
custom_ai_key                  : String
use_custom_ai_key              : Boolean
openai_api_key | openai_base_url | openai_model
anthropic_api_key | anthropic_model
gemini_api_key | gemini_model
```

### Adding a new AI feature

1. Add an entry to `AIFeature.kt`.
2. Add a settings key + Flow + saver in `SettingsRepository.kt`.
3. Add a branch in `AIFeatureGate.isEnabled()` and `observeIsEnabled()`.
4. Add a default implementation to `AIProviderService` that delegates to `generateCustomPrompt`.
5. Add a typed wrapper in `AIProviderManager` that calls `invoke(YourFeature) { ... }`.
6. Add UI: a new event in `NotesEvent.kt`, state field in `NotesState.kt`, handler in `NotesViewModel.onEvent`, a Composable in `add_edit_note/components/`.
7. Mention it in this doc and in the in-app `AIFeaturesScreen` (it'll show up automatically once added to the enum).

### How acceptance tracking works

For suggestion-style features (auto-tag, smart-reminder, linked-notes), we record TWO rows:
1. The initial invocation (`accepted = null`).
2. The user's accept/dismiss decision (`accepted = true | false`).

This means total invocations are inflated by 2× for suggestion features, but the acceptance-rate calculation is accurate (it filters on `accepted IS NOT NULL`).

If this becomes a problem, change `recordSuggestionAccepted` to `UPDATE` the latest row instead of inserting a new one.

---

## Troubleshooting

### "Feature is disabled in AI Settings"
You triggered a feature whose per-feature toggle is off (or the master switch is off). Open Settings → AI → AI Features → flip it on.

### "Invalid API key" / 401
Your custom key for the active provider is wrong or expired. Open Settings → AI → tap the provider → re-paste the key.

### "Rate limited"
You're hitting AI's shared key limit (or your own provider's limit). Either:
- Wait a few seconds and retry.
- Switch to a different provider.
- Use your own AI key (higher per-account limits).

### Auto-tag/smart-reminder never fires
- Master switch must be ON.
- The specific feature toggle must be ON.
- The note must be at least ~30 chars (auto-tag) / ~20 chars (smart-reminder) — too short and we skip it to save API calls.
- The save must succeed (red save indicator = AI suggestions also fail).

### Dashboard is empty even after using AI
- Check Settings → AI → "Track AI usage locally" is ON.
- Some legacy code paths (older summarize/grammar) record only when going through `AIProviderManager`. They will be migrated in a future release.

---

## Questions, bug reports, contributions

- GitHub: https://github.com/suvojeet-sengupta/NoteNext
- In-app: Settings → "Start Logging" → reproduce the bug → "Stop Logging" to share a logcat.

The full source for every AI feature is in the repo. Read it. We mean it about transparency.
