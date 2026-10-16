#!/usr/bin/env python3
"""Verifie le corpus dore de detection (app/src/test/resources/detection/corpus.json).

Usage : python3 -I docs/qa/verify_corpus.py [racine-du-depot]

Controles : JSON valide, champs obligatoires, ids de cas uniques, entryId present dans
la table, comptes par categorie et par piege, part de pieges >= 30 % par categorie, puis
mesure de reference (moteur de l'ADR 0008 reimplemente : jetons, normalisation NFD,
correspondance exacte de suites de jetons, sans fuzzy) et coherence des attendus.
Le script n'est PAS le moteur de production : il sert a detecter les erreurs du corpus.
"""
import json, re, sys, os, unicodedata
from collections import Counter, defaultdict

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
TAGS_OK = {"direct", "inflection", "elision", "accent", "case", "punctuation", "negation",
           "unlisted-inflection", "synonym", "substring", "homograph", "typo", "neutral"}
# tags dont l'attendu humain diverge VOLONTAIREMENT du moteur v1 (mesure du rappel / precision)
KNOWN_GAP = {"unlisted-inflection", "homograph"}
NOT_TRAP = {"direct", "neutral"}
errors = []

def norm(tok):
    s = unicodedata.normalize("NFD", tok.lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("œ", "oe").replace("æ", "ae")

def toks(text):
    return [norm(m.group()) for m in re.finditer(r"[^\W\d_][^\W\d_]*", text)]

def engine_hit(text, kws):
    tt = toks(text)
    for k in kws:
        kt = toks(k)
        for i in range(len(tt) - len(kt) + 1):
            if tt[i:i + len(kt)] == kt:
                return True
    return False

tables = {}
for t in ("story_elements", "sensory_words"):
    d = json.load(open(f"{ROOT}/app/src/main/assets/tables/{t}.json", encoding="utf-8"))
    tables[t] = {e["id"]: e for e in d["entries"]}

corpus = json.load(open(f"{ROOT}/app/src/test/resources/detection/corpus.json", encoding="utf-8"))
cases = corpus["cases"]
ids = Counter(c["id"] for c in cases)
by_cat = defaultdict(list)
tp = fp = fn = tn = 0
gap = Counter()
for c in cases:
    if set(c) != {"id", "entryId", "table", "text", "expected", "tag"}:
        errors.append(f"{c.get('id')}: champs inattendus {sorted(c)}")
        continue
    if ids[c["id"]] > 1:
        errors.append(f"{c['id']}: id duplique")
    if c["tag"] not in TAGS_OK:
        errors.append(f"{c['id']}: tag inconnu {c['tag']}")
    if not isinstance(c["expected"], bool) or not c["text"].strip():
        errors.append(f"{c['id']}: expected/text invalide")
    e = tables.get(c["table"], {}).get(c["entryId"])
    if e is None:
        errors.append(f"{c['id']}: entryId {c['entryId']} absent de {c['table']}")
        continue
    by_cat[(c["table"], e["category"])].append(c)
    got = engine_hit(c["text"], e["keywords"])
    if got and c["expected"]: tp += 1
    elif got: fp += 1
    elif c["expected"]: fn += 1
    else: tn += 1
    if got != c["expected"]:
        if c["tag"] in KNOWN_GAP:
            gap[c["tag"]] += 1
        else:
            errors.append(f"{c['id']} [{c['tag']}] attendu={c['expected']} reference={got} : {c['text']}")
    elif c["tag"] in KNOWN_GAP:
        errors.append(f"{c['id']}: tag {c['tag']} mais le moteur de reference est d'accord (tag incorrect)")

print(f"{len(cases)} cas, {sum(c['expected'] for c in cases)} positifs, {sum(not c['expected'] for c in cases)} negatifs")
print(f"{'categorie':<24}{'cas':>5}{'pos':>5}{'neg':>5}{'pieges':>8}{'%':>6}")
for key in sorted(by_cat):
    cs = by_cat[key]
    tr = sum(c["tag"] not in NOT_TRAP for c in cs)
    pct = 100 * tr / len(cs)
    print(f"{key[0][:5]+'/'+key[1]:<24}{len(cs):>5}{sum(c['expected'] for c in cs):>5}"
          f"{sum(not c['expected'] for c in cs):>5}{tr:>8}{pct:>6.0f}")
    if pct < 30:
        errors.append(f"{key}: pieges {pct:.0f} % < 30 %")
    if len(cs) < 90:
        errors.append(f"{key}: moins de 90 cas")
print("\npar type :")
for tag, n in sorted(Counter(c["tag"] for c in cases).items(), key=lambda x: -x[1]):
    print(f"  {tag:<22}{n:>5}")
alltr = sum(c["tag"] not in NOT_TRAP for c in cases)
print(f"\npieges total : {alltr}/{len(cases)} = {100*alltr/len(cases):.0f} %")
rec = tp / (tp + fn) if tp + fn else 0
prec = tp / (tp + fp) if tp + fp else 0
print(f"moteur de reference (v1, sans fuzzy) : rappel {rec:.3f}  precision {prec:.3f}  (tp={tp} fp={fp} fn={fn} tn={tn})")
print("ecarts voulus :", dict(gap))
if errors:
    print(f"\n{len(errors)} ERREUR(S) :")
    for x in errors:
        print("  -", x)
    sys.exit(1)
print("\nOK")
