"""Extract hardcoded strings from AiSummarySheet.kt -> stringResource."""

PATH = "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/components/AiSummarySheet.kt"

REPLACEMENTS = [
    (
        '                Text(\n'
        '                    text = "NoteNext AI",\n'
        '                    style = MaterialTheme.typography.titleLarge,',
        '                Text(\n'
        '                    text = stringResource(id = R.string.ai_summary_brand),\n'
        '                    style = MaterialTheme.typography.titleLarge,',
    ),
    (
        '                            val clip = ClipData.newPlainText("AI Summary", summary)',
        '                            val clip = ClipData.newPlainText(context.getString(R.string.ai_summary_clip_label), summary)',
    ),
    (
        '                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()',
        '                            Toast.makeText(context, context.getString(R.string.ai_summary_copied_toast), Toast.LENGTH_SHORT).show()',
    ),
    (
        '                            contentDescription = "Copy Summary",',
        '                            contentDescription = stringResource(id = R.string.ai_summary_copy_cd),',
    ),
    (
        '                                putExtra(Intent.EXTRA_SUBJECT, "Note Summary")',
        '                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ai_summary_share_subject))',
    ),
    (
        '                            context.startActivity(Intent.createChooser(shareIntent, "Share Summary"))',
        '                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.ai_summary_share_chooser)))',
    ),
    (
        '                            contentDescription = "Share Summary",',
        '                            contentDescription = stringResource(id = R.string.ai_summary_share_cd),',
    ),
    (
        '                        contentDescription = "Close",',
        '                        contentDescription = stringResource(id = R.string.ai_summary_close_cd),',
    ),
    (
        '                            Text(\n'
        '                                text = "Summarizing...", \n'
        '                                style = MaterialTheme.typography.bodyMedium,',
        '                            Text(\n'
        '                                text = stringResource(id = R.string.ai_summary_summarizing),\n'
        '                                style = MaterialTheme.typography.bodyMedium,',
    ),
    (
        '                                                Text(\n'
        '                                                    text = if (showThinking) "Hide thinking process" else "Thinking...",\n'
        '                                                    style = MaterialTheme.typography.labelLarge,',
        '                                                Text(\n'
        '                                                    text = if (showThinking) stringResource(id = R.string.ai_summary_hide_thinking) else stringResource(id = R.string.ai_summary_thinking),\n'
        '                                                    style = MaterialTheme.typography.labelLarge,',
    ),
    (
        '                                        Text(\n'
        '                                            text = "AI-generated content can be inaccurate. Please verify important information.",\n'
        '                                            style = MaterialTheme.typography.labelSmall,',
        '                                        Text(\n'
        '                                            text = stringResource(id = R.string.ai_summary_disclaimer),\n'
        '                                            style = MaterialTheme.typography.labelSmall,',
    ),
    (
        '                        } ?: Text(\n'
        '                            "No summary available.", \n'
        '                            style = MaterialTheme.typography.bodyLarge,',
        '                        } ?: Text(\n'
        '                            stringResource(id = R.string.ai_summary_unavailable),\n'
        '                            style = MaterialTheme.typography.bodyLarge,',
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
        "import androidx.compose.ui.platform.LocalContext",
        "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.stringResource",
        1,
    )
if "import com.suvojeet.notenext.R" not in content:
    content = content.replace(
        "import com.suvojeet.notenext.ui.components.springPress",
        "import com.suvojeet.notenext.R\nimport com.suvojeet.notenext.ui.components.springPress",
        1,
    )

with open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(content)

print(f"Applied: {applied}/{len(REPLACEMENTS)} edits")
if missing:
    for m in missing: print(f"  MISSING: {m}")
