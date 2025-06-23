package no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator

data class FoerstesideDto(
    val spraakkode: String,
    val adresse: FoerstesideAdresseDto,
    val netsPostboks: String,
    val avsender: FoerstesideAvsenderDto,
    val bruker: FoerstesideBrukerDto,
    val ukjentBrukerPersoninfo: String,
    val tema: String,
    val behandlingstema: String,
    val arkivtittel: String,
    val vedleggsliste: String,
    val navSkjemaId: String,
    val overskriftstittel: String,
    val dokumentlisteFoersteside: String,
    val foerstesidetype: String,
    val enhetsnummer: String,
    val arkivsak: FoerstesideArkivsakDto
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