# Izdavanje VOJS aplikacije

Svaka izmjena na main grani aktivira Android build i novo izdanje s APK + AAB. Workflow ne prikazuje nepotpisane datoteke kao spremne za distribuciju. Za potpisivanje treba dodati sigurno upravljanje keystoreom izvan git repozitorija. Nakon pokretanja provjeriti Actions logove i artefakte te tek tada potvrditi izdanje.

Za Google Play treba dostaviti vlastiti potpisani AAB, production-compatible versionCode te stvarne snimke zaslona; konceptualni prikazi nisu dokaz funkcionalne aplikacije.

Način dijeljenja: ACTION_SEND / audio/mp4 / FileProvider. Snimke ostaju privremeno na uređaju. Primatelj može imati vlastitu pohranu ili cloud.
