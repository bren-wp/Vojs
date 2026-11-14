# Pravila privatnosti – VOJS

**Posljednje ažuriranje:** 14. studenoga 2026.

VOJS je lokalna Android aplikacija za snimanje, preslušavanje i dijeljenje M4A glasovnih poruka.

## Podaci i mikrofon

- Za snimanje aplikacija traži Android dopuštenje `RECORD_AUDIO`. Korisnik ga može odbiti ili opozvati u postavkama uređaja.
- Snimke se privremeno čuvaju u privatnom cacheu aplikacije na uređaju. VOJS ne izrađuje korisničke račune, udaljenu audio bazu ni vlastitu uslugu pohrane.
- VOJS ne sadrži oglasni ili analitički SDK i ne šalje audiozapise na server koji kontrolira VOJS.
- Aplikacija po pokretanju pokušava obrisati lokalne privremene M4A datoteke starije od 24 sata; cache može ostati do sljedećeg pokretanja aplikacije. Deinstalacija aplikacije uklanja njezine privatne lokalne podatke u skladu s Android ponašanjem.

## Dijeljenje

Audiozapis se može predati drugim aplikacijama putem Androidova izbornika dijeljenja. Korisnik bira primateljsku aplikaciju. Ta aplikacija može koristiti mrežu i zadržati svoju kopiju, na što se primjenjuju njezina pravila privatnosti. VOJS ne može opozvati datoteku nakon predaje.

## Sigurnost i pohrana

Aplikacija ne zahtijeva Android internetsko dopuštenje i ne omogućuje promet nekriptiranog HTTP-a. Pristup privremenim datotekama kod dijeljenja daje se preko Android FileProvidera odabranoj aplikaciji. Sigurnost samog prijenosa dalje ovisi o odabranoj aplikaciji.

## Kontakt i izmjene

Izdavač: **BRENDIGO** · [brendigo.com](https://brendigo.com). Za prijavu problema u aplikaciji koristi [VOJS GitHub Issues](https://github.com/bren-wp/Vojs/issues), bez javnog priloženja privatnih audiozapisa.

Ova pravila mogu se dopuniti ako se promijene mogućnosti aplikacije. Prije objave na Google Playu potrebno je objaviti ovaj tekst na javno dostupnoj stabilnoj URL adresi, unijeti tu adresu u Play Console te potvrditi prijavljene podatke u obrascu Data safety.
