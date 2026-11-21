package com.brendigo.vojs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The in-app privacy policy remains available with airplane mode enabled.
 * Detailed hosting-specific disclosures still belong to the public policy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VojsPrivacySheet(
    dark: Boolean,
    secondary: Color,
    onDismiss: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Night else Paper,
        contentColor = if (dark) Paper else Ink
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            // The dismissal control remains visible even after reading the
            // last paragraph on compact displays or at larger font scale.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pravila privatnosti", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Zatvori", color = Violet, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier.fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
            Text("VOJS • 21. studenoga 2026.", color = secondary, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))

            PrivacySection("Snimanje i reprodukcija",
                "VOJS može snimati i preslušavati glasovne poruke bez interneta " +
                "ili web-stranice. Dopuštenje mikrofona traži se samo za snimanje. " +
                "Snimke su privremeno pohranjene u privatnoj memoriji aplikacije; " +
                "nisu automatski poslane na VOJS poslužitelj.", secondary)

            PrivacySection("Čuvanje snimki",
                "Nepodijeljena snimka briše se kada započneš novu. " +
                "Datoteka podijeljena putem drugih aplikacija može privremeno " +
                "ostati dostupna, a VOJS pokušava ukloniti takve kopije " +
                "starije od 24 sata pri idućem pokretanju. " +
                "Primatelj ili odabrana aplikacija mogu sačuvati svoju kopiju.", secondary)

            PrivacySection("Dijeljenje bez VOJS web-stranice",
                "Gumb za dijeljenje datoteke otvara dostupne aplikacije na uređaju. " +
                "VOJS za taj postupak ne zahtijeva vlastiti poslužitelj. " +
                "Odabrana aplikacija može zahtijevati internet i primijeniti " +
                "vlastita pravila privatnosti.", secondary)

            PrivacySection("Poveznice i mrežni podaci",
                "Poveznice su neobavezne i zahtijevaju internet na oba uređaja. " +
                "VOJS poslužitelj ne sprema zvuk. Privremeno obrađuje podatke " +
                "potrebne za uspostavu veze, koji mogu sadržavati mrežne adrese. " +
                "Poveznica vrijedi do 15 minuta; istekli podaci više nisu dostupni " +
                "preko usluge, ali se zapisi na poslužitelju mogu fizički " +
                "ukloniti kasnije kada se stvore nove sesije. " +
                "Mrežna usluga za povezivanje također može vidjeti javnu IP adresu.", secondary)

            PrivacySection("Zaštita od zlouporabe",
                "Za sprječavanje zlouporabe poslužitelj privremeno obrađuje " +
                "IP adresu i ograničava broj poveznica. Zapisi ograničenja " +
                "mogu ostati pohranjeni dok ne dođe do idućeg čišćenja. " +
                "Primatelj može blokirati pojedinu poveznicu, " +
                "a preuzetu kopiju može zadržati na svojem uređaju.", secondary)

            PrivacySection("Kontakt i puna pravila",
                "Izdavač: BRENDIGO. Javna pravila privatnosti i stranica " +
                "za prijavu zlouporabe dostupni su na vojs.com.hr. " +
                "Za posjet web-stranici potreban je internet; " +
                "ovo objašnjenje možeš pročitati i bez njega.", secondary)

            Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, description: String, secondary: Color) {
    Text(title, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Text(description, fontSize = 13.sp, lineHeight = 20.sp, color = secondary)
    Spacer(Modifier.height(18.dp))
}
