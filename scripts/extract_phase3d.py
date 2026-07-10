"""Extract hardcoded strings for Phase 3d files (NoteSearchBar, ChecklistEditor, AddEditNoteDialogs, Dialogs).

Each file gets a list of (old, new) tuples and required imports added.
"""

FILES = [
    {
        "path": "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/components/NoteSearchBar.kt",
        "replacements": [
            ('contentDescription = "Close search"', 'contentDescription = stringResource(id = R.string.note_search_close_cd)'),
            ('Text("Find in note...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))',
             'Text(stringResource(id = R.string.note_search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))'),
            ('contentDescription = "Next"', 'contentDescription = stringResource(id = R.string.note_search_next_cd)'),
            ('contentDescription = "Previous"', 'contentDescription = stringResource(id = R.string.note_search_previous_cd)'),
        ],
        "anchor_for_imports": "import androidx.compose.material3",
        "needs_R": True,
        "needs_stringResource": True,
    },
    {
        "path": "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/components/ChecklistEditor.kt",
        "replacements": [
            ('Text("Delete")', 'Text(stringResource(id = R.string.checklist_delete))'),
            ('contentDescription = "Delete Item"', 'contentDescription = stringResource(id = R.string.checklist_delete_item_cd)'),
            ('contentDescription = "Delete all"', 'contentDescription = stringResource(id = R.string.checklist_delete_all)'),
            ('contentDescription = "Reorder"', 'contentDescription = stringResource(id = R.string.checklist_reorder_cd)'),
        ],
        "anchor_for_imports": "import androidx.compose.material3",
        "needs_R": True,
        "needs_stringResource": True,
    },
    {
        "path": "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/components/AddEditNoteDialogs.kt",
        "replacements": [
            ('Text("Self-Destruct Timer")', 'Text(stringResource(id = R.string.note_dialog_self_destruct_title))'),
            ('Text("Select Time")', 'Text(stringResource(id = R.string.note_dialog_select_time))'),
            ('Text("Remove Timer")', 'Text(stringResource(id = R.string.note_dialog_remove_timer))'),
            ('Text("Set")', 'Text(stringResource(id = R.string.note_dialog_set))'),
            ('Text("Custom...")', 'Text(stringResource(id = R.string.note_dialog_custom))'),
            ('Text("Cancel")', 'Text(stringResource(id = R.string.cancel))'),
            ('Text("Next")', 'Text(stringResource(id = R.string.note_search_next_cd))'),
        ],
        "anchor_for_imports": "import androidx.compose.material3",
        "needs_R": True,
        "needs_stringResource": True,
    },
    {
        "path": "app/src/main/java/com/suvojeet/notenext/ui/add_edit_note/components/Dialogs.kt",
        "replacements": [
            ('Text("Insert Link")', 'Text(stringResource(id = R.string.link_dialog_title))'),
            ('Text("Link Options")', 'Text(stringResource(id = R.string.link_dialog_section_options))'),
            ('label = { Text("URL") }', 'label = { Text(stringResource(id = R.string.link_dialog_url)) }'),
            ('label = { Text("Display Text") }', 'label = { Text(stringResource(id = R.string.link_dialog_display_text)) }'),
            ('Text("Insert")', 'Text(stringResource(id = R.string.link_dialog_insert))'),
        ],
        "anchor_for_imports": "import androidx.compose.material3",
        "needs_R": True,
        "needs_stringResource": True,
    },
]

for entry in FILES:
    path = entry["path"]
    try:
        content = open(path, encoding="utf-8").read()
    except FileNotFoundError:
        print(f"SKIP (missing): {path}")
        continue

    applied = 0
    missing = []
    for old, new in entry["replacements"]:
        if old in content:
            content = content.replace(old, new)
            applied += 1
        else:
            missing.append(old[:60])

    if entry["needs_stringResource"] and "import androidx.compose.ui.res.stringResource" not in content:
        anchor = entry["anchor_for_imports"]
        # Find first matching import line and add after it
        lines = content.split("\n")
        for i, line in enumerate(lines):
            if anchor in line:
                lines.insert(i + 1, "import androidx.compose.ui.res.stringResource")
                break
        content = "\n".join(lines)

    if entry["needs_R"] and "import com.suvojeet.notenext.R" not in content:
        # Add after package line
        lines = content.split("\n")
        for i, line in enumerate(lines):
            if line.startswith("package "):
                lines.insert(i + 2, "import com.suvojeet.notenext.R")
                break
        content = "\n".join(lines)

    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(content)

    print(f"{path}: applied {applied}/{len(entry['replacements'])}", end="")
    if missing:
        print(f"  MISSING: {missing}")
    else:
        print()
