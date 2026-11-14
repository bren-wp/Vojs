# VOJS — Pravila privatnosti

**Ažurirano: 14. studenoga 2026.**

VOJS je BRENDIGO aplikacija za snimanje, preslušavanje i dijeljenje glasovnih poruka. Web i Android aplikacija ne zahtijevaju registraciju niti korisnički račun.

## Mikrofon i lokalno snimanje

Android koristi dopuštenje RECORD_AUDIO samo za snimanje. Snimke se nalaze u privatnom privremenom cacheu do dijeljenja; nepodijeljene snimke se uklanjaju prilikom novog snimanja, a privremene podijeljene datoteke starije od približno 24 sata pokušavaju se očistiti pri sljedećem pokretanju. Vanjska aplikacija može zadržati vlastitu kopiju.

Web snimač uz dopuštenje preglednika koristi MediaRecorder. Lokalni audio postoji u memoriji otvorene stranice i moguće ga je preuzeti. Offline service worker predmemorira samo statične web resurse, nikada audio snimke ni API odgovore.

## Dobrovoljno dijeljenje poveznicom

Samo izričitim odabirom gumba za stvaranje web poveznice audio se prenosi preko HTTPS-a na vojs.com.hr. Poslužitelj pohranjuje sadržaj izvan javnog web korijena najviše 24 sata, uz nasumični identifikator i zasebni tajni token za opoziv. **Svatko tko ima poveznicu može preslušati audio i sačuvati kopiju.** Riječ nije o end-to-end šifriranju.

Pošiljatelj može opozvati poveznicu tijekom sesije u kojoj je nastala. Nakon isteka 24 sata poslužitelj odbija pristup; stvarni diskovni podaci čiste se cron procesom ili prilikom novih učitavanja. Nije moguće opozvati prethodno preuzete kopije.

## Sigurnost, obrada IP adresa i ostali podaci

Usluga ograničava učitavanja na 8 MB po datoteci i broj novih učitavanja po izvornoj IP adresi. Za sigurnosno ograničavanje zahtjeva obrađuje IP adresu i kratkotrajno pohranjuje njezin HMAC hash; hosting može voditi pristupne i sigurnosne logove, uključujući IP adrese. Nema ugrađenih oglasnih ili analitičkih SDK-ova niti korisničkih profila. Privatnost primateljske aplikacije kod offline dijeljenja uređena je pravilima te aplikacije.

## Opoziv, prijava zloupotrebe i kontakt

Korisnik može opozvati svoju trenutačnu poveznicu pomoću posebnog tokena prikazanog samo u sesiji učitavanja. Bez tog tokena poveznica prestaje raditi nakon isteka najviše 24 sata. Zlouporabu ili neprimjeren sadržaj moguće je prijaviti putem https://vojs.com.hr/prijava-sadrzaja/ i https://vojs.com.hr/kontakt/. Nemoj slati privatni audio u javni GitHub issue.

Izdavač: BRENDIGO, https://brendigo.com. Prije produkcijske objave ova politika mora biti javno dostupna na https://vojs.com.hr/privatnost/ i usklađena sa stvarnim hosting logovima te Google Play Data safety prijavom.
