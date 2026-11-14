# VOJS — kontrolni popis spremnosti za Google Play

Datum provjere: 14. studenoga 2026.

## Automatski provjerljive stavke

- Android aplikacija: `com.brendigo.vojs`, `minSdk=24` i `targetSdk=36`. Google Play od 31. kolovoza 2026. zahtijeva API 36 za nove telefonske aplikacije i ažuriranja.
- Deklariran je INTERNET za dobrovoljno P2P povezivanje te RECORD_AUDIO za snimanje; ne koristi se ugrađeni analitički/oglasni SDK.
- Dopuštenje `RECORD_AUDIO` zatraženo tijekom uporabe; korisniku je pružena funkcija odbijanja i poruka o nedostupnom mikrofonu.
- `FileProvider` nije izvezen i dijeli samo privatni privremeni audio cache.
- `allowBackup=false` i `usesCleartextTraffic=false` u manifestu.
- Originalne Android ikone i svijetli/tamni splash su uključeni, uz 2,5-sekundni nativni prikaz.
- CI izrađuje debug APK, release APK i AAB, izvodi lint, radi provjeru izvornih resursa, realno snimanje zaslona i izvještaj o pikselnim odstupanjima.
- AndroidX Compose može tranzitivno uključiti nativnu biblioteku `libandroidx.graphics.path.so`. CI iz APK/AAB stvarno čita ELF `PT_LOAD` zapise kroz `scripts/check_native_alignment.py` i provodi `zipalign -c -P 16 -v 4`. Nije dovoljno pretpostaviti da aplikacija nema .so datoteka zato što sama ne koristi NDK.

## Obvezno dovršiti u Play Console / na fizičkim uređajima

- Konfigurirati **vlastiti dugoročno čuvani production upload ključ** kroz GitHub Secrets. Nepotpisani AAB nije za Play. Verificirati potpis i povećanje `versionCode`.
- Koristiti **Google Play App Signing** i potvrditi preostale zahtjeve za registraciju/potpisivanje na konkretnom računu.
- U **App content** ispuniti Data safety (WebRTC SDP signalizacija, STUN mrežni podaci i sigurnosna obrada IP adresa), Content rating, Target audience, Ads, App access, deklaracije dopuštenja, te po potrebi dodatne obrasce. U obrascu Data safety navesti stvarno postupanje s podacima prema primateljskim aplikacijama; bez automatske tvrdnje 'no data collected' bez provjere.
- Objaviti ažurirana [pravila privatnosti](PRIVACY-POLICY.md) na https://vojs.com.hr/privatnost/ i unijeti URL u Play listing. Web se ne postavlja automatski.
- Testirati Android 7/10/13/15/16, telefon i tablet, svijetlu/tamnu temu, dopuštenje/odbijanje mikrofona, 5-minutni limit, prekid snimanja, pauzu/nastavak, reprodukciju, pozadinsko ponašanje i FileProvider sa stvarnim aplikacijama za dijeljenje.
- Uz statičku provjeru ELF/ZIP poravnanja, testirati instalaciju i korištenje aplikacije na **stvarnom ili emulatornom Android uređaju s memorijskim stranicama od 16 KB**, uključujući RELRO granice. Potvrditi rezultat Google Play pre-launch reporta.
- Za trgovinu su potrebne **stvarne snimke zaslona**, ne konceptualni mockupovi; dostupne su u `docs/screenshots/`. Za dokaz 1:1 provjeriti stvarne ROI razlike na različitim gustoćama i omjerima zaslona.
- Proći Google Play pre-launch report i provjeriti rezultat, uključujući eventualni zahtjev za zatvoreno testiranje prema vrsti računa.

## Reference

- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/jetpack/androidx/releases/core
- https://developer.android.com/jetpack/androidx/releases/activity
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler

## Dobrovoljno online dijeljenje

- Provesti QA stvarnog HTTPS hostinga i stvarne sinkronizacije poveznica Android ↔ Web.
- Provjeriti pravila za korisnički generiran sadržaj (UGC), postupke prijave zloupotrebe i uklanjanja.
- Ne označiti automatski 'no data collected': online upload prenosi SDP i koristi sigurnosnu obradu IP-a.
- Offline način mora raditi bez internetske veze. [Upute za deployment](SHARING-DEPLOYMENT.md).

## Novo: bez poslužiteljske pohrane audija

- Stari endpointi vraćaju 410, bez novih snimki na serveru. Migracija prethodne poslužiteljske pohrane traži ručni sigurni zahvat na hostingu.
- Provjeriti da Android WebView sender ne pristupa lokalnim datotekama izvan dopuštenog cachea niti dopušta vanjsku navigaciju.
- Provjeriti hrvatski i engleski opis podataka za Play Data safety i aplikacijsku politiku privatnosti, uključujući privremene SDP mrežne podatke i STUN IP.
- UGoogle Play listingu ne obećavati trajne online poveznice ni uspješnost iza svih NAT-ova bez TURN infrastrukture.
- API 36, minSdk 24 i 16KB page size kontrola ostaju obvezni u CI, uz emulator i fizičke uređaje.
