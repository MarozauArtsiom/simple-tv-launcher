#!/usr/bin/env python3
"""Adds strings to all six language files at once, so no language is ever left behind.

Usage: scripts/add-strings.py < strings.json      (existing keys are replaced, order is kept)

JSON is a list of entries:
  {"key": "close", "en": "Close", "ru": "...", "be": "...", "pl": "...", "de": "...", "fr": "..."}
  {"key": "x", "translatable": false, "en": "Wi-Fi"}                 # only in values/
  {"key": "usb_files", "plural": {"en": {"one": "...", "other": "..."}, "ru": {...}, ...}}
`ConsistencyTest` fails when a language misses a key or a placeholder.
"""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
LANGS = ["en", "ru", "be", "pl", "de", "fr"]


def folder(lang):
    return RES / ("values" if lang == "en" else f"values-{lang}")


def escape(text):
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    # & and < are escaped by ElementTree when it writes the file; doing it here too gave "&amp;amp;".
    return ("\\" + text) if text[:1] in "@?" else text


def load(lang):
    path = folder(lang) / "strings.xml"
    if path.exists():
        return ET.parse(path).getroot()
    return ET.Element("resources")


def put(root, element):
    for existing in root:
        if existing.get("name") == element.get("name"):
            root.remove(existing)
            break
    root.append(element)


def main():
    entries = json.load(sys.stdin)
    roots = {lang: load(lang) for lang in LANGS}
    for e in entries:
        for lang in LANGS:
            if "plural" in e:
                if lang not in e["plural"]:
                    sys.exit(f"{e['key']}: missing plural for {lang}")
                el = ET.Element("plurals", name=e["key"])
                for quantity, text in e["plural"][lang].items():
                    item = ET.SubElement(el, "item", quantity=quantity)
                    item.text = escape(text)
                put(roots[lang], el)
            elif e.get("translatable") is False:
                if lang == "en":
                    el = ET.Element("string", name=e["key"], translatable="false")
                    el.text = escape(e["en"])
                    put(roots[lang], el)
            else:
                if lang not in e:
                    sys.exit(f"{e['key']}: missing {lang}")
                el = ET.Element("string", name=e["key"])
                el.text = escape(e[lang])
                put(roots[lang], el)
    for lang, root in roots.items():
        ET.indent(root, space="    ")
        folder(lang).mkdir(parents=True, exist_ok=True)
        ET.ElementTree(root).write(folder(lang) / "strings.xml", encoding="utf-8", xml_declaration=True)
        (folder(lang) / "strings.xml").write_text((folder(lang) / "strings.xml").read_text(encoding="utf-8").replace("<?xml version='1.0' encoding='utf-8'?>", '<?xml version="1.0" encoding="utf-8"?>') + "\n", encoding="utf-8")
    print(f"updated {len(entries)} entries in {len(LANGS)} languages")


main()
