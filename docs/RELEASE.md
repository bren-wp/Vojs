# VOJS: provjera Android CI i izdanja

## Uzrok prethodnog pada
Prethodni workflow koristio je `android-actions/setup-android@v3`, koji je pozivao `sdkmanager tools`; taj paket više ne postoji i build nije ni započeo. Novi CI instalira platformu Android 36 i koristi Gradle 8.11.1 na Ubuntu 24.04.

Raniji GitHub Pages workflow je uklonjen na zahtjev vlasnika. Web ostaje kao izvorni kod u `website/` bez automatskog hostinga.

## Izdanja i ključevi
Svaki push na `main` pokreće Gradle build i nakon uspješne izgradnje GitHub Release s tri datoteke:
- `VOJS-vX-debug.apk` — testni APK, potpisan Android debug ključem,
- `VOJS-vX-unsigned.apk` ili `VOJS-vX-release.apk` — release APK,
- `VOJS-vX-unsigned.aab` ili `VOJS-vX-release.aab` — release AAB.

Za instalabilni produkcijski APK i Google Play AAB potrebne su GitHub Actions secrets:
`VOJS_KEYSTORE_BASE64`, `VOJS_STORE_PASSWORD`, `VOJS_KEY_ALIAS`, `VOJS_KEY_PASSWORD`.
Kod ne stvara i ne sprema vlastite trajne produkcijske ključeve.

## Web
Statična prezentacija je u `website/`; nema GitHub Pages deploya i web ostaje unutar GitHuba.
Aktualna GitHub web prezentacija ima 13 ruta, ali nije binarno ni vizualno identična svim datotekama izvornog arhiva; izvorna web-arhiva i svi binarni materijali moraju se posebno prenijeti za vizualno 1:1 zadržavanje. Ne prikazivati nacrte iz `google-play/snimke-zaslona` kao snimke stvarne aplikacije.

## Privatnost
Snimanje ne koristi VOJS server. FileProvider privremeno izlaže privatni cache za ACTION_SEND. Dijeljenim sadržajem upravlja odabrana vanjska aplikacija.

## Google Play API kompatibilnost (2026)

Od 31. kolovoza 2026. nova Android telefonska izdanja i ažuriranja trebaju ciljati API 36. Trenutačna konfiguracija je `compileSdk = 36`, `targetSdk = 36`, AGP 8.10.1 i Gradle 8.11.1. CI instalira platforms;android-36. `minSdk = 24` je zadržan. Provjeriti na fizičkim uređajima i Android 16, naročito edge-to-edge prikaz, dopuštenje za mikrofon, dijeljenje URI-ja i rad s predmemorijom. Produkcijski potpisani AAB i dalje zahtijeva VOJS signing secrets.
