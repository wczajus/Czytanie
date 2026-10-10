#!/usr/bin/env python3
"""Polish syllabification for the story (D65): one vowel per syllable ("au", "eu" count as one); between vowels a consonant
cluster goes to the next syllable as far as it is a cluster a Polish word can start with (maximal
onset), except that a sonorant (r l ł m n ń j) opening the cluster stays with the previous vowel."""
VOW = set("aąeęioóuy")
SONORANTS = {"r", "l", "ł", "m", "n", "ń", "j"}
# Clusters a Polish word can start with, as joined lowercase consonant units; extended from the dictionary.
ONSETS = set("""st sk sp sm sn sł sw sz szk szt szcz szp szl szn szm szw szcz wsz wsk wst wz ws zw zr zł zm zn zb zd zg zdr
śm śn śp św śl ść śc ćw czł czw chw chł chr chm chl chc dr dw dł dż dźw dzw tr tw tł pr pl pł pt ps psz pch
kr kl kł kw ks kt ksz br bl bł gr gl gł gw gn gd wr wł wl wn wd wk wp wt fr fl rz rzm drz trz prz brz grz krz chrz
sk skr spr str stw zdr szcz szczw wzr wsp wst wsk wspr""".split())
def is_vowel(u): return u.lower() in VOW
def syllables(word_units):
    gs = word_units; low = [g.lower() for g in gs]
    # Nuclei as [start, end) unit ranges; "au" and "eu" ("auto", "Europa") are one nucleus.
    nuclei = []
    for k, g in enumerate(low):
        if g not in VOW: continue
        if g == "u" and nuclei and nuclei[-1][1] == k and low[k - 1] in ("a", "e"): nuclei[-1][1] = k + 1
        else: nuclei.append([k, k + 1])
    if len(nuclei) <= 1: return ["".join(gs)]
    cuts = []
    for (_, end), (start, _) in zip(nuclei, nuclei[1:]):
        cons = low[end:start]
        if len(cons) <= 1: cuts.append(end); continue
        if cons[0] in SONORANTS: cuts.append(end + 1); continue
        k = next(k for k in range(len(cons)) if k == len(cons) - 1 or "".join(cons[k:]) in ONSETS)
        cuts.append(end + k)
    parts, prev = [], 0
    for c in cuts + [len(gs)]:
        parts.append("".join(gs[prev:c])); prev = c
    return parts
def learn_onsets(words_units):
    for us in words_units:
        low = [u.lower() for u in us]; c = []
        for u in low:
            if u in VOW: break
            c.append(u)
        if len(c) >= 2 and len(c) < len(low): ONSETS.add("".join(c))


if __name__ == "__main__":
    # Prints the syllables of the words given as units, e.g.: python3 tools/syllabify.py "z a w sz e"
    import sys
    for arg in sys.argv[1:]:
        print("-".join(syllables(arg.split())))
