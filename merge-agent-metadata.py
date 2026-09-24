"""
Merges the native-image-agent output (reachability-metadata.json, GraalVM 23+ format) into the project metadata:
  * <config dir>/reachability-metadata.json - the agent output without the test harness types, sorted
    (the agent is expected to be run with config-merge-dir seeded with this file, so it accumulates);
  * the legacy reflect-config.json / serialization-config.json / jni-config.json / resource-config.json files
    used by GraalVM < 23. Existing entries are never removed: methods/fields are united, boolean flags are OR-ed.
Each legacy file keeps its current formatting and is rewritten only if something was added.

Usage: python merge-agent-metadata.py <agent reachability-metadata.json> <native-image config dir> [--no-legacy]
"""
import argparse
import json
import os
import re

PRIMITIVE_CODES = {"Z": "boolean", "B": "byte", "C": "char", "S": "short", "I": "int", "J": "long",
                   "F": "float", "D": "double"}
SKIP_TYPE_PREFIXES = ("com.dxfeed.NewCases", "com.dxfeed.NativeLibMain", "com.dxfeed.TestJmx")


def normalize(name):
    """[B -> byte[], [Ljava.lang.String; -> java.lang.String[] (used only for matching)."""
    dims = 0
    while name.startswith("["):
        dims += 1
        name = name[1:]
    if dims:
        name = PRIMITIVE_CODES.get(name, name[1:-1] if name.startswith("L") else name)
    return name + "[]" * dims


def java_regex(pattern):
    """Converts Java \\Q...\\E quoting to Python regex syntax."""
    return re.compile(re.sub(r"\\Q(.*?)\\E", lambda m: re.escape(m.group(1)), pattern))


def q(value):
    return json.dumps(value, ensure_ascii=False)


def read(path):
    with open(path, encoding="utf-8-sig") as f:
        return json.load(f)


def write_text(path, text):
    with open(path, "w", encoding="utf-8", newline="\r\n") as f:
        f.write(text)


