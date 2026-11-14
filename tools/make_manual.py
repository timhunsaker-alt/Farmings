#!/usr/bin/env python3
"""Builds the Field Manual (docs/field-manual.md): docs/manual/<chapter>/<page>.md -> assets/farmings/manual/en_us/<id>.json plus
_index.json.
    python3 tools/make_manual.py           write the JSON
    python3 tools/make_manual.py --check   check pages and that the JSON is up to date; exit 1 on any problem
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, ".."))
PAGES = os.path.join(REPO, "docs", "manual")
ASSETS = os.path.join(REPO, "common", "src", "main", "resources", "assets", "farmings")
OUT = os.path.join(ASSETS, "manual", "en_us")
LANG = os.path.join(ASSETS, "lang", "en_us.json")

CHAPTERS = [
    ("getting_started", "Getting started"),
    ("animals", "Animals"),
    ("genetics", "Genes and breeding"),
    ("buildings", "Buildings"),
    ("fields", "Fields and feed"),
    ("food", "Food and processing"),
    ("waste", "Waste and power"),
]
CHAPTER_IDS = [c for c, _ in CHAPTERS]

INLINE = re.compile(r"\*\*(.+?)\*\*|\*(.+?)\*|\[([^\]]+)\]\(([a-z0-9_\-]+)\)|\{(item|block):([a-z0-9_.\-]+:[a-z0-9_/.\-]+)\}")
ICON = re.compile(r"\{(item|block):([a-z0-9_.\-]+:[a-z0-9_/.\-]+)\}")
NUMBER = re.compile(r"(?<![\w.])\d[\d,]*(?:\.\d+)?(?![\w])")


def front_matter(text, path):
    if not text.startswith("---\n"):
        raise ValueError("%s: no front matter" % path)
    end = text.index("\n---\n", 4)
    meta = {}
    for line in text[4:end].splitlines():
        if line.strip():
            k, _, v = line.partition(":")
            meta[k.strip()] = v.strip()
    return meta, text[end + 5:]


def runs(text):
    """Inline markup to styled runs: text with b/i, links, icons."""
    out = []
    pos = 0
    for m in INLINE.finditer(text):
        if m.start() > pos:
            out.append({"t": text[pos:m.start()]})
        if m.group(1) is not None:
            out.append({"t": m.group(1), "b": True})
        elif m.group(2) is not None:
            out.append({"t": m.group(2), "i": True})
        elif m.group(3) is not None:
            out.append({"t": m.group(3), "link": m.group(4)})
        else:
            out.append({"icon": m.group(6)})
        pos = m.end()
    if pos < len(text):
        out.append({"t": text[pos:]})
    return out


def blocks(body, path):
    out = []
    para = []
    lines = body.splitlines()
    i = 0

    def flush():
        if para:
            out.append({"type": "para", "runs": runs(" ".join(para))})
            para.clear()

    while i < len(lines):
        line = lines[i].rstrip()
        if line.startswith("```grid"):
            flush()
            layers, key = [], {}
            i += 1
            while i < len(lines) and lines[i].strip() != "```":
                g = lines[i].rstrip()
                if g.startswith("layer"):
                    layers.append({"name": g.strip(), "rows": []})
                elif g.startswith("key "):
                    _, ch, block = g.split(None, 2)
                    key[ch] = block.strip()
                elif g.strip():
                    if not layers:
                        raise ValueError("%s: grid row before a 'layer' line" % path)
                    layers[-1]["rows"].append(g.strip())
                i += 1
            for layer in layers:
                for row in layer["rows"]:
                    for ch in row:
                        if ch != "." and ch not in key:
                            raise ValueError("%s: grid character %r has no key" % (path, ch))
            out.append({"type": "grid", "layers": layers, "key": key})
        elif line.startswith("## "):
            flush()
            out.append({"type": "heading", "runs": runs(line[3:])})
        elif line.startswith("- "):
            flush()
            items = []
            while i < len(lines) and lines[i].startswith(("- ", "  ")) and lines[i].strip():
                if lines[i].startswith("- "):
                    items.append(lines[i][2:].strip())
                else:
                    items[-1] += " " + lines[i].strip()
                i += 1
            out.append({"type": "list", "items": [runs(t) for t in items]})
            continue
        elif not line.strip():
            flush()
        elif ICON.sub("", line).strip() == "":
            flush()
            out.append({"type": "icons", "items": [m.group(2) for m in ICON.finditer(line)]})
        else:
            para.append(line.strip())
        i += 1
    flush()
    return out


def known_ids():
    """Farmings IDs from the generated lang; vanilla IDs from the client jar's item definitions, if found."""
    ids = set()
    with open(LANG) as f:
        for k in json.load(f):
            parts = k.split(".")
            if len(parts) == 3 and parts[0] in ("item", "block") and parts[1] == "farmings":
                ids.add("farmings:" + parts[2])
    vanilla = None
    version = ""
    with open(os.path.join(REPO, "gradle.properties")) as f:
        for line in f:
            if line.split("=", 1)[0].strip() == "minecraft_version":
                version = line.split("=", 1)[1].strip()
    jars = glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/%s/minecraft-client.jar" % version))
    for jar in jars:
        try:
            import zipfile
            with zipfile.ZipFile(jar) as z:
                names = [n for n in z.namelist() if n.startswith("assets/minecraft/items/") and n.endswith(".json")]
            if names:
                vanilla = {"minecraft:" + os.path.basename(n)[:-5] for n in names}
                break
        except Exception:
            continue
    return ids, vanilla


