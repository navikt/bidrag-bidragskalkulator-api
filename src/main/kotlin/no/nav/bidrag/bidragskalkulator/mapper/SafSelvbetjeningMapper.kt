package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.SafSelvbetjeningResponsDto
import no.nav.bidrag.bidragskalkulator.dto.minSide.Dokument
import no.nav.bidrag.bidragskalkulator.dto.minSide.Journalpost
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.dto.minSide.Part
import org.springframework.stereotype.Component

@Component
class SafSelvbetjeningMapper {

    fun mapSafSelvbetjeningRespons(respons: SafSelvbetjeningResponsDto): MinSideDokumenterDto = MinSideDokumenterDto(
        journalposter = respons.data.dokumentoversiktSelvbetjening.journalposter.map { journalpost ->
            Journalpost(
                tittel = journalpost.tittel,
                journalpostId = journalpost.journalpostId,
                dato = journalpost.datoSortering,
                mottaker = journalpost.mottaker?.let { Part(it.navn) },
                avsender = journalpost.avsender?.let { Part(it.navn) },
                dokumenter = journalpost.dokumenter.map {
                    Dokument(
                        dokumentInfoId = it.dokumentInfoId,
                        tittel = it.tittel,
                        kanÅpnes = true
                    )
                },
            )
        }
    )
}