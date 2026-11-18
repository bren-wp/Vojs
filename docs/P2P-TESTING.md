# VOJS — Android/Web P2P testiranje i kompatibilnost

## Što se sada provjerava

GitHub Actions pokreće stvarni Chromium s **testnim mikrofonom** te lokalni PHP poslužitelj. Test `scripts/test_p2p_browser.mjs`:

1. Snima stvarni, valjani audio u pregledniku, pauzira i nastavlja snimanje te završava zapis.
2. Pošiljatelj potvrđuje pravila korištenja i aktivira P2P poveznicu.
3. Drugi preglednik otvara istu vrstu poveznice i prima zvuk preko WebRTC DataChannela.
4. Uspoređuje se veličina datoteke, MIME i SHA-256 između pošiljatelja i primatelja.
5. Primatelj može blokirati poruku, a blokiranje se pamti lokalno.
6. Pošiljatelj opoziva sesiju; JSON poslužitelj prestaje posluživati signalizaciju za taj ID.

Testirani zvuk nastaje iz Chromium testnog mikrofona i nije korisnička snimka. P2P podaci nisu audio koji PHP sprema na disk. Za pregled CI i screenshotova pogledati GitHub Actions postupak **VOJS full web visual matrix**.

## Sinkronizacija verzija

Android APK sadrži svoju kopiju `p2p.js`; WebView je lokalno poslužuje umjesto proizvoljne naknadne verzije s hostinga. CI provjerava da su `website/p2p.js` i `app/src/main/assets/p2p.js` bajtno identični.

Prije P2P povezivanja oba klijenta provjeravaju `GET /api/status.php` i protokol `vojs-p2p-v1`, trajanje sesije 900 sekundi, odsutnost pohrane audija te odsutnost SQL baze i crona. Neusklađena ili nedostupna produkcijska instalacija daje jasnu pogrešku. Web stranica i Android imaju jednak pregled 15-minutnog trajanja poveznice.

**Važno:** paket aplikacije nije dovoljan ako na vojs.com.hr nije raspoređen novi PHP JSON API, web klijent i stranica za primanje poruke. Trenutačno nije potvrđeno da su te datoteke objavljene na produkcijskom poslužitelju.

## Vizualni 1:1 QA

I dalje se snima 12 stvarnih Android stanja i 112 web stranica/stanja na četiri širine i u dvije teme. Automatski web pixel-audit razlikuje `missing_reference` od `exact`; dok nema pravih podudarnih izvornih slika za svako stanje, potpuna 1:1 identičnost nije dokazana.

End-to-end test provjerava stvaran lokalni medijski prijenos, ali ne potvrđuje povezanost dvije fizičke mreže, NAT traversal bez TURN servisa ili kompatibilnost sa svim preglednicima. Konačno testiranje zahtijeva stvarne Android uređaje i javni HTTPS.
