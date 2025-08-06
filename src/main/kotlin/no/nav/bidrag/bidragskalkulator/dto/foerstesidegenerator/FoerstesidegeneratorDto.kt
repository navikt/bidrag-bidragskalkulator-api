package no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator


data class FoerstesideDto(
    val spraakkode: Spraakkode,
    val bruker: FoerstesideBrukerDto,
    val tema: String,
    val overskriftstittel: String,
    val arkivtittel: String,
    val foerstesidetype: Foerstesidetype,
    val netsPostboks: String,
    val navSkjemaId: String,
    val vedleggsliste: List<String>,
    val dokumentlisteFoersteside: List<String>
)

data class FoerstesideAdresseDto(
    val adresselinje1: String,
    val adresselinje2: String,
    val adresselinje3: String,
    val postnummer: String,
    val poststed: String
)

data class FoerstesideAvsenderDto(
    val avsenderId: String,
    val avsenderNavn: String
)

data class FoerstesideBrukerDto(
    val brukerId: String,
    val brukerType: String
)

data class FoerstesideArkivsakDto(
    val arkivsaksystem: String,
    val arkivsaksnummer: String
)

class GenererFoerstesideResultatDto(
    val foersteside: ByteArray,
    val loepenummer: String,
)

data class GenererFoerstesideRequestDto(
    val ident: String,
    val navSkjemaId: NavSkjemaId,
    val arkivtittel: String,
    val enhetsnummer: String,
    val spraakkode: Spraakkode = Spraakkode.NB,
)

enum class Spraakkode {
    NB, NN, EN
}

enum class Foerstesidetype {
    SKJEMA, INTERNT, ETTERSENDELSE, LOSPOST
}

enum class NavSkjemaId(val kode: String) {
    AVTALE_OM_BARNEBIDRAG_UNDER_18("NAV 55-00.60"),
    AVTALE_OM_BARNEBIDRAG_OVER_18("NAV 55-00.50"),
}