def doc_numbers(doc_paths):
    text = ""
    for d in doc_paths:
        with open(os.path.join(REPO, d)) as f:
            text += f.read() + "\n"
    # an old value kept as a note ("(was 0.6)", "corrected from 70.76") is not a current figure
    text = re.sub(r"\b(was|corrected[^.;)|\n]*? from)\b[^)|;\n]*", "", text)
    return {n.replace(",", "") for n in NUMBER.findall(text)}


def walk_runs(page):
    for b in page["blocks"]:
        if b["type"] in ("para", "heading"):
            yield from b["runs"]
        elif b["type"] == "list":
            for item in b["items"]:
                yield from item


def build():
    pages, problems = [], []
    for path in sorted(glob.glob(os.path.join(PAGES, "*", "*.md"))):
        rel = os.path.relpath(path, REPO)
        try:
            with open(path) as f:
                meta, body = front_matter(f.read(), rel)
            for k in ("id", "title", "chapter", "order"):
                if k not in meta:
                    raise ValueError("%s: front matter has no %s" % (rel, k))
            if meta["chapter"] not in CHAPTER_IDS:
                raise ValueError("%s: chapter %s is not one of %s" % (rel, meta["chapter"], CHAPTER_IDS))
            page = {"id": meta["id"], "title": meta["title"], "chapter": meta["chapter"], "order": int(meta["order"]),
                    "icon": meta.get("icon", ""), "docs": [d.strip() for d in meta.get("docs", "").split(",") if d.strip()],
                    "blocks": blocks(body, rel), "_file": rel}
            pages.append(page)
        except (ValueError, OSError) as e:
            problems.append(str(e))
    ids = {}
    for p in pages:
        if p["id"] in ids:
            problems.append("%s: id %s also used by %s" % (p["_file"], p["id"], ids[p["id"]]))
        ids[p["id"]] = p["_file"]
    farmings, vanilla = known_ids()

    def check_id(p, item):
        ns = item.split(":")[0]
        if ns == "farmings" and item not in farmings:
            problems.append("%s: unknown item %s" % (p["_file"], item))
        elif ns == "minecraft" and vanilla is not None and item not in vanilla:
            problems.append("%s: unknown vanilla item %s" % (p["_file"], item))
        elif ns not in ("farmings", "minecraft"):
            problems.append("%s: item %s is not Farmings' or vanilla's" % (p["_file"], item))

    for p in pages:
        if p["icon"]:
            check_id(p, p["icon"])
        numbers = None
        for d in p["docs"]:
            if not os.path.exists(os.path.join(REPO, d)):
                problems.append("%s: doc %s does not exist" % (p["_file"], d))
        if p["docs"] and all(os.path.exists(os.path.join(REPO, d)) for d in p["docs"]):
            numbers = doc_numbers(p["docs"])
        for b in p["blocks"]:
            if b["type"] == "icons":
                for item in b["items"]:
                    check_id(p, item)
            if b["type"] == "grid":
                for item in b["key"].values():
                    check_id(p, item)
        for r in walk_runs(p):
            if "icon" in r:
                check_id(p, r["icon"])
                continue
            if "link" in r and r["link"] not in ids:
                problems.append("%s: link to missing page %s" % (p["_file"], r["link"]))
            for n in NUMBER.findall(r["t"]):
                if numbers is None:
                    problems.append("%s: figure %s but the page names no docs" % (p["_file"], n))
                elif n.replace(",", "") not in numbers:
                    problems.append("%s: figure %s is not in %s" % (p["_file"], n, ", ".join(p["docs"])))
    if vanilla is None:
        print("make_manual: vanilla item list not found; minecraft: IDs are checked by the manual_pages_resolve game test only")
    return pages, problems


def outputs(pages):
    files = {}
    order = {c: i for i, c in enumerate(CHAPTER_IDS)}
    pages = sorted(pages, key=lambda p: (order[p["chapter"]], p["order"], p["id"]))
    for p in pages:
        files[p["id"] + ".json"] = {k: v for k, v in p.items() if not k.startswith("_") and k != "docs"}
    files["_index.json"] = {"chapters": [{"id": c, "title": t, "pages": [p["id"] for p in pages if p["chapter"] == c]}
                                         for c, t in CHAPTERS]}
    return {name: json.dumps(obj, indent=1, ensure_ascii=False) + "\n" for name, obj in files.items()}


def main():
    check = "--check" in sys.argv
    pages, problems = build()
    files = outputs(pages)
    existing = {os.path.basename(f) for f in glob.glob(os.path.join(OUT, "*.json"))}
    if check:
        for name, text in files.items():
            path = os.path.join(OUT, name)
            if not os.path.exists(path) or open(path).read() != text:
                problems.append("%s is out of date: run python3 tools/make_manual.py" % os.path.relpath(path, REPO))
        for name in existing - set(files):
            problems.append("%s has no page: run python3 tools/make_manual.py" % name)
        for p in problems:
            print("make_manual: " + p)
        print("make_manual: %d pages, %d problem%s" % (len(pages), len(problems), "" if len(problems) == 1 else "s"))
        sys.exit(1 if problems else 0)
    if problems:
        for p in problems:
            print("make_manual: " + p)
        sys.exit(1)
    os.makedirs(OUT, exist_ok=True)
    for name in existing - set(files):
        os.remove(os.path.join(OUT, name))
    for name, text in files.items():
        with open(os.path.join(OUT, name), "w") as f:
            f.write(text)
    print("make_manual: wrote %d pages" % len(pages))


if __name__ == "__main__":
    main()
