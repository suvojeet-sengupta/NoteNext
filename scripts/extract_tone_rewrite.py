"""Extract hardcoded strings from ToneRewriteScreen.kt -> stringResource."""

PATH = "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/ToneRewriteScreen.kt"

REPLACEMENTS = [
    (
        '                title = { Text("Rewrite tone") },',
        '                title = { Text(stringResource(id = R.string.ai_tone_title)) },',
    ),
    (
        'Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")',
        'Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back))',
    ),
    (
        'Icon(Icons.Rounded.Check, contentDescription = "Apply")',
        'Icon(Icons.Rounded.Check, contentDescription = stringResource(id = R.string.ai_tone_apply_cd))',
    ),
    (
        '                    Text(\n'
        '                        text = if (state.toneRewriteSelectedTone != null) \n'
        '                            "Rewriting in ${state.toneRewriteSelectedTone.displayName.lowercase()} tone" \n'
        '                            else "Pick a tone below",',
        '                    Text(\n'
        '                        text = if (state.toneRewriteSelectedTone != null)\n'
        '                            stringResource(id = R.string.ai_tone_rewriting_in, state.toneRewriteSelectedTone.displayName.lowercase())\n'
        '                            else stringResource(id = R.string.ai_tone_pick_below),',
    ),
    (
        '            Text(\n'
        '                "Preview", \n'
        '                style = MaterialTheme.typography.labelSmall, ',
        '            Text(\n'
        '                stringResource(id = R.string.ai_tone_preview),\n'
        '                style = MaterialTheme.typography.labelSmall, ',
    ),
    (
        '                            Text("AI is rewriting...", style = MaterialTheme.typography.bodyMedium)',
        '                            Text(stringResource(id = R.string.ai_tone_in_progress), style = MaterialTheme.typography.bodyMedium)',
    ),
    (
        '                    Text("Apply Changes")',
        '                    Text(stringResource(id = R.string.ai_tone_apply_changes))',
    ),
    (
        '                    Text("Try Again")',
        '                    Text(stringResource(id = R.string.ai_tone_try_again))',
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

if "import androidx.compose.ui.res.stringResource" not in content:
    content = content.replace(
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.res.stringResource\nimport androidx.compose.ui.unit.dp",
        1,
    )
if "import com.suvojeet.notenext.R" not in content:
    content = content.replace(
        "import com.suvojeet.notenext.data.ai.ToneOption",
        "import com.suvojeet.notenext.R\nimport com.suvojeet.notenext.data.ai.ToneOption",
        1,
    )

with open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(content)

print(f"Applied: {applied}/{len(REPLACEMENTS)} edits")
if missing:
    for m in missing: print(f"  MISSING: {m}")