def write_json(path, data):
    """reflect-config.json and serialization-config.json style."""
    write_text(path, json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def write_jni(path, data):
    """jni-config.json style (compact legacy agent output)."""
    blocks = []
    for e in data:
        lines = ['  "name":' + q(e["name"])]
        if "fields" in e:
            lines.append('  "fields":[' + ", ".join('{"name":' + q(f["name"]) + "}" for f in e["fields"]) + "]")
        if "methods" in e:
            lines.append('  "methods":[' + ", ".join(
                '{"name":' + q(m["name"]) + ',"parameterTypes":[' + ",".join(q(t) for t in m.get("parameterTypes", []))
                + "] }" for m in e["methods"]) + "]")
        if set(e) - {"name", "fields", "methods"}:
            raise ValueError(f"unsupported jni entry {e}")
        blocks.append("{\n" + ",\n".join(lines) + "\n}")
    write_text(path, "[\n" + ",\n".join(blocks) + "\n]\n")


def merge_entry(target, source, stats, label):
    for key, value in source.items():
        if key in ("type", "name", "serializable", "jniAccessible"):
            continue
        if key == "methods":
            known = {(m["name"], tuple(m.get("parameterTypes", []))) for m in target.get("methods", [])}
            added = False
            for m in value:
                covered = target.get("allDeclaredConstructors" if m["name"] == "<init>" else "allDeclaredMethods")
                if not covered and (m["name"], tuple(m.get("parameterTypes", []))) not in known:
                    target.setdefault("methods", []).append(m)
                    added = True
                    stats.append(f"{label} + method {target['name']}#{m['name']}({', '.join(m.get('parameterTypes', []))})")
            if added:
                target["methods"].sort(key=lambda m: m["name"])
        elif key == "fields":
            known = {f["name"] for f in target.get("fields", [])}
            for f in value:
                if not target.get("allDeclaredFields") and f["name"] not in known:
                    target.setdefault("fields", []).append(f)
                    stats.append(f"{label} + field {target['name']}.{f['name']}")
        elif isinstance(value, bool):
            if value and not target.get(key):
                target[key] = True
                stats.append(f"{label} + {key} {target['name']}")
        else:
            raise ValueError(f"unsupported key {key} in {source}")


def merge_into_list(entries, new_entries, stats, label):
    index = {normalize(e["name"]): e for e in entries}
    for src in new_entries:
        key = normalize(src["type"])
        if key not in index:
            entry = {"name": src["type"]}
            entries.append(entry)
            index[key] = entry
            stats.append(f"{label} + type {src['type']}")
        merge_entry(index[key], src, stats, label)
    entries.sort(key=lambda e: e["name"].casefold())


def merge_resources(path, resources, stats):
    """Inserts resources not matched by the existing patterns, keeping the file formatting as is."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    data = json.loads(text)
    patterns = [java_regex(p["pattern"]) for p in data["resources"]["includes"]]
    new_patterns = []
    for r in resources:
        glob = r.get("glob")
        if glob is None or r.get("module"):
            continue
        if any(ch in glob for ch in "*?[{"):
            raise ValueError(f"wildcard globs are not supported: {r}")
        if not any(p.fullmatch(glob) or p.fullmatch("/" + glob) for p in patterns):
            pattern = "\\Q" + glob + "\\E"
            patterns.append(java_regex(pattern))
            new_patterns.append(pattern)
            stats.append(f"resource + {glob}")
    for pattern in new_patterns:
        existing = re.findall(r'"pattern":"((?:[^"\\]|\\.)*)"', text)
        literal = q(pattern)[1:-1]
        preceding = [p for p in existing if json.loads('"' + p + '"').casefold() < pattern.casefold()]
        anchor = '"pattern":"' + (preceding[-1] if preceding else existing[0]) + '"'
        insert = '\n  }, {\n    "pattern":"' + literal + '"'
        pos = text.index(anchor)
        if preceding:
            pos += len(anchor)
            text = text[:pos] + insert + text[pos:]
        else:
            text = text[:pos] + '"pattern":"' + literal + '"\n  }, {\n    ' + text[pos:]
    if new_patterns:
        write_text(path, text)


def write_reachability_metadata(path, agent):
    """Writes the agent output without the test harness types, with a stable order and formatting."""
    data = {}
    reflection = [e for e in agent.get("reflection", []) if not e["type"].startswith(SKIP_TYPE_PREFIXES)]
    if reflection:
        data["reflection"] = sorted(reflection, key=lambda e: e["type"].casefold())
    for key in agent:
        if key != "reflection":
            items = agent[key]
            data[key] = sorted(items, key=lambda e: json.dumps(e, sort_keys=True)) if isinstance(items, list) else items
    write_json(path, data)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("agent_metadata")
    parser.add_argument("config_dir")
    parser.add_argument("--no-legacy", action="store_true", help="do not update the legacy *-config.json files")
    args = parser.parse_args()
    agent = read(args.agent_metadata)
    cfg = args.config_dir
    stats = []

    write_reachability_metadata(os.path.join(cfg, "reachability-metadata.json"), agent)
    if args.no_legacy:
        return

    reflection, jni, serialization = [], [], []
    for e in agent.get("reflection", []):
        if e["type"].startswith(SKIP_TYPE_PREFIXES):
            continue
        if "condition" in e:
            raise ValueError(f"conditional entries are not supported: {e}")
        if e.get("jniAccessible"):
            jni.append(e)
            continue
        if e.get("serializable"):
            serialization.append(e["type"])
        if set(e) - {"type", "serializable"}:
            reflection.append(e)

    path = os.path.join(cfg, "reflect-config.json")
    data, before = read(path), len(stats)
    merge_into_list(data, reflection, stats, "reflect")
    if len(stats) > before:
        write_json(path, data)

    path = os.path.join(cfg, "jni-config.json")
    data, before = read(path), len(stats)
    merge_into_list(data, jni, stats, "jni")
    if len(stats) > before:
        write_jni(path, data)

    path = os.path.join(cfg, "serialization-config.json")
    data, before = read(path), len(stats)
    known = {normalize(t["name"]) for t in data["types"]}
    for name in serialization:
        if normalize(name) not in known:
            data["types"].append({"name": name})
            known.add(normalize(name))
            stats.append(f"serialization + type {name}")
    data["types"].sort(key=lambda t: t["name"].casefold())
    if len(stats) > before:
        write_json(path, data)

    merge_resources(os.path.join(cfg, "resource-config.json"), agent.get("resources", []), stats)

    print("\n".join(stats) if stats else "nothing new")


if __name__ == "__main__":
    main()
