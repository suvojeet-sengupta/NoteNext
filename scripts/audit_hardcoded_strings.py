"""Audit hardcoded user-facing strings across all Kotlin files."""
import os
import re

KT_PATTERNS = [
    r'Text\(\s*"([^"\\]+)"',
    r'Text\(\s*text\s*=\s*"([^"\\]+)"',
    r'contentDescription\s*=\s*"([^"\\]+)"',
    r'label\s*=\s*\{\s*Text\(\s*"([^"\\]+)"',
    r'title\s*=\s*\{\s*Text\(\s*"([^"\\]+)"',
    r'placeholder\s*=\s*\{\s*Text\(\s*"([^"\\]+)"',
]

results = {}
for root, _, files in os.walk("."):
    if any(skip in root for skip in ["build", ".gradle", ".idea", "node_modules"]):
        continue
    for f in files:
        if not f.endswith(".kt"):
            continue
        p = os.path.join(root, f)
        with open(p, encoding="utf-8", errors="ignore") as fh:
            content = fh.read()
        hits = set()
        for pat in KT_PATTERNS:
            for m in re.finditer(pat, content):
                s = m.group(1).strip()
                if (len(s) < 3
                        or s.startswith(("http", "/", "com.", "android."))
                        or "$" in s or "%" in s
                        or s.replace(" ", "").isdigit()):
                    continue
                hits.add(s)
        if hits:
            results[p] = len(hits)

sorted_files = sorted(results.items(), key=lambda x: -x[1])
total = sum(results.values())
print(f"Total hardcoded user-facing strings (heuristic): ~{total}")
print(f"Files with hardcoded strings: {len(results)}")
print()
print("Top 30 by count:")
for p, n in sorted_files[:30]:
    rel = p.replace("\\", "/").lstrip("./")
    print(f"  {n:3d}  {rel}")
