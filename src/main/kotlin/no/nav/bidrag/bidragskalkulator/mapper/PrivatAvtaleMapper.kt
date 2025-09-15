package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.GenererPrivatAvtalePdfRequest
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdf
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.FørstesideBrukerDto
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.GenererFørstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.Foerstesidetype
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.NavSkjemaId
import no.nav.bidrag.domene.ident.Personident

fun PrivatAvtalePdf.navnSkjemaIdFor(): NavSkjemaId = when (this) {
    is PrivatAvtaleBarnUnder18RequestDto -> NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18
    is PrivatAvtaleBarnOver18RequestDto -> NavSkjemaId.AVTALE_OM_BARNEBIDRAG_OVER_18
}

fun PrivatAvtalePdf.tilGenererPrivatAvtalePdfRequest(): GenererPrivatAvtalePdfRequest = GenererPrivatAvtalePdfRequest(
    privatAvtalePdf = this,
    navSkjemaId = this.navnSkjemaIdFor()
)

fun PrivatAvtalePdf.tilGenererFørstesideRequestDto(innsenderIdent: Personident,
                                      netsPostboks: String = "1400",
                                      arkivtittel: String = "Avtale om barnebidrag",
                                      ): GenererFørstesideRequestDto {
    val navSkjemaIdKode = this.navnSkjemaIdFor().kode

    return GenererFørstesideRequestDto(
        spraakkode = this.språk,
        netsPostboks = netsPostboks,
        bruker = FørstesideBrukerDto(
            brukerId = innsenderIdent,
            brukerType = "PERSON"
        ),
        tema = "BID",
        vedleggsliste = listOf("${navSkjemaIdKode} ${arkivtittel}"),
        dokumentlisteFoersteside = listOf("${navSkjemaIdKode} ${arkivtittel}"),
        arkivtittel = arkivtittel,
        navSkjemaId = navSkjemaIdKode,
        overskriftstittel = "${navSkjemaIdKode} ${arkivtittel}",
        foerstesidetype = Foerstesidetype.SKJEMA
    )
}

fun PrivatAvtaleBarnUnder18RequestDto.normalisert(): PrivatAvtaleBarnUnder18RequestDto =
    copy(
        barn = barn
            .asSequence()
            .sortedBy { it.fraDato }
            .map { it.copy(fraDato = it.fraDato) }
            .toList()
    )


fun PrivatAvtaleBarnOver18RequestDto.normalisert(): PrivatAvtaleBarnOver18RequestDto =
    copy(
        bidrag = bidrag.sortedBy { it.fraDato }
    )


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
