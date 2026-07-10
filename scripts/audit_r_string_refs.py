"""Audit R.string refs in app module Kotlin files; flag refs that resolve only via core.R."""
import re, os, subprocess

# Get .kt files modified in last 3 commits
result = subprocess.run(['git', 'diff', '--name-only', 'HEAD~3', 'HEAD'], capture_output=True, text=True)
files = [f for f in result.stdout.strip().split('\n') if f.endswith('.kt')]

# Collect app's own R keys (only values/ folders, not values-xx/)
app_keys = set()
for root, _, fs in os.walk('app/src/main/res/values'):
    for f in fs:
        if f.endswith('.xml'):
            with open(os.path.join(root, f), encoding='utf-8') as fh:
                app_keys.update(re.findall(r'<string name="(\w+)"', fh.read()))

# Collect core's R keys
core_keys = set()
for root, _, fs in os.walk('core/src/main/res/values'):
    for f in fs:
        if f.endswith('.xml'):
            with open(os.path.join(root, f), encoding='utf-8') as fh:
                core_keys.update(re.findall(r'<string name="(\w+)"', fh.read()))

print(f"App keys (values/ root): {len(app_keys)}")
print(f"Core keys (values/ root): {len(core_keys)}")
print()

bad = []
for f in files:
    if not os.path.exists(f): continue
    if not f.startswith('app/'): continue
    content = open(f, encoding='utf-8').read()
    # Determine which R is imported
    has_core_import = 'import com.suvojeet.notenext.core.R' in content
    # Find naked R.string refs (not preceded by `core.`)
    for m in re.finditer(r'(?<![\w.])R\.string\.(\w+)', content):
        key = m.group(1)
        # skip if imported as core.R
        if has_core_import:
            # this would mean the file aliases core.R as R; check for app.R import
            if 'import com.suvojeet.notenext.R' not in content:
                continue  # all R refs go through core
        # If app.R is the imported R, key must be in app
        if key not in app_keys:
            if key in core_keys:
                bad.append((f, key, 'CORE-ONLY (use core.R)'))
            else:
                bad.append((f, key, 'UNDEFINED'))

# Dedupe
seen = set()
for f, k, reason in bad:
    sig = (f, k, reason)
    if sig in seen: continue
    seen.add(sig)
    print(f"{f}:{k} -> {reason}")
print(f"\nTotal issues: {len(seen)}")
