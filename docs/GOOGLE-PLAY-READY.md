# VOJS — spremnost za Google Play

Ažurirano: **22. studenoga 2026.**

## Stanje i kriteriji

VOJS je **pripremljen za tehničku provjeru**, ali potpisani AAB, produkcijski hosting i odobrenje u Play Consoleu nisu automatski potvrđeni. Ne predstavljati nepotpisane verzije kao produkcijska Play izdanja.

| Provjera | Status i način potvrde |
| --- | --- |
| Android API | minSdk 24, targetSdk 36, compileSdk 37; potvrđuje Gradle/AAPT CI |
| Dopuštenja | Samo RECORD_AUDIO i INTERNET; provjerava manifest i APK |
| Offline Android | Lokalni M4A, pauza/nastavak, reprodukcija, Android Sharesheet i pravila privatnosti ne zahtijevaju VOJS web |
| Četiri emulator profila | Pixel 2/API 29, Pixel 6/API 35, Pixel 6/API 36, Pixel C/API 35 |
| Mediji za Play | Ikona 512×512, feature graphic 1024×500, šest stvarnih ADB snimki; provjera SHA-256 |
| P2P | Opcionalna internetska poveznica; web/E2E provjerava SHA-256, signalizacija je JSON bez audija |
| 16 KiB | CI provjerava ELF/ZIP poravnanje; potreban stvarni 16 KiB emulator/uređaj |
| Potpisani produkcijski APK/AAB | **Nije potvrđeno** dok signing secrets nisu valjani |
| Play Console | **Nije potvrđeno** dok se ne potvrde Data safety, privacy URL i pre-launch report |

## Distribucija i sigurnost izdanja

Workflow `android-release.yml` radi Gradle build, lint i provjere privatnosti/medija. **Ako nema produkcijskog upload ključa, izrađuje samo CI testne artefakte i ne objavljuje novi GitHub Release.** Ako su signing secrets dostupni, provjerava potpis APK/AAB te čeka uspjeh sva tri post-merge CI tijeka:

1. VOJS multi-device Android smoke
2. Capture real Android screenshots
3. VOJS full web visual matrix

Tek zatim dopušta novo verzionirano GitHub izdanje s potpisanim APK i AAB paketima. GitHub Release nije automatska objava u Play Consoleu.

**Potrebni tajni podaci** (pohraniti isključivo kao GitHub Actions secrets): `VOJS_KEYSTORE_BASE64`, `VOJS_STORE_PASSWORD`, `VOJS_KEY_ALIAS`, `VOJS_KEY_PASSWORD`. Potpisni ključ ne smije biti u repozitoriju niti u dokumentaciji. Provjeriti certifikat u odnosu na postojeći Play upload key, Play App Signing i rastući versionCode. Trenutačno objavljena v1.0.48 sadrži nepotpisani release APK/AAB; nije Play-ready.

## Prava offline funkcionalnost

Glavna Android aplikacija snima, zaustavlja, pauzira/nastavlja, preslušava i dijeli lokalnu M4A datoteku preko Android Sharesheeta **bez pokretanja web-stranice**. Dopuštenje mikrofona traži se pri odabiru snimanja. In-app pravila privatnosti mogu se otvoriti offline. Poveznicu korisnik izričito odabire, tada je potreban internet. Aplikacija ne sprema audio na VOJS server.

Emulator test `scripts/test_offline_mode.py` uključuje zrakoplovni način prije prvog pokretanja, snima stvaran zvuk i provjerava lokalnu datoteku, Android dijeljenje i pravila privatnosti. Ugovor `scripts/test_offline_contract.py` štiti granicu. Ne zamjenjivati emulator test samo statičkom provjerom.

## Marketing i javni tekstovi

- [Marketinški README s originalnom ikonom, feature grafikom i stvarnim snimkama](../README.md)
- [Hrvatski kratki i puni opis za Play](PLAY-STORE-LISTING-HR.md)
- [Izvorni Play mediji](play-store-assets/)
- [Provjera marketinga i snimki zaslona](../scripts/test_marketing_readme.py)
- [Službena pravila privatnosti](PRIVACY-POLICY.md)

Screenshotovi su zabilježeni u `docs/screenshots/manifest.json`. Tvrdnja o 1:1 podudaranju ostalih ekrana nije dopuštena bez ekvivalentnih referenci i izmjerenih RGB razlika.

## Preostali vanjski koraci

- Potvrditi produkcijski HTTPS za https://vojs.com.hr/privatnost/ i aktualni JSON signalizacijski API; GitHub ne postavlja web automatski.
- Ispravno ispuniti Data safety i deklaracije za mikrofon, anonimnu P2P signalizaciju, STUN, IP adrese i hosting logove.
- Napraviti interni/zatvoreni Play test i test na stvarnim uređajima, odobrenom upload ključu i 16 KiB konfiguraciji.
- Pregledati Play pre-launch izvještaj i uskladiti dobne skupine, ocjene, marketing i kontakt.
- Objaviti u Google Play Consoleu tek nakon svih provjera i odobrenja.

Poznato ograničenje: izravni prijenos ne uspijeva na svim NAT mrežama bez TURN poslužitelja. Offline dijeljenje datoteke ostaje neovisno.

Dodatno: [Play Console kontrolna lista](PLAY-CONSOLE-SUBMISSION.md) · [Google Play pravila za API](https://support.google.com/googleplay/android-developer/answer/11926878).
