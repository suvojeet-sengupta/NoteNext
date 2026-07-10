package com.suvojeet.notenext.data.ai

/**
 * Catalog of every AI-powered feature in NoteNext.
 *
 * Every value here corresponds to:
 *   - a user-facing toggle in the AI Features settings screen,
 *   - a usage-tracking bucket in the AIUsageRepository,
 *   - a routing path in AIProviderManager.
 *
 * Adding a new AI feature? Add an entry here and wire the toggle through
 * SettingsRepository + AIFeatureGate + the unified settings screen.
 */
enum class AIFeature(
    val id: String,
    val displayName: String,
    val description: String,
    val helpText: String,
    val isSuggestionFeature: Boolean,
    val isOnDevice: Boolean = false
) {
    SUMMARIZE(
        id = "summarize",
        displayName = "Summarize Notes",
        description = "Condense long notes into a short summary on demand.",
        helpText = "Sends the note's text to your selected AI provider when you tap the summarize button. Helps when you want the gist of a long note without re-reading.",
        isSuggestionFeature = false
    ),
    CHECKLIST(
        id = "checklist",
        displayName = "Generate Checklist",
        description = "Turn a topic into a ready-made checklist.",
        helpText = "You give it a topic ('packing for trip'), it returns a checklist you can edit or insert into a note. Saves typing for repetitive lists.",
        isSuggestionFeature = false
    ),
    TODOS(
        id = "todos",
        displayName = "Generate Todos",
        description = "Convert messy text into structured tasks.",
        helpText = "Paste a paragraph of plans or meeting notes; it returns titled todo items you can drop into the Todo screen.",
        isSuggestionFeature = false
    ),
    GRAMMAR(
        id = "grammar",
        displayName = "Fix Grammar",
        description = "Polish spelling, grammar and punctuation.",
        helpText = "Sends the selected text (or whole note) to fix typos and grammar. The original is kept for one tap of undo.",
        isSuggestionFeature = false
    ),
    AUTO_TAG(
        id = "auto_tag",
        displayName = "Smart Auto-Tagging",
        description = "Suggests labels based on what you wrote.",
        helpText = "After you save a note, the AI proposes 1–3 labels. Nothing is applied automatically — you tap to accept. Drops noise from your label list and improves searchability.",
        isSuggestionFeature = true
    ),
    SMART_REMINDER(
        id = "smart_reminder",
        displayName = "Smart Reminder Detection",
        description = "Spots dates and commitments in your notes.",
        helpText = "Reads your note for phrases like 'next Tuesday at 3pm' and offers to set a reminder. You confirm before any reminder is created.",
        isSuggestionFeature = true
    ),
    LINKED_NOTES(
        id = "linked_notes",
        displayName = "Linked Notes",
        description = "Discover related notes you may have forgotten.",
        helpText = "Shows up to 5 related notes at the bottom of the editor based on shared keywords/topics. Computed locally — no network call is ever made.",
        isSuggestionFeature = true,
        isOnDevice = true
    ),
    TONE_REWRITE(
        id = "tone_rewrite",
        displayName = "Tone Rewriter",
        description = "Rewrite text in a different tone.",
        helpText = "Pick a tone (Professional, Casual, Concise, Formal, Friendly, Bullets) and the AI rewrites the selected text. A diff preview is shown before you accept.",
        isSuggestionFeature = false
    ),
    CUSTOM_PROMPT(
        id = "custom_prompt",
        displayName = "Custom Prompt",
        description = "Run your own prompt against note content.",
        helpText = "For power users — define a system + user prompt and run it against any note's text.",
        isSuggestionFeature = false
    );

    companion object {
        fun fromId(id: String): AIFeature? = values().firstOrNull { it.id == id }
    }
}
