# VOJS — Google Play Console priprema (21. studenoga 2026.)

**Status:** projekt nije automatski odobren za Google Play. Ova kontrolna lista pokriva izvorni kod i izdanje, ali za stvarno prihvaćanje potrebno je poslati produkcijski potpisani AAB i dobiti potvrdu u Play Console.

## Automatski provjerljivo

- ID \`com.brendigo.vojs\`, cilj API 36, minimum API 24, kompilacija protiv Android API 37 (noviji stabilni Compose zahtjev). Android 16 i 16 KiB stranice pokrivene su build i ELF/ZIP kontrolama; za potpuno jamstvo potrebni su stvarni uređaji.
- Jedina Android dopuštenja: \`RECORD_AUDIO\` (pokreće korisnik radi snimanja) i \`INTERNET\` (dobrovoljno P2P dijeljenje).
- Nema Google/Firebase analitičkog SDK-a ni reklamnih biblioteka u izravnim ovisnostima; tranzitivne ovisnosti treba provjeriti izvještajem.
- \`allowBackup=false\` i eksplicitni \`data-extraction-rules\` isključuju cloud i device-to-device prijenos lokalnih podataka; ne koristiti sistemski backup za glasovne poruke.
- Datoteke se nazivaju nasumično, dijele preko neizvezenog FileProvidera samo uz dozvolu čitanja i brišu prema jasno navedenim pravilima.
- \`usesCleartextTraffic=false\` i HTTPS za P2P signalizaciju; WebRTC DTLS šifrira sadržaj prijenosa.
- Izvorne light/dark splash slike se potvrđuju hashom i stvarnim Pixel 6 ROI pixel testom; ne proglašavati druge ekrane 1:1 bez istih referenci.

## Obavezni koraci za vlasnika Google Play računa

1. Postaviti trajni **upload key** i GitHub secrets \`VOJS_KEYSTORE_BASE64\`, \`VOJS_STORE_PASSWORD\`, \`VOJS_KEY_ALIAS\`, \`VOJS_KEY_PASSWORD\`. Ne dijeliti privatne ključeve javno. Aktivirati Play App Signing ili koristiti postojeći odgovarajući Google Play upload certifikat. Provjeriti \`versionCode\` te stvaran certifikat izgrađenog AAB-a.
2. Na vlastitoj domeni objaviti \`https://vojs.com.hr/privatnost/\`, \`https://vojs.com.hr/uvjeti/\`, \`https://vojs.com.hr/prijava-sadrzaja/\`, te ažurirani JSON P2P API. Mora raditi bez prijave i vraćati stvarna pravila koja odgovaraju kodu. GitHub release sam ne objavljuje web.
3. Dovršiti **Data safety** na temelju stvarnog hostinga i P2P prijenosa: glasovni zapis ostaje lokalno ili se korisnikovim izborom šalje primatelju; SDP i IP/metapodaci obradit će se na poslužitelju, a STUN ima uvid u IP. Ne označavati automatski „No data collected” jer obrada ovisi o stvarnom hostingu/logovima i pravilima pružatelja usluga.
4. Dovršiti Content rating, ciljne dobne skupine, Ads deklaraciju (nema oglasa ako ih doista nema), App access (nema računa), dopuštenja i obrazac o privatnosti. Osigurati funkcionalne opcije prijave i blokiranja privatnog korisničkog sadržaja.
5. Objaviti na zatvorenom/internom testnom kanalu, testirati Android 7/10/13/15/16, fizičke uređaje, mikrofon odbijen/dozvoljen, stop/pause, reprodukciju, 5-minutni limit, rad offline, ACTION_SEND i P2P u dvije stvarne mreže. Prikupiti i riješiti Play Pre-launch report.
6. U Play listing uključiti **stvarne** screenshotove aplikacije, pravilne ikonice/opis i točne tvrdnje o mogućnostima. U pregledniku dizajn i P2P značajke ovise o dostupnosti produkcijskog API-ja. Ne obećavati trajne linkove, offline online-sinkronizaciju ili 100% uspješnost bez TURN servisa.
7. Objava ovisi o završnom Google Play pregledu i stanju računa. Nijedan lokalni test ne može jamčiti odobrenje.

## Poznate granice vizualnog audita

Dostupne su originalne light/dark splash reference i marketinške kompozicije za snimanje/dijeljenje. Originalni splash ROI ima prethodno izmjereno 0% odstupanja (status/navigation bar izuzeti). Ostalih 10 Android stanja i 112 web prikaza nemaju sve ekvivalentne dizajnerske datoteke, te se ne smiju proglasiti 1:1.

## Provjera prije objave

- \`python3 scripts/test_play_readiness.py\`
- \`gradle :app:assembleRelease :app:bundleRelease :app:lintRelease\`
- CI: native 16KiB check, original splash ROI, 3 emulator profila, 112 web rendering stanja, pravi lokalni P2P test.

**Izvori:**
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/9876937
- https://developer.android.com/identity/data/autobackup

## Službene grafike i snimke za Play

Izvorni VOJS vizuali uključuju ikonu 512 × 512 i istaknutu grafiku 1024 × 500; njihove dimenzije i granice veličine provjerava CI. Izvorna ikona je RGB PNG, a Google Play traži **32-bitni PNG s alfa-kanalom**. Stvarne ADB snimke su RGBA, dok Play traži **24-bitni RGB PNG bez alfe**. Zato `scripts/prepare_play_assets.py` konvertira **samo kanale**, bez promjene ijednog RGB piksela, proporcija ili izmjene UI-ja. Izvedeni podaci nalaze se u `docs/play-store-assets/` nakon stvarnog emulator screenshot workflowa, gdje se sprema šest stvarnih Android snimki (2–8 dopušteno). Izvorni `branding/` i `docs/screenshots/` ostaju neizmijenjeni. Nikada ne zamjenjivati stvarne snimke marketinškim mockupovima.

Google Play prima omjere zaslona do 2.3:1; Pixel 6 fotografije 1080 × 2400 udovoljavaju tom zahtjevu, ali 9:16 je preporučeno za širu Play promociju. Za 9:16 treba **nova stvarna snimka s odgovarajućeg emuliranog ili fizičkog zaslona**, ne sintetičko rastezanje ili obrezivanje stvarnih kontrola.

## Android 16 emulator test

Nakon objave izdanja multi-device workflow uključuje dodatni Pixel 6 profil s **API 36 (Android 16)**, uz postojeći Android 10 telefon, Android 15 telefon i tablet. Time se testira stvarni `targetSdk=36` i pristupačnost kontrola. ELF/ZIP provjera 16 KiB poravnanja zasebna je i **ne znači** da je aplikacija već testirana na fizičkom 16 KiB uređaju; prije Play objave provjeriti i stvarno pokretanje na takvom uređaju ili 16 KiB emulatoru.

## Samostalan rad bez vojs.com.hr

Aplikacija ne otvara URL niti provjerava hosting pri pokretanju, snimanju, preslušavanju, dijeljenju lokalne datoteke ili čitanju pravila privatnosti. Lokalni test na emulatoru namjerno uključuje zrakoplovni način prije prvog pokretanja. Web-stranica i mreža potrebne su **samo** ako korisnik izričito odabere izradu poveznice. Poznata ograničenja P2P-a nisu skrivena u Play opisu.

Uklonjeni su tehnički tekstovi poput *P2P*, *WebRTC* i *JSON* iz vidljivih kontrola aplikacije. Tehnički detalji ostaju u dokumentaciji i javnim pravilima, gdje su potrebni radi transparentnosti. Originalni tamni/svijetli splash i dalje se provjeravaju 1:1 u istom stvarnom mjernom području; za ostale zaslone ne postoji potpuna originalna referenca.
