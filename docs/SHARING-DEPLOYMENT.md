# VOJS — Postavljanje anonimnog dijeljenja

## Obavezna konfiguracija

Izvorni kod je u repozitoriju, ali nije automatski instaliran na vojs.com.hr. Za online poveznice treba aktivan HTTPS poslužitelj.

- PHP 8.1+ s dozvoljenim prijenosom datoteka; upload_max_filesize barem 8M, post_max_size barem 9M.
- Dokument korijen weba mora sadržavati mapu website. API rute: /api/upload.php, /api/audio.php, /api/delete.php, /api/status.php.
- Audio se čuva **izvan javnog web korijena**. Postavi VOJS_PRIVATE_STORAGE na apsolutnu privatnu putanju (npr. /home/USER/vojs-private) s pravom pisanja samo PHP procesu. Bez varijable koristi se mapa vojs-private pokraj DocumentRoota. Endpoint odbija putanju unutar web korijena.
- TLS certifikat, HTTPS redirect, sigurnosna HTTP zaglavlja, ograničenja zahtjeva na CDN/WAF/proxy sloju i pristupni logovi nužni su za javnu anonimnu uslugu.
- Osigurati privatni diskovni prostor, nadzor kvote i prijavu zloupotrebe. Ne izlagati private direktorij kroz symlink.
- Postaviti satni cron za uklanjanje isteklih datoteka (prilagoditi stvarne putanje):

    VOJS_DOCUMENT_ROOT=/home/USER/public_html VOJS_PRIVATE_STORAGE=/home/USER/vojs-private /usr/bin/php /home/USER/public_html/api/cleanup.php

## API ugovor

- POST /api/upload.php: multipart polje audio do 8 MB, M4A/WebM/Ogg osnovni potpis. Vraća id, url, delete_token, expires_at.
- GET ili HEAD /api/audio.php?id=...: reproducira ili vraća 404 nakon isteka, podržava HTTP byte ranges.
- POST /api/delete.php: JSON id i delete_token, uspjeh 204 i brisanje datoteke.
- GET /api/status.php: provjerava dostupnost i privatnu konfiguraciju.

Rate-limit je 10 anonimnih učitavanja po IP-u u 24 sata; globalna diskovna kvota 1 GB. Sigurnosni diskovni lock služi sprečavanju paralelnog prekoračenja kvote. Identifikator je slučajan i nije popisiv.

**Bitno:** poslužitelj trenutačno provjerava magična zaglavlja formata, a ne puni sadržaj, trajanje ili mogućnost dekodiranja audio datoteke. Prije otvaranja masovnog javnog anonimnog upload servisa treba dodati sandboxiranu provjeru dekodiranja i trajanja (npr. ffprobe), dodatne anti-abuse mjere i pravila za prijavu te uklanjanje sadržaja. Ovo je sigurnosno ograničenje trenutne implementacije.

## Testiranje prije objave

1. Lokalno pokrenuti bash scripts/test_web_link_api.sh, zatim PHP lint i node syntax check.
2. Potvrditi HTTPS certifikat i ispravan URL na stvarnom vojs.com.hr, testirati Android upload/revoke i web reprodukciju.
3. Testirati prekoračenje datoteke, 429 limit, pad mreže, istek i opoziv, REST Range, odbijanje neuobičajenih formata i path traversal.
4. Na Androidu potvrditi offline snimanje, preslušavanje i Sharesheet pri isključenom internetu.
5. U preglednicima Chrome, Firefox i Safari testirati MediaRecorder podršku, offline service worker nakon prvog posjeta i različite audio kodeke.
6. Objaviti stvarnu politiku privatnosti i uskladiti Google Play Data safety s dobrovoljnim učitavanjem audija i sigurnosnom obradom IP adresa.

Ne tvrdi se da je produkcijski deployment, pen-test, Play pre-launch ili fizički uređaj test već potvrđen.
