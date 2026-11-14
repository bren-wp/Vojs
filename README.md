<div align="center">
<img src="branding/logotip/logotip-svijetla-podloga.png" width="260" alt="Originalni VOJS logotip" />

# VOJS
### Neke stvari bolje zvuče.
**Snimi glas. Preslušaj. Podijeli trenutak.**

🎙️ Bez računa · 📱 Android + Web · ✈️ Offline · 🔗 Privremene poveznice

[🌐 VOJS Web](https://vojs.com.hr) · [📲 Preuzmi Android](https://github.com/bren-wp/Vojs/releases/latest) · [💜 BRENDIGO](https://brendigo.com)
</div>

---

## Tvoj glas. Tvoja poruka.

VOJS snima glasovne poruke do **5 minuta** bez registracije. Snimi spontanu poruku, poslušaj je prije dijeljenja i izaberi želiš li poslati **datoteku** ili **privremenu poveznicu**.

| 🎧 Lokalno / offline | 🔗 Online poveznica |
| --- | --- |
| Snimi, pauziraj, nastavi i preslušaj na uređaju. | Prenesi snimku tek kada izričito odabereš stvaranje poveznice. |
| Podijeli M4A kroz Android Sharesheet ili preuzmi audio iz web preglednika. | Primatelj sluša u pregledniku, bez instalacije aplikacije. |
| Za lokalne funkcije nije potreban internet. | Poveznica traje najviše 24 sata i može se opozvati tijekom sesije. |
| Ne šalje zvuk na VOJS server. | Potreban je internet i HTTPS prijenos zvuka na VOJS server. |

**Bez obveznog računa, oglasa i ugrađene analitike.** Poveznica nije end-to-end kriptirana: svatko tko je dobije može preslušati snimku, a primatelj može sačuvati svoju kopiju.

## Stvarni Android zasloni

Ove slike su snimljene tijekom stvarnog izvođenja Android aplikacije na emulatoru, ne generirani mockupovi. Novi ekran s web poveznicom zahtijeva zasebnu snimku nakon provjere.

<table>
<tr><td><img src="docs/screenshots/01-home.png" alt="VOJS početni zaslon" width="230"/></td>
<td><img src="docs/screenshots/02-recording.png" alt="VOJS snimanje" width="230"/></td>
<td><img src="docs/screenshots/03-ready.png" alt="VOJS preslušavanje" width="230"/></td></tr>
<tr><td><strong>Snimi</strong></td><td><strong>Govori</strong></td><td><strong>Preslušaj</strong></td></tr>
</table>

[Pravi prikazi svih postojećih zaslona](docs/screenshots/) · [Vizualna provjera](docs/VISUAL-QA.md) · [Android CI](https://github.com/bren-wp/Vojs/actions)

## Android i web — usklađeno iskustvo

**Android aplikacija:** nativni Kotlin i Jetpack Compose, lokalni AAC/M4A audio, sigurno dijeljenje datoteka preko Android FileProvidera. Novo je opcionalno HTTPS dijeljenje, uz zasebne kontrole za kopiranje, slanje i opoziv poveznice.

**Web snimač:** [vojs.com.hr/snimi/](https://vojs.com.hr/snimi/) koristi mikrofon preglednika, lokalno preslušavanje, preuzimanje i dobrovoljni online upload. Snimanje se može koristiti offline nakon što se web resursi jednom predmemoriraju u kompatibilnom pregledniku. Web snimka može biti M4A, WebM ili Ogg, ovisno o pregledniku.

**Web preslušavanje:** [vojs.com.hr/poruka/](https://vojs.com.hr/poruka/) omogućuje reprodukciju snimke s valjanom poveznicom. Primatelj ne treba VOJS aplikaciju ni račun, ali mora imati pristup internetu.

> **Važno:** PHP backend i Android klijent implementirani su u izvorni kod. Nije potvrđeno da su novi API endpointi objavljeni na živom vojs.com.hr poslužitelju. Potreban je zaseban deployment i produkcijski test prije korištenja poveznica.

## Privatnost je dio funkcionalnosti

- Mikrofon se koristi samo s dopuštenjem korisnika; nema snimanja u pozadini.
- Lokalni Android cache čisti privremene snimke nakon približno 24 sata pri sljedećem pokretanju; vanjske aplikacije mogu zadržati kopije.
- Online upload nije automatski. Server koristi slučajne identifikatore, poseban token za opoziv, dnevna ograničenja učitavanja i maksimalnu veličinu 8 MB.
- Poveznice istječu nakon 24 sata. Sadržaj se čuva izvan javnog web korijena, a fizičko brisanje zahtijeva pravilno postavljen cron.
- Nema računa, oglasa ili ugrađenog analitičkog SDK-a. Server obrađuje podatke o pristupu za sigurnost i ograničavanje zloupotrebe.
- Poveznice nisu end-to-end šifrirane i nisu namijenjene tajnim ili posebno osjetljivim informacijama.

[Pravila privatnosti](docs/PRIVACY-POLICY.md) · [Postavljanje poslužitelja](docs/SHARING-DEPLOYMENT.md) · [Google Play kontrolni popis](docs/GOOGLE-PLAY-READY.md)

## Struktura i održavanje

| Datoteka ili mapa | Namjena |
| --- | --- |
| app/src/main/java/com/brendigo/vojs/MainActivity.kt | Dopuštenje mikrofona, teme, navigacija |
| VoiceScreen.kt | Compose ekrani, kontrole i dostupnost |
| VoiceWaveform.kt | Valni prikaz i formatiranje vremena |
| VoiceRecorder.kt | Snimanje, reprodukcija, pauziranje i offline dijeljenje |
| VoiceLinkService.kt | HTTPS upload, copy/share i opoziv URL-a |
| website/snimi/ + website/recorder.js | Web snimač |
| website/poruka/ + website/message.js | Privremene web poruke |
| website/api/ | PHP anonimni upload, čitanje, brisanje i ograničenja |
| website/sw.js | Offline cache statičkih web resursa |

## Android izgradnja i objava

U razvojnom okruženju s Android SDK-om:

    gradle --no-daemon :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:lintRelease

- Min. Android SDK 24, ciljani Android SDK 36, podrška za telefone i veće zaslone.
- Dopuštenje RECORD_AUDIO za snimanje, INTERNET za korisnički zatražen HTTPS upload.
- Debug APK je za testiranje. Nepotpisani release APK i AAB **nisu spremni za Google Play**.
- Production potpis zahtijeva četiri ispravno konfigurirane tajne: VOJS_KEYSTORE_BASE64, VOJS_STORE_PASSWORD, VOJS_KEY_ALIAS i VOJS_KEY_PASSWORD.
- GitHub workflow objavljuje izdanje nakon uspješnog spajanja i builda na main, uz provjeru potpisa i Play zahtjeva prije distribucije.

## Kvaliteta dizajna

VOJS zadržava izvorne logotipe, svijetlu/tamnu/sistemsku temu i splash. CI uključuje Android lint, realne emulator scenarije i izvještaje o odstupanjima slike. **Potpuna 1:1 podudarnost svih zaslona i provjera na svim fizičkim uređajima nije još dokazana.**

---

<div align="center">

**VOJS by BRENDIGO** · [brendigo.com](https://brendigo.com)

</div>
