package no.nav.bidrag.bidragskalkulator.dto

data class SafSelvbetjeningResponsDto(
    var data: DataDto
)

data class DataDto(
    var dokumentoversiktSelvbetjening: DokumentoversiktSelvbetjeningDto
)

data class DokumentoversiktSelvbetjeningDto(
    var journalposter: List<SafJournalpostDto>
)

data class SafJournalpostDto(
    var tema: String,
    var tittel: String,
    var datoSortering: String,
    var journalpostId: String,
    var mottaker: PersonDto?,
    var avsender: PersonDto?,
    var dokumenter: List<DokumentDto>
)

data class PersonDto(
    var navn: String
)

data class DokumentDto(
    var dokumentInfoId: String,
    var tittel: String,
    var dokumentvarianter: List<DokumentVariantDto>
)

data class DokumentVariantDto(
    var variantformat: String
)