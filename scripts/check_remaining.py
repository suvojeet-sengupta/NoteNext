"""Check remaining hardcoded strings in BackupScreen.kt."""
import re

content = open(
    "app/src/main/java/com/suvojeet/notenext/ui/settings/BackupScreen.kt",
    encoding="utf-8",
).read()

# Use raw strings only, avoid escape collisions in source.
PATTERNS = [
    r'Text\(\s*"([^"]+?)"',
    r'contentDescription\s*=\s*"([^"]+?)"',
    r'Text\(\s*text\s*=\s*"([^"]+?)"',
]
remaining = []
for pat in PATTERNS:
    for m in re.finditer(pat, content):
        s = m.group(1).strip()
        if (
            len(s) >= 3
            and not s.startswith(("http", "/", "com.", "android."))
            and "$" not in s
            and not s.replace(" ", "").isdigit()
        ):
            remaining.append(s)
print(f"Remaining hardcoded strings (heuristic): {len(remaining)}")
for s in sorted(set(remaining)):
    print(f'  "{s}"')
