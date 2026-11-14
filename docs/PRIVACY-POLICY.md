# Pravila privatnosti — VOJS

**Ažurirano: 14. studenoga 2026.**

VOJS by BRENDIGO snima i dijeli glasovne poruke u Android aplikaciji i web pregledniku. Ne zahtijeva račun. Nema VOJS pohrane audio snimki na web serveru.

## Audiozapisi i mikrofon

Android za snimanje traži dopuštenje RECORD_AUDIO. Snimka se lokalno sprema u privatni cache uređaja za reprodukciju i dijeljenje. Nepodijeljena snimka briše se pri novom snimanju. Podijeljene privremene M4A datoteke starije od 24 sata pokušavaju se ukloniti na idućem pokretanju aplikacije. Odabrane vanjske aplikacije mogu pohraniti vlastitu kopiju.

Web traži dopuštenje mikrofona preko MediaRecorder API-ja i privremeno drži zvuk u memoriji preglednika. Lokalno spremanje i dijeljenje preko uređaja ne zahtijevaju prijenos na VOJS server. Offline service worker predmemorira samo statičke web datoteke, a ne audio ili API odgovore.

## Izravno P2P dijeljenje

Ako korisnik odabere izravno dijeljenje, VOJS stvara vremenski ograničenu poveznicu. PHP poslužitelj prima i privremeno pohranjuje **samo WebRTC SDP JSON podatke za uspostavu veze**, ne prima audio. SDP može sadržavati mrežne adrese i informacije o mrežnoj vezi. WebRTC audio datoteka prenosi se kroz šifrirani DataChannel izravno između uređaja. Korisnik mora ostati online tijekom prijenosa. Primatelj može sačuvati preuzetu datoteku.

Server čuva nasumični ID, hash vlasničkog tokena, SDP offer i answer te rok valjanosti do 15 minuta. Nakon isteka pristup JSON sesiji odbija se, a datoteke se fizički čiste prilikom stvaranja idućih sesija. Bez prometa JSON može ostati na disku dulje, ali nakon isteka nije dostupan putem API-ja. Ne koristi se cron ni SQL.

P2P veza koristi javni STUN poslužitelj za pronalazak mrežnih putanja; STUN poslužitelj može vidjeti javnu IP adresu i mrežne informacije. TURN relay zasad nije uključen, pa povezivanje ne uspijeva na svim mrežama. Podaci nisu namijenjeni profiliranju ili oglašavanju.

## IP adrese i server logovi

Poslužitelj obrađuje IP adresu radi ograničavanja anonimnih sesija i privremeno pohranjuje pseudonimizirani HMAC identifikator i broj pokušaja tijekom razdoblja do 24 sata. Hosting pružatelj može imati dodatne pristupne i sigurnosne logove, čija se stvarna konfiguracija mora provjeriti prije javnog pokretanja. Nema ugrađenih oglasnih ili analitičkih SDK-ova.

## Uklanjanje i prijava

Pošiljatelj može zatvoriti trenutačnu P2P sesiju, a sesija svakako prestaje biti dostupna nakon 15 minuta. Nakon što primatelj preuzme datoteku, VOJS ne može izbrisati tu kopiju. Štetan sadržaj ili zloupotrebu moguće je prijaviti preko https://vojs.com.hr/prijava-sadrzaja/ ili https://vojs.com.hr/kontakt/. Nemoj javno objavljivati privatne snimke u prijavama.

Izdavač: **BRENDIGO** — https://brendigo.com. Ova pravila trebaju biti dostupna na https://vojs.com.hr/privatnost/ i u Android aplikaciji te u Google Play listingu, nakon usklađivanja s konkretnim hostingom i provedene produkcijske provjere.
