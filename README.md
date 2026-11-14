<div align="center">
<img src="branding/logotip/logotip-svijetla-podloga.png" alt="VOJS" width="245">

# VOJS
### Neke stvari bolje zvuče.
**Snimi glas. Preslušaj. Podijeli trenutak.**

**Android + Web • Bez računa • Offline snimanje • P2P dijeljenje bez pohrane zvuka na serveru**

[🌐 VOJS web](https://vojs.com.hr) · [📱 Najnoviji Android APK/AAB](https://github.com/bren-wp/Vojs/releases/latest) · [💜 BRENDIGO](https://brendigo.com)
</div>

---

## Glasovne poruke jednostavno, bez prijave

VOJS omogućuje snimanje glasovnih poruka do pet minuta, pauziranje, preslušavanje i dijeljenje. Privatnost je dio dizajna: **za lokalno snimanje ne treba internet**, a server **ne prima audio datoteke**.

| 📱 Android | 🌐 VOJS web |
| --- | --- |
| Nativni Kotlin + Jetpack Compose | Responzivni web snimač s osnovnom PWA offline podrškom |
| Snimanje i M4A preslušavanje bez interneta | Snimanje u pregledniku i lokalno preuzimanje |
| Android Sharesheet za slanje datoteke | Web Share API kada je podržan |
| Izravni WebRTC prijenos iz Android aplikacije | P2P primanje u pregledniku bez instalacije |
| Bez obveznog računa, oglasa i VOJS clouda | Bez SQL baze, bez crona, bez pohrane zvuka na serveru |

## Kako radi P2P poveznica?

1. Snimi poruku na Androidu ili webu.
2. Odaberi **Podijeli izravno (P2P)**. VOJS izradi nepredvidljivu poveznicu s kratkotrajnom WebRTC signalizacijom.
3. Pošalji poveznicu i **ostani na zaslonu za dijeljenje**.
4. Primatelj otvori poveznicu dok si online. Zvuk se prenosi kroz šifrirani WebRTC DataChannel i može se preslušati ili spremiti na primateljevu uređaju.

**Nema pohrane audio snimki na VOJS poslužitelju.** PHP pohranjuje samo privremenu JSON signalizaciju (SDP, rok isteka i hash tokena), u direktorij izvan javnog web korijena. Sesija istječe za najviše 15 minuta; stare JSON datoteke čiste se prilikom stvaranja novih sesija, bez cron posla.

**Važna ograničenja:** oba uređaja moraju istodobno biti online. Poveznica nije trajni cloud snimak. Neke mobilne mreže i strogi NAT zahtijevaju TURN poslužitelj, koji u ovom izdanju nije konfiguriran. P2P tada može zakazati; kao pouzdanu alternativu koristi dijeljenje M4A datoteke.

## Stvarne Android snimke zaslona

Navedene slike snimljene su na Android emulatoru. Nisu generirani mockupovi i ne predstavljaju još neprovjerene nove P2P zaslone.

<table>
<tr>
<td><img src="docs/screenshots/01-home.png" alt="VOJS početni zaslon" width="230"></td>
<td><img src="docs/screenshots/02-recording.png" alt="VOJS snimanje" width="230"></td>
<td><img src="docs/screenshots/03-ready.png" alt="VOJS završena snimka" width="230"></td>
</tr>
<tr><td><strong>Početak</strong></td><td><strong>Snimanje</strong></td><td><strong>Preslušavanje</strong></td></tr>
</table>

[Svi stvarni prikazi](docs/screenshots/) · [Android testovi](https://github.com/bren-wp/Vojs/actions) · [Vizualna kvaliteta](docs/VISUAL-QA.md)

## Privatnost i sigurnost

- **Mikrofon:** dopuštenje se traži samo za snimanje; nema prikrivenog snimanja u pozadini.
- **Lokalne datoteke:** Android privremeno drži M4A snimke u svom privatnom cacheu radi reprodukcije i dijeljenja. Cache se čisti lokalno; primatelj može zadržati vlastitu kopiju.
- **Server:** nema baze podataka, snimki, javnog imenika audija niti scheduled cron joba. Postojeći legacy upload/audio/delete API vraćaju HTTP 410.
- **JSON signalizacija:** do 15 minuta s nasumičnim ID-om; sadrži SDP koji može uključivati mrežne informacije. Server privremeno obrađuje IP adrese za zaštitu od zloupotrebe.
- **WebRTC:** šifrirani DTLS prijenos između krajnjih uređaja; Googleov STUN poslužitelj može vidjeti mrežne podatke. TURN relay nije konfiguriran.
- **Pristup bez računa:** povezivanje se provodi putem teško pogodive poveznice; svatko tko je dobije može pokušati primiti prijenos dok je pošiljatelj online.
- **Nema aplikacijske analitike ni oglasa.** Za stvarni hosting još je potrebno uskladiti pristupne logove i javno objavljenu politiku privatnosti.

[Pravila privatnosti](docs/PRIVACY-POLICY.md) · [WebRTC arhitektura](docs/P2P-ARCHITECTURE.md) · [Hosting upute](docs/SHARING-DEPLOYMENT.md)

## Organizacija koda

| Dio | Datoteke |
| --- | --- |
| Android UI / lokalno snimanje | MainActivity.kt, VoiceScreen.kt, VoiceWaveform.kt, VoiceRecorder.kt |
| Android ↔ Web P2P | PeerSenderActivity.kt, VoiceLinkService.kt, app/src/main/assets/p2p-android-sender.html |
| Web snimač i prijem | website/snimi/, website/recorder.js, website/poruka/, website/message.js |
| WebRTC peer-to-peer | website/p2p.js |
| PHP JSON signalizacija | website/api/_signal.php, website/api/signal.php, website/api/status.php |
| Web offline podrška | website/sw.js |
| Testovi | scripts/test_web_link_api.sh, .github/workflows/ |

## Google Play

Aplikacija ima applicationId com.brendigo.vojs, minSdk 24 i targetSdk 36. Dopuštenja su RECORD_AUDIO i INTERNET; nema obveznih računa i nema VOJS audio servera.

Android release workflow izrađuje debug APK, nepotpisani release APK/AAB i, samo uz valjane signing secrets, potpisane release pakete. **Nepotpisani AAB nije spreman za Play.** Službena Play spremnost zahtijeva još provjeru produkcijskog HTTPS-a, Data safety dokumentacije, privatnosti, pre-launch reporta, WebRTC prijenosa i stvarnih uređaja.

[Provjere za Google Play](docs/GOOGLE-PLAY-READY.md) · [Izdanja](https://github.com/bren-wp/Vojs/releases)

## Postavljanje na hosting

Za web je dovoljan HTTPS shared hosting s PHP 8.1+ i zapisivim privatnim direktorijem za **JSON signalizaciju**. Nisu potrebni SQL, cron, Firebase ili pohrana zvuka. [Upute za instalaciju](docs/SHARING-DEPLOYMENT.md).

> Promjene iz repozitorija ne postavljaju se automatski na živu domenu vojs.com.hr. Živi P2P prijenos Android ↔ Web nije potvrđen dok se nova web verzija ne instalira i testira. Potpuna 1:1 vizualna podudarnost svih novih zaslona također nije izmjerena.

---

<div align="center">**VOJS by BRENDIGO** · [brendigo.com](https://brendigo.com)</div>
