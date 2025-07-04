package no.nav.bidrag.bidragskalkulator.dto.minSide

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.bidragskalkulator.dto.SafJournalpostDto

@Schema(description = "Inneholder liste over journalposter som er tilgjengelige for Min Side")
data class MinSideDokumenterDto(
    @Schema(description = "Liste over journalposter", required = true)
    val journalposter: List<Journalpost> = emptyList()
)

@Schema(description = "Representerer en part (mottaker eller avsender)")
data class Part(
    @Schema(description = "Navnet på parten", example = "Ola Nordmann")
    val navn: String
)

@Schema(description = "Representerer et dokument i en journalpost")
data class Dokument(
    @Schema(description = "Unik identifikator for dokumentet", example = "12345")
    val dokumentInfoId: String,

    @Schema(description = "Tittelen på dokumentet", example = "Vedtak om barnebidrag")
    val tittel: String,

    @Schema(description = "Angir om dokumentet kan åpnes", example = "true")
    val kanÅpnes: Boolean
)

@Schema(description = "Representerer en journalpost med tilhørende dokumenter")
data class Journalpost(
    @Schema(description = "Unik identifikator for journalposten", example = "BID-123456")
    val journalpostId: String,

    @Schema(description = "Tittelen på journalposten", example = "Vedtak om barnebidrag")
    val tittel: String,

    @Schema(description = "Datoen for journalposten", example = "2023-04-15")
    val dato: String,

    @Schema(description = "Informasjon om mottakeren av journalposten")
    val mottaker: Part? = null,

    @Schema(description = "Informasjon om avsenderen av journalposten")
    val avsender: Part? = null,

    @Schema(description = "Liste over dokumenter i journalposten")
    val dokumenter: List<Dokument>
)
