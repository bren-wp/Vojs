# VOJS — vizualna provjera svih web stranica

Za stvarnu tvrdnju 1:1 potrebna je izvorna referentna slika istog stanja, veličine i preglednika. Prikaz bez reference nije uspješno verificiran.

## Automatska matrica

GitHub Actions postupak VOJS full web visual matrix pokreće lokalni PHP poslužitelj i stvarni Chromium. Snima svih 14 javnih ruta na širinama 320, 390, 820 i 1440 px u tamnoj i svijetloj temi. Rezultat su 112 stvarnih PNG datoteka, manifest i pixel-audit u artefaktu akcije; slike se ne generiraju u razgovoru.

Provjerava se zadana ili ručno odabrana tema, tipka za promjenu teme (minimalno 44 px), ispravnost lokalnih grafičkih elemenata, odsutnost horizontalnog preljeva i JavaScript iznimaka.

## Usporedba s izvornim referencama

Ako su prave reference dostupne, postavljaju se u direktorij docs/web-reference/<velicina>/<tema>/<stranica>.png. Primjer je docs/web-reference/phone/dark/snimi.png. Imenovanje točno odgovara putanjama snimljenih datoteka.

Pomoću skripte scripts/audit_web_visual.py uspoređuju se RGB pikseli stvarnih i referentnih snimki, uz provjeru jednakih dimenzija. Izvještaj po svakom zaslonu daje status: exact, different, wrong_size ili missing_reference. Neusporediva referenca nikada ne dobiva 0% odstupanja. Opcija --strict zahtijeva 112 identičnih referenci ili prijavljuje neuspjeh.

Za strogi režim referentni materijali moraju odgovarati i verziji Chromiuma, DPR-u, fontu, visini i stanju sučelja. Uz fiksni Chromium i isključene animacije razlike su obično manje, ali to ne dokazuje vizualnu jednakost proizvoljnih ekrana i uređaja.

## Lokalno izvođenje

~~~bash
npm install --no-save playwright
npx playwright install chromium
php -S 127.0.0.1:8765 -t website &
node scripts/capture_web_screenshots.mjs
python scripts/audit_web_visual.py --captures artifacts/web-visual --references docs/web-reference
~~~

Niti jedna automatizacija ne smije izmišljati vrijednosti reference ili mijenjati izvorne korisničke slike. Android audit i stvarne emulator snimke opisani su u DEVICE-TEST-MATRIX.md.

Ova provjera radi na lokalnom web poslužitelju. Ne dokazuje da je produkcijska domena vojs.com.hr ažurirana niti potvrđuje P2P prijenos preko stvarnih mreža.
