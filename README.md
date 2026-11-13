<div align="center">
<img src="website/logo.svg" width="96" alt="VOJS logo"/>

# VOJS
### Neke stvari bolje zvuče.

**Tvoj glas. Tvoja poruka. Tvoj izbor.**

[🌐 Web](website/) · [📱 Izdanja za Android](https://github.com/bren-wp/Vojs/releases) · [💜 BRENDIGO](https://brendigo.com)

</div>

## 🎙️ Glasovne poruke bez komplikacija

VOJS je **nativna Android aplikacija** za snimanje, preslušavanje i dijeljenje glasovnih poruka do pet minuta. Ne treba korisnički račun, vlastiti server, bazu podataka ni VOJS cloud.

| 🎤 Snimi | 🎧 Preslušaj | 📤 Podijeli |
| :--- | :--- | :--- |
| Mikrofon, pauza i nastavak | Reproduciraj prije slanja | Android Sharesheet, M4A audio |

**💜 Brending**: ljubičasta `#7868FF`, tamnoljubičasta `#5145CB`, naglasak `#FF7C91`. Aplikaciju i web potpisuje **BRENDIGO — brendigo.com**.

## ✨ Prednosti

- ✅ Izvorni Kotlin i Jetpack Compose; bez WebViewa
- ✅ Offline snimanje i preslušavanje
- ✅ Dijeljenje datoteke kroz WhatsApp, Viber, Bluetooth, Quick Share i druge kompatibilne Android aplikacije
- ✅ Bez registracije, oglasa, Firebasea i vlastitog servera
- ✅ Privatni cache za privremeni audio i automatsko uklanjanje starijih datoteka pri pokretanju
- ✅ Android 7.0+ (minSdk 24), cilj Android 15 (targetSdk 35)

## 📱 Prikaz izgleda

![VOJS web i sučelje](website/logo.svg)

*Prikaz identiteta; stvarne snimke Android zaslona potrebno je prikupiti na uređaju prije objave na trgovini.*

## 🔐 Privatnost i dijeljenje

Audio je kratkotrajno pohranjen **lokalno u privatnom cacheu** zbog Androidova FileProvider dijeljenja. Nema trajne interne galerije snimki ni VOJS prijenosa prema serveru. Datoteke starije od 24 sata brišu se pri sljedećem pokretanju aplikacije. Dijeljenje preko neke druge aplikacije podliježe pravilima i infrastrukturi te aplikacije. Ovo **nije** usluga dijeljenja javnih web-poveznica ni poruka s vremenskim istekom.

## 🧰 Izgradnja i izdanja

Otvorite projekt u Android Studiju ili instalirajte JDK 17, Android SDK 35 i Gradle 8.10.2:

```bash
gradle :app:assembleRelease :app:bundleRelease
```

Workflow `.github/workflows/android-release.yml` pri promjeni `main` grane izrađuje novo GitHub izdanje s **nepotpisanim** APK i AAB datotekama. Za instalaciju na uređaj ili Google Play **potrebno je zasebno konfigurirati potpisivanje**. Nemojte distribuirati artefakte kao produkcijski potpisane bez provjere.

## 🌐 Web

`website/` je statička javna prezentacija prilagođena arhitekturi bez servera. Web se može objaviti putem GitHub Pages (uključiti Pages / GitHub Actions u postavkama repozitorija).

## ❤️ Autor

**BRENDIGO** · [brendigo.com](https://brendigo.com)
