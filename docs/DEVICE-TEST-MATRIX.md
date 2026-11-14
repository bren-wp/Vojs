# VOJS: stvarno Android testiranje na različitim uređajima

Workflow [VOJS multi-device Android smoke](https://github.com/bren-wp/Vojs/actions/workflows/responsive.yml) testira nativnu aplikaciju na trima emulatorima: malom Android 10 telefonu (Pixel 2, povećani font), standardnom Android 15 telefonu (Pixel 6) i Android 15 tabletu (Pixel C, povećani font).

Za svaki test snimaju se **stvarni ADB PNG zasloni** i zapisuje manifest s rezultatom: početni ekran, snimanje, pauza, nastavak, gotova poruka, reprodukcija, dijeljenje, postavke, promjena teme i ponovno pokretanje. Izvještaj sadrži i fatalne Android iznimke ako se pojave. Ni jedna snimka nije generirani marketinški prikaz.

[ROI izvještaj svih 11 referentnih snimki](screenshots/all-screens-roi-audit.json) razdvaja promjene u odnosu na prethodni stvarni snimak od **usporedbe s konceptom**. Svijetli i tamni splash dodatno se uspoređuju s originalnim bitmapama. Ne postoji puna usporedba 1:1 sa statičnom referencom za svaki funkcionalni zaslon: Google Play grafike uključuju okvir uređaja i elemente koji su vezani uz serverski sustav i nisu dio offline aplikacije.

**Fizički Android uređaji nisu testirani putem GitHub Actions.** To ostaje zasebna QA obveza. Na namjenskom Android uređaju s instaliranim kompatibilnim testnim APK-om i omogućenim ADB-om može se pokrenuti:

    python3 scripts/verify_responsive.py --profile fizicki-test

Skripta pokreće stvarno snimanje zvuka, reprodukciju i Android izbornik dijeljenja. Ne briše korisničke podatke ni ne mijenja font ako nije naveden --font-scale. Za fizički test posebno zabilježiti dopuštenje mikrofona, prekid pozivom, 5-minutno ograničenje, 16 KB memorijske stranice, rotaciju, Android 7/10/13/15/16 i kompatibilnost aplikacije primatelja. Potrebna je potvrda na stvarnim uređajima prije jamčenja produkcijske stabilnosti.

## Napomena o vremenu reprodukcije u CI-ju

Kratak zvuk od tri sekunde nije prikladan za provjeru stanja gumba „Zaustavi” jer sam Androidov `uiautomator dump` na sporijim emulatorima može trajati dulje od reprodukcije. Automatizirani test zato snima oko 18 sekundi zvuka; **to ne mijenja korisničko maksimalno trajanje snimke (5 minuta)**. Test i dalje izričito mora vidjeti gumb za zaustavljanje i potvrditi njegov rad, bez lažno pozitivnih rezultata.
