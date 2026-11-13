# VOJS vizualna provjera — izvorne reference

Izvor je `VOJS-Android-brending-1.0.0.zip` iz korisnikovih privitaka:
- `google-play/snimke-zaslona/01-snimi-1080x1920.png`
- `google-play/snimke-zaslona/02-trajanje-1080x1920.png`
- `google-play/snimke-zaslona/03-podijeli-1080x1920.png`

Ove tri slike su **konceptualni marketinški prikazi**, ne pokrenuta Android aplikacija. Odvajati vanjsku ljubičastu marketinšku pozadinu i okvir mobitela od stvarnog sučelja unutar ekrana.

## Mapa vizualnih svojstava

| Svojstvo | Referenca | Android implementacija |
| --- | --- | --- |
| Primarna boja | #7868FF | #7868FF |
| Tamni tekst | #20213B | #20213B |
| Naslov | Nova poruka | Nova poruka |
| Grafika | 43 tankih, zaobljenih audio stupaca | Nativni Compose Canvas s 43 stupca, snimanje očitava realnu amplitudu |
| Odbrojavanje | Velike minute i sekunde | 00:00 – 05:00, aktivno vrijeme snimanja |
| Snimanje | Veliki okrugli gumb | Veliki okrugli gumb s pristupačnim nazivom |
| Rezultat | Jedan primarni ljubičasti gumb | Jedan primarni gumb za dijeljenje M4A datoteke |
| Trajanje web-poruka | 1 sat / 1 dan / 7 dana / Nikada | Izostavljeno: bez vlastitog servera nije moguće opozvati primateljsku kopiju |
| Link za poruku i brisanje | Javni URL | Izostavljeno: umjesto toga ACTION_SEND / FileProvider |
| Privatnost | Bez spremanja udaljeno | Privremeni lokalni cache za dijeljenje; bez vlastitih servera |

**Status:** Izvedba je vizualno usklađivana sa stilom izvora, ali **nije izmjereno niti potvrđeno 0 % odstupanja**. Potrebne su usporedne snimke na istim dimenzijama/font-scale postavkama te ispitivanje svih klikova, dopuštenja i stanja zaslona.

Stvarne Android snimke su u [docs/screenshots](screenshots/) i snima ih emulator workflow. Za Google Play koristiti samo te snimke ili nove snimke s fizičkog uređaja, ne tri konceptualna prikaza.
