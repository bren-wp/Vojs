# Originalni VOJS web i brending — bez GitHub Pages

**Oba izvorna ZIP arhiva sa svim binarnim datotekama nalaze se u GitHubu**:
- `source-archives/VOJS.zip` — 87 izvornih datoteka (13 HTML stranica, CSS/JS, fontovi, 2 fotografije i favicon)
- `source-archives/VOJS-Android-brending-1.0.0.zip` — 55 izvornih datoteka (Android bitmap ikone, splash, logotipi, Google Play grafike, PDF smjernice)

Arhivi su preneseni bez preinaka te imaju kontrolne SHA256 sažetke u `source-archives/SHA256SUMS`.

`scripts/import_original_assets.py` provjerava SHA256 i CRC, zatim čuva izvorni web pod `original-web/public_html/`, brending u `branding/` i kopira originalne bitmap Android resurse u `app/src/main/res/`. Workflow `import-original-assets.yml` automatski izvršava uvoz na main grani i zatim komitira izdvojene datoteke.

**`website/` se ne objavljuje.** Sva web dokumentacija ostaje isključivo u GitHub repozitoriju; `pages.yml` je uklonjen. Originalni web služi kao referenca i nije povezan s izvornom Android aplikacijom. Originalne stranice o javnim poveznicama, istekima i brisanju na serveru nisu dostupne funkcije ovog offline izdanja.

**Marketinške konceptualne slike** pod `branding/google-play/snimke-zaslona/` nisu stvarni Android zasloni. Samo `docs/screenshots/`, generiran na emulatoru, smije se prikazati kao stvarna snimka.
