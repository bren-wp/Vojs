# VOJS — stvarna izdanja i Android/Play razvoj

Ažurirano 21. studenoga 2026. Projekt nadograđuje Android build na AGP 9.4 i Gradle 9.6 te koristi nove stabilne AndroidX verzije. Cilj za Google Play ostaje API 36, a kompilira se protiv API-ja 37 jer ga zahtijevaju najnovije Compose ovisnosti. Ta su dva zahtjeva odvojena.

## Reprodukcija i izdavanje

Svaki push u `main` pokreće Android build, lint i GitHub Release s testnim debug APK-om te release APK/AAB paketima. Bez četiri pravilno konfigurirana VOJS signing secret-a release APK/AAB ostaju **nepotpisani**. Produkcijski upload certifikat mora biti trajno očuvan i usklađen s Play App Signing računom. Aplikacija nije automatski objavljena u Play Storeu.

Potrebni secrets: `VOJS_KEYSTORE_BASE64`, `VOJS_STORE_PASSWORD`, `VOJS_KEY_ALIAS`, `VOJS_KEY_PASSWORD`. Tajne se ne upisuju u repozitorij.

## Testovi

PR build provodi Android debug/release build, AAB, lint, Play privacy policy gate, API i permission verifikaciju i 16 KiB ELF/ZIP kontrolu. Workflowi pokrenuti nakon spajanja izvode stvarne Android emulator testove na tri veličine/skaliranja, snimaju 12 Android stanja, provjeravaju originalne splash slike i približnu metriku konzistentnosti izvornog vizualnog koncepta te svih 112 web prikaza.

Nijedan test ne jamči prihvaćanje aplikacije u Play Console. Puni postupak opisan je u [PLAY-CONSOLE-SUBMISSION.md](PLAY-CONSOLE-SUBMISSION.md), a točan popis ovisnosti u [DEPENDENCIES-2026.md](DEPENDENCIES-2026.md).

## Web

`website/` se **ne postavlja automatski** na `vojs.com.hr`. Potrebno je rasporediti PHP JSON API, statički web i pravila privatnosti na hosting. Dok to nije učinjeno i verificirano, P2P preko javne domene nije potvrđen. Lokalno snimanje i nativno Android dijeljenje datoteke rade neovisno o VOJS hostingu.
