package no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator

import no.nav.bidrag.domene.ident.Personident


data class GenererFørstesideRequestDto(
    val spraakkode: Språkkode,
    val bruker: FørstesideBrukerDto,
    val tema: String,
    val overskriftstittel: String,
    val arkivtittel: String,
    val foerstesidetype: Foerstesidetype,
    val netsPostboks: String,
    val navSkjemaId: String,
    val vedleggsliste: List<String>,
    val dokumentlisteFoersteside: List<String>
)

data class FørstesideAdresseDto(
    val adresselinje1: String,
    val adresselinje2: String,
    val adresselinje3: String,
    val postnummer: String,
    val poststed: String
)

data class FørstesideAvsenderDto(
    val avsenderId: String,
    val avsenderNavn: String
)

data class FørstesideBrukerDto(
    val brukerId: Personident,
    val brukerType: String
)

data class FørstesideArkivsakDto(
    val arkivsaksystem: String,
    val arkivsaksnummer: String
)

class GenererFørstesideResultatDto(
    val foersteside: ByteArray,
    val loepenummer: String,
)


enum class Språkkode {
    NB, NN, EN
}

enum class Foerstesidetype {
    SKJEMA, INTERNT, ETTERSENDELSE, LOSPOST
}

enum class NavSkjemaId(val kode: String) {
    AVTALE_OM_BARNEBIDRAG_UNDER_18("NAV 55-00.60"),
    AVTALE_OM_BARNEBIDRAG_OVER_18("NAV 55-00.63"),
}
