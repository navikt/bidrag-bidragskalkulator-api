package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.GenererPrivatAvtalePdfRequest
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdf
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId

fun PrivatAvtalePdf.navnSkjemaIdFor(): NavSkjemaId = when (this) {
    is PrivatAvtaleBarnUnder18RequestDto -> NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18
    is PrivatAvtaleBarnOver18RequestDto -> NavSkjemaId.AVTALE_OM_BARNEBIDRAG_OVER_18
}

fun PrivatAvtalePdf.tilGenererFoerstesideRequestDto(innsenderIdent: String,
                                                    arkivtittel: String = "Avtale om barnebidrag",
                                                    enhetsnummer: String = "1234"): GenererFoerstesideRequestDto =
    GenererFoerstesideRequestDto(
            ident = innsenderIdent,
            navSkjemaId = this.navnSkjemaIdFor(),
            arkivtittel = arkivtittel,
            enhetsnummer = enhetsnummer,
            språkkode = this.språk
    )

fun PrivatAvtalePdf.tilGenererPrivatAvtalePdfRequest(): GenererPrivatAvtalePdfRequest = when (this) {
    is PrivatAvtaleBarnUnder18RequestDto -> GenererPrivatAvtalePdfRequest(
        privatAvtalePdf = this.medNorskeDatoer(),
        navSkjemaId = this.navnSkjemaIdFor()
    )
    is PrivatAvtaleBarnOver18RequestDto -> GenererPrivatAvtalePdfRequest(
        privatAvtalePdf = this,
        navSkjemaId = this.navnSkjemaIdFor()
    )
}

/**
 * Sjekker om førsteside skal genereres basert på oppgjørsform og avtaletype.
 * - For ny avtale: Generer kun når ønsket oppgjørsform er INNKREVING.
 * - For eksisterende avtale: Generer for alle unntatt PRIVAT -> PRIVAT.
 */
fun Oppgjør.skalFørstesideGenereres(): Boolean {
    if (nyAvtale) {
        // Ny avtale: generer kun når ønsket er INNKREVING
        return oppgjørsformØnsket == Oppgjørsform.INNKREVING
    } else {
        // Eksisterende avtale: generer for alle unntatt PRIVAT -> PRIVAT
        val idag = requireNotNull(oppgjørsformIdag) { "oppgjørsformIdag må settes når nyAvtale=false" }

        val privatTilPrivat = idag == Oppgjørsform.PRIVAT && oppgjørsformØnsket == Oppgjørsform.PRIVAT;

        return !privatTilPrivat
    }
}
