"""Extract hardcoded strings from AISettingsScreen.kt."""

PATH = "app/src/main/java/com/suvojeet/notenext/ui/settings/ai/AISettingsScreen.kt"

REPLACEMENTS = [
    (
        '                    Text(\n'
        '                        text = "AI",\n'
        '                        style = MaterialTheme.typography.headlineLarge,',
        '                    Text(\n'
        '                        text = stringResource(id = R.string.ai_settings_title),\n'
        '                        style = MaterialTheme.typography.headlineLarge,',
    ),
    (
        'Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")',
        'Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back))',
    ),
    (
        '                ExpressiveSection(\n'
        '                    title = "Master switch",\n'
        '                    description = "Turn every AI feature on or off. NoteNext is offline-first — AI is opt-in."\n'
        '                ) {',
        '                ExpressiveSection(\n'
        '                    title = stringResource(id = R.string.ai_settings_master_section),\n'
        '                    description = stringResource(id = R.string.ai_settings_master_section_desc)\n'
        '                ) {',
    ),
    (
        '                            title = if (masterEnabled) "AI is enabled" else "AI is disabled",\n'
        '                            subtitle = if (masterEnabled)\n'
        '                                "Per-feature toggles below take effect"\n'
        '                            else\n'
        '                                "All AI features are off — nothing will be sent to any provider",',
        '                            title = if (masterEnabled) stringResource(id = R.string.ai_settings_master_on_title) else stringResource(id = R.string.ai_settings_master_off_title),\n'
        '                            subtitle = if (masterEnabled)\n'
        '                                stringResource(id = R.string.ai_settings_master_on_subtitle)\n'
        '                            else\n'
        '                                stringResource(id = R.string.ai_settings_master_off_subtitle),',
    ),
    (
        '                ExpressiveSection(\n'
        '                    title = "Manage",\n'
        '                    description = "Pick which features run, see what they\'re doing"\n'
        '                ) {',
        '                ExpressiveSection(\n'
        '                    title = stringResource(id = R.string.ai_settings_manage_section),\n'
        '                    description = stringResource(id = R.string.ai_settings_manage_section_desc)\n'
        '                ) {',
    ),
    (
        '                            title = "AI Features",\n'
        '                            subtitle = "Enable or disable each feature individually",',
        '                            title = stringResource(id = R.string.ai_settings_nav_features),\n'
        '                            subtitle = stringResource(id = R.string.ai_settings_nav_features_subtitle),',
    ),
    (
        '                            title = "On-Device Features",\n'
        '                            subtitle = "Smart features that run locally without AI",',
        '                            title = stringResource(id = R.string.ai_settings_nav_ondevice),\n'
        '                            subtitle = stringResource(id = R.string.ai_settings_nav_ondevice_subtitle),',
    ),
    (
        '                            title = "Usage Dashboard",\n'
        '                            subtitle = if (totalCalls == 0) "No AI calls yet" else "$totalCalls AI invocations recorded",',
        '                            title = stringResource(id = R.string.ai_settings_nav_dashboard),\n'
        '                            subtitle = if (totalCalls == 0) stringResource(id = R.string.ai_settings_nav_dashboard_empty) else stringResource(id = R.string.ai_settings_nav_dashboard_count, totalCalls),',
    ),
    (
        '                ExpressiveSection(\n'
        '                    title = "Provider",\n'
        '                    description = "Choose which service handles your AI requests. Default provider is built in — others need an API key."\n'
        '                ) {',
        '                ExpressiveSection(\n'
        '                    title = stringResource(id = R.string.ai_settings_provider_section),\n'
        '                    description = stringResource(id = R.string.ai_settings_provider_section_desc)\n'
        '                ) {',
    ),
    (
        '                            ProviderCard(\n'
        '                                name = "AI Provider",\n'
        '                                description = "Fast inference, bundled with the app",\n'
        '                                icon = Icons.Rounded.Bolt,\n'
        '                                iconColor = Color(0xFFFFC107),\n'
        '                                isSelected = selectedProvider == AIProvider.GROQ,\n'
        '                                badge = "Default",',
        '                            ProviderCard(\n'
        '                                name = stringResource(id = R.string.ai_settings_provider_default_name),\n'
        '                                description = stringResource(id = R.string.ai_settings_provider_default_desc),\n'
        '                                icon = Icons.Rounded.Bolt,\n'
        '                                iconColor = Color(0xFFFFC107),\n'
        '                                isSelected = selectedProvider == AIProvider.GROQ,\n'
        '                                badge = stringResource(id = R.string.ai_settings_badge_default),',
    ),
    (
        '                            ProviderCard(\n'
        '                                name = "OpenAI",\n'
        '                                description = "GPT-4o, GPT-4.1 — bring your own key",\n'
        '                                icon = Icons.Rounded.AutoAwesome,\n'
        '                                iconColor = Color(0xFF10A37A),\n'
        '                                isSelected = selectedProvider == AIProvider.OPENAI,\n'
        '                                badge = if (openAIKey.isNotBlank()) "Configured" else null,',
        '                            ProviderCard(\n'
        '                                name = "OpenAI",\n'
        '                                description = stringResource(id = R.string.ai_settings_provider_openai_desc),\n'
        '                                icon = Icons.Rounded.AutoAwesome,\n'
        '                                iconColor = Color(0xFF10A37A),\n'
        '                                isSelected = selectedProvider == AIProvider.OPENAI,\n'
        '                                badge = if (openAIKey.isNotBlank()) configuredBadge else null,',
    ),
    (
        '                            ProviderCard(\n'
        '                                name = "Anthropic (Claude)",\n'
        '                                description = "Claude Sonnet, Haiku, Opus — bring your own key",\n'
        '                                icon = Icons.Rounded.Psychology,\n'
        '                                iconColor = Color(0xFFFF5722),\n'
        '                                isSelected = selectedProvider == AIProvider.ANTHROPIC,\n'
        '                                badge = if (anthropicKey.isNotBlank()) "Configured" else null,',
        '                            ProviderCard(\n'
        '                                name = stringResource(id = R.string.ai_provider_anthropic_name),\n'
        '                                description = stringResource(id = R.string.ai_settings_provider_anthropic_desc),\n'
        '                                icon = Icons.Rounded.Psychology,\n'
        '                                iconColor = Color(0xFFFF5722),\n'
        '                                isSelected = selectedProvider == AIProvider.ANTHROPIC,\n'
        '                                badge = if (anthropicKey.isNotBlank()) configuredBadge else null,',
    ),
    (
        '                            ProviderCard(\n'
        '                                name = "Google Gemini",\n'
        '                                description = "Gemini Flash & Pro — bring your own key",\n'
        '                                icon = Icons.Rounded.ModelTraining,\n'
        '                                iconColor = Color(0xFF4285F4),\n'
        '                                isSelected = selectedProvider == AIProvider.GEMINI,\n'
        '                                badge = if (geminiKey.isNotBlank()) "Configured" else null,',
        '                            ProviderCard(\n'
        '                                name = stringResource(id = R.string.ai_provider_gemini_name),\n'
        '                                description = stringResource(id = R.string.ai_settings_provider_gemini_desc),\n'
        '                                icon = Icons.Rounded.ModelTraining,\n'
        '                                iconColor = Color(0xFF4285F4),\n'
        '                                isSelected = selectedProvider == AIProvider.GEMINI,\n'
        '                                badge = if (geminiKey.isNotBlank()) configuredBadge else null,',
    ),
    (
        '                ExpressiveSection(\n'
        '                    title = "Privacy",\n'
        '                    description = "Local-only telemetry — nothing here ever leaves your device"\n'
        '                ) {',
        '                ExpressiveSection(\n'
        '                    title = stringResource(id = R.string.ai_settings_privacy_section),\n'
        '                    description = stringResource(id = R.string.ai_settings_privacy_section_desc)\n'
        '                ) {',
    ),
    (
        '                            title = "Track AI usage locally",\n'
        '                            subtitle = "Powers the dashboard. Counts which features ran, when, and the result. The note content is never recorded.",',
        '                            title = stringResource(id = R.string.ai_settings_track_usage_title),\n'
        '                            subtitle = stringResource(id = R.string.ai_settings_track_usage_subtitle),',
    ),
    (
        '                Text(\n'
        '                    text = "How AI works in NoteNext",\n'
        '                    style = MaterialTheme.typography.titleMedium,',
        '                Text(\n'
        '                    text = stringResource(id = R.string.ai_settings_banner_title),\n'
        '                    style = MaterialTheme.typography.titleMedium,',
    ),
    (
        '                Text(\n'
        '                    text = "Every AI feature is OFF by default. When you turn one on, the relevant text from your note is sent to the provider you\'ve chosen — and nothing else. We never upload your full database, attachments, or labels in bulk. You can disable everything with one tap above.",\n'
        '                    style = MaterialTheme.typography.bodyMedium,',
        '                Text(\n'
        '                    text = stringResource(id = R.string.ai_settings_banner_body),\n'
        '                    style = MaterialTheme.typography.bodyMedium,',
    ),
    (
        '                    contentDescription = "Selected",',
        '                    contentDescription = stringResource(id = R.string.ai_settings_provider_selected_cd),',
    ),
]

