
## Test stvarne sinkronizacije Android–Web protokola

Androidova ugrađena P2P stranica i web stranica koriste isti \`website/p2p.js\` WebRTC klijent i \`/api/signal.php\` JSON signalizaciju. GitHub Actions sada provjerava i **stvarnu razmjenu podataka** između dva Chromium klijenta preko lokalnog PHP poslužitelja: 32 KiB determinističkih testnih bajtova, provjera SHA-256 na primatelju, prikaz preuzetog sadržaja, lokalno blokiranje i opoziv sesije. Test ne tvrdi da su sintetički testni bajtovi valjan glasovni zapis ni da je proveden prijenos između dva fizička uređaja.

Sama provjera na lokalnom računalu ne dokazuje dostupnost produkcijske domene vojs.com.hr, potpunu prohodnost kroz NAT/mobilne mreže ili 1:1 usklađenost s dizajnerskim referencama.
