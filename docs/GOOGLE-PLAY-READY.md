# VOJS — kontrolni popis spremnosti za Google Play

Ažurirano: 21. studenoga 2026.

Provjere izvornog koda i Google Play Console obveze navedene su u [PLAY-CONSOLE-SUBMISSION.md](PLAY-CONSOLE-SUBMISSION.md). Status se ne smije označiti „odobreno” bez stvarnog Google Play pregleda.

Trenutačne provjere: API 36, minSdk 24, RECORD_AUDIO/INTERNET, uređajne kopije i D2D isključene, lokalni FileProvider, autentični izvori light/dark splash slika, stvarni emulator screenshotovi, P2P JSON bez serverskog audija, 16 KiB ELF/ZIP provjera, Gradle release APK/AAB i lint. Službeni upload potpis, stvarni javni HTTPS web i Data Safety obrazac zahtijevaju ručnu verifikaciju.

## Potrebno prije objave

- Produkcijski AAB potpisan ispravnim upload ključem, Play App Signing i rastući versionCode.
- Dostupna i istinita javna pravila privatnosti, uvjeti i kontakt na domeni.
- App content / Content rating / Data safety, uključen P2P/STUN tok metapodataka, Google Play test track i Pre-launch report.
- Stvarni uređaji, P2P preko različitih mreža i 16 KiB page-size test.
- Originalni splash: pixel-podudarnost mjerena samo u definiranoj ROI regiji, bez statusne i navigacijske trake. Za ostale zaslone nedostaju ekvivalentne referentne slike.

Objava izdanja na GitHubu sama po sebi ne objavljuje ili potpisuje aplikaciju za Play Store i ne postavlja `website/` na produkcijsku domenu.