content = open(PATH, encoding="utf-8").read()
applied = 0
missing = []
for old, new in REPLACEMENTS:
    if old in content:
        content = content.replace(old, new, 1)
        applied += 1
    else:
        missing.append(old[:80].replace("\n", "\\n"))

# Add helper var for "Configured" badge string within @Composable scope
content = content.replace(
    '    val geminiModel by viewModel.geminiModel.collectAsStateWithLifecycle()\n\n',
    '    val geminiModel by viewModel.geminiModel.collectAsStateWithLifecycle()\n'
    '    val configuredBadge = stringResource(id = R.string.ai_settings_badge_configured)\n\n',
)

if "import androidx.compose.ui.res.stringResource" not in content:
    content = content.replace(
        "import androidx.compose.ui.unit.sp",
        "import androidx.compose.ui.res.stringResource\nimport androidx.compose.ui.unit.sp",
        1,
    )
if "import com.suvojeet.notenext.R" not in content:
    content = content.replace(
        "import com.suvojeet.notenext.data.ai.AIProvider",
        "import com.suvojeet.notenext.R\nimport com.suvojeet.notenext.data.ai.AIProvider",
        1,
    )

with open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(content)

print(f"Applied: {applied}/{len(REPLACEMENTS)} edits")
if missing:
    for m in missing: print(f"  MISSING: {m}")
