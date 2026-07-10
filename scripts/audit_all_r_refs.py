"""Audit all R.string refs in app module Kotlin files."""
import re, os

# Collect app's own R keys (values/ only)
app_keys = set()
for root, _, fs in os.walk('app/src/main/res/values'):
    for f in fs:
        if f.endswith('.xml'):
            with open(os.path.join(root, f), encoding='utf-8') as fh:
                app_keys.update(re.findall(r'<string name="(\w+)"', fh.read()))

# Core keys
core_keys = set()
for root, _, fs in os.walk('core/src/main/res/values'):
    for f in fs:
        if f.endswith('.xml'):
            with open(os.path.join(root, f), encoding='utf-8') as fh:
                core_keys.update(re.findall(r'<string name="(\w+)"', fh.read()))

# Walk all .kt under app/src/main/java
bad = []
for root, _, fs in os.walk('app/src/main/java'):
    for f in fs:
        if not f.endswith('.kt'): continue
        path = os.path.join(root, f)
        content = open(path, encoding='utf-8').read()
        # Skip if file imports core.R only (not app.R)
        has_app_R = 'import com.suvojeet.notenext.R\n' in content or 'import com.suvojeet.notenext.R$' in content
        has_core_R = 'import com.suvojeet.notenext.core.R' in content
        # If only core.R imported, all `R.string.*` go to core
        # If app.R imported, naked `R.string.*` goes to app

        for m in re.finditer(r'(?<![\w.])R\.string\.(\w+)', content):
            key = m.group(1)
            if has_app_R or not has_core_R:
                # Naked R is app.R
                if key not in app_keys:
                    if key in core_keys:
                        bad.append((path, key, 'CORE-ONLY'))
                    else:
                        bad.append((path, key, 'UNDEFINED'))

seen = set()
for f, k, reason in bad:
    if (f, k) in seen: continue
    seen.add((f, k))
    print(f"{f}:{k} -> {reason}")
print(f"\nTotal: {len(seen)}")
