# Izvorni VOJS web i Android brending

Originalni `VOJS.zip` i `VOJS-Android-brending-1.0.0.zip` sadrže 13 HTML stranica, 2 izvorne fotografije, fontove, JavaScript/CSS pakete, Android PNG resurse i Google Play grafike.

Repozitorij uključuje `scripts/import_original_assets.py`. Stavi originalna dva ZIP-a u isti lokalni direktorij i pokreni:

```bash
python3 scripts/import_original_assets.py /putanja/do/zipova
```

Skripta sigurno kopira web **bez izmjena** u `original-web/public_html/`, kompletan branding u `branding/`, te Android bitmap ikone u `app/src/main/res/`. Zatim je potreban commit/push promjena na GitHub.

**Napomena:** Originalna web aplikacija govori o poveznicama i roku trajanja snimki. Bez servera te funkcije nisu dostupne; postojeći `website/` za objavu opisuje samo stvarni lokalni APK tijek rada. Originalni web čuva se kao referenca, ne postavlja se automatski u produkciju.

**Snimke:** tri slike iz originalnog Play brendinga su konceptualne. Samo `docs/screenshots/` koje stvori workflow [Capture real Android screenshots](https://github.com/bren-wp/Vojs/actions/workflows/screenshots.yml) iz emulatora smiju se nazvati stvarnima.
