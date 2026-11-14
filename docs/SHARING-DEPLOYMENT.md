# VOJS web — PHP JSON WebRTC signaling (bez SQL baze, crona i audio pohrane)

## Što se postavlja

1. Kopiraj mapu website/ na HTTPS hosting za vojs.com.hr (PHP 8.1+). Potreban je javni web root i ispravan HTTPS certifikat.
2. Postavi apsolutnu privatnu mapu izvan DocumentRoota za JSON. Preporuka: VOJS_SIGNAL_STORAGE=/home/USER/vojs-signal-data (0700, dostupna samo PHP računu). Bez env var, backend koristi sibling direktorij vojs-signal-data uz DocumentRoot.
3. PHP endpointi: /api/signal.php (create/offer/answer/close/GET), /api/status.php (health). Audio se ne prima niti poslužuje; stari /api/upload.php, /api/audio.php i /api/delete.php vraćaju HTTP 410.
4. **Nema crona**: istečene sesije odbijaju se već pri čitanju, a fizičko uklanjanje JSON datoteka događa se prilikom novih stvaranja sesija.
5. **Nema SQL baze**: JSON stanje zaključava se s flock u privatnom direktoriju.
6. Za produkciju konfiguriraj reverse-proxy/WAF limiti zahtjeva (posebno GET polling), zaštitu od DoS-a, HTTPS, sigurnosne logove i postupak prijave zloupotrebe. Aplikacija ograničava 20 novih sesija po IP-u u 24 h, 100 istovremenih sesija i 12 KB SDP opis.
7. WebRTC koristi STUN (trenutačno stun.l.google.com:19302). Bez TURN relay usluge **nije moguće jamčiti povezivanje** iza svakog mobilnog NAT-a ili firewalla. Ako se naknadno doda TURN, njime se mogu prenositi šifrirani WebRTC paketi, ali audio i dalje ne treba pohranjivati.
8. Android PeerSenderActivity otvara lokalno zapakiranu stranicu na vojs.com.hr originu. Web stranica i /p2p.js moraju biti objavljeni na domeni prije rada native P2P-a.

## Prije uključivanja

- Prethodna verzija pohranjivala je audio na disk u privatnoj mapi vojs-private. **Promjena koda ne briše postojeće snimke ni backup kopije.** Operater hostinga treba identificirati i sigurno ukloniti legacy audio/.audio/.m4a datoteke prema pravilima čuvanja podataka, uz prethodnu provjeru što smije brisati. Ne treba dirati korisničke/administratorske račune niti druge mape.
- Pokreni bash scripts/test_web_link_api.sh na stagingu i PHP/JS lint kroz CI.
- Provjeri da nema javnog pristupa JSON direktoriju, da HTTP zahtjevi ne izlažu audio ni signalizacijske JSON datoteke, da POST i GET poštuju metode i veličine.
- U stvarnom Chrome/Firefox/Safari pregledniku testiraj istovremeno pošiljatelja i primatelja, te Android ↔ Web P2P, prvi offline posjet, prekid veze, loš NAT, duplicirani answer i istek sesije.
- Ažuriraj https://vojs.com.hr/privatnost/ prije objave i pregledaj Google Play Data safety s obzirom na SDP/IP i STUN.

## Ograničenja

PHP pohranjuje samo signalizacijski JSON, ne audio. Poveznica radi samo kada je pošiljatelj online; bez poslužiteljske pohrane **ne može se otvoriti kasnije kada je pošiljatelj offline**. Do 8 MB, max 5 minuta snimanja i 15 minuta valjanosti P2P signalizacije. U vrlo ograničenim mrežama može biti potreban TURN relay (nije dio ovog izdanja).

**Status:** GitHub repozitorij nije potvrda da je aplikacija zaista postavljena na javni hosting ili da je prošao Play Console review.
